/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.publicalerts.cap;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import java.io.Reader;
import java.io.StringReader;
import java.security.KeyException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;

/**
 * Uses the java XML Digital Signature API to validate an enveloped
 * XML signature. Based on sample code at
 * <a href="http://download.oracle.com/javase/6/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html">
 * http://download.oracle.com/javase/6/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html</a>
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class XmlSignatureValidator {
  private final TrustStrategy trustStrategy;

  public XmlSignatureValidator(TrustStrategy trustStrategy) {
    this.trustStrategy = trustStrategy;
  }

  /**
   * Encapsulates the result a call to {@link #validate}.
   *
   * <p>To determine whether the XML signature was valid, use
   * {@link #isSignatureValid()}.
   */
  public static class Result {
    public enum Detail {
      SIGNATURE_INVALID,
      SIGNATURE_MISSING,
      KEY_MISSING,
      KEY_UNTRUSTED,
    }
    private final boolean isSignatureValid;
    private final Set<Detail> details;

    Result(boolean isSignatureValid, Detail... details) {
      this.isSignatureValid = isSignatureValid;
      Set<Detail> mutableDetails = new HashSet<Detail>();
      for (Detail detail : details) {
        mutableDetails.add(detail);
      }
      this.details = Collections.unmodifiableSet(mutableDetails);
    }

    /**
     * Determines whether the XML document contained a valid XML signature
     * signed by a trusted key as defined by {@link TrustStrategy#isKeyTrusted}
     * - if so, return {@code true}.
     *
     * <p>If the signature was valid but key was not trusted, return
     * {@code true} iff {@link TrustStrategy#allowUntrustedCredentials} is
     * true.
     *
     * <p>If the signature was missing or a suitable validation Key could not be
     * found, return {@code true} iff
     * {@link TrustStrategy#allowMissingSignatures} is true.
     *
     * <p>Otherwise, return {@code false}.
     */
    public boolean isSignatureValid() {
      return isSignatureValid;
    }

    /**
     * Returns zero or more details to elaborate on the reason for validation
     * failure (if {@link #isSignatureValid()} is {@code false}), or things we
     * permitted because {@link TrustStrategy} allowed leniency
     * (if {@link #isSignatureValid()} is {@code true}.
     */
    public Set<Detail> details() {
      return details;
    }
  }

  /**
   * Validates the given XML document's digital signature.
   * See {@link Result#isSignatureValid()} for details.
   *
   * @param str the string containing the xml document
   * @return a boolean as described above
   * @throws SAXParseException on error parsing the XML document
   */
  public Result validate(String str) throws SAXParseException {
    return validate(new StringReader(str));
  }

  /**
   * Validates the given XML document's digital signature.
   * See {@link Result#isSignatureValid()} for details.
   *
   * @param reader the reader containing the XML document
   * @return a boolean as described above
   * @throws SAXParseException on error parsing the XML document
   */
  public Result validate(Reader reader) throws SAXParseException {
    return validate(new InputSource(reader));
  }

  /**
   * Validates the given XML document's digital signature.
   * See {@link Result#isSignatureValid()} for details.
   *
   * @param is the input source containing the XML document
   * @return a boolean as described above
   * @throws SAXParseException on error parsing the XML document
   */
  public Result validate(InputSource is) throws SAXParseException {
    Document doc = XmlUtil.parseDocument(is);
    NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
    if (nl.getLength() == 0) {
      return new Result(trustStrategy.allowMissingSignatures(), Result.Detail.SIGNATURE_MISSING);
    }

    DOMValidateContext context = new DOMValidateContext(new KeyValueKeySelector(), nl.item(0));
    XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");

    try {
      // Is the signature valid?
      XMLSignature signature = factory.unmarshalXMLSignature(context);
      boolean valid = signature.validate(context);
      if (!valid) {
        return new Result(false, Result.Detail.SIGNATURE_INVALID);
      }

      // Do we trust the PublicKey used to validate the signature?
      SimpleKeySelectorResult keySelectorResult = SimpleKeySelectorResult.getCachedResult(context);
      return trustStrategy.isKeyTrusted(keySelectorResult.getKey())
          ? new Result(true)
          : new Result(trustStrategy.allowUntrustedCredentials(), Result.Detail.KEY_UNTRUSTED);
    } catch (MarshalException e) {
      throw new RuntimeException(e);
    } catch (XMLSignatureException e) {
      // This can mean an error happened during PublicKey acquisition OR during Signature validation

      SimpleKeySelectorResult keySelectorResult = SimpleKeySelectorResult.getCachedResult(context);
      if (keySelectorResult != null && keySelectorResult.getKey() == null) {
        // Our KeySelector got as far as caching a Result, so we know the XMLSignatureException
        // was thrown because we couldn't find a PublicKey to use.
        // Treat this the same as a "missing signature" and let it slide iff TrustStrategy says to.
        return new Result(trustStrategy.allowMissingSignatures(), Result.Detail.KEY_MISSING);
      }

      // XMLSignatureException thrown for some other reason; propagate.
      throw new RuntimeException(e);
    }
  }

  private static class KeyValueKeySelector extends KeySelector {

    @Override
    public KeySelectorResult select(KeyInfo keyInfo,
        KeySelector.Purpose purpose,
        AlgorithmMethod method,
        XMLCryptoContext context)
        throws KeySelectorException {

      if (keyInfo == null) {
        return SimpleKeySelectorResult.cacheKeyResult((PublicKey) null, context);
      }
      SignatureMethod sm = (SignatureMethod) method;

      @SuppressWarnings("unchecked") // KeyInfo#getContent javadoc guarantees XmlStructure
      List<XMLStructure> keyInfoChildNodes = keyInfo.getContent();
      for (XMLStructure keyInfoChildNode : keyInfoChildNodes) {
        if (keyInfoChildNode instanceof KeyValue) {
          PublicKey pk;
          try {
            pk = ((KeyValue) keyInfoChildNode).getPublicKey();
          } catch (KeyException ke) {
            throw new KeySelectorException(ke);
          }

          // Make sure algorithm is compatible with method
          if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
            return SimpleKeySelectorResult.cacheKeyResult(pk, context);
          }
        }
      }

      return SimpleKeySelectorResult.cacheKeyResult((PublicKey) null, context);
    }

    private static boolean algEquals(String algUri, String algName) {
      if (algName.equalsIgnoreCase("DSA") &&
          algUri.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
        return true;
      } else if (algName.equalsIgnoreCase("RSA") &&
          algUri.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
        return true;
      }
      return false;
    }
  }

  private static class SimpleKeySelectorResult implements KeySelectorResult {
    /** Property name used to stash this Result in an XmlCryptoContext */
    private static final String PROPERTY_NAME =
        SimpleKeySelectorResult.class.getName() + ".property";
    private final PublicKey key;

    /** Don't instantiate directly -- use {@link #cacheKeyResult} */
    private SimpleKeySelectorResult(PublicKey key) {
      this.key = key;
    }

    @Override
    public PublicKey getKey() {
      return key;
    }

    /**
     * Instantiates a new Result with the given key, caches it in the given context,
     * then returns the result.
     *
     * Use this method rather than the constructor so code that executes after
     * {@link XMLSignature#validate} may gain access to this Result.
     */
    static SimpleKeySelectorResult cacheKeyResult(/* Nullable */ PublicKey key,
        XMLCryptoContext context) {
      SimpleKeySelectorResult result = new SimpleKeySelectorResult(key);
      context.put(PROPERTY_NAME, result);
      return result;
    }

    /**
     * Returns a SimpleKeySelectorResult cached in the given context, or {@code null} if none
     */
    static SimpleKeySelectorResult getCachedResult(XMLCryptoContext context) {
      return (SimpleKeySelectorResult) context.get(PROPERTY_NAME);
    }
  }
}

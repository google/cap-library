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
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.util.List;

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
 * http://download.oracle.com/javase/6/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html
 *
 * TODO(shakusa) This class is not yet fully ready; it needs to accept
 * a KeyStore and validate that the KeyInfo parsed from the XML is trusted.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class XmlSignatureValidator {

  /**
   * If the given XML document contains an XML signature and it is valid,
   * return true. Else, if {@code missingSignatureIsValid} is true
   * and the XML document is missing a signature, return true.  Otherwise,
   * return false.
   *
   * @param str the string containing the xml document
   * @param missingSignatureIsValid true if a missing signature is considered
   * valid
   * @return a boolean as described above
   * @throws SAXParseException on error parsing the XML document
   */
  public boolean isSignatureValid(String str, boolean missingSignatureIsValid)
      throws SAXParseException {
    return isSignatureValid(new StringReader(str), missingSignatureIsValid);
  }

  /**
   * If the given XML document contains an XML signature and it is valid,
   * return true. Else, if {@code missingSignatureIsValid} is true
   * and the XML document is missing a signature, return true.  Otherwise,
   * return false.
   *
   * @param reader the reader containing the XML document
   * @param missingSignatureIsValid true if a missing signature is considered
   * valid
   * @return a boolean as described above
   * @throws SAXParseException on error parsing the XML document
   */
  public boolean isSignatureValid(Reader reader,
      boolean missingSignatureIsValid) throws SAXParseException {
    return isSignatureValid(new InputSource(reader), missingSignatureIsValid);
  }

  /**
   * If the given XML document contains an XML signature and it is valid,
   * return true. Else, if {@code missingSignatureIsValid} is true
   * and the XML document is missing a signature, return true.  Otherwise,
   * return false.
   *
   * @param is the input source containing the XML document
   * @param missingSignatureIsValid true if a missing signature is considered
   * valid
   * @return a boolean as described above
   * @throws SAXParseException on error parsing the XML document
   */
  public boolean isSignatureValid(InputSource is,
      boolean missingSignatureIsValid) throws SAXParseException {
    Document doc = XmlUtil.parseDocument(is);
    NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
    if (nl.getLength() == 0) {
      return missingSignatureIsValid;
    }

    DOMValidateContext valContext = new DOMValidateContext(
        new KeyValueKeySelector(), nl.item(0));
    XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");

    try {
      XMLSignature signature = factory.unmarshalXMLSignature(valContext);
      return signature.validate(valContext);
    } catch (MarshalException e) {
      throw new RuntimeException(e);
    } catch (XMLSignatureException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * TODO(shakusa) This is a very simple KeySelector implementation,
   * designed for illustration rather than real-world usage.
   * A more practical example of a KeySelector is one that searches a KeyStore
   * for trusted keys that match X509Data information (for example,
   * X509SubjectName, X509IssuerSerial, X509SKI, or X509Certificate elements)
   * contained in a KeyInfo.
   */
  private static class KeyValueKeySelector extends KeySelector {

    public KeySelectorResult select(KeyInfo keyInfo,
        KeySelector.Purpose purpose,
        AlgorithmMethod method,
        XMLCryptoContext context)
        throws KeySelectorException {

      if (keyInfo == null) {
        throw new KeySelectorException("Null KeyInfo object!");
      }
      SignatureMethod sm = (SignatureMethod) method;
      @SuppressWarnings("unchecked")
      List<XMLStructure> list = (List<XMLStructure>) keyInfo.getContent();

      for (int i = 0; i < list.size(); i++) {
        XMLStructure xmlStructure = list.get(i);
        if (xmlStructure instanceof KeyValue) {
          PublicKey pk;
          try {
            pk = ((KeyValue)xmlStructure).getPublicKey();
          } catch (KeyException ke) {
            throw new KeySelectorException(ke);
          }
          // make sure algorithm is compatible with method
          if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {

            return new SimpleKeySelectorResult(pk);
          }
        }
      }
      throw new KeySelectorException("No KeyValue element found!");
    }

    static boolean algEquals(String algUri, String algName) {
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

    private final Key key;

    public SimpleKeySelectorResult(Key key) {
      this.key = key;
    }

    public Key getKey() {
      return key;
    }
  }
}

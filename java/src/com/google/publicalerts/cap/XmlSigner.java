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

import com.google.common.collect.Lists;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

/**
 * Uses the java XML Digital Signature API to generate an enveloped XML
 * signature. Based on sample code at
 * <a href="http://download.oracle.com/javase/6/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html">
 * http://download.oracle.com/javase/6/docs/technotes/guides/security/xmldsig/XMLDigitalSignature.html</a>
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class XmlSigner {

  private final KeyPair keyPair;

  /**
   * Creates a new {@link XmlSigner} using the given public/private
   * {@link KeyPair}.
   *
   * @param keyPair the keys for the signer
   * @return the signer
   */
  public static XmlSigner newInstanceFromKeyPair(KeyPair keyPair) {
    return new XmlSigner(keyPair);
  }

  /**
   * Creates a new {@link XmlSigner} by loading a keystore at the given
   * location.
   *
   * @param file the location of the JKS keystore file
   * @param password the password to access the keystore
   * @param certificatAlias the alias of the keys
   * @return the signer
   * @throws IOException on error loading the keys
   */
  public static XmlSigner newInstanceFromKeyStore(File file, String password,
      String certificatAlias) throws IOException {
    InputStream certStream = new FileInputStream(file);

    try {
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(certStream, password.toCharArray());
      Certificate cert = ks.getCertificate(certificatAlias);
      PrivateKey privateKey = (PrivateKey) ks.getKey(
          certificatAlias, password.toCharArray());
      return new XmlSigner(new KeyPair(cert.getPublicKey(), privateKey));
    } catch (KeyStoreException kse) {
      throw wrap(kse, "Couldn't get Java Keystore");
    } catch (NoSuchAlgorithmException nsae) {
      throw wrap(nsae, "Couldn't find algorithm for private key");
    } catch (CertificateException ce) {
      throw wrap(ce, "Couldn't load certificate");
    } catch (UnrecoverableKeyException uke) {
      throw wrap(uke, "Couldn't load private key");
    } catch (EOFException ee) {
      throw wrap(ee, "Can't read key file");
    } finally {
      certStream.close();
    }
  }

  private static IOException wrap(Throwable exceptionToWrap, String message) {
    IOException ioe = new IOException(message);
    ioe.initCause(exceptionToWrap);
    return ioe;
  }

  /**
   * Most useful for testing.
   *
   * @return a new signer with a randomly generated DSA key pair
   */
  public static XmlSigner newInstanceWithRandomKeyPair() {
    KeyPairGenerator keyPairGenerator;
    try {
      keyPairGenerator = KeyPairGenerator.getInstance("DSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    keyPairGenerator.initialize(1024);
    return new XmlSigner(keyPairGenerator.generateKeyPair());
  }

  private XmlSigner(KeyPair keyPair) {
    this.keyPair = keyPair;
  }

  /**
   * Signs the given XML document, provided as a string.
   *
   * @param str the XML document to sign, as a string
   * @return the signed XML document, again as a string
   * @throws SAXParseException on error parsing the document
   */
  public String sign(String str) throws SAXParseException {
    StringWriter writer = new StringWriter();
    try {
      sign(new StringReader(str), writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  /**
   * Signs the given XML document, writing the output to the given writer.
   *
   * @param reader the XML document to sign
   * @param writer to write the signed XML output
   * @throws SAXParseException on error parsing the document
   * @throws IOException on error writing to writer
   */
  public void sign(Reader reader, Writer writer)
      throws SAXParseException, IOException {
    sign(new InputSource(reader), writer);
  }

  /**
   * Signs the given XML document, writing the output to the given writer.
   *
   * @param is the XML document to sign
   * @param writer to write the signed XML output
   * @throws SAXParseException on error parsing the document
   * @throws IOException on error writing to writer
   */
  public void sign(InputSource is, Writer writer)
      throws SAXParseException, IOException {
    Document document = XmlUtil.parseDocument(is);

    // Factory to assemble the components of the signature
    XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

    Reference reference;
    SignedInfo signedInfo;

    try {
      // Create the Reference object
      String root = "";
      reference = fac.newReference(root,
          fac.newDigestMethod(DigestMethod.SHA1, null),
          Lists.newArrayList(fac.newTransform(Transform.ENVELOPED,
              (TransformParameterSpec) null)), null, null);

      // Now create the SignedInfo object
      signedInfo = fac.newSignedInfo(fac.newCanonicalizationMethod(
          CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
          (C14NMethodParameterSpec) null),
          fac.newSignatureMethod(SignatureMethod.DSA_SHA1, null),
          Lists.newArrayList(reference));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }

    // Now the KeyInfo and KeyValue objects
    KeyInfoFactory keyInfoFactory = fac.getKeyInfoFactory();

    KeyValue keyValue;
    try {
      keyValue = keyInfoFactory.newKeyValue(keyPair.getPublic());
    } catch (KeyException e) {
      throw new RuntimeException(e);
    }
    KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Lists.newArrayList(keyValue));

    // Now the XML Signature object
    XMLSignature signature = fac.newXMLSignature(signedInfo, keyInfo);

    // Finally, generate the signature
    DOMSignContext domSignContext = new DOMSignContext(
        keyPair.getPrivate(), document.getDocumentElement());
    try {
      signature.sign(domSignContext);
    } catch (MarshalException e) {
      throw new RuntimeException(e);
    } catch (XMLSignatureException e) {
      throw new RuntimeException(e);
    }

    XmlUtil.writeDocument(document, writer, null);
  }

  /* VisibleForTesting */ PublicKey getPublicKey() {
    return keyPair.getPublic();
  }
}

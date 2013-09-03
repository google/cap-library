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

import com.google.publicalerts.cap.XmlSignatureValidator.Result.Detail;
import com.google.publicalerts.cap.testing.MockTrustStrategy;
import com.google.publicalerts.cap.testing.TestResources;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Tests for {@link XmlSigner} and {@link XmlSignatureValidator}.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class XmlSignAndValidateTest extends TestCase {
  private XmlSigner signer;
  private MockTrustStrategy trustStrategy;
  private XmlSignatureValidator validator;

  public XmlSignAndValidateTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    signer = XmlSigner.newInstanceWithRandomKeyPair();
    trustStrategy = new MockTrustStrategy()
        .setAllowMissingSignatures(false)
        .setAllowUntrustedCredentials(false)
        .addTrustedKey(signer.getPublicKey());
    validator = new XmlSignatureValidator(trustStrategy);
  }

  public void testSign() throws Exception {
    String headline = "UNIQUE headline";
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<alert xmlns=\"urn:oasis:names:tc:emergency:cap:1.2\">"
        + "<info><headline>" + headline + "</headline></info></alert>";

    trustStrategy.setAllowMissingSignatures(true);
    XmlSignatureValidator.Result result = validator.validate(xml);
    assertTrue("Missing signatures must be tolerated when TrustStrategy allows them",
        result.isSignatureValid());
    assertDetail(result, Detail.SIGNATURE_MISSING);

    trustStrategy.setAllowMissingSignatures(false);
    result = validator.validate(xml);
    assertFalse("Missing signatures must not be tolerated when TrustStrategy disallows them",
        result.isSignatureValid());
    assertDetail(result, Detail.SIGNATURE_MISSING);

    String signedXml = signer.sign(xml);
    result = validator.validate(signedXml);
    assertTrue("XML Signed with a trusted key should pass validation",
        result.isSignatureValid());
    assertTrue(result.details().isEmpty());

    String tamperedSignedXml = signedXml.replace(headline, "attacker headline");
    result = validator.validate(tamperedSignedXml);
    assertFalse("XML tampered with after signing -- should fail validation",
        result.isSignatureValid());
    assertDetail(result, Detail.SIGNATURE_INVALID);

    trustStrategy.clearTrustedKeys();
    result = validator.validate(signedXml);
    assertFalse("XML signed with an untrusted key should fail validation",
        result.isSignatureValid());
    assertDetail(result, Detail.KEY_UNTRUSTED);

    trustStrategy.setAllowUntrustedCredentials(true);
    result = validator.validate(signedXml);
    assertTrue("XML signed with an untrusted key should be tolerated when TrustStrategy allows it",
        result.isSignatureValid());
    assertDetail(result, Detail.KEY_UNTRUSTED);

    String signedXmlWithoutKey = signedXml.replaceAll("(?s)<KeyInfo>.*</KeyInfo>", "");
    result = validator.validate(signedXmlWithoutKey);
    assertFalse("Signed XML with missing key should fail validation",
        result.isSignatureValid());
    assertDetail(result, Detail.KEY_MISSING);

    trustStrategy.setAllowMissingSignatures(true);
    result = validator.validate(signedXmlWithoutKey);
    assertTrue("Signed XML with missing key should be treated as missing signature when " +
        "TrustStrategy allows it",
        result.isSignatureValid());
    assertDetail(result, Detail.KEY_MISSING);
  }

  private void assertDetail(XmlSignatureValidator.Result result, Detail detail) {
    assertEquals(1, result.details().size());
    assertEquals(detail, result.details().iterator().next());
  }

  public void testValidateExternallySignedAlert() throws Exception {
    trustStrategy.setAllowUntrustedCredentials(true);
    String signedXml = TestResources.load("earthquake_signed.cap");
    assertTrue("Alert signed by 3rd party with in-band key",
        validator.validate(signedXml).isSignatureValid());
  }

  public void testValidateExternallySignedX509AlertWithMultipleSignatures() throws Exception {
    CertificateFactory factory = CertificateFactory.getInstance("X.509");

    // Neither key is trusted -- signature doesn't validate
    byte[] xmlBytes = TestResources.loadBytes("canada_signed.cap");
    ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes);
    assertFalse("Alert signed by 3rd party with in-band X509 cert",
        validator.validate(new InputSource(bais)).isSignatureValid());

    // Only trust the key associated with first signature
    X509Certificate naadsCert = (X509Certificate) factory.generateCertificate(
        new ByteArrayInputStream(TestResources.loadBytes("naads_x509_cert.pem")));
    trustStrategy.addTrustedKey(naadsCert.getPublicKey());

    bais = new ByteArrayInputStream(xmlBytes);
    assertTrue("Alert signed by 3rd party with in-band X509 cert",
        validator.validate(new InputSource(bais)).isSignatureValid());

    // Only trust the key associated with second signature
    trustStrategy.clearTrustedKeys();
    X509Certificate envCanadaCert = (X509Certificate) factory.generateCertificate(
        new ByteArrayInputStream(TestResources.loadBytes("environment_canada_x509_cert.pem")));
    trustStrategy.addTrustedKey(envCanadaCert.getPublicKey());

    bais = new ByteArrayInputStream(xmlBytes);
    assertTrue("Alert signed by 3rd party with in-band X509 cert",
        validator.validate(new InputSource(bais)).isSignatureValid());
  }
}

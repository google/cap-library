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

import com.google.publicalerts.cap.testing.CapTestUtil;
import com.google.publicalerts.cap.testing.MockTrustStrategy;

import junit.framework.TestCase;

/**
 * End to end test for the CAP library.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class EndToEndTest extends TestCase {

  public EndToEndTest(String name) {
    super(name);
  }

  public void testEndToEnd() throws Exception {
    // Generate a CAP document
    Alert alert = CapTestUtil.getValidAlertBuilder().build();

    // Write it out to XML
    CapXmlBuilder builder = new CapXmlBuilder();
    String xml = builder.toXml(alert);

    // Sign it
    XmlSigner signer = XmlSigner.newInstanceWithRandomKeyPair();
    String signedXml = signer.sign(xml);

    // Validate the signature
    XmlSignatureValidator signatureValidator = new XmlSignatureValidator(
        new MockTrustStrategy().setAllowMissingSignatures(false));
    assertTrue(signatureValidator.validate(signedXml).isSignatureValid());

    // Parse it, with validation
    CapXmlParser parser = new CapXmlParser(true);
    Alert parsedAlert = parser.parseFrom(signedXml);

    // Assert lossless
    assertEquals(alert, parsedAlert);
  }
}

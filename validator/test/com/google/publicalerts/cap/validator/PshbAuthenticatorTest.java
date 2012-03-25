/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.publicalerts.cap.validator;

import static com.google.publicalerts.cap.validator.PshbAuthenticator.AuthResult;

import junit.framework.TestCase;

/**
 * Tests for {@link PshbAuthenticator}.
 *
 * @author Steven Hakusa (shakusa@google.com)
 */
public class PshbAuthenticatorTest extends TestCase {

  private static final String SECRET = "secret";
  private static final String DATA = "Lorum ipsum dolor";
  // python -c "import hmac; import hashlib;				\
  // print hmac.new('secret', 'Lorum ipsum dolor', hashlib.sha1).hexdigest()"
  private static final String SIGNATURE =
      "sha1=f9b38b49ae9a6615f0a28f59945f57ac28f34e29";

  private PshbAuthenticator auth;

  public PshbAuthenticatorTest(String testCase) {
    super(testCase);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    auth = new PshbAuthenticator();
  }

  public void testMissingData() {
    assertEquals(AuthResult.MISSING_DATA,
        auth.authenticate(null, SIGNATURE, SECRET));
  }

  public void testMissingHeader() {
    assertEquals(AuthResult.MISSING_HEADER,
        auth.authenticate(DATA, null, SECRET));
  }

  public void testHeaderNotSha1() {
    assertEquals(AuthResult.HEADER_NOT_SHA1,
        auth.authenticate(DATA, "foo", SECRET));
  }

  public void testSignatureNot40Bytes() {
    assertEquals(AuthResult.SIGNATURE_NOT_40_BYTES,
	auth.authenticate(DATA, SIGNATURE + "a", SECRET));
  }

 public void testNoMatch() {
   char c = SIGNATURE.charAt(SIGNATURE.length() - 1);
   String badSignature =
       SIGNATURE.substring(0, SIGNATURE.length() - 1) + (char) (c + 1);
   assertEquals(AuthResult.NO_MATCH,
       auth.authenticate(DATA, badSignature, SECRET));
 }

  public void testOk() {
    assertEquals(AuthResult.OK, auth.authenticate(DATA, SIGNATURE, SECRET));
  }
}

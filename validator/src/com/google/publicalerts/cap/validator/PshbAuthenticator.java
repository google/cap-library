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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Authenticates a post body and digital signature according to
 * http://pubsubhubbub.googlecode.com/svn/trunk/pubsubhubbub-core-0.3.html#authednotify
 *
 * @author Steven Hakusa (shakusa@google.com)
 */
public class PshbAuthenticator {
  
  public static final String SIGNATURE_HEADER = "X-Hub-Signature";

  static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
  static final String ISO_8859_1 = "ISO-8859-1";
  static final String SIGNATURE_PREFIX = "sha1=";

  /**
   * Result of authenticating.
   */
  public enum AuthResult {
      MISSING_DATA,
      MISSING_HEADER,
      HEADER_NOT_SHA1,
      SIGNATURE_NOT_40_BYTES,
      NO_MATCH,
      OK
  }

  /**
   * Authenticates the given request.
   * 
   * @param data the post body
   * @param header the signature header
   * @param secret shared secret between hub and this server
   * @return the result of authenticating
   */
  public AuthResult authenticate(String data, String header, String secret) {
    if (data == null) {
      return AuthResult.MISSING_DATA;
    }

    if (header == null) {
      return AuthResult.MISSING_HEADER;
    }

    if (!header.startsWith(SIGNATURE_PREFIX)) {
      return AuthResult.HEADER_NOT_SHA1;
    }

    String signature = header.substring(SIGNATURE_PREFIX.length());
    if (signature.length() != 40) {
      return AuthResult.SIGNATURE_NOT_40_BYTES;
    }

    return signature.equals(sha1Hmac(data, secret)) ? AuthResult.OK : AuthResult.NO_MATCH;
    }

  /**
   * Returns the SHA-1 HMAC of the given data and secret
   * @param data the data to hash
   * @param secret the shared secret
   * @return the SHA-1 HMAC
   */
  public static String sha1Hmac(String data, String secret) {
    byte[] secretBytes = getBytes(secret);

    try {
      SecretKeySpec signingKey = new SecretKeySpec(secretBytes, HMAC_SHA1_ALGORITHM);
      Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
      mac.init(signingKey);
      byte[] rawHmac = mac.doFinal(getBytes(data));
      return hexDigest(rawHmac);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  static byte[] getBytes(String s) {
    try {
      return s.getBytes(ISO_8859_1);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  static String hexDigest(byte[] rawHmac) {
    StringBuilder hexDigest = new StringBuilder();
    for (byte b : rawHmac) {
      String hex = Integer.toHexString(b);
      if (hex.length() == 1) {
	hex = "0" + hex;
      }
      hex = hex.substring(hex.length() - 2);
      hexDigest.append(hex);
    }
    return hexDigest.toString();
  }
}


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

import java.security.PublicKey;

/**
 * Determines how stringently to validate digital signatures, and which
 * PublicKeys are valid for such use.
 *
 * @author pcoakley@google.com (Phil Coakley)
 */
public interface TrustStrategy {

  /**
   * Return {@code false} if valid digital signatures are always required.
   * Return {@code true} if a missing digital signature or Public Key should
   * be tolerated.  When both signature and Public Key are available, they will
   * always be validated irrespective of this configuration setting.
   */
  boolean allowMissingSignatures();

  /**
   * Return {@code false} if we must always have explicit trust in the
   * credentials used to validate a digital signature (as defined by
   * {@link #isKeyTrusted}).
   * Return {@code true} if untrusted credentials should be tolerated.
   */
  boolean allowUntrustedCredentials();

  /**
   * Return {@code true} if the given PublicKey should be trusted in the
   * context of validating the digital signatures of CAP alerts.
   * Normally this would mean ensuring the PublicKey can be associated with an
   * explicitly trusted 3rd party, e.g. via the use of X.509 certificates.
   * Return {@code false} if the given PublicKey is not trusted.
   *
   * Note this method may be called irrespective of the return value of
   * {@link #allowUntrustedCredentials}
   */
  boolean isKeyTrusted(PublicKey key);
}

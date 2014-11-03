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

package com.google.publicalerts.cap.testing;

import com.google.common.collect.Sets;
import com.google.publicalerts.cap.TrustStrategy;

import java.security.PublicKey;
import java.util.Set;

/**
 * A testing implementation of {@link TrustStrategy} that defaults to skipping
 * all validation.
 *
 * @author pcoakley@google.com (Phil Coakley)
 */
public class MockTrustStrategy implements TrustStrategy {
  private boolean allowMissingSignatures = true;
  private boolean allowUntrustedCredentials = true;
  private final Set<PublicKey> trustedKeys = Sets.newHashSet();

  @Override
  public boolean allowMissingSignatures() {
    return allowMissingSignatures;
  }

  @Override
  public boolean allowUntrustedCredentials() {
    return allowUntrustedCredentials;
  }

  @Override
  public boolean isKeyTrusted(PublicKey key) {
    return trustedKeys.contains(key);
  }

  public MockTrustStrategy setAllowMissingSignatures(boolean allowMissingSignatures) {
    this.allowMissingSignatures = allowMissingSignatures;
    return this;
  }

  public MockTrustStrategy setAllowUntrustedCredentials(boolean allowUntrustedCredentials) {
    this.allowUntrustedCredentials = allowUntrustedCredentials;
    return this;
  }

  public MockTrustStrategy addTrustedKey(PublicKey trustedKey) {
    this.trustedKeys.add(trustedKey);
    return this;
  }

  public MockTrustStrategy removeTrustedKey(PublicKey untrustedKey) {
    this.trustedKeys.remove(untrustedKey);
    return this;
  }

  public MockTrustStrategy clearTrustedKeys() {
    this.trustedKeys.clear();
    return this;
  }
}

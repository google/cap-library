/*
 * Copyright (C) 2014 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;

import static com.google.publicalerts.cap.Reason.Level.ERROR;
import static com.google.publicalerts.cap.Reason.Level.RECOMMENDATION;
import static com.google.publicalerts.cap.Reason.Level.WARNING;

import com.google.publicalerts.cap.CapException.ReasonType;

import junit.framework.TestCase;

/**
 * Tests for {@link Reason}.
 *
* @author sschiavoni@google.com (Stefano Schiavoni)
 */
public class ReasonTest extends TestCase {

  public ReasonTest(String s) {
    super(s);
  }

  public void testGetHigherLevels() {
    assertThat(RECOMMENDATION.getHigherLevels()).containsExactly(WARNING, ERROR).inOrder();
    assertThat(WARNING.getHigherLevels()).containsExactly(ERROR).inOrder();
    assertThat(ERROR.getHigherLevels()).isEmpty();
  }
  
  public void testPrefixWithXpath() {
    Reason reason =
        new Reason("/alert[1]", ReasonType.INVALID_AREA, "param1", "param2");
    Reason expectedReason =
        new Reason("/feed[1]/entry[1]/alert[1]", ReasonType.INVALID_AREA, "param1", "param2");
    
    assertThat(reason.prefixWithXpath("/feed[1]/entry[1]")).isEqualTo(expectedReason);
  }
}

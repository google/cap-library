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

import com.google.common.collect.ImmutableList;
import com.google.publicalerts.cap.CapException.ReasonType;
import com.google.publicalerts.cap.Reason.Level;

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
    assertEquals(
        ImmutableList.of(Level.WARNING,  Level.ERROR),
        Level.RECOMMENDATION.getHigherLevels());
    assertEquals(
        ImmutableList.of(Level.ERROR),
        Level.WARNING.getHigherLevels());
    assertEquals(
        ImmutableList.of(),
        Level.ERROR.getHigherLevels());
  }
  
  public void testPrefixWithXpath() {
    Reason reason =
        new Reason("/alert", ReasonType.INVALID_AREA, "param1", "param2");
    Reason expectedReason = new Reason("/feed/entry[1]/alert",
        ReasonType.INVALID_AREA, "param1", "param2");
    
    assertEquals(expectedReason, reason.prefixWithXpath("/feed/entry[1]"));
  }
}

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

package com.google.publicalerts.cap.profile;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.AlertOrBuilder;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapXmlParser;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.Reasons;
import com.google.publicalerts.cap.testing.CapTestUtil;
import com.google.publicalerts.cap.testing.TestResources;

import junit.framework.TestCase;

/**
 * Test case for {@link CapProfile} tests.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public abstract class CapProfileTestCase extends TestCase {
  protected AbstractCapProfile profile;

  public CapProfileTestCase(String name) {
    super(name);
  }

  protected void runTestParseFrom(String filename) throws Exception {
    // No CapException when parsing through plain CapXmlParser
    String cap = TestResources.load(filename);
    new CapXmlParser(true).parseFrom(cap);

    try {
      // ...but there is an exception when parsing with profile
      loadAlert(filename);
      fail("Expected CapException");
    } catch (CapException e) {
      // expected
    }
  }

  protected Alert loadAlert(String filename) throws Exception {
    String cap = TestResources.load(filename);
    return profile.parseFrom(cap);
  }

  protected void assertNoReasons(AlertOrBuilder alert, Reason.Level level) {
    assertReasons(alert, level);
  }

  protected void assertReasons(AlertOrBuilder alert, Reason.Level level, Reason... expected) {
    CapTestUtil.assertReasons(
        Reasons.newBuilder().addAll(profile.validate(alert).getWithLevel(level)).build(), expected);
  }
}

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

import junit.framework.TestCase;

/**
 * Tests for {@link CapUtil}.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class CapUtilTest extends TestCase {

  public CapUtilTest(String s) {
    super(s);
  }

  public void testCamelCase() {
    assertEquals("", CapUtil.camelCase(""));
    assertEquals("aBC", CapUtil.camelCase("A_b_c"));
  }

  public void testToUnderscoreCase() {
    assertEquals("foo", CapUtil.underscoreCase("foo"));
    assertEquals("foo_bar", CapUtil.underscoreCase("foo_bar"));
    assertEquals("foo_bar_baz", CapUtil.underscoreCase("fooBarBaz"));
    assertEquals("Foo__bar", CapUtil.underscoreCase("Foo_Bar"));
  }
  
  public void testIsDateParseable() {
    assertTrue(CapUtil.isValidDate("2003-04-02T14:39:01-05:00"));
    assertTrue(CapUtil.isValidDate("2008-02-29T24:59:59-00:00"));
    assertTrue(CapUtil.isValidDate("2003-12-31T07:00:00+00:00"));

    assertFalse(CapUtil.isValidDate("2003/04/02T14:39:01-05:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:61:01-05:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:39:61-05:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02 14:39:01-05:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:39:01-24:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:39:01-05:61"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:39:01-24:000"));
  }
}

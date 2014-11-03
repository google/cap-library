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

import junit.framework.TestCase;

import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

/**
 * Tests fpr {@link CapDateUtil}
 */
public class CapDateUtilTest extends TestCase {

  public void testIsDateParseable() {
    assertTrue(CapDateUtil.isValidDate("2003-04-02T14:39:01-05:00"));
    assertTrue(CapDateUtil.isValidDate("2008-02-29T24:00:00-00:00"));
    assertTrue(CapDateUtil.isValidDate("2003-12-31T07:00:00+00:00"));

    assertFalse(CapDateUtil.isValidDate("2003/04/02T14:39:01-05:00"));
    assertFalse(CapDateUtil.isValidDate("2003-04-02T14:61:01-05:00"));
    assertFalse(CapDateUtil.isValidDate("2003-04-02T14:39:61-05:00"));
    assertFalse(CapDateUtil.isValidDate("2003-04-02 14:39:01-05:00"));
    assertFalse(CapDateUtil.isValidDate("2003-04-02T14:39:01-24:00"));
    assertFalse(CapDateUtil.isValidDate("2003-04-02T14:39:01-05:61"));
    assertFalse(CapDateUtil.isValidDate("2003-04-02T14:39:01-24:000"));
  }

  public void testGetTimezoneOffset() {
    assertEquals(0, CapDateUtil.getTimezoneOffset("invalid"));

    assertEquals(-300, CapDateUtil.getTimezoneOffset("2003-04-02T14:39:01-05:00"));
    assertEquals(300, CapDateUtil.getTimezoneOffset("2003-04-02T14:39:01+05:00"));
    assertEquals(89, CapDateUtil.getTimezoneOffset("2003-04-02T14:39:01+01:29"));
    assertEquals(-89, CapDateUtil.getTimezoneOffset("2003-04-02T14:39:01-01:29"));
    assertEquals(0, CapDateUtil.getTimezoneOffset("2003-04-02T14:39:01+00:00"));
    assertEquals(0, CapDateUtil.getTimezoneOffset("2003-04-02T14:39:01-00:00"));
  }

  public void testFormatCapDate() {
    GregorianCalendar cal = new GregorianCalendar(
        SimpleTimeZone.getTimeZone("UTC"));
    cal.set(2012, 9, 28, 12, 0, 1);  // remember month is zero-based
    assertEquals("2012-10-28T12:00:01+00:00", CapDateUtil.formatCapDate(cal));

    cal = new GregorianCalendar(
        SimpleTimeZone.getTimeZone("America/New_York"));
    cal.set(2012, 9, 28, 17, 0, 1); // remember month is zero-based
    assertEquals("2012-10-28T17:00:01-04:00", CapDateUtil.formatCapDate(cal));

    cal = new GregorianCalendar(
        SimpleTimeZone.getTimeZone("Australia/North"));
    cal.set(2012, 9, 28, 17, 0, 1);  // remember month is zero-based
    assertEquals("2012-10-28T17:00:01+09:30", CapDateUtil.formatCapDate(cal));
  }
}

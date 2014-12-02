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

import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

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
    assertEquals("ABC", CapUtil.camelCase("A_b_c"));
    assertEquals("AbCdEf", CapUtil.camelCase("AB_CD__EF"));
    assertEquals("AllClear", CapUtil.camelCase("ALL_CLEAR"));
    assertEquals("Cbrne", CapUtil.camelCase("CBRNE"));
  }

  public void testJavaCase() {
    assertEquals("", CapUtil.javaCase(""));
    assertEquals("aBC", CapUtil.javaCase("A_b_c"));
    assertEquals("abCdEf", CapUtil.javaCase("AB_CD__EF"));
  }

  public void testToUnderscoreCase() {
    assertEquals("", CapUtil.underscoreCase(""));
    assertEquals("foo", CapUtil.underscoreCase("foo"));
    assertEquals("foo_bar", CapUtil.underscoreCase("foo_bar"));
    assertEquals("foo_Bar_Baz", CapUtil.underscoreCase("fooBarBaz"));
    assertEquals("Foo_Bar", CapUtil.underscoreCase("Foo_Bar"));
    assertEquals("FOOBAR", CapUtil.underscoreCase("FOOBAR"));
    assertEquals("FOO_BAR", CapUtil.underscoreCase("FOO_BAR"));
  }

  public void testFormatCapReference() {
    GregorianCalendar cal = new GregorianCalendar(
        SimpleTimeZone.getTimeZone("UTC"));
    cal.set(2012, 9, 28, 12, 0, 1); // remember month is zero-based

    assertEquals(
        "sender,past:event:id,2012-10-28T12:00:01+00:00",
        CapUtil.formatCapReference("sender", "past:event:id", cal));
  }

  public void testStripXmlPreamble() {
    assertEquals(
        "foobar",
        CapUtil.stripXmlPreamble("<?xml encoding='UTF-8'>foobar"));
  }
  
  public void testIsBase64() {
    assertTrue(CapUtil.isBase64(""));
    assertTrue(CapUtil.isBase64("cGxlYXN1cmUu"));
    assertTrue(CapUtil.isBase64("ZWFzdXJlLg=="));
    assertTrue(CapUtil.isBase64("ZW\nFzdXJlLg=="));
    
    assertFalse(CapUtil.isBase64("a"));
    assertFalse(CapUtil.isBase64("cGxlYXN1%mUu"));
    assertFalse(CapUtil.isBase64("ZWFzdXJlLg="));
    assertFalse(CapUtil.isBase64("ZW\nFzdXJlLg =="));
  }
  
  public void testParseReferenceIdentifier() {
    assertEquals("2.49.0.1.124.76bd23f1.2014",
        CapUtil.parseReferenceIdentifier(
            "2.49.0.1.124.76bd23f1.2014/2014-05-18T12:26:00-00:00", 10));
    assertEquals("TRI13970876.1",
        CapUtil.parseReferenceIdentifier(
            "trinet@caltech.edu,TRI13970876.1,2003-06-11T20:30:00-07:00", 12));
    assertNull(CapUtil.parseReferenceIdentifier("foobar", 10));
    assertNull(CapUtil.parseReferenceIdentifier("foobar", 12));
  }
  
  public void testParseReferenceSent() {
    GregorianCalendar expectedCalendar = new GregorianCalendar(
        SimpleTimeZone.getTimeZone("UTC"));
    expectedCalendar.set(2012, 9, 28, 12, 0, 1); // remember month is zero-based
    GregorianCalendar actualCalendar = new GregorianCalendar();
    actualCalendar.setTime(CapUtil.parseReferenceSent(
        "hsas@dhs.gov,123,2012-10-28T12:00:01+00:00"));
    assertEquals(CapDateUtil.formatCapDate(expectedCalendar),
        CapDateUtil.formatCapDate(expectedCalendar));
    
    assertNotNull(
        CapUtil.parseReferenceSent("hsas@dhs.gov,123,2012-10-28T12:00:01+00:00"));
    assertNull(CapUtil.parseReferenceSent("foobar"));
    assertNull(
        CapUtil.parseReferenceSent("hsas@dhs.gov,123,2003-99-02T14:39:01-05:00"));
  }
  
  public void testParseUri() {
    assertNotNull(CapUtil.parseUri("http://google.com"));
    assertNotNull(CapUtil.parseUri("mailto:nobody@google.com"));
    assertNull(CapUtil.parseUri("some other string"));
  }
  
  public void testContainsHtmlEntities() {
    assertFalse(CapUtil.containsHtmlEntities("a string"));
    assertTrue(CapUtil.containsHtmlEntities("a&nbsp;string"));
    assertTrue(CapUtil.containsHtmlEntities("a&#160;string"));
    
    // Invalid HTML entity
    assertFalse(CapUtil.containsHtmlEntities("a&zzzzzzz;string"));
  }
  
  public void testContainsHtmlTags() {
    assertFalse(CapUtil.containsHtmlTags("a string"));
    
    assertTrue(CapUtil.containsHtmlTags("a <strong>string</strong>"));
    assertTrue(CapUtil.containsHtmlTags("a <strong>string"));
    assertTrue(CapUtil.containsHtmlTags("a string</strong>"));
    assertTrue(CapUtil.containsHtmlTags("a <a href=\"foobar\">string</a>"));
  }
}

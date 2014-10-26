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

package com.google.publicalerts.cap.validator;

import junit.framework.TestCase;

import com.google.publicalerts.cap.testing.TestResources;
import com.google.publicalerts.cap.validator.LineOffsetParser.LineOffsets;

/**
 * Tests for {@link LineOffsetParser}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class LineOffsetParserTest extends TestCase {

  private static final LineOffsetParser parser = new LineOffsetParser();
  
  public LineOffsetParserTest(String name) {
    super(name);
  }

  public void testLineOffsets_cap() throws Exception {
    String cap = TestResources.load("weather.cap");
    LineOffsets offsets = parser.parse(cap);

    // Invalid queries
    assertEquals(0, offsets.getEntryLineNumber(-1));
    assertEquals(0, offsets.getXPathLineNumber("invalid"));
    assertEquals(0, offsets.getEntryLineNumber(0));
    
    // getXPathLineNumber
    assertEquals(2, offsets.getXPathLineNumber("/alert"));
    assertEquals(8, offsets.getXPathLineNumber("/alert/msgType"));
    assertEquals(22, offsets.getXPathLineNumber("/alert/info[0]/expires"));
    assertEquals(48, offsets.getXPathLineNumber(
        "/alert/info[0]/parameter[1]/value[0]"));
  }
  
  public void testLineOffsets_atomFeed() throws Exception {
    String atom = TestResources.load("weather.atom");
    LineOffsets offsets = parser.parse(atom);

    // Invalid queries
    assertEquals(0, offsets.getEntryLineNumber(-1));
    assertEquals(0, offsets.getXPathLineNumber("invalid"));

    // getEntryLineNumber
    assertEquals(17, offsets.getEntryLineNumber(0));
    
    // getXPathLineNumber
    assertEquals(17, offsets.getXPathLineNumber(
        "/feed/entry[0]"));
    assertEquals(28, offsets.getXPathLineNumber(
        "/feed/entry[0]/content/alert"));
    assertEquals(37, offsets.getXPathLineNumber(
        "/feed/entry[0]/content/alert/info[0]"));
    assertEquals(91, offsets.getXPathLineNumber(
        "/feed/entry[0]/content/alert/info[0]/area[0]/geocode[1]"));
    
    // getLinkLineNumber
    assertEquals(16, offsets.getLinkLineNumber(
        "http://www.weather.gov/alerts-beta/"));
    assertEquals(25, offsets.getLinkLineNumber(
        "http://www.weather.gov/alerts-beta/wwacapget.php?" +
        "x=DC20100829202900LWXAirQualityAlertLWX20100831040000DC"));
  }
  
  public void testLineOffsets_atomFeedFullyQualifiedTags() throws Exception {
    String atom = TestResources.load("amber.atom");
    LineOffsets offsets = parser.parse(atom);

    // Invalid queries
    assertEquals(0, offsets.getEntryLineNumber(-1));
    assertEquals(0, offsets.getXPathLineNumber("invalid"));
    
    // getEntryLineNumber
    assertEquals(5, offsets.getEntryLineNumber(0));
    
    // getXPathLineNumber
    assertEquals(5, offsets.getXPathLineNumber("/feed/entry[0]"));
    assertEquals(16, offsets.getXPathLineNumber(
        "/feed/entry[0]/content/alert"));
    assertEquals(24, offsets.getXPathLineNumber(
        "/feed/entry[0]/content/alert/info[0]"));
    assertEquals(43, offsets.getXPathLineNumber(
        "/feed/entry[0]/content/alert/info[0]/area[0]/geocode[0]"));
  }
  
  public void testLineOffsets_rssFeed() throws Exception {
    String rss = TestResources.load("ny_index.rss");
    LineOffsets offsets = parser.parse(rss);

    // Invalid queries
    assertEquals(0, offsets.getEntryLineNumber(-1));
    assertEquals(0, offsets.getXPathLineNumber("invalid"));

    // getEntryLineNumber
    assertEquals(11, offsets.getEntryLineNumber(0));
    assertEquals(20, offsets.getEntryLineNumber(1));
    
    // getXPathLineNumber
    assertEquals(3, offsets.getXPathLineNumber("/rss/channel"));
    assertEquals(6, offsets.getXPathLineNumber("/rss/channel/link[0]"));
    assertEquals(21, offsets.getXPathLineNumber("/rss/channel/item[1]/title"));
    
    // getLinkLineNumber
    assertEquals(6, offsets.getLinkLineNumber("http://www.nyalert.gov/"));
    assertEquals(14, offsets.getLinkLineNumber("http://www.nyalert.gov/Public/"
        + "News/GetCapAlert.aspx?capID=NYAlert-ALLHAZALERT-3167113"));
    assertEquals(23, offsets.getLinkLineNumber("http://www.nyalert.gov/Public/"
        + "News/GetCapAlert.aspx?capID=NYAlert-ALLHAZALERT-3162202"));
  }
  
  public void testLineOffsets_invalidXml() throws Exception {
    String invalidXML = "<alert>\n<info>\n</alert>";

    try {
      parser.parse(invalidXML);
      fail();
    } catch (IllegalArgumentException expected) { }
  }
}

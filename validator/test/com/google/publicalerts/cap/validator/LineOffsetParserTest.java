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

import static com.google.common.truth.Truth.assertThat;

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
    assertThat(offsets.getXPathLineNumber("invalid")).isEqualTo(0);
    
    // getXPathLineNumber
    assertThat(offsets.getXPathLineNumber("/alert[1]")).isEqualTo(2);
    assertThat(offsets.getXPathLineNumber("/alert[1]/msgType[1]")).isEqualTo(8);
    assertThat(offsets.getXPathLineNumber("/alert[1]/info[1]/expires[1]")).isEqualTo(22);
    assertThat(offsets.getXPathLineNumber("/alert[1]/info[1]/parameter[2]/value[1]"))
        .isEqualTo(48);
  }
  
  public void testLineOffsets_atomFeed() throws Exception {
    String atom = TestResources.load("weather.atom");
    LineOffsets offsets = parser.parse(atom);

    // Invalid queries
    assertThat(offsets.getXPathLineNumber("invalid")).isEqualTo(0);

    // getXPathLineNumber
    assertThat(offsets.getXPathLineNumber("/feed[1]/entry[1]")).isEqualTo(17);
    assertThat(offsets.getXPathLineNumber("/feed[1]/entry[1]/content[1]/alert[1]")).isEqualTo(28);
    assertThat(offsets.getXPathLineNumber("/feed[1]/entry[1]/content[1]/alert[1]/info[1]"))
        .isEqualTo(37);
    assertThat(offsets.getXPathLineNumber(
        "/feed[1]/entry[1]/content[1]/alert[1]/info[1]/area[1]/geocode[2]")).isEqualTo(91);
    
    // getLinkLineNumber
    assertThat(offsets.getLinkLineNumber("http://www.weather.gov/alerts-beta/")).isEqualTo(16);
    assertThat(offsets.getLinkLineNumber("http://www.weather.gov/alerts-beta/wwacapget.php"
        + "?x=DC20120829202900LWXAirQualityAlertLWX20120831040000DC")).isEqualTo(25);
  }
  
  public void testLineOffsets_atomFeedFullyQualifiedTags() throws Exception {
    String atom = TestResources.load("amber.atom");
    LineOffsets offsets = parser.parse(atom);

    // Invalid queries
    assertThat(offsets.getXPathLineNumber("invalid")).isEqualTo(0);
    
    // getXPathLineNumber
    assertThat(offsets.getXPathLineNumber("/feed[1]/entry[1]")).isEqualTo(5);
    assertThat(offsets.getXPathLineNumber("/feed[1]/entry[1]/content[1]/alert[1]")).isEqualTo(16);
    assertThat(offsets.getXPathLineNumber("/feed[1]/entry[1]/content[1]/alert[1]/info[1]"))
        .isEqualTo(24);
    assertThat(offsets.getXPathLineNumber(
        "/feed[1]/entry[1]/content[1]/alert[1]/info[1]/area[1]/geocode[1]")).isEqualTo(43);
  }
  
  public void testLineOffsets_rssFeed() throws Exception {
    String rss = TestResources.load("ny_index.rss");
    LineOffsets offsets = parser.parse(rss);

    // Invalid queries
    assertThat(offsets.getXPathLineNumber("invalid")).isEqualTo(0);
    
    // getXPathLineNumber
    assertThat(offsets.getXPathLineNumber("/rss[1]/channel[1]")).isEqualTo(3);
    assertThat(offsets.getXPathLineNumber("/rss[1]/channel[1]/link[1]")).isEqualTo(6);
    assertThat(offsets.getXPathLineNumber("/rss[1]/channel[1]/item[2]/title[1]")).isEqualTo(21);
    
    // getLinkLineNumber
    assertThat(offsets.getLinkLineNumber("http://www.nyalert.gov/")).isEqualTo(6);
    assertThat(offsets.getLinkLineNumber("http://www.nyalert.gov/Public/"
        + "News/GetCapAlert.aspx?capID=NYAlert-ALLHAZALERT-3167113")).isEqualTo(14);
    assertThat(offsets.getLinkLineNumber("http://www.nyalert.gov/Public/"
        + "News/GetCapAlert.aspx?capID=NYAlert-ALLHAZALERT-3162202")).isEqualTo(23);
  }
  
  public void testLineOffsets_edxldeFeed() throws Exception {
    String rss = TestResources.load("bushfire_valid.edxlde");
    LineOffsets offsets = parser.parse(rss);

    // Invalid queries
    assertThat(offsets.getXPathLineNumber("invalid")).isEqualTo(0);
    
    // getXPathLineNumber
    assertThat(offsets.getXPathLineNumber("/EDXLDistribution[1]/contentObject[1]/xmlContent[1]/"
        + "embeddedXMLContent[1]/alert[1]")).isEqualTo(18);
    assertThat(offsets.getXPathLineNumber("/EDXLDistribution[1]/contentObject[2]/xmlContent[1]/"
        + "embeddedXMLContent[1]/alert[1]/sender[1]")).isEqualTo(96);
    assertThat(offsets.getXPathLineNumber("/EDXLDistribution[1]/contentObject[2]/xmlContent[1]/"
        + "embeddedXMLContent[1]/alert[1]/info[1]/parameter[2]")).isEqualTo(122);
  }
  
  public void testLineOffsets_invalidXml() throws Exception {
    String invalidXML = "<alert>\n<info>\n</alert>";

    try {
      parser.parse(invalidXML);
      fail();
    } catch (IllegalArgumentException expected) { }
  }
}

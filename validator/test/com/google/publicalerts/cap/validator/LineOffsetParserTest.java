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

import com.google.publicalerts.cap.feed.TestResources;
import com.google.publicalerts.cap.validator.LineOffsetParser.LineOffsets;

/**
 * Tests for {@link LineOffsetParser}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class LineOffsetParserTest extends TestCase {

  public LineOffsetParserTest(String name) {
    super(name);
  }

  public void testLineOffsets() throws Exception {
    String atom = TestResources.load("weather.atom");
    LineOffsetParser parser = new LineOffsetParser();
    LineOffsets offsets = parser.parse(atom);

    assertEquals(0, offsets.getEntryLineNumber(-1));
    assertEquals(0, offsets.getXPathLineNumber("invalid"));

    assertEquals(17, offsets.getEntryLineNumber(0));
    assertEquals(17, offsets.getXPathLineNumber(
        "/feed/entry[0]"));
    assertEquals(28, offsets.getXPathLineNumber(
        "/feed/entry[0]/content/alert"));
    assertEquals(37, offsets.getXPathLineNumber(
        "/feed/entry[0]/content/alert/info[0]"));

    assertEquals(16, offsets.getLinkLineNumber(
        "http://www.weather.gov/alerts-beta/"));
    assertEquals(25, offsets.getLinkLineNumber(
        "http://www.weather.gov/alerts-beta/wwacapget.php?" +
        "x=DC20100829202900LWXAirQualityAlertLWX20100831040000DC"));
  }
}

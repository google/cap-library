/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.publicalerts.cap.feed;

import com.google.common.collect.Lists;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.testing.TestResources;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests for {@link SimpleFatCapAtomFeedBuilder}
 *
 * @author pcoakley@google.com (Phil Coakley)
 */
public class SimpleFatCapAtomFeedBuilderTest extends TestCase {
  private CapFeedParser capFeedParser;
  private SimpleFatCapAtomFeedBuilder capFeedBuilder;

  public SimpleFatCapAtomFeedBuilderTest(String testCaseName) {
    super(testCaseName);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    capFeedParser = new CapFeedParser(true);
    capFeedBuilder = new SimpleFatCapAtomFeedBuilder();
  }

  private List<Alert> extractAlerts(SyndFeed feed) throws Exception {
    List<Alert> alerts = Lists.newArrayList();
    @SuppressWarnings("unchecked")
    List<SyndEntry> entries = feed.getEntries();
    for (SyndEntry entry : entries) {
      alerts.add(capFeedParser.parseAlert(entry));
    }
    return alerts;
  }

  public void testToAtomFeed() throws Exception {
    SyndFeed expectedFeed =
        capFeedParser.parseFeed(TestResources.load("weather.atom"));

    String constructedFeedString = capFeedBuilder.toAtomFeed(
        expectedFeed.getTitle(),
        expectedFeed.getUri(),
        expectedFeed.getPublishedDate(),
        extractAlerts(expectedFeed));
    SyndFeed constructedFeed = capFeedParser.parseFeed(constructedFeedString);

    assertEquals("Title", expectedFeed.getTitle(), constructedFeed.getTitle());
    assertEquals("Uri", expectedFeed.getUri(), constructedFeed.getUri());
    assertEquals("Date", expectedFeed.getPublishedDate(),
        constructedFeed.getPublishedDate());
    assertEquals("CAP alerts", extractAlerts(expectedFeed),
        extractAlerts(constructedFeed));
  }
}

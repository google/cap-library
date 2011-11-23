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

package com.google.publicalerts.cap.feed;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.NotCapException;
import com.google.publicalerts.cap.TestUtil;
import com.google.publicalerts.cap.feed.CapFeedException.FeedErrorType;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.feed.synd.SyndLinkImpl;
import com.sun.syndication.io.FeedException;

/**
 * Tests for {@link CapFeedParser}.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class CapFeedParserTest extends TestCase {

  private CapFeedParser parser;

  public CapFeedParserTest(String s) {
    super(s);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    parser = new CapFeedParser(true);
  }

  public void testParseAtomFeed() throws Exception {
    String feedStr = TestResources.load("weather.atom");
    SyndFeed feed = parser.parseFeed(feedStr);
    assertEquals(1, feed.getEntries().size());
  }
  
  public void testParseRssFeed() throws Exception {
    String feedStr = TestResources.load("ny_index.rss");
    SyndFeed feed = parser.parseFeed(feedStr);
    assertEquals(2, feed.getEntries().size());    
  }
  
  public void testParseMalformedFeed() throws Exception {
    try {
      parser.parseFeed("invalid");
      fail("Expected FeedException");
    } catch (FeedException e) {
      // expected
    }
    try {
      parser.parseFeed("<feed></fee");
      fail("Expected FeedException");
    } catch (FeedException e) {
      // expected
    }
  }

  public void testParseInvalidXml() throws Exception {
    try {
      parser.parseFeed("<someotherxml></someotherxml>");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testParseInvalidFeed1() throws Exception {
    assertCapException("no_id.atom",
        FeedErrorType.ATOM_ID_IS_REQUIRED,
        FeedErrorType.ATOM_ENTRY_ID_IS_REQUIRED);
  }

  public void testParseInvalidFeed2() throws Exception {
    assertCapException("no_title.atom",
        FeedErrorType.ATOM_ENTRY_TITLE_IS_REQUIRED);
  }
  
  public void testParseInvalidFeed4() throws Exception {
    assertCapException("no_link.atom",
        FeedErrorType.ATOM_ENTRY_MISSING_CAP_LINK);
  }

  public void testParseInvalidFeed5() throws Exception {
    assertCapException("invalid_element.atom",
        FeedErrorType.OTHER);
  }

  public void testParseInvalidRssFeed() throws Exception {
    assertCapException("invalid.rss",
        FeedErrorType.RSS_ITEM_MISSING_CAP_LINK,
        FeedErrorType.RSS_ITEM_TITLE_OR_DESCRIPTION_IS_REQUIRED);
  }

  public void testParseIndexFeed() throws Exception {
    String feedStr = TestResources.load("weather_index.atom");
    SyndFeed feed = parser.parseFeed(feedStr);
    assertEquals(2, feed.getEntries().size());

    try {
      parser.parseAlerts(feed);
      fail("Expected NotCapException");
    } catch (NotCapException e) {
      // expected
    }
  }
  
  public void testParseAlerts() throws Exception {
    String feedStr = TestResources.load("weather.atom");
    SyndFeed feed = parser.parseFeed(feedStr);
    List<Alert> alerts = parser.parseAlerts(feed);
    assertEquals(1, alerts.size());

    Alert alert = alerts.get(0);
    assertEquals("w-nws.webmaster@noaa.gov", alert.getSender());
    assertEquals(1, alert.getInfoCount());
  }

  public void testParseAlert() throws Exception {
    String alertStr = TestResources.load("earthquake.cap");
    Alert alert = parser.parseAlert(alertStr);
    assertEquals(
        "http://earthquake.usgs.gov/research/monitoring/anss/neic/",
        alert.getSender());
    assertEquals(2, alert.getInfoCount());
  }
  
  public void testParseAlert2() throws Exception {
    String alertStr = TestResources.load("canada.cap");
    Alert alert = parser.parseAlert(alertStr);
    assertEquals(2, alert.getInfoCount());
  }

  public void testParseEmptyAlert() throws Exception {
    try {
      parser.parseAlert("");
      fail("Expected NotCapException");
    } catch (NotCapException e) {
      // expected
    }
  }

  public void testParseInvalidAlert() throws Exception {
    String alertStr = TestResources.load("invalid.cap");

    // Try both the version that throws an exception and the one that doesn't

    try {
      parser.parseAlert(alertStr);
      fail("Excpected CapException");
    } catch (CapException expected) {
      // expected
    }

    List<CapException.Reason> reasons = new ArrayList<CapException.Reason>();
    Alert alert = parser.parseAlert(alertStr, reasons);
    assertNotNull(alert);
    assertFalse(reasons.isEmpty());
  }

  public void testGetCapUrl() {
    SyndEntry entry = new SyndEntryImpl();
    assertNull(parser.getCapUrl(entry));

    SyndLink link1 = new SyndLinkImpl();
    link1.setHref("http://example.org/cap?id=123");
    List<SyndLink> links = new ArrayList<SyndLink>();
    links.add(link1);
    entry.setLinks(links);
    assertEquals(link1.getHref(), parser.getCapUrl(entry));

    SyndLink link2 = new SyndLinkImpl();
    link2.setHref("http://example.org/cap?id=456");
    links.add(link2);
    entry.setLinks(links);
    assertNull(parser.getCapUrl(entry));

    link2.setType(CapFeedParser.CAP_CONTENT_TYPE);
    assertEquals(link2.getHref(), parser.getCapUrl(entry));
  }

  private void assertCapException(String feedFile, FeedErrorType... types)
      throws Exception {
    String feed = TestResources.load(feedFile);
    try {
      parser.parseFeed(feed);
      fail("Expected CapException");
    } catch (CapException e) {
      TestUtil.assertErrorTypes(e.getReasons(), types);
    }
  }
}

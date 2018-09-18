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

import com.google.common.collect.Lists;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.NotCapException;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.Reason.Level;
import com.google.publicalerts.cap.Reasons;
import com.google.publicalerts.cap.feed.CapFeedException.ReasonType;
import com.google.publicalerts.cap.testing.CapTestUtil;
import com.google.publicalerts.cap.testing.TestResources;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.feed.synd.SyndLinkImpl;
import com.sun.syndication.io.FeedException;

import junit.framework.TestCase;

import java.util.List;

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

  public void testParseEdxldeFeed() throws Exception {
    String feedStr = TestResources.load("bushfire_valid.edxlde");
    SyndFeed feed = parser.parseFeed(feedStr);
    assertEquals(7, feed.getEntries().size());
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
    assertReasons("no_id.atom",
        new Reason("/feed[1]", ReasonType.ATOM_ID_IS_REQUIRED),
        new Reason("/feed[1]/entry[1]", ReasonType.ATOM_ENTRY_ID_IS_REQUIRED));
  }

  public void testParseInvalidFeed2() throws Exception {
    assertReasons("no_title.atom", new Reason("/feed[1]/entry[1]",
        ReasonType.ATOM_ENTRY_TITLE_IS_REQUIRED));
  }

  public void testParseInvalidFeed4() throws Exception {
    assertReasons("no_link.atom", new Reason("/feed[1]/entry[1]",
        ReasonType.ATOM_ENTRY_MISSING_CAP_LINK));
  }

  public void testParseInvalidFeed5() throws Exception {
    assertReasons("invalid_element.atom", new Reason("/feed[1]/foo[1]",
        ReasonType.OTHER));
  }

  public void testParseFeedPreventsXee() throws Exception {
    String feedStr = TestResources.load("xee.atom");
    try {
      parser.parseFeed(feedStr);
      fail("Expected CapException");
    } catch (CapException e) {
      // expected
    }
  }

  public void testParseAlertPreventsXee() throws Exception {
    String alertStr = TestResources.load("xee.cap");
    try {
      parser.parseAlert(alertStr);
      fail("Expected CapException");
    } catch (CapException e) {
      // expected
    }
  }

  /**
   * Tests the case of a valid Atom feed not containing any &lt;entry&gt;.
   * 
   * <p>In {@code no_entries.atom}, a CAP alert exists, but it is misplaced and
   * seen as foreign markup, thus ignored.
   */
  public void testNoAtomEntries() throws Exception {
    String feedStr = TestResources.load("no_entries.atom");
    SyndFeed feed = parser.parseFeed(feedStr);
    assertEquals(0, feed.getEntries().size());

    try {
      parser.parseAlerts(feed);
      fail("Expected NotCapException");
    } catch (NotCapException e) {
      // expected
    }
  }

  public void testParseInvalidRssFeed() throws Exception {
    assertReasons("invalid.rss",
        new Reason("/rss[1]/channel[1]/item[1]",
            ReasonType.RSS_ITEM_MISSING_CAP_LINK),
        new Reason("/rss[1]/channel[1]/item[1]",
            ReasonType.RSS_ITEM_TITLE_OR_DESCRIPTION_IS_REQUIRED),
        new Reason("/rss[1]/channel[1]/item[1]",
            ReasonType.RSS_ITEM_GUID_IS_RECOMMENDED),
        new Reason("/rss[1]/channel[1]", ReasonType.RSS_PUBDATE_IS_RECOMMENDED));
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
    assertEquals(1, alert.getInfoCount());
  }

  public void testParseAlert2() throws Exception {
    String alertStr = TestResources.load("canada.cap");
    Alert alert = parser.parseAlert(alertStr);
    assertEquals(2, alert.getInfoCount());
  }

  public void testParseAlertFromIso88591Bytes() throws Exception {
    byte[] bytes = TestResources.loadBytes("earthquake-iso8859-1.cap");
    Alert alert = parser.parseAlert(bytes);
    assertEquals(1, alert.getInfoCount());
    assertTrue(alert.getInfo(0).getArea(0).getAreaDesc().contains("\u00E1"));
  }

  public void testParseEdxldeAlert() throws Exception {
    String alertStr = TestResources.load("bushfire_valid.edxlde");
    SyndFeed syndFeed = parser.parseFeed(alertStr);
    assertEquals(syndFeed.getEntries().size(), 7);

    @SuppressWarnings("unchecked")
    List<SyndEntry> syndEntries = syndFeed.getEntries();
    for (SyndEntry entry : syndEntries) {
      assertEquals(1, entry.getContents().size());
      @SuppressWarnings("unchecked")
      SyndContent content = ((List<SyndContent>) entry.getContents()).get(0);
      Alert capAlert = parser.parseAlert(entry);
      assertNotNull(content.getValue());
      assertTrue(content.getValue().contains("<cap:alert"));
      assertNotNull(capAlert);
      assertEquals(1, capAlert.getInfoCount());
    }
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

    Reasons.Builder reasons = Reasons.newBuilder();
    Alert alert = parser.parseAlert(alertStr, reasons);
    assertNotNull(alert);
    assertTrue(reasons.build().containsWithLevelOrHigher(Level.ERROR));
  }

  public void testGetCapUrl_atom() throws Exception {
    SyndEntry entry = new SyndEntryImpl();

    // For atom, both "link" and "links" is set
    entry.setLink("http://example.org/cap?id=123");

    SyndLink link1 = new SyndLinkImpl();
    link1.setHref("http://example.org/cap?id=123");
    List<SyndLink> links = Lists.newArrayList();
    links.add(link1);
    entry.setLinks(links);
    assertEquals(link1.getHref(), parser.getCapUrl(entry));

    link1.setType("text/xml");
    assertEquals(link1.getHref(), parser.getCapUrl(entry));

    link1.setType(CapFeedParser.CAP_MIME_TYPE);
    assertEquals(link1.getHref(), parser.getCapUrl(entry));

    link1.setType(CapFeedParser.ALTERNATE_CAP_MIME_TYPE);
    assertEquals(link1.getHref(), parser.getCapUrl(entry));

    link1.setType("text/html");
    assertNull(parser.getCapUrl(entry));

    SyndLink link2 = new SyndLinkImpl();
    link2.setHref("http://example.org/cap?id=456");
    links.add(link2);
    entry.setLinks(links);
    assertEquals(link2.getHref(), parser.getCapUrl(entry));

    link2.setType("text/html");
    assertNull(parser.getCapUrl(entry));

    link2.setType("application/xml");
    assertEquals(link2.getHref(), parser.getCapUrl(entry));

    link2.setType(CapFeedParser.CAP_MIME_TYPE);
    assertEquals(link2.getHref(), parser.getCapUrl(entry));

    link1.setType("text/xml");
    assertEquals(link2.getHref(), parser.getCapUrl(entry));

    link1.setType(CapFeedParser.CAP_MIME_TYPE);
    assertEquals(link1.getHref(), parser.getCapUrl(entry));
  }

  public void testGetCapUrl_rss() throws Exception {
    SyndEntry entry = new SyndEntryImpl();
    assertNull(parser.getCapUrl(entry));

    entry.setLink("http://example.org/cap?id=123");
    assertEquals(entry.getLink(), parser.getCapUrl(entry));

    // illegal protocol
    entry.setLink("file:///");
    try {
      assertNull(parser.getCapUrl(entry));
      fail("Expected FeedException");
    } catch (FeedException e) {
      // expected
    }

    // malformed url
    entry.setLink("htpp://abc");
    assertNull(parser.getCapUrl(entry));
  }

  private void assertReasons(String feedFile,  Reason... expectedReasons)
      throws Exception {
    String feed = TestResources.load(feedFile);
    
    try {
      parser.parseFeed(feed);
      fail("Expected CapException");
    } catch (CapException e) {
      CapTestUtil.assertCapException(e, expectedReasons);
    }
  }
}

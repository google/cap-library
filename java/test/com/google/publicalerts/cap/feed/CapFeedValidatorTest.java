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

import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.edxl.DistributionFeed;
import com.google.publicalerts.cap.edxl.types.ContentObject;
import com.google.publicalerts.cap.edxl.types.NonXmlContent;
import com.google.publicalerts.cap.edxl.types.XmlContent;
import com.google.publicalerts.cap.feed.CapFeedException.ReasonType;
import com.google.publicalerts.cap.testing.CapTestUtil;
import com.google.publicalerts.cap.testing.TestResources;

import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import junit.framework.TestCase;

/**
 * Tests for {@link CapFeedValidator}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapFeedValidatorTest extends TestCase {

  CapFeedValidator validator;

  public CapFeedValidatorTest(String s) {
    super(s);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    validator = new CapFeedValidator();
  }

  @SuppressWarnings("unchecked")
  public void testAtomErrors() throws Exception {
    SyndFeed syndFeed = loadFeed("earthquake_index.atom", false);
    assertNoReasons(syndFeed);

    Feed feed = (Feed) syndFeed.originalWireFeed();
    Entry entry = (Entry) feed.getEntries().get(0);
    entry.getOtherLinks().clear();
    entry.getAlternateLinks().clear();
    assertReasons(
        syndFeed, ReasonType.ATOM_ENTRY_MISSING_CAP_LINK, "/feed[1]/entry[1]");

    Link link = new Link();
    link.setType("text/plain");
    entry.getAlternateLinks().add(link);
    assertNoReasons(syndFeed);

    // Ignore image, audio, video mime types
    link = new Link();
    link.setType("image/jpeg");
    entry.getOtherLinks().add(link);

    link = new Link();
    link.setType("audio/mpeg");
    entry.getOtherLinks().add(link);

    link = new Link();
    link.setType("video/mpeg");
    entry.getOtherLinks().add(link);

    assertNoReasons(syndFeed);

    // But add another link with no CAP type and it's ambiguous
    link = new Link();
    entry.getOtherLinks().add(link);
    assertEquals(4, entry.getOtherLinks().size());
    assertReasons(
        syndFeed, ReasonType.ATOM_ENTRY_MISSING_CAP_LINK, "/feed[1]/entry[1]");
    
    syndFeed = loadFeed("weather_index.atom", false);
    feed = (Feed) syndFeed.originalWireFeed();
    Entry entry1 = (Entry) feed.getEntries().get(0);
    Entry entry2 = (Entry) feed.getEntries().get(1);
    entry2.setId(entry1.getId());
    assertReasons(
        syndFeed, ReasonType.ATOM_ENTRY_NON_UNIQUE_IDS, "/feed[1]/entry[2]");
  }

  public void testRssErrors() throws Exception {
    SyndFeed syndFeed = loadFeed("ny_index.rss", true);
    assertNoReasons(syndFeed);

    Channel channel = (Channel) syndFeed.originalWireFeed();

    Item item = (Item) channel.getItems().get(1);
    item.setTitle(null);
    item.setDescription(null);
    item.setLink(null);
    assertReasons(syndFeed,
        new Reason("/rss[1]/channel[1]/item[2]",
            ReasonType.RSS_ITEM_TITLE_OR_DESCRIPTION_IS_REQUIRED),
        new Reason("/rss[1]/channel[1]/item[2]",
            ReasonType.RSS_ITEM_MISSING_CAP_LINK));
  }

  public void testAtomRecommendations() throws Exception {
    SyndFeed syndFeed = loadFeed("earthquake_index.atom", false);
    assertNoReasons(syndFeed);
  }

  public void testRssRecommendations() throws Exception {
    SyndFeed syndFeed = loadFeed("ny_index.rss", false);
    assertNoReasons(syndFeed);

    Channel channel = (Channel) syndFeed.originalWireFeed();
    channel.setPubDate(null);

    Item item = (Item) channel.getItems().get(1);
    item.setGuid(null);

    assertReasons(syndFeed,
        new Reason("/rss[1]/channel[1]", ReasonType.RSS_PUBDATE_IS_RECOMMENDED),
        new Reason("/rss[1]/channel[1]/item[2]",
            ReasonType.RSS_ITEM_GUID_IS_RECOMMENDED));
  }

  public void testEdxldeErrors_missingContentObjects() throws Exception {
    DistributionFeed feed = new DistributionFeed("edxlde_1.0");
    feed.setDateTimeSent("2011-10-17T11:18:00-00:00");
    SyndFeed syndFeed = new SyndFeedImpl(feed, true);
    assertReasons(syndFeed, ReasonType.EDXLDE_CONTENT_OBJECT_IS_REQUIRED,
        "/EDXLDistribution[1]");
  }

  public void testEdxldeErrors_invalidXml() throws Exception {
    DistributionFeed feed = new DistributionFeed("edxlde_1.0");
    feed.setDateTimeSent("2011-10-17T11:18:00-00:00");
    ContentObject content = new ContentObject();
    XmlContent xml = new XmlContent();
    xml.addEmbeddedXmlContent("non-cap xml");
    content.setXmlContent(xml);
    feed.addContentObject(content);
    SyndFeed syndFeed = new SyndFeedImpl(feed, true);
    assertReasons(syndFeed, ReasonType.EDXLDE_NO_CAP_IN_CONTENT_OBJECT,
        "/EDXLDistribution[1]/contentObject[1]/xmlContent[1]");
  }

  public void testEdxldeErrors_validXml() throws Exception {
    SyndFeed syndFeed = loadFeed("bushfire_valid.edxlde", false);
    assertNoReasons(syndFeed);
  }

  public void testEdxldeErrors_validNonXml() throws Exception {
    runTestEdxldeErrors(CapFeedParser.CAP_MIME_TYPE);
  }

  public void testEdxldeErrors_validNonXmlAlternateMimeType() throws Exception {
    runTestEdxldeErrors(CapFeedParser.ALTERNATE_CAP_MIME_TYPE);
  }

  private void runTestEdxldeErrors(String contentType) throws Exception {
    DistributionFeed feed = new DistributionFeed("edxlde_1.0");
    feed.setDateTimeSent("2011-10-17T11:18:00-00:00");
    ContentObject content = new ContentObject();
    NonXmlContent nonXml = new NonXmlContent(contentType);
    nonXml.setUri("capUri");
    content.setNonXmlContent(nonXml);
    feed.addContentObject(content);
    SyndFeed syndFeed = new SyndFeedImpl(feed, true);
    assertNoReasons(syndFeed);
  }

  public void testEdxldeErrors_invalidNonXmlUri() throws Exception {
    DistributionFeed feed = new DistributionFeed("edxlde_1.0");
    feed.setDateTimeSent("2011-10-17T11:18:00-00:00");
    ContentObject content = new ContentObject();
    NonXmlContent nonXml = new NonXmlContent(CapFeedParser.CAP_MIME_TYPE);
    content.setNonXmlContent(nonXml);
    feed.addContentObject(content);
    SyndFeed syndFeed = new SyndFeedImpl(feed, true);
    assertReasons(syndFeed, ReasonType.EDXLDE_NO_CAP_IN_CONTENT_OBJECT,
        "/EDXLDistribution[1]/contentObject[1]/nonXMLContent[1]");
  }

  public void testEdxldeErrors_invalidNonXmlMimeType() throws Exception {
    DistributionFeed feed = new DistributionFeed("edxlde_1.0");
    feed.setDateTimeSent("2011-10-17T11:18:00-00:00");
    ContentObject content = new ContentObject();
    NonXmlContent nonXml = new NonXmlContent("foo");
    nonXml.setUri("capUri");
    content.setNonXmlContent(nonXml);
    feed.addContentObject(content);
    SyndFeed syndFeed = new SyndFeedImpl(feed, true);
    assertReasons(syndFeed, ReasonType.EDXLDE_NO_CAP_IN_CONTENT_OBJECT,
        "/EDXLDistribution[1]/contentObject[1]/nonXMLContent[1]");
  }

  private SyndFeed loadFeed(String file, boolean validate) throws Exception {
    return new CapFeedParser(validate).parseFeed(TestResources.load(file));
  }

  private void assertNoReasons(SyndFeed feed) throws Exception {
    assertReasons(feed);
  }
  
  private void assertReasons(SyndFeed feed, Reason... expectedReasons)
      throws Exception {
    CapTestUtil.assertReasons(validator.validate(feed), expectedReasons);
  }
  
  private void assertReasons(SyndFeed feed, Reason.Type reasonType,
      String xPath) throws Exception {
    assertReasons(feed, new Reason(xPath, reasonType));
  }
}

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

import java.util.List;

import junit.framework.TestCase;

import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.TestUtil;
import com.google.publicalerts.cap.feed.CapFeedException.FeedErrorType;
import com.google.publicalerts.cap.feed.CapFeedException.FeedRecommendationType;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.feed.synd.SyndFeed;

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
    
  public void testAtomErrors() throws Exception {
    SyndFeed syndFeed = loadFeed("earthquake_index.atom");
    assertNoErrors(syndFeed);
    
    Feed feed = (Feed) syndFeed.originalWireFeed();
    Entry entry = (Entry) feed.getEntries().get(0);
    entry.getOtherLinks().clear();
    entry.getAlternateLinks().clear();
    assertErrors(syndFeed, FeedErrorType.ATOM_ENTRY_MISSING_CAP_LINK);
  }
  
  public void testRssErrors() throws Exception {
    SyndFeed syndFeed = loadFeed("ny_index.rss");
    assertNoErrors(syndFeed);
    
    Channel channel = (Channel) syndFeed.originalWireFeed();    
    
    Item item = (Item) channel.getItems().get(1);
    item.setTitle(null);
    item.setDescription(null);
    item.setLink(null);
    assertErrors(syndFeed, FeedErrorType.RSS_ITEM_TITLE_OR_DESCRIPTION_IS_REQUIRED,
        FeedErrorType.RSS_ITEM_MISSING_CAP_LINK);
  }
  
  public void testAtomRecommendations() throws Exception {
    SyndFeed syndFeed = loadFeed("earthquake_index.atom");
    assertNoRecommendations(syndFeed);
  }
  
  public void testRssRecommendations() throws Exception {
    SyndFeed syndFeed = loadFeed("ny_index.rss");
    assertNoRecommendations(syndFeed);

    Channel channel = (Channel) syndFeed.originalWireFeed();
    channel.setPubDate(null);
    
    Item item = (Item) channel.getItems().get(1);
    item.setGuid(null);
    
    assertRecommendations(syndFeed,
        FeedRecommendationType.RSS_PUBDATE_IS_RECOMMENDED,
        FeedRecommendationType.RSS_ITEM_GUID_IS_RECOMMENDED);
  }
  
  private SyndFeed loadFeed(String file) throws Exception {
    return new CapFeedParser(false).parseFeed(TestResources.load(file));
  }

  private void assertNoErrors(SyndFeed feed) {
    assertErrors(feed, new FeedErrorType[] {});
  }

  private void assertErrors(SyndFeed feed, FeedErrorType...types) {
    try {
      validator.checkForErrors(feed);
    } catch (CapException e) {
      TestUtil.assertErrorTypes(e.getReasons(), types);      
    }
  }
  
  private void assertNoRecommendations(SyndFeed feed) {
    assertRecommendations(feed, new FeedRecommendationType[] {});
  }

  private void assertRecommendations(
      SyndFeed feed, FeedRecommendationType...types) {
    List<Reason> reasons = validator.checkForRecommendations(feed);
    TestUtil.assertErrorTypes(reasons, types);

  }
}

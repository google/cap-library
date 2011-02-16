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
import java.util.Collections;
import java.util.List;

import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.feed.CapFeedException.FeedErrorType;
import com.google.publicalerts.cap.feed.CapFeedException.FeedRecommendationType;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;

/**
 * Validates portions of Atom/RSS feeds (fat or thin) of CAP alerts
 * beyond what is done in by the schemas in {@link CapFeedParser}.
 * 
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapFeedValidator {

  /**
   * Checks the given feed for errors. Assumes the given feed
   * has a non-null {@code originalWireFeed()}.
   *
   * @param syndFeed the parsed feed to validate
   * @throws FeedException if the feed is invalid
   */
  public void checkForErrors(SyndFeed feed) throws CapFeedException {
    if (feed.originalWireFeed() instanceof Feed) {
      checkForErrors((Feed) feed.originalWireFeed());
    } else if (feed.originalWireFeed() instanceof Channel) {
      checkForErrors((Channel) feed.originalWireFeed());
    } else {
      throw new IllegalArgumentException("Unsupported feed " + feed);
    }
  }

  public void checkForErrors(Feed feed) throws CapFeedException {
    @SuppressWarnings("unchecked")
    List<Entry> entries = (List<Entry>) feed.getEntries();
    for (int i = 0; i < entries.size(); i++) {
      Entry entry = entries.get(i);
      if (entry.getContents().isEmpty() && !hasCapLink(entry)) {
        throw new CapFeedException(new Reason("/feed/entry[" + i + "]",
            FeedErrorType.ATOM_ENTRY_MISSING_CAP_LINK));
      }
    }
  }
    
  private boolean hasCapLink(Entry entry) {
    @SuppressWarnings("unchecked")
    List<Link> links = (List<Link>) entry.getAlternateLinks();
    @SuppressWarnings("unchecked")
    List<Link> otherLinks = (List<Link>) entry.getOtherLinks();
    links.addAll(otherLinks);
    if (links.size() == 1) {
      return true;
    }
    for (Link link : links) {
      if (CapFeedParser.CAP_CONTENT_TYPE.equals(link.getType())) {
        return true;
      }
    }
    return false;
  }

  public void checkForErrors(Channel channel) throws CapFeedException {
    List<Reason> reasons = new ArrayList<Reason>();

    List<Reason> itemReasons = new ArrayList<Reason>();
    @SuppressWarnings("unchecked")
    List<Item> items = (List<Item>) channel.getItems();
    for (int i = 0; i < items.size(); i++) {
      Item item = items.get(i);
      String xpath = "/channel/item[" + i + "]";
      if (CapUtil.isEmptyOrWhitespace(item.getTitle()) 
          && (item.getDescription() == null
          || CapUtil.isEmptyOrWhitespace(item.getDescription().getValue()))) {
        itemReasons.add(new Reason(xpath,
            FeedErrorType.RSS_ITEM_TITLE_OR_DESCRIPTION_IS_REQUIRED));
      }
      if (CapUtil.isEmptyOrWhitespace(item.getLink())) {
        itemReasons.add(new Reason(xpath,
            FeedErrorType.RSS_ITEM_MISSING_CAP_LINK));
      }
      // It'd be annoying to see these errors repeated once per entry,
      // so just throw after the first set of errors we see.
      if (!itemReasons.isEmpty()) {
        reasons.addAll(itemReasons);
        break;
      }
    }
    if (!reasons.isEmpty()) {
      throw new CapFeedException(reasons);
    }
  }

  /**
   * Checks the given feed for recommendations. Assumes the given feed
   * has a non-null {@code originalWireFeed()}.
   *
   * @param syndFeed the parsed feed to validate
   * @return a list of recommendations, empty list if there are none
   * @throws FeedException if the feed is invalid
   */
  public List<Reason> checkForRecommendations(SyndFeed feed) {
    if (feed.originalWireFeed() instanceof Feed) {
      return checkForRecommendations((Feed) feed.originalWireFeed());
    } else if (feed.originalWireFeed() instanceof Channel) {
      return checkForRecommendations((Channel) feed.originalWireFeed());
    }
    return Collections.<Reason>emptyList();
  }
  
  public List<Reason> checkForRecommendations(Feed feed) {
    return Collections.<Reason>emptyList();
  }

  public List<Reason> checkForRecommendations(Channel channel) {
    List<Reason> reasons = new ArrayList<Reason>();
    if (channel.getPubDate() == null) {
      reasons.add(new Reason("/channel",
          FeedRecommendationType.RSS_PUBDATE_IS_RECOMMENDED));
    }

    List<Reason> itemReasons = new ArrayList<Reason>();
    @SuppressWarnings("unchecked")
    List<Item> items = (List<Item>) channel.getItems();
    for (int i = 0; i < items.size(); i++) {
      Item item = items.get(i);
      if (item.getGuid() == null ||
          CapUtil.isEmptyOrWhitespace(item.getGuid().getValue())) {
        itemReasons.add(new Reason("/channel/item[" + i + "]",
            FeedRecommendationType.RSS_ITEM_GUID_IS_RECOMMENDED));
      }
      // It'd be annoying to see these errors repeated once per entry,
      // so just throw after the first set of errors we see.
      if (!itemReasons.isEmpty()) {
        reasons.addAll(itemReasons);
        break;
      }
    }
    return reasons;
  }  
}

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

import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.CapValidator;
import com.google.publicalerts.cap.feed.CapFeedException.FeedErrorType;
import com.google.publicalerts.cap.feed.CapFeedException.FeedRecommendationType;
import com.google.publicalerts.cap.edxl.types.ContentObject;
import com.google.publicalerts.cap.edxl.DistributionFeed;

import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.feed.synd.SyndFeed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
   * @param feed the parsed feed to validate
   * @throws CapFeedException if the feed is invalid
   */
  public void checkForErrors(SyndFeed feed) throws CapFeedException {
    if (feed.originalWireFeed() instanceof Feed) {
      checkForErrors((Feed) feed.originalWireFeed());
    } else if (feed.originalWireFeed() instanceof Channel) {
      checkForErrors((Channel) feed.originalWireFeed());
    } else if (feed.originalWireFeed() instanceof DistributionFeed) {
      checkForErrors((DistributionFeed) feed.originalWireFeed());
    } else {
      throw new IllegalArgumentException("Unsupported feed " + feed);
    }
  }

  /**
   * Checks the given Atom feed for errors.
   *
   * @param feed the feed to check
   * @throws CapFeedException if the feed is invalid
   */
  public void checkForErrors(Feed feed) throws CapFeedException {
    @SuppressWarnings("unchecked")
    List<Entry> entries = feed.getEntries();
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
    List<Link> links = entry.getAlternateLinks();
    @SuppressWarnings("unchecked")
    List<Link> otherLinks = entry.getOtherLinks();
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

  /**
   * Checks the given RSS channel for errors.
   *
   * @param channel the channel to check
   * @throws CapFeedException if the channel is invalid
   */
  public void checkForErrors(Channel channel) throws CapFeedException {
    List<Reason> reasons = new ArrayList<Reason>();

    List<Reason> itemReasons = new ArrayList<Reason>();
    @SuppressWarnings("unchecked")
    List<Item> items = channel.getItems();
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
   * Checks the given EDXL-DE distribution feed for errors.
   *
   * In particular, ensures that every contentObject has either an xmlContent
   * object containing a CAP alert, or a nonXMLContent object containing a
   * reference to a CAP alert. Note that this method does not ensure that the
   * CAP alert can be successfully created, but merely that a CAP specification
   * exists.
   *
   * @param feed the distribution feed object to check
   * @throws CapFeedException if the feed is invalid
   */
  public void checkForErrors(DistributionFeed feed) throws CapFeedException {
    List<Reason> reasons = new ArrayList<Reason>();
    boolean foundcap = false;
    boolean errors = false;

    if (feed.getContentObjects().isEmpty()) {
      throw new CapFeedException(new Reason("/EDXLDistribution",
          FeedErrorType.EDXLDE_CONTENT_OBJECT_IS_REQUIRED));
    }

    for (int i = 0; i < feed.getContentObjects().size(); i++) {
      ContentObject content = feed.getContentObjects().get(i);
      foundcap = false;

      if (content.getXmlContent() != null) {
        for (String xml : content.getXmlContent().getEmbeddedXmlContent()) {
          if (xml.contains(CapValidator.CAP10_XMLNS)
              || xml.contains(CapValidator.CAP11_XMLNS)
              || xml.contains(CapValidator.CAP12_XMLNS)) {
            foundcap = true;
          }
        }

        if (!foundcap) {
          reasons.add(new Reason(
              "/EDXLDistribution/contentObject[" + i + "]/xmlContent",
              FeedErrorType.EDXLDE_NO_CAP_IN_CONTENT_OBJECT));
        }
      }

      if (content.getNonXmlContent() != null) {
        if (content.getNonXmlContent().getMimeType().equalsIgnoreCase(
            CapFeedParser.CAP_CONTENT_TYPE)
            && content.getNonXmlContent().getUri() != null) {
          foundcap = true;
        } else {
          reasons.add(new Reason(
              "/EDXLDistribution/contentObject[" + i + "]/nonMXLContent",
              FeedErrorType.EDXLDE_NO_CAP_IN_CONTENT_OBJECT));
        }
      }

      if (!foundcap) {
        errors = true;
      }
    }

    if (errors) {
      throw new CapFeedException(reasons);
    }
  }

  /**
   * Checks the given feed for recommendations. Assumes the given feed
   * has a non-null {@code originalWireFeed()}.
   *
   * @param feed the feed to check
   * @return a list of recommendations, empty list if there are none
   */
  public List<Reason> checkForRecommendations(SyndFeed feed) {
    if (feed.originalWireFeed() instanceof Feed) {
      return checkForRecommendations((Feed) feed.originalWireFeed());
    } else if (feed.originalWireFeed() instanceof Channel) {
      return checkForRecommendations((Channel) feed.originalWireFeed());
    }
    return Collections.<Reason>emptyList();
  }

  /**
   * Checks the given Atom feed for recommendations. Currently a no-op.
   *
   * @param feed the feed to check
   * @return an empty list
   */
  public List<Reason> checkForRecommendations(Feed feed) {
    return Collections.<Reason>emptyList();
  }

  /**
   * Checks the given RSS feed for recommendations.
   *
   * @param channel the channel to check
   * @return a list of recommendations, empty list if there are none
   */
  public List<Reason> checkForRecommendations(Channel channel) {
    List<Reason> reasons = new ArrayList<Reason>();
    if (channel.getPubDate() == null) {
      reasons.add(new Reason("/channel",
          FeedRecommendationType.RSS_PUBDATE_IS_RECOMMENDED));
    }

    List<Reason> itemReasons = new ArrayList<Reason>();
    @SuppressWarnings("unchecked")
    List<Item> items = channel.getItems();
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

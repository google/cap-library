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
import com.google.common.collect.Sets;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.CapValidator;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.Reasons;
import com.google.publicalerts.cap.edxl.DistributionFeed;
import com.google.publicalerts.cap.edxl.types.ContentObject;
import com.google.publicalerts.cap.feed.CapFeedException.ReasonType;

import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.feed.synd.SyndFeed;

import java.util.List;
import java.util.Set;

/**
 * Validates portions of Atom/RSS feeds (fat or thin) of CAP alerts
 * beyond what is done in by the schemas in {@link CapFeedParser}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapFeedValidator {

  /**
   * Validates a feed.
   * 
   * <p>Assumes that the given feed has a non-null {@code originalWireFeed()}.
   *
   * @param feed The parsed feed to validate.
   * @return A collection of reasons storing errors, warnings or
   * recommendations.
   */
  public Reasons validate(SyndFeed feed) {
    
    if (feed.originalWireFeed() instanceof Feed) {
      return validate((Feed) feed.originalWireFeed());
    } else if (feed.originalWireFeed() instanceof Channel) {
      return validate((Channel) feed.originalWireFeed());
    } else if (feed.originalWireFeed() instanceof DistributionFeed) {
      return validate((DistributionFeed) feed.originalWireFeed());
    } else {
      throw new IllegalArgumentException("Unsupported feed " + feed);
    }
  }

  /**
   * Validates a feed.
   * 
   * @param feed The feed to validate.
   * @return A collection of reasons storing errors, warnings or
   * recommendations.
   */
  private Reasons validate(Feed feed) {
    Reasons.Builder reasons = Reasons.newBuilder();
    checkForErrors(feed, reasons);
    return reasons.build();
  }
  
  /**
   * Validates a channel.
   * 
   * @param channel the Channel to validate.
   * @return A collection of reasons storing errors, warnings or
   * recommendations.
   */
  private Reasons validate(Channel channel) {
    Reasons.Builder reasons = Reasons.newBuilder();
    checkForErrors(channel, reasons);
    checkForRecommendations(channel, reasons);
    return reasons.build();
  }
  
  /**
   * Validates an EDXL-DE distribution feed.
   * 
   * @param distributionFeed The distribution feed to validate.
   * @return A collection of reasons storing errors, warnings or
   * recommendations.
   */
  private Reasons validate(DistributionFeed distributionFeed) {
    Reasons.Builder reasons = Reasons.newBuilder();
    checkForErrors(distributionFeed, reasons);
    return reasons.build();
  }
  
  /**
   * Checks the feed for errors and populates the collection provided as input.
   */
  private void checkForErrors(Feed feed, Reasons.Builder reasons) {
    @SuppressWarnings("unchecked")
    List<Entry> entries = feed.getEntries();
    Set<String> entryIds = Sets.newHashSet();
    for (int i = 0; i < entries.size(); i++) {
      // It'd be annoying to see these errors repeated once per entry,
      // so just return after the first set of errors we see.

      Entry entry = entries.get(i);
      if (entry.getContents().isEmpty() && !hasCapLink(entry)) {
        reasons.add(new Reason("/feed[1]/entry[" + (i + 1) + "]",
            ReasonType.ATOM_ENTRY_MISSING_CAP_LINK));
        return;
      }
      
      if (entryIds.contains(entry.getId())) {
        reasons.add(new Reason("/feed[1]/entry[" + (i + 1) + "]",
            ReasonType.ATOM_ENTRY_NON_UNIQUE_IDS, entry.getId()));
        return;
      } else {
        entryIds.add(entry.getId());
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
    int possibleLinkCount = 0;
    for (Link link : links) {
      String type = link.getType();
      if (CapFeedParser.CAP_MIME_TYPE.equalsIgnoreCase(type) ||
          CapFeedParser.ALTERNATE_CAP_MIME_TYPE.equalsIgnoreCase(type)) {
        return true;
      } else if (type == null || (type != null && !type.startsWith("image") &&
          !type.startsWith("audio") && !type.startsWith("video"))) {
        possibleLinkCount++;
      }
    }
    return possibleLinkCount == 1;
  }

  /**
   * Checks the channel for errors and populates the collection provided as
   * input.
   */
  private void checkForErrors(Channel channel, Reasons.Builder reasons) {
    @SuppressWarnings("unchecked")
    List<Item> items = channel.getItems();
    boolean errorFound = false;
    
    for (int i = 0; i < items.size(); i++) {
      Item item = items.get(i);
      String xpath = "/rss[1]/channel[1]/item[" + (i + 1) + "]";
      if (CapUtil.isEmptyOrWhitespace(item.getTitle())
          && (item.getDescription() == null
          || CapUtil.isEmptyOrWhitespace(item.getDescription().getValue()))) {
        reasons.add(new Reason(xpath,
            ReasonType.RSS_ITEM_TITLE_OR_DESCRIPTION_IS_REQUIRED));
        errorFound = true;
      }
      if (CapUtil.isEmptyOrWhitespace(item.getLink())) {
        reasons.add(new Reason(xpath,
            ReasonType.RSS_ITEM_MISSING_CAP_LINK));
        errorFound = true;
      }
      
      // It'd be annoying to see these errors repeated once per entry,
      // so just return after the first set of errors we see.
      if (errorFound) {
        return;
      }
    }
  }

  /**
   * Checks the given EDXL-DE distribution feed for errors and populates the
   * collection provided as input.
   *
   * <p>In particular, ensures that every contentObject has either an xmlContent
   * object containing a CAP alert, or a nonXMLContent object containing a
   * reference to a CAP alert. Note that this method does not ensure that the
   * CAP alert can be successfully created, but merely that a CAP specification
   * exists.
   */
  private void checkForErrors(DistributionFeed feed, Reasons.Builder reasons) {
    if (feed.getContentObjects().isEmpty()) {
      reasons.add(new Reason("/EDXLDistribution[1]",
          ReasonType.EDXLDE_CONTENT_OBJECT_IS_REQUIRED));
      return;
    }

    for (int i = 0; i < feed.getContentObjects().size(); i++) {
      boolean foundcap = false;
      List<Reason> reasonBuffer = Lists.newArrayList();
      
      ContentObject content = feed.getContentObjects().get(i);

      if (content.getXmlContent() != null) {
        for (String xml : content.getXmlContent().getEmbeddedXmlContent()) {
          if (xml.contains(CapValidator.CAP10_XMLNS)
              || xml.contains(CapValidator.CAP11_XMLNS)
              || xml.contains(CapValidator.CAP12_XMLNS)) {
            foundcap = true;
          }
        }
        
        if (!foundcap) {
          reasonBuffer.add(new Reason(
              "/EDXLDistribution[1]/contentObject[" + (i + 1) + "]/xmlContent[1]",
              ReasonType.EDXLDE_NO_CAP_IN_CONTENT_OBJECT));
        }
      }

      if (content.getNonXmlContent() != null) {
        String mimeType = content.getNonXmlContent().getMimeType();
        if ((mimeType.equalsIgnoreCase(CapFeedParser.CAP_MIME_TYPE)
            || mimeType.equalsIgnoreCase(CapFeedParser.ALTERNATE_CAP_MIME_TYPE))
            && content.getNonXmlContent().getUri() != null) {
          foundcap = true;
        } else {
          reasonBuffer.add(new Reason(
              "/EDXLDistribution[1]/contentObject[" + (i + 1) + "]/nonXMLContent[1]",
              ReasonType.EDXLDE_NO_CAP_IN_CONTENT_OBJECT));
        }
      }

      if (!foundcap) {
        reasons.addAll(reasonBuffer);
      }
    }
  }

  /**
   * Checks the channel for recommendations and populates the collection
   * provided as input.
   */
  private void checkForRecommendations(
      Channel channel, Reasons.Builder reasons) {
    if (channel.getPubDate() == null) {
      reasons.add(new Reason("/rss[1]/channel[1]",
          ReasonType.RSS_PUBDATE_IS_RECOMMENDED));
    }

    List<Reason> itemReasons = Lists.newArrayList();
    @SuppressWarnings("unchecked")
    List<Item> items = channel.getItems();
    for (int i = 0; i < items.size(); i++) {
      Item item = items.get(i);
      if (item.getGuid() == null ||
          CapUtil.isEmptyOrWhitespace(item.getGuid().getValue())) {
        itemReasons.add(new Reason("/rss[1]/channel[1]/item[" + (i + 1) + "]",
            ReasonType.RSS_ITEM_GUID_IS_RECOMMENDED));
      }
      // It'd be annoying to see these errors repeated once per entry,
      // so just throw after the first set of errors we see.
      if (!itemReasons.isEmpty()) {
        reasons.addAll(itemReasons);
        break;
      }
    }
  }
}

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

import static com.google.publicalerts.cap.CapUtil.stripXmlPreamble;
import static java.util.Collections.singletonList;

import com.google.common.collect.Lists;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapDateUtil;
import com.google.publicalerts.cap.CapXmlBuilder;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Writes one or more Alerts in XML format to an Atom feed.
 * This class uses simple defaults for setting Atom fields, which cannot be
 * customized by the user. The CAP content is embedded inside the feed ("fat").
 *
 * @see CapXmlBuilder
 * @author pcoakley@google.com (Phil Coakley)
 */
public class SimpleFatCapAtomFeedBuilder {
  private final CapXmlBuilder capXmlBuilder;

  public SimpleFatCapAtomFeedBuilder() {
    this.capXmlBuilder = new CapXmlBuilder(2 /* indent 2 spaces */);
  }

  /**
   * Constructs an Atom feed containing the given complete ("fat") CAP messages.
   */
  public String toAtomFeed(String feedTitle, String feedUri, Date feedPublishedDate,
      Collection<Alert> capAlerts) {
    SyndFeed syndFeed = new SyndFeedImpl();
    syndFeed.setFeedType("atom_1.0");
    syndFeed.setTitle(feedTitle);
    syndFeed.setUri(feedUri);
    syndFeed.setPublishedDate(feedPublishedDate);

    List<SyndEntry> feedEntries = Lists.newArrayList();
    for (Alert alert : capAlerts) {
      SyndEntry entry = new SyndEntryImpl();
      entry.setUri(alert.getIdentifier());
      entry.setTitle(alert.getInfo(0).getEvent());
      entry.setUpdatedDate(CapDateUtil.toJavaDate(alert.getSent()));

      SyndContent content = new SyndContentImpl();
      content.setType("text/xml");
      String capXml = capXmlBuilder.toXml(alert);
      content.setValue(stripXmlPreamble(capXml));

      entry.setContents(singletonList(content));
      feedEntries.add(entry);
    }
    syndFeed.setEntries(feedEntries);

    SyndFeedOutput output = new SyndFeedOutput();
    try {
      return output.outputString(syndFeed, true /* pretty-print */);
    } catch (FeedException e) {
      throw new RuntimeException("Error creating feed", e);
    }
  }
}

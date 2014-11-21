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

package com.google.publicalerts.cap.validator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.CharMatcher;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.NotCapException;
import com.google.publicalerts.cap.Reason.Level;
import com.google.publicalerts.cap.Reasons;
import com.google.publicalerts.cap.edxl.DistributionFeed;
import com.google.publicalerts.cap.feed.CapFeedException;
import com.google.publicalerts.cap.feed.CapFeedParser;
import com.google.publicalerts.cap.feed.CapFeedValidator;
import com.google.publicalerts.cap.profile.CapProfile;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;

import org.apache.xerces.impl.dv.util.Base64;

/**
 * Validates CAP messages and feeds of CAP messages where the actual
 * CAP may or may not have to be loaded via URL.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidator {
  private static final Logger log =
      Logger.getLogger(CapValidator.class.getName());

  private static final int REQUEST_DEADLINE_MS = 20000;

  private final CapFeedParser capFeedParser;

  public CapValidator() {
    this.capFeedParser = new CapFeedParser(true);
  }

  /**
   * Validates the given input
   *
   * @param input the input content to validate. Input can be a feed of CAP
   *   alerts, a feed of links to CAP alerts, a list of CAP alerts, a URL
   *   pointing to any of the above, or gibberish.
   * @param profiles an optional set of CAP profiles to apply during
   *   validation; can be empty.
   */
  public ValidationResult validate(String input, Set<CapProfile> profiles) {
    ValidationResult result = new ValidationResult(input);

    // Is the input a URL?
    try {
      URL feedUrl = new URL(input);
      log.info("URLRequest");
      input = loadUrl(feedUrl.toString());
      result = new ValidationResult(input);
    } catch (MalformedURLException e) {
      // Input is not a URL... continue
      // TODO(shakusa) Maybe some heuristics here if it's almost a URL to show
      // a URL-based error instead of a confusing XML one?
    } catch (IOException e) {
      // There was a problem loading the feed from a URL
      log.info("URLRequest: Error");
      result.addValidationMessage(1, Level.ERROR, "URL retriever",
          "Unable to load content from " + input);
      return result;
    }
    result.recordTiming("Feed URL detection/loading");

    // Is the input a feed?
    SyndFeed feed;
    try {
      feed = capFeedParser.parseFeed(input);
    } catch (IllegalArgumentException e) {
      feed = null;
    } catch (FeedException e) {
      log.info("FeedRequest: Syntax Error");
      result.addValidationMessage(e);
      return result;
    } catch (CapFeedException e) {
      log.info("FeedRequest: Error");
      result.addValidationMessages(e);
      return result;
    }
    result.recordTiming("Feed detection/parsing");

    if (feed == null) {
      // Is the input CAP?
      try {
        Reasons.Builder reasonsBuilder = Reasons.newBuilder();
        Alert alert = capFeedParser.parseAlert(input, reasonsBuilder);
        result.addValidationMessages(reasonsBuilder.build());
        checkProfiles(
            alert, result, profiles, -1 /* entryIndex */, null /* linkUrl */, null);
      } catch (CapException ce) {
        log.info("CAPRequest: Error");
        result.addValidationMessages(ce);
      } catch (NotCapException nce) {
        log.info("InvalidRequest");
        result.addValidationMessage(1, Level.ERROR, "CAP", "The input must be "
            + "a CAP 1.0, 1.1, or 1.2 message or an RSS, Atom or EDXL-DE feed of CAP "
            + "messages");
        return result;
      }
      result.recordTiming("Alert parsing");
    } else {
      List<SyndEntry> entries = feed.getEntries();
      log.info("FeedRequest: " + entries.size() + " entries");
      
      if (entries.isEmpty()) {
        result.addValidationMessage(1, Level.ERROR, "CAP", "The input must be "
            + "a CAP 1.0, 1.1, or 1.2 message or an RSS, Atom or EDXL-DE feed of CAP "
            + "messages");
      }
      
      for (int i = 0; i < entries.size(); i++) {

        SyndEntry entry = entries.get(i);
        try {
          Reasons.Builder reasonsBuilder = Reasons.newBuilder();
          Alert alert = capFeedParser.parseAlert(entry, reasonsBuilder);
          
          result.addValidationMessages(
              prefixReasonForContent(reasonsBuilder.build(), i, feed));
          checkProfiles(alert, result, profiles, i, null, feed);
          result.recordTiming("Alert parsing");
        } catch (CapException e) {
          result.addValidationMessages(
              prefixReasonForContent(e.getReasons(), i, feed));
        } catch (NotCapException e) {
          // No CAP in the <content> field, maybe there's a link to an alert?
          Alert alert = handleThinEntry(entry, result);
          checkProfiles(alert, result, profiles, i, capFeedParser.getCapUrl(entry), feed);
          result.recordTiming(
              String.valueOf(capFeedParser.getCapUrl(entry)) + "load/parse");
        }
      }

      result.addValidationMessages(new CapFeedValidator().validate(feed));
      result.recordTiming("Feed recommendations");
    }

    return result;
  }

  private Reasons prefixReasonForContent(Reasons reasons, int entryIndex, SyndFeed syndFeed) {
    if (syndFeed.originalWireFeed() instanceof Feed) {
      return reasons.prefixWithXpath("/feed[1]/entry[" + (entryIndex + 1) + "]/content[1]");
    } else if (syndFeed.originalWireFeed() instanceof DistributionFeed) {
      return reasons.prefixWithXpath("/EDXLDistribution[1]/contentObject[" + (entryIndex + 1)
          + "]/xmlContent[1]/embeddedXMLContent[1]");
    }

    throw new IllegalArgumentException();
  }
  
  private void checkProfiles(Alert alert, ValidationResult result,
      Set<CapProfile> profiles, int entryIndex, String linkUrl, SyndFeed syndFeed) {
    if (alert == null) {
      return;
    }
    result.addValidAlert(alert);

    for (CapProfile profile: profiles) {
      Reasons reasons = profile.validate(alert);
      if (linkUrl != null) {
        result.addValidationMessageForLink(linkUrl, reasons);
      } else {
        
        if (entryIndex >= 0) {
          reasons = prefixReasonForContent(reasons, entryIndex, syndFeed);
        }
        result.addValidationMessages(reasons);
      }
    }
  }

  private Alert handleThinEntry(SyndEntry entry, ValidationResult result) {
    String capUrl = capFeedParser.getCapUrl(entry);
    if (capUrl == null) {
      // Feed parser already added an error
      return null;
    }

    long reqTime = System.currentTimeMillis() - result.getStartTimeMillis();
    if (reqTime > REQUEST_DEADLINE_MS) {
      result.addValidationMessageForLink(capUrl, Level.ERROR, "",
          "Validate request timed out before loading: " + capUrl);
      return null;
    }

    String cap;
    try {
      cap = loadUrl(capUrl);
    } catch (IOException e) {
      result.addValidationMessageForLink(capUrl, Level.ERROR, "URL retriever",
          "Unable to load CAP link: " + capUrl);
      return null;
    }

    try {
      return capFeedParser.parseAlert(cap);
    } catch (CapException e) {
      result.addValidationMessageForLink(capUrl, e.getReasons());
      return null;
    } catch (NotCapException e) {
      result.addValidationMessageForLink(
          capUrl, Level.ERROR, "CAP", "Link does not point to a CAP message");
      return null;
    }
  }

  String loadUrl(String stringUrl) throws IOException {
    URL url = new URL(stringUrl);
    URLConnection urlConnection = url.openConnection();

    if (url.getUserInfo() != null) {
      String encodedUserInfo =
          new String(Base64.encode(url.getUserInfo().getBytes()));
        urlConnection.setRequestProperty(
            "Authorization", "Basic " + encodedUserInfo);
    }

    // Stripping Unicode Character 65279 (BOM), added by NotePad
    return CharMatcher.anyOf("\uFEFF").removeFrom(
        ValidatorUtil.readFully(urlConnection.getInputStream()));
  }
}

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
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.NotCapException;
import com.google.publicalerts.cap.feed.CapFeedException;
import com.google.publicalerts.cap.feed.CapFeedParser;
import com.google.publicalerts.cap.feed.CapFeedValidator;
import com.google.publicalerts.cap.profile.CapProfile;
import com.google.publicalerts.cap.profile.CapProfiles;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;

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

  private final CapFeedParser capParser;

  public CapValidator() {
    this.capParser = new CapFeedParser(true);
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
      result.addError(1, 1, "Unable to load content from " + input);
      return result;
    }
    result.recordTiming("Feed URL detection/loading");

    // Is the input a feed?
    SyndFeed feed;
    try {
      feed = capParser.parseFeed(input);
    } catch (IllegalArgumentException e) {
      feed = null;
    } catch (FeedException e) {
      log.info("FeedRequest: Syntax Error");
      result.addFeedError(e);
      return result;
    } catch (CapFeedException e) {
      log.info("FeedRequest: Error");
      result.addFeedError(e);
      return result;
    }
    result.recordTiming("Feed detection/parsing");

    if (feed == null) {
      // Is the input CAP?
      try {
        Alert alert = capParser.parseAlert(input);
        log.info("CAPRequest: Success");
        checkProfiles(
            alert, result, profiles, -1 /* entryIndex */, null /* linkUrl */);
      } catch (CapException ce) {
        log.info("CAPRequest: Error");
        result.addError(ce);
      } catch (NotCapException nce) {
        log.info("InvalidRequest");
        result.addError(1, 1, "The input must be a CAP 1.0, 1.1, " +
            "or 1.2 message or an RSS or Atom feed of CAP messages");
        return result;
      }
      result.recordTiming("Alert parsing");
    } else {
      @SuppressWarnings("unchecked")
      List<SyndEntry> entries = (List<SyndEntry>) feed.getEntries();
      log.info("FeedRequest: " + entries.size() + " entries");
      for (int i = 0; i < entries.size(); i++) {
        SyndEntry entry = entries.get(i);
        try {
          Alert alert = capParser.parseAlert(entry);
          checkProfiles(alert, result, profiles, i, null);
          result.recordTiming("Alert parsing");
        } catch (CapException e) {
          result.addCapContentError(e, i);
        } catch (NotCapException e) {
          // No CAP in the <content> field, maybe there's a link to an alert?
          Alert alert = handleThinEntry(entry, result);
          checkProfiles(alert, result, profiles, i, capParser.getCapUrl(entry));
          result.recordTiming(
              String.valueOf(capParser.getCapUrl(entry)) + "load/parse");
        }
      }

      result.addFeedRecommendations(
          new CapFeedValidator().checkForRecommendations(feed));
      result.recordTiming("Feed recommendations");
    }

    return result;
  }

  private void checkProfiles(Alert alert, ValidationResult result,
      Set<CapProfile> profiles, int entryIndex, String linkUrl) {
    if (alert == null) {
      return;
    }
    result.addValidAlert(alert);

    for (CapProfile profile: profiles) {
      List<Reason> errors = profile.checkForErrors(alert);
      List<Reason> suggestions = profile.checkForRecommendations(alert);
      if (linkUrl != null) {
        result.addProfileResult(profile, linkUrl, errors, suggestions);
      } else {
        result.addProfileResult(profile, entryIndex, errors, suggestions);
      }
    }
  }

  private Alert handleThinEntry(SyndEntry entry, ValidationResult result) {
    String capUrl = capParser.getCapUrl(entry);
    if (capUrl == null) {
      // Feed parser already added an error
      return null;
    }

    long reqTime = System.currentTimeMillis() - result.getStartTimeMillis();
    if (reqTime > REQUEST_DEADLINE_MS) {
      result.addCapLinkError(capUrl,
          "Validate request timed out before loading: " + capUrl);
      return null;
    }

    String cap;
    try {
      cap = loadUrl(capUrl);
    } catch (IOException e) {
      result.addCapLinkError(capUrl, "Unable to load CAP link: " + capUrl);
      return null;
    }

    try {
      return capParser.parseAlert(cap);
    } catch (CapException e) {
      result.addCapLinkError(capUrl, e);
      return null;
    } catch (NotCapException e) {
      result.addCapLinkError(capUrl, "Link does not point to a CAP message");
      return null;
    }
  }

  String loadUrl(String capUrl) throws IOException {
    URL url = new URL(capUrl);
    return ValidatorUtil.readFully(url.openStream());
  }
}

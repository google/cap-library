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

package com.google.publicalerts.cap.validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
 * Main servlet for the CAP validator.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidatorServlet extends HttpServlet {
  private static final long serialVersionUID = 7456177189138605168L;

  private static final Logger log =
      Logger.getLogger(CapValidatorServlet.class.getName());

  private static final int REQUEST_DEADLINE_MS = 20000;
  private static final String GOOGLE_ANALYTICS_ID = "google_analytics_id";

  private final CapFeedParser capParser;
  private final ServletFileUpload upload;

  public CapValidatorServlet() {
    this.capParser = new CapFeedParser(true);
    this.upload = new ServletFileUpload();
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    req.setAttribute("input", "");
    req.setAttribute("profiles", getProfilesJsp(null));
    render(req, resp);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String input = null;
    String fileInput = null;
    String example = null;
    Set<CapProfile> profiles = Sets.newHashSet();

    try {
      FileItemIterator itemItr = upload.getItemIterator(req);
      while (itemItr.hasNext()) {
        FileItemStream item = itemItr.next();
        if ("input".equals(item.getFieldName())) {
          input = readFully(item.openStream());
        } else if (item.getFieldName().startsWith("inputfile")) {
          fileInput = readFully(item.openStream());
        } else if ("example".equals(item.getFieldName())) {
          example = readFully(item.openStream());
        } else if ("profile".equals(item.getFieldName())) {
          String profileCode = readFully(item.openStream());
          for (CapProfile profile : CapProfiles.getProfiles()) {
          if (profile.getCode().equals(profileCode)) {
              profiles.add(profile);
            }
          }
        }
      }
    } catch (FileUploadException e) {
      throw new ServletException(e);
    }

    if (!CapUtil.isEmptyOrWhitespace(example)) {
      log.info("ExampleRequest: " + example);
      input = loadExample(example);
      profiles = ImmutableSet.of();
    } else if (!CapUtil.isEmptyOrWhitespace(fileInput)) {
      log.info("FileInput");
      input = fileInput;
    }

    input = (input == null) ? "" : input.trim();
    if ("".equals(input)) {
      log.info("EmptyRequest");
      doGet(req, resp);
      return;
    }

    ValidationResult result = handleInput(input, profiles);

    req.setAttribute("input", input);
    req.setAttribute("profiles", getProfilesJsp(profiles));
    req.setAttribute("errors", result.getByLineErrorMap());
    req.setAttribute("recommendations", result.getByLineRecommendationMap());
    req.setAttribute("lines", Arrays.asList(result.getInput().split("\n")));
    req.setAttribute("timing", result.getTiming());
    JSONArray alertsJs =
        new MapVisualizer(result.getValidAlerts()).getAlertsJs();
    if (alertsJs != null) {
      req.setAttribute("alertsJs", alertsJs.toString());
    }
    render(req, resp);
  }

  ValidationResult handleInput(String input, Set<CapProfile> profiles) {
    // Input could be a feed of CAP alerts, a feed of links to CAP alerts,
    // a list of CAP alerts, a URL pointing to any of the above, or gibberish

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
        checkProfiles(alert, result, profiles, -1, null);
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
    return readFully(url.openStream());
  }

  String loadExample(String filename) throws IOException {
    InputStream stream = CapValidatorServlet.class.getResourceAsStream(
        "examples/" + filename);
    return stream == null ? "" : readFully(stream);
  }

  String readFully(InputStream stream) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(stream,
        Charset.forName("UTF-8")));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line).append('\n');
    }
    if (sb.length() > 0) {
      sb.setLength(sb.length() - 1);
    }
    return sb.toString();
  }

  List<String[]> getProfilesJsp(Set<CapProfile> selected) {
    selected = selected == null ? ImmutableSet.<CapProfile>of() : selected;
    List<String[]> ret = Lists.newArrayList();
    for (CapProfile profile : CapProfiles.getProfiles()) {
      String checked = (selected.contains(profile) ? "checked" : "");
      ret.add(new String[] {profile.getCode(), checked,
          profile.getDocumentationUrl(), profile.getName()});
    }
    return ret;
  }

  private void render(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    req.setAttribute("analyticsId", System.getProperty(GOOGLE_ANALYTICS_ID));
    resp.setCharacterEncoding("UTF-8");
    resp.setContentType("text/html");
    addNoCachableHeaders(resp);

    resp.setStatus(HttpServletResponse.SC_OK);
    req.getRequestDispatcher("/validator.jsp").include(req, resp);
  }

  private void addNoCachableHeaders(HttpServletResponse resp) {
    resp.setHeader("Cache-Control",
        "no-cache, no-store, max-age=0, must-revalidate");
    resp.setHeader("Pragma", "no-cache");
    resp.setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
    resp.setDateHeader("Date", System.currentTimeMillis());
  }
}

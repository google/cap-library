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
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.profile.CapProfile;
import com.google.publicalerts.cap.profile.CapProfiles;

/**
 * Validator utils, common to multiple servlets.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class ValidatorUtil {

  public static final String GOOGLE_ANALYTICS_ID = "google_analytics_id";
  public static final String ALERT_HUB_SECRET = "alert_hub_secret";
  public static final String ALERT_HUB_URL = "alert_hub_url";

  private static final Charset UTF8 = Charset.forName("UTF-8");

  /**
   * Returns the given set of selected cap profiles in a format
   * suitable for displaying in the validator JSPs.
   *
   * @param selected the selected CAP profiles, may be empty
   * @return the JSP-formatted profiles
   */
  public static List<String[]> getProfilesJsp(Set<CapProfile> selected) {
    selected = selected == null ? ImmutableSet.<CapProfile>of() : selected;
    List<String[]> ret = Lists.newArrayList();
    for (CapProfile profile : CapProfiles.getProfiles()) {
      String checked = (selected.contains(profile) ? "checked" : "");
      ret.add(new String[] {profile.getCode(), checked,
          profile.getDocumentationUrl(), profile.getName()});
    }
    return ret;
  }

  public static Set<CapProfile> parseProfiles(String profileStr) {
    if (CapUtil.isEmptyOrWhitespace(profileStr)) {
      return ImmutableSet.of();
    }

    Splitter splitter = Splitter.on(",").trimResults().omitEmptyStrings();
    List<String> profileList = Lists.newArrayList(splitter.split(profileStr));
    Set<CapProfile> profiles = Sets.newHashSet();
    for (CapProfile profile : CapProfiles.getProfiles()) {
      for (String p : profileList) {
        if (profile.getCode().equals(p)) {
	    profiles.add(profile);
	}
      }
    }
    return profiles;
  }

  /**
   * Adds headers to the given response to mark it as not cacheable.
   *
   * @param resp the response to add headers to.
   */
  public static void addNoCachableHeaders(HttpServletResponse resp) {
    resp.setHeader("Cache-Control",
        "no-cache, no-store, max-age=0, must-revalidate");
    resp.setHeader("Pragma", "no-cache");
    resp.setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
    resp.setDateHeader("Date", System.currentTimeMillis());
  }

  /**
   * Creates a PSHB callback url given the request url, topic url, and email.
   *
   * @param reqUrl the URL of the request to the validator
   * @param topic the URL of the feed on whose behalf we are subscribing the
   *   validator
   * @param email the email address to send errors
   */
  public static String toCallbackUrl(String reqUrl, String topic, String email) {
    try {
      return reqUrl + "?topic=" + URLEncoder.encode(topic, "UTF-8") +
          "&email=" + URLEncoder.encode(email, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads the given stream to a string and closes it.
   *
   * @param stream the stream to read
   * @throws IOException on error reading from the stream
   */
  public static String readFully(InputStream stream) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(stream,
        UTF8));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line).append('\n');
    }
    if (sb.length() > 0) {
      sb.setLength(sb.length() - 1);
    }
    stream.close();
    return sb.toString();
  }


  private ValidatorUtil() {}
}

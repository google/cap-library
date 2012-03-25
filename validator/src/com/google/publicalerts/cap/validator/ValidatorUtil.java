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

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
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

  public static void addNoCachableHeaders(HttpServletResponse resp) {
    resp.setHeader("Cache-Control",
        "no-cache, no-store, max-age=0, must-revalidate");
    resp.setHeader("Pragma", "no-cache");
    resp.setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
    resp.setDateHeader("Date", System.currentTimeMillis());
  }

  public static String toCallbackUrl(String reqUrl, String topic, String email) {
    try {
      return reqUrl + "?topic=" + URLEncoder.encode(topic, "UTF-8") +
          "&email=" + URLEncoder.encode(email, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private ValidatorUtil() {}
}

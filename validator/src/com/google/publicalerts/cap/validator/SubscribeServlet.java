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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.publicalerts.cap.profile.CapProfile;

/**
 * Handles subscribing and unsubscribing an email address to
 * receive mail when alerts in the feed are invalid.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class SubscribeServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private final HttpPoster poster;

  public SubscribeServlet() {
    this(new HttpPoster());
  }

  public SubscribeServlet(HttpPoster poster) {
    this.poster = poster;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String topic = Strings.nullToEmpty(req.getParameter("topic"));
    String email = Strings.nullToEmpty(req.getParameter("email"));
    String profileStr = req.getParameter("profiles");

    req.setAttribute("msg", null);
    req.setAttribute("topic", topic);
    req.setAttribute("email", email);
    req.setAttribute("profiles", ValidatorUtil.getProfilesJsp(
        ValidatorUtil.parseProfiles(profileStr)));
    render(req, resp);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String topic = Strings.nullToEmpty(req.getParameter("topic"));
    String email = Strings.nullToEmpty(req.getParameter("email"));
    String profileStr = req.getParameter("profiles");
    String button = Strings.nullToEmpty(req.getParameter("submit"));

    // TODO(shakusa) Validate email

    button = button.toLowerCase();
    Set<CapProfile> profiles = ValidatorUtil.parseProfiles(profileStr);

    String requestUrl = req.getRequestURL().toString();
    String msg = subscribe(button, topic, requestUrl, email, profiles);

    req.setAttribute("msg", msg);
    req.setAttribute("topic", topic);
    req.setAttribute("email", email);
    req.setAttribute("profiles", ValidatorUtil.getProfilesJsp(profiles));
    render(req, resp);
  }

  String subscribe(String mode, String topic, String reqUrl, String email,
      Set<CapProfile> profiles) {
    Map<String, String> params = Maps.newHashMap();

    reqUrl = reqUrl.replace("/subscribe", "/pshb");
    params.put("hub.callback",
        ValidatorUtil.toCallbackUrl(reqUrl, topic, email));
    params.put("hub.topic", topic);
    params.put("hub.secret",
        System.getProperty(ValidatorUtil.ALERT_HUB_SECRET));
    params.put("hub.mode", mode);
    params.put("hub.verify", "sync");
    params.put("hub.verify_token",
        System.getProperty(ValidatorUtil.ALERT_HUB_SECRET));

    boolean success = false;
    String errorMsg = "";
    try {
      int returnCode = poster.post(
          System.getProperty(ValidatorUtil.ALERT_HUB_URL) + "/subscribe",
          params);
      if (returnCode >= 200 && returnCode <= 300) {
        success = true;
      }
    } catch (IOException e) {
      success = false;
    }
    return success
        ? mode + " successful for (" + email  + ", "+ topic + ")"
        : mode + " failed.  Please try again [" + errorMsg + "]";
  }


  void render(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    req.setAttribute("analyticsId",
        System.getProperty(ValidatorUtil.GOOGLE_ANALYTICS_ID));
    resp.setCharacterEncoding("UTF-8");
    resp.setContentType("text/html");
    ValidatorUtil.addNoCachableHeaders(resp);

    resp.setStatus(HttpServletResponse.SC_OK);
    req.getRequestDispatcher("/subscribe.jsp").include(req, resp);
  }
}

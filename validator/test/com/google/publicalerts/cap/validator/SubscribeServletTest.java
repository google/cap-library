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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.google.publicalerts.cap.profile.CapProfile;

import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link SubscribeServlet}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class SubscribeServletTest extends TestCase {

  private HttpPoster httpPoster;
  private SubscribeServlet servlet;

  private HttpServletRequest req;
  private HttpServletResponse resp;

  @Override
  public void setUp() throws Exception {
    httpPoster = mock(HttpPoster.class);
    servlet = new SubscribeServlet(httpPoster);

    System.setProperty(ValidatorUtil.ALERT_HUB_URL,
        "https://alert-hub.appspot.com");
    req = mock(HttpServletRequest.class);
    resp = mock(HttpServletResponse.class);
  }

  public void testPost_invalidEmail() {
    // TODO(shakusa)
  }

  @SuppressWarnings("unchecked")
  public void testSubscribe() throws Exception {
    Set<CapProfile> profiles = Sets.newHashSet();

    ArgumentCaptor<Map> paramsCaptor =
        ArgumentCaptor.forClass(Map.class);
    when(httpPoster.post(
        eq("https://alert-hub.appspot.com/subscribe"), paramsCaptor.capture()))
        .thenReturn(HttpServletResponse.SC_OK);

    String result = servlet.subscribe("subscribe", "http://feed.com/feed",
        "http://localhost/subscribe", "test@example.com", profiles);
    assertTrue(result.contains("successful"));

    Map<String, String> params = (Map<String, String>) paramsCaptor.getValue();
    assertEquals("subscribe", params.get("hub.mode"));
    assertEquals(
        "http://localhost/pshb?topic=http%3A%2F%2Ffeed.com%2Ffeed" +
        "&email=test%40example.com",
        params.get("hub.callback"));
  }

  @SuppressWarnings("unchecked")
  public void testUnsubscribe() throws Exception {
    Set<CapProfile> profiles = Sets.newHashSet();

    ArgumentCaptor<Map> paramsCaptor =
        ArgumentCaptor.forClass(Map.class);
    when(httpPoster.post(
        eq("https://alert-hub.appspot.com/subscribe"), paramsCaptor.capture()))
        .thenReturn(HttpServletResponse.SC_OK);

    String result = servlet.subscribe("unsubscribe", "http://feed.com/feed",
        "http://localhost/subscribe", "test@example.com", profiles);
    assertTrue(result.contains("successful"));

    Map<String, String> params = (Map<String, String>) paramsCaptor.getValue();
    assertEquals("unsubscribe", params.get("hub.mode"));
    assertEquals(
        "http://localhost/pshb?topic=http%3A%2F%2Ffeed.com%2Ffeed" +
        "&email=test%40example.com",
        params.get("hub.callback"));
  }
}

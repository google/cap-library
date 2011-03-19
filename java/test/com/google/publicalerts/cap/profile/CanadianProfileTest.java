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

package com.google.publicalerts.cap.profile;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.Alert.MsgType;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapXmlParser;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.feed.TestResources;
import com.google.publicalerts.cap.profile.CanadianProfile.ErrorType;
import com.google.publicalerts.cap.profile.CanadianProfile.RecommendationType;

/**
 * Tests for {@link CanadianProfile}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CanadianProfileTest extends CapProfileTestCase {

  public CanadianProfileTest(String name) {
    super(name);
  }

  @Override
  public void setUp() {
    profile = new CanadianProfile();
  }

  public void testParseFrom() throws Exception {
    runTestParseFrom("earthquake.cap");
  }

  public void testCheckForErrors() throws Exception {
    Alert.Builder alert = loadAlert("canada.cap").toBuilder();
    assertNoErrors(alert);

    alert.clearCode();
    assertErrors(alert, ErrorType.VERSION_CODE_REQUIRED);

    alert.setMsgType(MsgType.Update)
        .clearReferences();
    assertErrors(alert, ErrorType.VERSION_CODE_REQUIRED,
        ErrorType.UPDATE_OR_CANCEL_MUST_REFERENCE);

    alert = loadAlert("canada.cap").toBuilder();
    alert.getInfoBuilder(0).clearEventCode();
    assertErrors(alert, ErrorType.EVENT_CODES_MUST_MATCH,
        ErrorType.RECOGNIZED_EVENT_CODE_REQUIRED);

    alert = loadAlert("canada.cap").toBuilder();
    alert.getInfoBuilder(1).getAreaBuilder(0).clearGeocode();
    assertErrors(alert, ErrorType.AREA_GEOCODE_IS_REQUIRED);

    alert.getInfoBuilder(1).clearArea();
    assertErrors(alert, ErrorType.AREA_IS_REQUIRED);

    alert = loadAlert("canada.cap").toBuilder();
    alert.clearInfo();
    assertErrors(alert, ErrorType.IS_REQUIRED);
    alert.setMsgType(MsgType.Ack);
    assertNoErrors(alert);
  }

  public void testCheckForRecommendations() throws Exception {
    Alert.Builder alert = loadAlert("canada.cap").toBuilder();
    assertNoRecommendations(alert);

    alert = loadAlert("canada.cap").toBuilder();
    alert.setSent("2002-01-01T00:00:00+00:00")
        .getInfoBuilder(0)
        .setEffective("2003-01-01T00:00:00+00:00")
        .setOnset("2004-01-01T00:00:00+00:00")
        .setExpires("2005-01-01T00:00:00+00:00");
    assertRecommendations(alert,
        RecommendationType.SENT_INCLUDE_TIMEZONE_OFFSET,
        RecommendationType.EFFECTIVE_INCLUDE_TIMEZONE_OFFSET,
        RecommendationType.ONSET_INCLUDE_TIMEZONE_OFFSET,
        RecommendationType.EXPIRES_INCLUDE_TIMEZONE_OFFSET);

    alert = loadAlert("canada.cap").toBuilder();
    alert.getInfoBuilder(0)
        .setLanguage("es")
        .clearExpires()
        .clearSenderName()
        .clearResponseType()
        .clearInstruction();
    assertRecommendations(alert,
        RecommendationType.ENGLISH_AND_FRENCH,
        RecommendationType.EXPIRES_STRONGLY_RECOMMENDED,
        RecommendationType.SENDER_NAME_STRONGLY_RECOMMENDED,
        RecommendationType.RESPONSE_TYPE_STRONGLY_RECOMMENDED,
        RecommendationType.INSTRUCTION_STRONGLY_RECOMMENDED);

    alert = loadAlert("canada.cap").toBuilder();
    Info.Builder info = alert.getInfoBuilder(0);
    for (Area.Builder area : info.getAreaBuilderList()) {
      area.clearPolygon().clearCircle();
    }
    assertRecommendations(alert,
        RecommendationType.CIRCLE_POLYGON_ENCOURAGED);
  }
}

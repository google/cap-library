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

package com.google.publicalerts.cap.profile.au;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.Alert.MsgType;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.Group;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.ValuePair;
import com.google.publicalerts.cap.profile.au.AustralianProfile.ErrorType;
import com.google.publicalerts.cap.profile.au.AustralianProfile.RecommendationType;
import com.google.publicalerts.cap.profile.CapProfileTestCase;

/**
 * Tests for {@link AustralianProfile}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class AustralianProfileTest extends CapProfileTestCase {

  public AustralianProfileTest(String name) {
    super(name);
  }

  @Override
  public void setUp() {
    profile = new AustralianProfile();
  }

  public void testParseFrom() throws Exception {
    runTestParseFrom("earthquake.cap");
  }

  public void testCheckForErrors() throws Exception {
    Alert.Builder alert = loadAlert("australia.cap").toBuilder();
    assertNoErrors(alert);

    alert.clearCode();
    assertErrors(alert, ErrorType.VERSION_CODE_REQUIRED);

    alert.setMsgType(MsgType.UPDATE)
        .clearReferences();
    assertErrors(alert, ErrorType.VERSION_CODE_REQUIRED,
        ErrorType.UPDATE_OR_CANCEL_MUST_REFERENCE);

    alert = loadAlert("australia.cap").toBuilder();
    alert.setMsgType(Alert.MsgType.CANCEL)
        .setReferences(Group.newBuilder().addValue("a,b,c"));
    // twice, once per <info>
    assertErrors(alert, ErrorType.DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL,
        ErrorType.DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearCategory();
    assertErrors(alert, ErrorType.CATEGORIES_MUST_MATCH);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).setEvent("foo");
    assertErrors(alert, ErrorType.EVENTS_IN_SAME_LANGUAGE_MUST_MATCH,
        ErrorType.EVENT_AND_EVENT_CODE_MUST_MATCH);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValueName("foo");
    assertErrors(alert, ErrorType.EVENT_CODES_MUST_MATCH);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(0).clearEventCode();
    alert.getInfoBuilder(1).clearEventCode();
    assertErrors(alert, ErrorType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT,
        ErrorType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(0).getEventCodeBuilder(0).setValue("foo");
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValue("foo");
    assertErrors(alert, ErrorType.UNRECOGNIZED_EVENT_CODE,
        ErrorType.UNRECOGNIZED_EVENT_CODE);

    alert = loadAlert("australia.cap").toBuilder();
    ValuePair secondEventCode = ValuePair.newBuilder().setValueName(
        AustralianProfile.CAP_AU_EVENT_CODE_VALUE_NAME)
        .setValue("foo")
        .build();
    alert.getInfoBuilder(0).addEventCode(secondEventCode);
    alert.getInfoBuilder(1).addEventCode(secondEventCode);
    assertErrors(alert, ErrorType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT,
        ErrorType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearArea();
    assertErrors(alert, ErrorType.AREA_IS_REQUIRED);

    alert = loadAlert("australia.cap").toBuilder();
    alert.clearInfo();
    assertErrors(alert, ErrorType.INFO_IS_REQUIRED);
    alert.setMsgType(MsgType.ACK);
    assertNoErrors(alert);
  }

  public void testCheckForRecommendations() throws Exception {
    Alert.Builder alert = loadAlert("australia.cap").toBuilder();
    assertNoRecommendations(alert);

    alert = loadAlert("australia.cap").toBuilder();
    alert.setSender("foo")
        .setStatus(Alert.Status.TEST)
        .removeInfo(1)
        .getInfoBuilder(0)
        .setLanguage("es")
        .clearExpires()
        .clearSenderName()
        .clearResponseType()
        .clearInstruction();
    assertRecommendations(alert,
        RecommendationType.SENDER_SHOULD_BE_EMAIL,
        RecommendationType.TEST_ALERT_WILL_NOT_BE_BROADCAST,
        RecommendationType.EXPIRES_STRONGLY_RECOMMENDED,
        RecommendationType.SENDER_NAME_STRONGLY_RECOMMENDED,
        RecommendationType.RESPONSE_TYPE_STRONGLY_RECOMMENDED,
        RecommendationType.INSTRUCTION_STRONGLY_RECOMMENDED);

    alert = loadAlert("australia.cap").toBuilder();
    Info.Builder info = alert.getInfoBuilder(0);
    for (Area.Builder area : info.getAreaBuilderList()) {
      area.clearPolygon().clearCircle();
    }
    assertRecommendations(alert,
        RecommendationType.CIRCLE_POLYGON_ENCOURAGED);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).getAreaBuilder(0).getGeocodeBuilder(0)
        .setValueName("foo");
    assertRecommendations(alert,
        RecommendationType.AREA_GEOCODE_IS_RECOMMENDED);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).getAreaBuilder(0).clearGeocode();
    assertRecommendations(alert,
        RecommendationType.AREA_GEOCODE_IS_RECOMMENDED);
  }
}

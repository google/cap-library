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

import static com.google.publicalerts.cap.Reason.Level.ERROR;
import static com.google.publicalerts.cap.Reason.Level.RECOMMENDATION;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.AREA_GEOCODE_IS_RECOMMENDED;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.AREA_IS_REQUIRED;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.CATEGORIES_MUST_MATCH;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.CIRCLE_POLYGON_ENCOURAGED;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.EVENTS_IN_SAME_LANGUAGE_MUST_MATCH;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.EVENT_AND_EVENT_CODE_MUST_MATCH;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.EVENT_CODES_MUST_MATCH;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.EXPIRES_STRONGLY_RECOMMENDED;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.INFO_IS_REQUIRED;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.INSTRUCTION_STRONGLY_RECOMMENDED;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.RESPONSE_TYPE_STRONGLY_RECOMMENDED;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.SENDER_NAME_STRONGLY_RECOMMENDED;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.SENDER_SHOULD_BE_EMAIL;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.TEST_ALERT_WILL_NOT_BE_BROADCAST;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.UNRECOGNIZED_EVENT_CODE;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE;
import static com.google.publicalerts.cap.profile.au.AustralianProfile.ReasonType.VERSION_CODE_REQUIRED;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.Alert.MsgType;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.Group;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.ValuePair;
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

  public void testValidate_errors() throws Exception {
    Alert.Builder alert = loadAlert("australia.cap").toBuilder();
    assertNoReasons(alert, ERROR);

    alert.clearCode();
    assertReasons(alert, ERROR, new Reason("/alert[1]", VERSION_CODE_REQUIRED));

    alert.setMsgType(MsgType.UPDATE)
        .clearReferences();
    assertReasons(alert, ERROR,
        new Reason("/alert[1]", VERSION_CODE_REQUIRED),
        new Reason("/alert[1]/msgType[1]", UPDATE_OR_CANCEL_MUST_REFERENCE));

    alert = loadAlert("australia.cap").toBuilder();
    alert.setMsgType(Alert.MsgType.CANCEL).setReferences(Group.newBuilder().addValue("a,b,c"));
    // twice, once per <info>
    assertReasons(alert, ERROR,
        new Reason("/alert[1]/info[1]", DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL),
        new Reason("/alert[1]/info[2]", DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearCategory();
    assertReasons(alert, ERROR, new Reason("/alert[1]/info[2]", CATEGORIES_MUST_MATCH));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).setEvent("foo");
    assertReasons(alert, ERROR,
        new Reason("/alert[1]/info[2]", EVENTS_IN_SAME_LANGUAGE_MUST_MATCH),
        new Reason("/alert[1]/info[2]", EVENT_AND_EVENT_CODE_MUST_MATCH));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValueName("foo");
    assertReasons(alert, ERROR, new Reason("/alert[1]/info[2]", EVENT_CODES_MUST_MATCH));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(0).clearEventCode();
    alert.getInfoBuilder(1).clearEventCode();
    assertReasons(alert, ERROR,
        new Reason("/alert[1]/info[1]", ONE_AUTHORIZED_EVENT_CODE_PER_ALERT),
        new Reason("/alert[1]/info[2]", ONE_AUTHORIZED_EVENT_CODE_PER_ALERT));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(0).getEventCodeBuilder(0).setValue("foo");
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValue("foo");
    assertReasons(alert, ERROR, 
        new Reason("/alert[1]/info[1]", UNRECOGNIZED_EVENT_CODE),
        new Reason("/alert[1]/info[2]", UNRECOGNIZED_EVENT_CODE));

    alert = loadAlert("australia.cap").toBuilder();
    ValuePair secondEventCode = ValuePair.newBuilder().setValueName(
        AustralianProfile.CAP_AU_EVENT_CODE_VALUE_NAME)
        .setValue("foo")
        .build();
    alert.getInfoBuilder(0).addEventCode(secondEventCode);
    alert.getInfoBuilder(1).addEventCode(secondEventCode);
    assertReasons(alert, ERROR, 
        new Reason("/alert[1]/info[1]", ONE_AUTHORIZED_EVENT_CODE_PER_ALERT),
        new Reason("/alert[1]/info[2]", ONE_AUTHORIZED_EVENT_CODE_PER_ALERT));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearArea();
    assertReasons(alert, ERROR, new Reason("/alert[1]/info[2]", AREA_IS_REQUIRED));

    alert = loadAlert("australia.cap").toBuilder();
    alert.clearInfo();
    assertReasons(alert, ERROR, new Reason("/alert[1]", INFO_IS_REQUIRED));
    alert.setMsgType(MsgType.ACK);
    assertNoReasons(alert, ERROR);
  }

  public void testValidate_recommendations() throws Exception {
    Alert.Builder alert = loadAlert("australia.cap").toBuilder();
    assertReasons(alert, RECOMMENDATION);

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
    assertReasons(alert, RECOMMENDATION,
        new Reason("/alert[1]/sender[1]", SENDER_SHOULD_BE_EMAIL),
        new Reason("/alert[1]/status[1]", TEST_ALERT_WILL_NOT_BE_BROADCAST),
        new Reason("/alert[1]/info[1]", EXPIRES_STRONGLY_RECOMMENDED),
        new Reason("/alert[1]/info[1]", SENDER_NAME_STRONGLY_RECOMMENDED),
        new Reason("/alert[1]/info[1]", RESPONSE_TYPE_STRONGLY_RECOMMENDED),
        new Reason("/alert[1]/info[1]", INSTRUCTION_STRONGLY_RECOMMENDED));

    alert = loadAlert("australia.cap").toBuilder();
    Info.Builder info = alert.getInfoBuilder(0);
    for (Area.Builder area : info.getAreaBuilderList()) {
      area.clearPolygon().clearCircle();
    }
    assertReasons(alert, RECOMMENDATION,
        new Reason("/alert[1]/info[1]/area[1]", CIRCLE_POLYGON_ENCOURAGED));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).getAreaBuilder(0).getGeocodeBuilder(0)
        .setValueName("foo");
    assertReasons(alert, RECOMMENDATION,
        new Reason("/alert[1]/info[2]/area[1]", AREA_GEOCODE_IS_RECOMMENDED));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).getAreaBuilder(0).clearGeocode();
    assertReasons(alert, RECOMMENDATION,
        new Reason("/alert[1]/info[2]/area[1]", AREA_GEOCODE_IS_RECOMMENDED));
  }
}

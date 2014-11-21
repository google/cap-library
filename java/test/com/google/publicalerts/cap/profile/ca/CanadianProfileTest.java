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

package com.google.publicalerts.cap.profile.ca;

import static com.google.publicalerts.cap.Reason.Level.ERROR;
import static com.google.publicalerts.cap.Reason.Level.RECOMMENDATION;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.AREA_GEOCODE_IS_REQUIRED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.AREA_IS_REQUIRED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.CIRCLE_POLYGON_ENCOURAGED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.ENGLISH_AND_FRENCH;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.EVENT_CODES_MUST_MATCH;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.EXPIRES_STRONGLY_RECOMMENDED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.INSTRUCTION_STRONGLY_RECOMMENDED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.IS_REQUIRED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.RECOGNIZED_EVENT_CODE_REQUIRED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.RESPONSE_TYPE_STRONGLY_RECOMMENDED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.SENDER_NAME_STRONGLY_RECOMMENDED;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE;
import static com.google.publicalerts.cap.profile.ca.CanadianProfile.ReasonType.VERSION_CODE_REQUIRED;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.Alert.MsgType;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.profile.CapProfileTestCase;

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

  public void testValidate_errors() throws Exception {
    Alert.Builder alert = loadAlert("canada.cap").toBuilder();
    assertNoReasons(alert, Reason.Level.ERROR);

    alert.clearCode();
    assertReasons(alert, ERROR, new Reason("/alert[1]", VERSION_CODE_REQUIRED));

    alert.setMsgType(MsgType.UPDATE).clearReferences();
    assertReasons(alert, ERROR,
        new Reason("/alert[1]", VERSION_CODE_REQUIRED),
        new Reason("/alert[1]/msgType[1]", UPDATE_OR_CANCEL_MUST_REFERENCE));

    alert = loadAlert("canada.cap").toBuilder();
    alert.getInfoBuilder(0).clearEventCode();
    assertReasons(alert, ERROR,
        new Reason("/alert[1]/info[2]", EVENT_CODES_MUST_MATCH),
        new Reason("/alert[1]/info[1]", RECOGNIZED_EVENT_CODE_REQUIRED));

    alert = loadAlert("canada.cap").toBuilder();
    alert.getInfoBuilder(1).getAreaBuilder(0).clearGeocode();
    assertReasons(alert, ERROR, new Reason("/alert[1]/info[2]/area[1]", AREA_GEOCODE_IS_REQUIRED));

    alert.getInfoBuilder(1).clearArea();
    assertReasons(alert, ERROR, new Reason("/alert[1]/info[2]", AREA_IS_REQUIRED));

    alert = loadAlert("canada.cap").toBuilder();
    alert.clearInfo();
    assertReasons(alert, ERROR, new Reason("/alert[1]", IS_REQUIRED));
    alert.setMsgType(MsgType.ACK);
    assertReasons(alert, Reason.Level.ERROR);
  }

  public void testValidate_recommendations() throws Exception {
    Alert.Builder alert = loadAlert("canada.cap").toBuilder();
    assertNoReasons(alert, Reason.Level.RECOMMENDATION);

    alert = loadAlert("canada.cap").toBuilder();
    alert.getInfoBuilder(0)
        .setLanguage("es")
        .clearExpires()
        .clearSenderName()
        .clearResponseType()
        .clearInstruction();
    assertReasons(alert, RECOMMENDATION,
        new Reason("/alert[1]", ENGLISH_AND_FRENCH),
        new Reason("/alert[1]/info[1]", EXPIRES_STRONGLY_RECOMMENDED),
        new Reason("/alert[1]/info[1]", SENDER_NAME_STRONGLY_RECOMMENDED),
        new Reason("/alert[1]/info[1]", RESPONSE_TYPE_STRONGLY_RECOMMENDED),
        new Reason("/alert[1]/info[1]", INSTRUCTION_STRONGLY_RECOMMENDED));

    alert = loadAlert("canada.cap").toBuilder();
    Info.Builder info = alert.getInfoBuilder(0);
    for (Area.Builder area : info.getAreaBuilderList()) {
      area.clearPolygon().clearCircle();
    }
    assertReasons(alert, RECOMMENDATION,
        new Reason("/alert[1]/info[1]/area[1]", CIRCLE_POLYGON_ENCOURAGED));
  }
}

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

package com.google.publicalerts.cap.profile.us;

import static com.google.publicalerts.cap.Reason.Level.ERROR;
import static com.google.publicalerts.cap.Reason.Level.RECOMMENDATION;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.AREA_IS_REQUIRED;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.AREA_SAME_GEOCODE_RECOMMENDED;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.CATEGORIES_MUST_MATCH;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.EVENT_CODES_MUST_MATCH;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.EXPIRES_INCLUDE_TIMEZONE_OFFSET;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.EXPIRES_IS_REQUIRED;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.INFO_DESCRIPTION_RECOMMENDED;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.INFO_EFFECTIVE_IS_IGNORED;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.INFO_INSTRUCTION_RECOMMENDED;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.INFO_ONSET_IS_IGNORED;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.SAME_EVENT_CODE_REQUIRED;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE;
import static com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType.VERSION_CODE_REQUIRED;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.Alert.MsgType;
import com.google.publicalerts.cap.Info.Category;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.ValuePair;
import com.google.publicalerts.cap.profile.CapProfileTestCase;

/**
 * Tests for {@link Ipaws1Profile}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class Ipaws1ProfileTest extends CapProfileTestCase {

  public Ipaws1ProfileTest(String name) {
    super(name);
  }

  @Override
  public void setUp() {
    profile = new Ipaws1Profile();
  }

  public void testParseFrom() throws Exception {
    runTestParseFrom("canada.cap");
  }

  public void testValidate_errors() throws Exception {
    Alert.Builder alert = loadAlertWithTwoInfos();
    assertNoReasons(alert, ERROR);

    alert.clearCode();
    assertReasons(alert, ERROR, new Reason("/alert[1]", VERSION_CODE_REQUIRED));

    alert = loadAlertWithTwoInfos();
    alert.setMsgType(MsgType.UPDATE);
    alert.clearReferences();
    assertReasons(alert, ERROR,
        new Reason("/alert[1]/msgType[1]", UPDATE_OR_CANCEL_MUST_REFERENCE));

    alert = loadAlertWithTwoInfos();
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValue("changed");
    alert.getInfoBuilder(0).addCategory(Category.CBRNE);
    assertReasons(alert, ERROR,
        new Reason("/alert[1]/info[2]", EVENT_CODES_MUST_MATCH),
        new Reason("/alert[1]/info[2]", CATEGORIES_MUST_MATCH),
        new Reason("/alert[1]/info[2]", SAME_EVENT_CODE_REQUIRED));

    alert = loadAlertWithTwoInfos();
    alert.getInfoBuilder(0)
        .clearExpires()
        .clearArea()
        .clearEventCode();
    assertReasons(alert, ERROR,
        new Reason("/alert[1]/info[2]", EVENT_CODES_MUST_MATCH),
        new Reason("/alert[1]/info[1]", EXPIRES_IS_REQUIRED),
        new Reason("/alert[1]/info[1]", AREA_IS_REQUIRED),
        new Reason("/alert[1]/info[1]", SAME_EVENT_CODE_REQUIRED));

    alert = loadAlertWithTwoInfos();
    alert.getInfoBuilder(0).setExpires("2002-01-01T00:00:00+00:00");
    assertReasons(alert, ERROR, 
        new Reason("/alert[1]/info[1]/expires[1]", EXPIRES_INCLUDE_TIMEZONE_OFFSET));
  }

  public void testValidate_recommendations() throws Exception {
    Alert.Builder alert = loadAlertWithTwoInfos();
    alert.getInfoBuilder(0).setInstruction("test")
        .getAreaBuilder(0).addGeocode(
            ValuePair.newBuilder().setValueName("SAME").setValue("AKZ001"));
    alert.getInfoBuilder(1).setInstruction("test")
        .getAreaBuilder(0).addGeocode(
            ValuePair.newBuilder().setValueName("SAME").setValue("AKZ001"));
    assertNoReasons(alert, RECOMMENDATION);

    alert.getInfoBuilder(0)
        .setEffective("2002-01-01T00:00:00+00:00")
        .setOnset("2002-01-01T00:00:00+00:00")
        .clearDescription()
        .clearInstruction()
        .getAreaBuilder(0)
        .clearGeocode();
    assertReasons(alert, RECOMMENDATION,
        new Reason("/alert[1]/info[1]/effective[1]", INFO_EFFECTIVE_IS_IGNORED),
        new Reason("/alert[1]/info[1]/onset[1]", INFO_ONSET_IS_IGNORED),
        new Reason("/alert[1]/info[1]", INFO_DESCRIPTION_RECOMMENDED),
        new Reason("/alert[1]/info[1]", INFO_INSTRUCTION_RECOMMENDED),
        new Reason("/alert[1]/info[1]/area[1]", AREA_SAME_GEOCODE_RECOMMENDED));
  }

  private Alert.Builder loadAlertWithTwoInfos() throws Exception {
    Alert.Builder alert = loadAlert("earthquake.cap").toBuilder();
    assertEquals(1, alert.getInfoCount());
    alert.addInfo(alert.getInfoBuilder(0).clone().setLanguage("sp-US"));
    return alert;
  }
}

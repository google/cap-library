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

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.Alert.MsgType;
import com.google.publicalerts.cap.Info.Category;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.ValuePair;
import com.google.publicalerts.cap.profile.CapProfileTestCase;
import com.google.publicalerts.cap.profile.us.Ipaws1Profile.ReasonType;

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
    assertNoReasons(alert, Reason.Level.ERROR);

    alert.clearCode();
    assertReasons(alert, Reason.Level.ERROR,
        new Reason("/alert", ReasonType.VERSION_CODE_REQUIRED));

    alert = loadAlertWithTwoInfos();
    alert.setMsgType(MsgType.UPDATE);
    alert.clearReferences();
    assertReasons(alert, Reason.Level.ERROR, new Reason("/alert/msgType",
        ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE));

    alert = loadAlertWithTwoInfos();
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValue("changed");
    alert.getInfoBuilder(0).addCategory(Category.CBRNE);
    assertReasons(alert, Reason.Level.ERROR,
        new Reason("/alert/info[1]", ReasonType.EVENT_CODES_MUST_MATCH),
        new Reason("/alert/info[1]", ReasonType.CATEGORIES_MUST_MATCH),
        new Reason("/alert/info[1]", ReasonType.SAME_EVENT_CODE_REQUIRED));

    alert = loadAlertWithTwoInfos();
    alert.getInfoBuilder(0).clearExpires()
        .clearArea()
        .clearEventCode();
    assertReasons(alert, Reason.Level.ERROR,
        new Reason("/alert/info[1]", ReasonType.EVENT_CODES_MUST_MATCH),
        new Reason("/alert/info[0]", ReasonType.EXPIRES_IS_REQUIRED),
        new Reason("/alert/info[0]", ReasonType.AREA_IS_REQUIRED),
        new Reason("/alert/info[0]", ReasonType.SAME_EVENT_CODE_REQUIRED));

    alert = loadAlertWithTwoInfos();
    alert.getInfoBuilder(0).setExpires("2002-01-01T00:00:00+00:00");
    assertReasons(alert, Reason.Level.ERROR, 
        new Reason("/alert/info[0]/expires",
            ReasonType.EXPIRES_INCLUDE_TIMEZONE_OFFSET));
  }

  public void testValidate_recommendations() throws Exception {
    Alert.Builder alert = loadAlertWithTwoInfos();
    alert.getInfoBuilder(0).setInstruction("test")
        .getAreaBuilder(0).addGeocode(
            ValuePair.newBuilder().setValueName("SAME").setValue("AKZ001"));
    alert.getInfoBuilder(1).setInstruction("test")
        .getAreaBuilder(0).addGeocode(
            ValuePair.newBuilder().setValueName("SAME").setValue("AKZ001"));
    assertNoReasons(alert, Reason.Level.RECOMMENDATION);

    alert.getInfoBuilder(0)
        .setEffective("2002-01-01T00:00:00+00:00")
        .setOnset("2002-01-01T00:00:00+00:00")
        .clearDescription()
        .clearInstruction()
        .getAreaBuilder(0)
        .clearGeocode();
    assertReasons(alert, Reason.Level.RECOMMENDATION,
        new Reason("/alert/info[0]/effective",
            ReasonType.INFO_EFFECTIVE_IS_IGNORED),
        new Reason("/alert/info[0]/onset",
            ReasonType.INFO_ONSET_IS_IGNORED),
        new Reason("/alert/info[0]",
            ReasonType.INFO_DESCRIPTION_RECOMMENDED),
        new Reason("/alert/info[0]",
            ReasonType.INFO_INSTRUCTION_RECOMMENDED),
        new Reason("/alert/info[0]/area[0]",
            ReasonType.AREA_SAME_GEOCODE_RECOMMENDED));
  }

  private Alert.Builder loadAlertWithTwoInfos() throws Exception {
    Alert.Builder alert = loadAlert("earthquake.cap").toBuilder();
    assertEquals(1, alert.getInfoCount());
    alert.addInfo(alert.getInfoBuilder(0).clone().setLanguage("sp-US"));
    return alert;
  }
}

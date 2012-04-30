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
import com.google.publicalerts.cap.ValuePair;
import com.google.publicalerts.cap.profile.CapProfileTestCase;
import com.google.publicalerts.cap.profile.us.Ipaws1Profile.ErrorType;
import com.google.publicalerts.cap.profile.us.Ipaws1Profile.RecommendationType;

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

  public void testCheckForErrors() throws Exception {
    Alert.Builder alert = loadAlert("earthquake.cap").toBuilder();
    assertNoErrors(alert);

    alert.clearCode();
    assertErrors(alert, ErrorType.VERSION_CODE_REQUIRED);

    alert = loadAlert("earthquake.cap").toBuilder();
    alert.setMsgType(MsgType.UPDATE);
    alert.clearReferences();
    assertErrors(alert, ErrorType.UPDATE_OR_CANCEL_MUST_REFERENCE);

    alert = loadAlert("earthquake.cap").toBuilder();
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValue("changed");
    alert.getInfoBuilder(0).addCategory(Category.CBRNE);
    assertErrors(alert, ErrorType.EVENT_CODES_MUST_MATCH,
        ErrorType.CATEGORIES_MUST_MATCH,
        ErrorType.SAME_EVENT_CODE_REQUIRED);

    alert = loadAlert("earthquake.cap").toBuilder();
    alert.getInfoBuilder(0).clearExpires()
        .clearArea()
        .clearEventCode();
    assertErrors(alert, ErrorType.EVENT_CODES_MUST_MATCH,
        ErrorType.EXPIRES_IS_REQUIRED,
        ErrorType.AREA_IS_REQUIRED,
        ErrorType.SAME_EVENT_CODE_REQUIRED);

    alert = loadAlert("earthquake.cap").toBuilder();
    alert.getInfoBuilder(0).setExpires("2002-01-01T00:00:00+00:00");
    assertErrors(alert, ErrorType.EXPIRES_INCLUDE_TIMEZONE_OFFSET);
  }

  public void testCheckForRecommendations() throws Exception {
    Alert.Builder alert = loadAlert("earthquake.cap").toBuilder();
    alert.getInfoBuilder(0).setInstruction("test")
        .getAreaBuilder(0).addGeocode(
            ValuePair.newBuilder().setValueName("SAME").setValue("AKZ001"));
    alert.getInfoBuilder(1).setInstruction("test")
        .getAreaBuilder(0).addGeocode(
            ValuePair.newBuilder().setValueName("SAME").setValue("AKZ001"));
    assertNoRecommendations(alert);

    alert.getInfoBuilder(0)
        .setEffective("2002-01-01T00:00:00+00:00")
        .setOnset("2002-01-01T00:00:00+00:00")
        .clearDescription()
        .clearInstruction()
        .getAreaBuilder(0)
        .clearGeocode();
    assertRecommendations(alert,
        RecommendationType.INFO_EFFECTIVE_IS_IGNORED,
        RecommendationType.INFO_ONSET_IS_IGNORED,
        RecommendationType.INFO_DESCRIPTION_RECOMMENDED,
        RecommendationType.INFO_INSTRUCTION_RECOMMENDED,
        RecommendationType.AREA_SAME_GEOCODE_RECOMMENDED);
  }
}

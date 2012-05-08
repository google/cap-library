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
import com.google.publicalerts.cap.Circle;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.Point;
import com.google.publicalerts.cap.profile.GoogleProfile.ErrorType;
import com.google.publicalerts.cap.profile.GoogleProfile.RecommendationType;

/**
 * Tests for {@link GoogleProfile}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class GoogleProfileTest extends CapProfileTestCase {

  public GoogleProfileTest(String name) {
    super(name);
  }

  @Override
  public void setUp() {
    profile = new GoogleProfile();
  }

  public void testParseFrom() throws Exception {
    runTestParseFrom("no_optional_fields.cap");
  }

  public void testCheckForErrors() throws Exception {
    Alert.Builder alert = loadAlert("australia.cap").toBuilder();
    assertNoErrors(alert);

    alert.setMsgType(MsgType.UPDATE)
        .clearReferences();
    assertErrors(alert, ErrorType.UPDATE_OR_CANCEL_MUST_REFERENCE);

    alert = loadAlert("australia.cap").toBuilder();
    alert.clearInfo();
    assertErrors(alert, ErrorType.INFO_IS_REQUIRED);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearCategory();
    assertErrors(alert, ErrorType.CATEGORIES_MUST_MATCH);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).setEvent("foo");
    assertErrors(alert, ErrorType.EVENTS_IN_SAME_LANGUAGE_MUST_MATCH);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1)
        .setEffective("2012-05-08T12:34:56-04:00")
        .setExpires("2012-05-08T12:34:56-04:00");
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValueName("foo");
    assertErrors(alert, ErrorType.EVENT_CODES_MUST_MATCH);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1)
        .setEffective("2012-05-08T12:34:57-04:00")
        .setExpires("2012-05-08T12:34:56-04:00");
    assertErrors(alert, ErrorType.EFFECTIVE_NOT_AFTER_EXPIRES);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearDescription()
        .clearWeb()
        .clearExpires()
        .clearUrgency()
        .clearSeverity()
        .clearCertainty();
    assertErrors(alert, ErrorType.DESCRIPTION_IS_REQUIRED,
        ErrorType.WEB_IS_REQUIRED,
        ErrorType.EXPIRES_IS_REQUIRED,
        ErrorType.URGENCY_IS_REQUIRED,
        ErrorType.SEVERITY_IS_REQUIRED,
        ErrorType.CERTAINTY_IS_REQUIRED);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearArea();
    assertErrors(alert, ErrorType.AREA_IS_REQUIRED);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).getAreaBuilder(0)
        .clearGeocode()
        .clearPolygon()
        .clearCircle();
    assertErrors(alert, ErrorType.CIRCLE_POLYGON_OR_GEOCODE_IS_REQUIRED);
  }

  public void testCheckForRecommendations() throws Exception {
    Alert.Builder alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(0).setContact("Call 911");
    alert.getInfoBuilder(1).setContact("Call 911");
    assertNoRecommendations(alert);

    alert.setSent("2002-01-01T00:00:00+00:00")
        .getInfoBuilder(0)
        .clearHeadline()
        .setEffective("2003-01-01T00:00:00+00:00")
        .setOnset("2004-01-01T00:00:00+00:00")
        .setExpires("2005-01-01T00:00:00+00:00");
    assertRecommendations(alert,
        RecommendationType.SENT_INCLUDE_TIMEZONE_OFFSET,
        RecommendationType.EFFECTIVE_INCLUDE_TIMEZONE_OFFSET,
        RecommendationType.ONSET_INCLUDE_TIMEZONE_OFFSET,
        RecommendationType.EXPIRES_INCLUDE_TIMEZONE_OFFSET);

    alert = loadAlert("australia.cap").toBuilder();
    String longHeadline = "This is a headline that is longer than 140 " +
        "characters; I don't know why anyone would want to make a " +
        "headline that long. It seems rather absurd.";
    alert.getInfoBuilder(0)
        .clearSenderName()
        .clearResponseType()
        .clearInstruction()
        .setHeadline(longHeadline)
        .setDescription(longHeadline)
        .setUrgency(Info.Urgency.UNKNOWN_URGENCY)
        .setSeverity(Info.Severity.UNKNOWN_SEVERITY)
        .setCertainty(Info.Certainty.UNKNOWN_CERTAINTY)
        .clearContact()
        .getAreaBuilder(0).addCircle(Circle.newBuilder()
            .setPoint(Point.newBuilder().setLatitude(0).setLongitude(0))
            .setRadius(0));
    alert.getInfoBuilder(1).setContact("Call 911");
    assertRecommendations(alert,
        RecommendationType.SENDER_NAME_STRONGLY_RECOMMENDED,
        RecommendationType.RESPONSE_TYPE_STRONGLY_RECOMMENDED,
        RecommendationType.INSTRUCTION_STRONGLY_RECOMMENDED,
        RecommendationType.HEADLINE_TOO_LONG,
        RecommendationType.HEADLINE_AND_DESCRIPTION_SHOULD_DIFFER,
        RecommendationType.UNKNOWN_URGENCY_DISCOURAGED,
        RecommendationType.UNKNOWN_SEVERITY_DISCOURAGED,
        RecommendationType.UNKNOWN_CERTAINTY_DISCOURAGED,
        RecommendationType.CONTACT_IS_RECOMMENDED,
        RecommendationType.NONZERO_CIRCLE_RADIUS_RECOMMENDED);

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(0).setContact("Call 911");
    alert.getInfoBuilder(1).setContact("Call 911");
    Info.Builder info = alert.getInfoBuilder(0);
    info.setDescription("foo")
        .setInstruction("foo");
    for (Area.Builder area : info.getAreaBuilderList()) {
      area.clearPolygon().clearCircle();
    }
    assertRecommendations(alert,
        RecommendationType.DESCRIPTION_AND_INSTRUCTION_SHOULD_DIFFER,
        RecommendationType.CIRCLE_POLYGON_ENCOURAGED);
  }
}

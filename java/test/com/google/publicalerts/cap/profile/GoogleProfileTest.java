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
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.profile.GoogleProfile.ReasonType;

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

  public void testValidate_errors() throws Exception {
    Alert.Builder alert = loadAlert("australia.cap").toBuilder();
    assertNoReasons(alert, Reason.Level.ERROR);

    alert.setMsgType(MsgType.UPDATE)
        .clearReferences();
    assertReasons(alert, Reason.Level.ERROR, new Reason("/alert/msgType",
        ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE));

    alert = loadAlert("australia.cap").toBuilder();
    alert.clearInfo();
    assertReasons(alert, Reason.Level.ERROR,
        new Reason("/alert", ReasonType.INFO_IS_REQUIRED));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearCategory();
    assertReasons(alert, Reason.Level.ERROR, new Reason(
        "/alert/info[1]/category", ReasonType.CATEGORIES_MUST_MATCH));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1)
        .setEffective("2012-05-08T12:34:56-04:00")
        .setExpires("2012-05-08T12:34:56-04:00");
    alert.getInfoBuilder(1).getEventCodeBuilder(0).setValueName("foo");
    assertReasons(alert, Reason.Level.ERROR, new Reason(
        "/alert/info[1]/eventCode", ReasonType.EVENT_CODES_MUST_MATCH));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1)
        .setEffective("2012-05-08T12:34:57-04:00")
        .setExpires("2012-05-08T12:34:56-04:00");
    assertReasons(alert, Reason.Level.ERROR, new Reason(
        "/alert/info[1]/effective", ReasonType.EFFECTIVE_NOT_AFTER_EXPIRES));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearDescription()
        .clearWeb()
        .clearExpires()
        .clearUrgency()
        .clearSeverity()
        .clearCertainty();
    assertReasons(alert, Reason.Level.ERROR,
        new Reason("/alert/info[1]", ReasonType.DESCRIPTION_IS_REQUIRED),
        new Reason("/alert/info[1]", ReasonType.WEB_IS_REQUIRED),
        new Reason("/alert/info[1]", ReasonType.EXPIRES_IS_REQUIRED),
        new Reason("/alert/info[1]", ReasonType.URGENCY_IS_REQUIRED),
        new Reason("/alert/info[1]", ReasonType.SEVERITY_IS_REQUIRED),
        new Reason("/alert/info[1]", ReasonType.CERTAINTY_IS_REQUIRED));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).clearArea();
    assertReasons(alert, Reason.Level.ERROR,
        new Reason("/alert/info[1]", ReasonType.AREA_IS_REQUIRED));

    alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(1).getAreaBuilder(0)
        .clearGeocode()
        .clearPolygon()
        .clearCircle();
    assertReasons(alert, Reason.Level.ERROR,
        new Reason("/alert/info[1]/area[0]",
            ReasonType.CIRCLE_POLYGON_OR_GEOCODE_IS_REQUIRED));
  }

  public void testValidate_recommendations() throws Exception {
    Alert.Builder alert = loadValidAustraliaCap();
    assertNoReasons(alert, Reason.Level.RECOMMENDATION);

    alert.setSent("2002-01-01T00:00:00+00:00")
        .getInfoBuilder(0)
        .clearHeadline()
        .setEffective("2003-01-01T00:00:00+00:00")
        .setOnset("2004-01-01T00:00:00+00:00")
        .setExpires("2005-01-01T00:00:00+00:00");
    assertReasons(alert, Reason.Level.INFO,
        new Reason("/alert/sent", ReasonType.SENT_INCLUDES_UTC_TIMEZONE),
        new Reason("/alert/info[0]/effective",
            ReasonType.EFFECTIVE_INCLUDES_UTC_TIMEZONE),
        new Reason("/alert/info[0]/onset",
            ReasonType.ONSET_INCLUDES_UTC_TIMEZONE),
        new Reason("/alert/info[0]/expires",
            ReasonType.EXPIRES_INCLUDES_UTC_TIMEZONE));

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
    assertReasons(alert, Reason.Level.RECOMMENDATION,
        new Reason("/alert/info[0]",
            ReasonType.SENDER_NAME_STRONGLY_RECOMMENDED),
        new Reason("/alert/info[0]",
            ReasonType.RESPONSE_TYPE_STRONGLY_RECOMMENDED),
        new Reason("/alert/info[0]",
            ReasonType.INSTRUCTION_STRONGLY_RECOMMENDED),
        new Reason("/alert/info[0]/headline",
            ReasonType.HEADLINE_TOO_LONG),
        new Reason("/alert/info[0]/headline",
            ReasonType.HEADLINE_AND_DESCRIPTION_SHOULD_DIFFER),
        new Reason("/alert/info[0]/urgency",
            ReasonType.UNKNOWN_URGENCY_DISCOURAGED),
        new Reason("/alert/info[0]/severity",
            ReasonType.UNKNOWN_SEVERITY_DISCOURAGED),
        new Reason("/alert/info[0]/certainty",
            ReasonType.UNKNOWN_CERTAINTY_DISCOURAGED),
        new Reason("/alert/info[0]", ReasonType.CONTACT_IS_RECOMMENDED),
        new Reason("/alert/info[0]/area[0]/circle[1]",
            ReasonType.NONZERO_CIRCLE_RADIUS_RECOMMENDED));

    alert = loadValidAustraliaCap();
    Info.Builder info = alert.getInfoBuilder(0);
    info.setDescription("foo")
        .setInstruction("foo");
    for (Area.Builder area : info.getAreaBuilderList()) {
      area.clearPolygon().clearCircle();
    }
    assertReasons(alert, Reason.Level.RECOMMENDATION,
        new Reason("/alert/info[0]/description",
            ReasonType.DESCRIPTION_AND_INSTRUCTION_SHOULD_DIFFER),
        new Reason("/alert/info[0]/area[0]",
            ReasonType.CIRCLE_POLYGON_ENCOURAGED));
    
    alert = loadValidAustraliaCap();
    alert.setStatus(Alert.Status.TEST);
    assertReasons(alert, Reason.Level.RECOMMENDATION,
        new Reason("/alert/status", ReasonType.TEST_ALERT_DISCOURAGED));

    // Empty <valueName> field
    alert = loadValidAustraliaCap();
    alert.getInfoBuilder(1).getAreaBuilder(0).getGeocodeBuilder(0).setValueName("");
    assertReasons(alert, Reason.Level.RECOMMENDATION,
        new Reason("/alert/info[1]/area[0]/geocode[0]",
            ReasonType.EMPTY_GEOCODE_FIELD));

    // Empty <value> field
    alert = loadValidAustraliaCap();
    alert.getInfoBuilder(0).getAreaBuilder(0).getGeocodeBuilder(0).setValue("");
    assertReasons(alert, Reason.Level.RECOMMENDATION,
        new Reason("/alert/info[0]/area[0]/geocode[0]",
            ReasonType.EMPTY_GEOCODE_FIELD));

    // Different <event> with same <language> in separate <info> blocks
    alert = loadValidAustraliaCap();
    alert.getInfoBuilder(1).setEvent("foo");
    assertReasons(alert, Reason.Level.RECOMMENDATION,
        new Reason("/alert/info[1]/event",
            ReasonType.EVENTS_IN_SAME_LANGUAGE_SHOULD_MATCH));
  }

 /**
  * Returns an Alert.Builder, based on the australia CAP example, which will not
  * throw any errors or warnings when checked for validity. Loads the alert in the
  * australia.cap testdata file and then sets the <contact> field for both <info>
  * blocks therein in order to avoid triggering a CONTACT_IS_RECOMMENDED warning.
  */
  private Alert.Builder loadValidAustraliaCap() throws Exception {
    Alert.Builder alert = loadAlert("australia.cap").toBuilder();
    alert.getInfoBuilder(0).setContact("Call 911");
    alert.getInfoBuilder(1).setContact("Call 911");
    return alert;
  }
}

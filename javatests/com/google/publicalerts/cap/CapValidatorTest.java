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

package com.google.publicalerts.cap;

import junit.framework.TestCase;

import com.google.publicalerts.cap.CapException.Type;

/**
 * Tests for {@link CapValidator}.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidatorTest extends TestCase {

  private CapValidator validator = new CapValidator();

  public CapValidatorTest(String s) {
    super(s);
  }

  public void testAlertValidatesOk() throws Exception {
    validator.validateAlert(TestUtil.getValidAlertBuilder().build());
  }

  @SuppressWarnings("deprecation")
  public void testAlertParseErrors() {
    Alert.Builder alert = TestUtil.getValidAlertBuilder();

    alert.clearXmlns();
    assertValidateErrors(alert, Type.XMLNS_IS_REQUIRED);
    alert.setXmlns(CapValidator.CAP_LATEST_XMLNS);

    alert.clearIdentifier();
    assertValidateErrors(alert, Type.IDENTIFIER_IS_REQUIRED);

    alert.setIdentifier("");
    assertValidateErrors(alert, Type.IDENTIFIER_IS_REQUIRED);

    alert.setIdentifier("invalid<");
    assertValidateErrors(alert, Type.INVALID_IDENTIFIER);

    alert.setIdentifier("&invalid");
    assertValidateErrors(alert, Type.INVALID_IDENTIFIER);
    alert.setIdentifier("valid_id");

    alert.setPassword("password");
    assertValidateErrors(alert, Type.PASSWORD_DEPRECATED);
    alert.clearPassword();

    alert.clearSender();
    assertValidateErrors(alert, Type.SENDER_IS_REQUIRED);

    alert.setSender("");
    assertValidateErrors(alert, Type.SENDER_IS_REQUIRED);

    alert.setSender("inv,alid");
    assertValidateErrors(alert, Type.INVALID_SENDER);

    alert.setSender("inv alid");
    assertValidateErrors(alert, Type.INVALID_SENDER);
    alert.setSender("valid_sender");

    alert.clearSent();
    assertValidateErrors(alert, Type.SENT_IS_REQUIRED);

    alert.setSent("invalid");
    assertValidateErrors(alert, Type.INVALID_SENT);
    alert.setSent("2003-04-02T14:39:01-05:00");

    alert.clearStatus();
    assertValidateErrors(alert, Type.STATUS_IS_REQUIRED);
    alert.setStatus(Alert.Status.Actual);

    alert.clearMsgType();
    assertValidateErrors(alert, Type.MSGTYPE_IS_REQUIRED);
    alert.setMsgType(Alert.MsgType.Alert);

    alert.clearScope();
    assertValidateErrors(alert, Type.SCOPE_IS_REQUIRED);
    alert.setScope(Alert.Scope.Public);

    alert.setRestriction("restriction");
    assertValidateErrors(alert, Type.RESTRICTION_SCOPE_MISMATCH);
    alert.clearRestriction();

    alert.setAddresses(Group.newBuilder().addValue("addresses").build());
    assertValidateErrors(alert, Type.ADDRESSES_SCOPE_MISMATCH);
    alert.setAddresses(Group.newBuilder().addValue("").build());
    assertValidateErrors(alert, Type.ADDRESSES_SCOPE_MISMATCH);

    alert.getInfoBuilder(0).clearUrgency();
    assertValidateErrors(alert, Type.ADDRESSES_SCOPE_MISMATCH,
        Type.INFO_URGENCY_IS_REQUIRED);
  }

  public void testInfoParseErrors() {
    Info.Builder info = TestUtil.getValidInfoBuilder();

    info.clearCategory();
    assertValidateErrors(info, Type.INFO_CATEGORY_IS_REQUIRED);
    info.addCategory(Info.Category.Met);

    info.clearEvent();
    assertValidateErrors(info, Type.INFO_EVENT_IS_REQUIRED);
    info.setEvent("event");

    info.clearUrgency();
    assertValidateErrors(info, Type.INFO_URGENCY_IS_REQUIRED);
    info.setUrgency(Info.Urgency.Expected);

    info.clearSeverity();
    assertValidateErrors(info, Type.INFO_SEVERITY_IS_REQUIRED);
    info.setSeverity(Info.Severity.Severe);

    info.clearCertainty();
    assertValidateErrors(info, Type.INFO_CERTAINTY_IS_REQUIRED);
    info.setCertainty(Info.Certainty.VeryLikely);
    assertValidateErrors(info, Type.INFO_CERTAINTY_VERY_LIKELY_DEPRECATED);
    info.setCertainty(Info.Certainty.Likely);

    info.setEffective("invalid");
    info.setOnset("invalid");
    info.setExpires("invalid");
    assertValidateErrors(info, Type.INFO_INVALID_EFFECTIVE,
        Type.INFO_INVALID_ONSET, Type.INFO_INVALID_EXPIRES);
    info.setEffective("2003-04-02T14:39:01-05:00");
    info.setOnset("2003-04-02T14:39:01-05:00");
    info.setExpires("2003-04-02T14:39:01-05:00");

    info.setWeb("invalid");
    assertValidateErrors(info, Type.INFO_INVALID_WEB);
    info.setWeb("http://www.example.org/alert");

    info.getAreaBuilder(0).clearAreaDesc();
    info.getResourceBuilder(0).clearResourceDesc();
    assertValidateErrors(info, Type.AREA_AREA_DESC_IS_REQUIRED,
        Type.RESOURCE_RESOURCE_DESC_IS_REQUIRED);
  }

  public void testParseAreaParseErrors() {
    Area.Builder area = TestUtil.getValidAreaBuilder();

    area.clearAreaDesc();
    assertValidateErrors(area, Type.AREA_AREA_DESC_IS_REQUIRED);
    area.setAreaDesc("Area desc");

    area.clearPolygon();
    area.addPolygon(Polygon.newBuilder()
        .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .addPoint(Point.newBuilder().setLatitude(3).setLongitude(4).build())
        .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .build());
    assertValidateErrors(area, Type.AREA_INVALID_POLYGON_NUM_POINTS);

    area.clearPolygon();
    area.addPolygon(Polygon.newBuilder()
        .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .addPoint(Point.newBuilder().setLatitude(3).setLongitude(4).build())
        .addPoint(Point.newBuilder().setLatitude(5).setLongitude(6).build())
        .addPoint(Point.newBuilder().setLatitude(7).setLongitude(8).build())
        .build());
    assertValidateErrors(area, Type.AREA_INVALID_POLYGON_START_END);

    area.clearPolygon();
    area.addPolygon(Polygon.newBuilder()
        .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .addPoint(Point.newBuilder().setLatitude(-91).setLongitude(181).build())
        .addPoint(Point.newBuilder().setLatitude(5).setLongitude(6).build())
        .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .build());
    assertValidateErrors(area, Type.AREA_INVALID_POINT_LATITUDE,
        Type.AREA_INVALID_POINT_LONGITUDE);

    area.clearPolygon();

    area.clearCircle();
    area.addCircle(Circle.newBuilder()
        .setPoint(Point.newBuilder().setLatitude(91).setLongitude(-181).build())
        .setRadius(-3).build());
    assertValidateErrors(area, Type.AREA_INVALID_POINT_LATITUDE,
        Type.AREA_INVALID_POINT_LONGITUDE, Type.AREA_INVALID_CIRCLE_RADIUS);

    area.clearCircle();
    area.clearAltitude();
    assertValidateErrors(area, Type.AREA_INVALID_CEILING);

    area.setAltitude(area.getCeiling() + 1);
    assertValidateErrors(area, Type.AREA_INVALID_ALTITUDE_CEILING_RANGE);
  }


  public void testParseResourceParseErrors() {
    Resource.Builder resource = TestUtil.getValidResourceBuilder();

    resource.clearResourceDesc();
    assertValidateErrors(resource, Type.RESOURCE_RESOURCE_DESC_IS_REQUIRED);
    resource.setResourceDesc("desc");

    resource.clearMimeType();
    assertValidateErrors(resource, Type.RESOURCE_MIME_TYPE_IS_REQUIRED);
    resource.setMimeType("image/gif");

    resource.setSize(-1);
    assertValidateErrors(resource, Type.RESOURCE_INVALID_SIZE);
    resource.setSize(1);

    resource.setUri("invalid");
    assertValidateErrors(resource, Type.RESOURCE_INVALID_URI);
  }

  @SuppressWarnings("deprecation")
  public void testValidateCap10() throws Exception {
    Alert.Builder alert = TestUtil.getValidAlertBuilder();
    alert.setXmlns(CapValidator.CAP10_XMLNS)
        .setPassword("password")
        .clearScope()
        .getInfoBuilder(0)
        .setCertainty(Info.Certainty.VeryLikely)
        .clearCategory();

    validator.validateAlert(alert);
  }

  @SuppressWarnings("deprecation")
  public void testValidateCap11() throws Exception {
    Alert.Builder alert = TestUtil.getValidAlertBuilder();
    alert.setXmlns(CapValidator.CAP11_XMLNS)
        .setPassword("password")
        .clearScope()
        .getInfoBuilder(0)
        .setCertainty(Info.Certainty.VeryLikely)
        .clearCategory();

    assertValidateErrors(alert, Type.PASSWORD_DEPRECATED,
        Type.SCOPE_IS_REQUIRED, Type.INFO_CATEGORY_IS_REQUIRED,
        Type.INFO_CERTAINTY_VERY_LIKELY_DEPRECATED);
  }

  @SuppressWarnings("deprecation")
  public void testCap12ParseErrors() {
    Alert.Builder alert = TestUtil.getValidAlertBuilder();
    alert.setXmlns(CapValidator.CAP12_XMLNS)
        .setPassword("password")
        .clearScope()
        .getInfoBuilder(0)
        .setCertainty(Info.Certainty.VeryLikely)
        .addResponseType(Info.ResponseType.Avoid)
        .addResponseType(Info.ResponseType.AllClear)
        .clearCategory();

    assertValidateErrors(alert, Type.PASSWORD_DEPRECATED,
        Type.SCOPE_IS_REQUIRED, Type.INFO_CATEGORY_IS_REQUIRED,
        Type.INFO_CERTAINTY_VERY_LIKELY_DEPRECATED);
  }

  public void testGetVersion() {
    assertEquals(10, validator.getValidateVersion(CapValidator.CAP10_XMLNS));
    assertEquals(11, validator.getValidateVersion(CapValidator.CAP11_XMLNS));
    assertEquals(12, validator.getValidateVersion(CapValidator.CAP12_XMLNS));
    assertEquals(12, validator.getValidateVersion(null));
    assertEquals(12, validator.getValidateVersion("foo"));
  }

  public void testIsDateParseable() {
    assertTrue(CapUtil.isValidDate("2003-04-02T14:39:01-05:00"));
    assertTrue(CapUtil.isValidDate("2008-02-29T24:59:59-00:00"));
    assertTrue(CapUtil.isValidDate("2003-12-31T07:00:00+00:00"));

    assertFalse(CapUtil.isValidDate("2003/04/02T14:39:01-05:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:61:01-05:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:39:61-05:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02 14:39:01-05:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:39:01-24:00"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:39:01-05:61"));
    assertFalse(CapUtil.isValidDate("2003-04-02T14:39:01-24:000"));
  }

  public void testIsAbsoluteUri() {
    assertTrue(validator.isAbsoluteUri("http://www.example.com"));
    assertTrue(validator.isAbsoluteUri(
        "http://www.example.com/alert?foo=bar#baz"));
    assertFalse(validator.isAbsoluteUri("/alert?foo=bar#baz"));
  }

  private void assertValidateErrors(AlertOrBuilder alert, Type...types) {
    try {
      validator.validateAlert(alert);
      fail("Expected CapException");
    } catch (CapException expected) {
      TestUtil.assertErrorTypes(expected.getReasons(), types);
    }
  }

  private void assertValidateErrors(InfoOrBuilder info, Type...types) {
    TestUtil.assertErrorTypes(validator.validateInfo(info, 0, 12, true), types);
  }

  private void assertValidateErrors(AreaOrBuilder area, Type...types) {
    TestUtil.assertErrorTypes(validator.validateArea(area, 0, 12, true), types);
  }

  private void assertValidateErrors(ResourceOrBuilder resource, Type...types) {
    TestUtil.assertErrorTypes(
        validator.validateResource(resource, 0, 12), types);
  }
}

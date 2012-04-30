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

import com.google.publicalerts.cap.CapException.Type;
import com.google.publicalerts.cap.testing.CapTestUtil;

import junit.framework.TestCase;

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
    validator.validateAlert(CapTestUtil.getValidAlertBuilder().build());
  }

  @SuppressWarnings("deprecation")
  public void testAlertParseErrors() {
    Alert.Builder alert = CapTestUtil.getValidAlertBuilder();

    alert.setAddresses(Group.newBuilder().addValue("addresses").build());
    alert.setRestriction("restriction");
    assertValidateErrors(alert, Type.RESTRICTION_SCOPE_MISMATCH);
    alert.clearRestriction();

    alert.getInfoBuilder(0).getAreaBuilder(0).clearCircle();
    alert.getInfoBuilder(0).getAreaBuilder(0).clearAltitude();
    assertValidateErrors(alert, Type.INVALID_AREA);
  }

  public void testInfoParseErrors() {
    Info.Builder info = CapTestUtil.getValidInfoBuilder();

    assertNoValidateErrors(info);
    info.clearLanguage();
    assertNoValidateErrors(info);

    for (String lang : new String[] { "en", "EN", "fr-CA", "en-scouse",
        "i-klingon", "x-pig-latin", "de-1901", "sgn-CH-de", "zh-cmn-Hans",
        "zh-Hans-HK"}) {
      info.setLanguage(lang);
      assertNoValidateErrors(info);
    }

    for (String lang : new String[] { "12345", "toolonglang", "en_US", "??"}) {
      info.setLanguage(lang);
      assertValidateErrors(info, Type.INVALID_LANGUAGE);
    }
    info.clearLanguage();

    info.getAreaBuilder(0).clearCircle();
    info.getAreaBuilder(0).clearAltitude();
    assertValidateErrors(info, Type.INVALID_AREA);
  }

  public void testParseAreaParseErrors() {
    Area.Builder area = CapTestUtil.getValidAreaBuilder();

    area.clearPolygon();
    area.addPolygon(Polygon.newBuilder()
        .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .addPoint(Point.newBuilder().setLatitude(3).setLongitude(4).build())
        .addPoint(Point.newBuilder().setLatitude(5).setLongitude(6).build())
        .addPoint(Point.newBuilder().setLatitude(7).setLongitude(8).build())
        .build());
    assertValidateErrors(area, Type.INVALID_POLYGON);

    area.clearPolygon()
        .clearCircle()
        .clearAltitude();
    assertValidateErrors(area, Type.INVALID_AREA);

    area.setAltitude(area.getCeiling() + 1);
    assertValidateErrors(area, Type.INVALID_ALTITUDE_CEILING_RANGE);
  }

  public void testGetVersion() {
    assertEquals(10, validator.getValidateVersion(CapValidator.CAP10_XMLNS));
    assertEquals(11, validator.getValidateVersion(CapValidator.CAP11_XMLNS));
    assertEquals(12, validator.getValidateVersion(CapValidator.CAP12_XMLNS));
    assertEquals(12, validator.getValidateVersion(null));
    assertEquals(12, validator.getValidateVersion("foo"));
  }

  private void assertValidateErrors(AlertOrBuilder alert, Type...types) {
    try {
      validator.validateAlert(alert);
      fail("Expected CapException");
    } catch (CapException expected) {
      CapTestUtil.assertErrorTypes(expected.getReasons(), types);
    }
  }

  private void assertNoValidateErrors(InfoOrBuilder info) {
    assertValidateErrors(info);
  }

  private void assertValidateErrors(InfoOrBuilder info, Type...types) {
    CapTestUtil.assertErrorTypes(
        validator.validateInfo(info, "/alert/info[0]", 12, true), types);
  }

  private void assertValidateErrors(AreaOrBuilder area, Type...types) {
    CapTestUtil.assertErrorTypes(
        validator.validateArea(area, "/alert/info[0]", 12, true), types);
  }
}

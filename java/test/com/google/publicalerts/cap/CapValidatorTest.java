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

import com.google.publicalerts.cap.CapException.ReasonType;
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
    validator.validateAlert(CapTestUtil.getValidAlertBuilder().build(), true);
  }

  public void testAlertParseErrors() {
    Alert.Builder alert = CapTestUtil.getValidAlertBuilder();
    alert.setRestriction("restriction");
    assertReasons(alert,
        ReasonType.RESTRICTION_SCOPE_MISMATCH, "/alert[1]/restriction[1]");
    
    alert = CapTestUtil.getValidAlertBuilder();
    alert.getReferencesBuilder().addValue(
        "hsas@dhs.gov," + alert.getIdentifier() + ",2003-02-02T14:39:01-05:00");
    assertReasons(alert,
        ReasonType.CIRCULAR_REFERENCE, "/alert[1]/references[1]");
    
    alert = CapTestUtil.getValidAlertBuilder();
    alert.getReferencesBuilder().addValue(
        "hsas@dhs.gov,43b080713722,2023-02-02T14:39:01-05:00");
    assertReasons(
        alert,  ReasonType.POSTDATED_REFERENCE, "/alert[1]/references[1]");
    
    alert = CapTestUtil.getValidAlertBuilder();
    alert.getInfoBuilder(0).getAreaBuilder(0).clearCircle();
    alert.getInfoBuilder(0).getAreaBuilder(0).clearAltitude();
    assertReasons(alert, ReasonType.INVALID_AREA, "/alert[1]/info[1]/area[1]");
  }

  public void testInfoParseErrors() {
    Info.Builder info = CapTestUtil.getValidInfoBuilder();

    assertNoReasons(info);
    info.clearLanguage();
    assertNoReasons(info);

    for (String lang : new String[] { "en", "EN", "fr-CA", "en-scouse",
        "i-klingon", "x-pig-latin", "de-1901", "sgn-CH-de", "zh-cmn-Hans",
        "zh-Hans-HK"}) {
      info.setLanguage(lang);
      assertNoReasons(info);
    }

    for (String lang : new String[] { "12345", "toolonglang", "en_US", "??"}) {
      info.setLanguage(lang);
      assertReasons
          (info, ReasonType.INVALID_LANGUAGE, "/alert[1]/info[1]/language[1]");
    }
    info.clearLanguage();

    info.getAreaBuilder(0).clearCircle();
    info.getAreaBuilder(0).clearAltitude();
    assertReasons(info, ReasonType.INVALID_AREA, "/alert[1]/info[1]/area[1]");
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
    assertReasons(area, ReasonType.INVALID_POLYGON,
        "/alert[1]/info[1]/area[1]/polygon[1]");

    area.clearPolygon()
        .clearCircle()
        .clearAltitude();
    assertReasons(area, ReasonType.INVALID_AREA, "/alert[1]/info[1]/area[1]");

    area.setAltitude(area.getCeiling() + 1);
    assertReasons(area, ReasonType.INVALID_ALTITUDE_CEILING_RANGE,
        "/alert[1]/info[1]/area[1]/ceiling[1]");
  }

  public void testValidateResourceErrors() {
    Resource.Builder resource = CapTestUtil.getValidResourceBuilder();
    resource.setMimeType("foo-type/bar");
    assertReasons(resource, ReasonType.INVALID_MIME_TYPE,
        "/alert[1]/resource[1]/mimeType[1]");
    
    resource = CapTestUtil.getValidResourceBuilder();
    resource.setDerefUri("foo");
    assertReasons(resource, ReasonType.INVALID_DEREF_URI,
        "/alert[1]/resource[1]/derefUri[1]");
  }
  
  public void testGetVersion() {
    assertEquals(10, validator.getValidateVersion(CapValidator.CAP10_XMLNS));
    assertEquals(11, validator.getValidateVersion(CapValidator.CAP11_XMLNS));
    assertEquals(12, validator.getValidateVersion(CapValidator.CAP12_XMLNS));
    assertEquals(12, validator.getValidateVersion(null));
    assertEquals(12, validator.getValidateVersion("foo"));
  }

  private void assertReasons(
      AlertOrBuilder alert, ReasonType reasonType, String xPath) {
    
    Reasons actual = validator.validateAlert(alert, true);
    CapTestUtil.assertReasons(actual, new Reason(xPath, reasonType));
  }

  private void assertNoReasons(InfoOrBuilder info) {
    CapTestUtil.assertReasons(
        validator.validateInfo(info, "/alert[1]/info[1]", 12, true));
  }

  private void assertReasons(
      InfoOrBuilder info, ReasonType reasonType, String xPath) {
    CapTestUtil.assertReasons(
        validator.validateInfo(info, "/alert[1]/info[1]", 12, true),
        new Reason(xPath, reasonType));
  }
  
  private void assertReasons(
      AreaOrBuilder area, ReasonType reasonType, String xPath) {
    CapTestUtil.assertReasons(
        validator.validateArea(area, "/alert[1]/info[1]/area[1]", 12, true),
        new Reason(xPath, reasonType));
  }
  
  private void assertReasons(
      ResourceOrBuilder resource, ReasonType reasonType, String xPath) {

    CapTestUtil.assertReasons(
        validator.validateResource(resource, "/alert[1]/resource[1]", 12),
        new Reason(xPath, reasonType));
  }
}

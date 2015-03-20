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

import static com.google.publicalerts.cap.CapException.ReasonType.CIRCULAR_REFERENCE;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_ALTITUDE_CEILING_RANGE;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_AREA;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_DEREF_URI;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_LANGUAGE;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_MIME_TYPE;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_POLYGON;
import static com.google.publicalerts.cap.CapException.ReasonType.POSTDATED_REFERENCE;
import static com.google.publicalerts.cap.CapException.ReasonType.RESTRICTION_SCOPE_MISMATCH;
import static com.google.publicalerts.cap.CapException.ReasonType.SAME_TEXT_DIFFERENT_LANGUAGE;
import static com.google.publicalerts.cap.CapException.ReasonType.TEXT_CONTAINS_HTML_ENTITIES;
import static com.google.publicalerts.cap.CapException.ReasonType.TEXT_CONTAINS_HTML_TAGS;

import com.google.publicalerts.cap.CapException.ReasonType;
import com.google.publicalerts.cap.testing.CapTestUtil;

import junit.framework.TestCase;

/**
 * Tests for {@link CapValidator}.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidatorTest extends TestCase {

  public CapValidatorTest(String s) {
    super(s);
  }

  public void testAlertValidatesOk() throws Exception {
    new CapValidator().validateAlert(CapTestUtil.getValidAlertBuilder().build());
  }

  public void testAlertParseErrors() {
    Alert.Builder alert = CapTestUtil.getValidAlertBuilder();
    alert.setRestriction("restriction");
    assertReasons(alert, RESTRICTION_SCOPE_MISMATCH, "/alert[1]/restriction[1]");
    
    alert = CapTestUtil.getValidAlertBuilder();
    alert.getReferencesBuilder().addValue(
        "hsas@dhs.gov," + alert.getIdentifier() + ",2003-02-02T14:39:01-05:00");
    assertReasons(alert, CIRCULAR_REFERENCE, "/alert[1]/references[1]");
    
    alert = CapTestUtil.getValidAlertBuilder();
    alert.getReferencesBuilder().addValue("hsas@dhs.gov,43b080713722,2023-02-02T14:39:01-05:00");
    assertReasons(alert,  POSTDATED_REFERENCE, "/alert[1]/references[1]");
    
    alert = CapTestUtil.getValidAlertBuilder();
    alert.getInfoBuilder(0).getAreaBuilder(0).clearCircle();
    alert.getInfoBuilder(0).getAreaBuilder(0).clearAltitude();
    assertReasons(alert, INVALID_AREA, "/alert[1]/info[1]/area[1]");
    
    alert = CapTestUtil.getValidAlertBuilder();
    alert.addInfo(alert.getInfoBuilder(0));
    alert.getInfoBuilder(0).setLanguage("en-US");
    alert.getInfoBuilder(1)
        .setLanguage("fr-CA")
        .setHeadline("toFrench(" + alert.getInfoBuilder(0).getHeadline() + ")")
        .setDescription("toFrench(" + alert.getInfoBuilder(0).getDescription() + ")")
        .setEvent("toFrench(" + alert.getInfoBuilder(0).getEvent() + ")");
    
    assertReasons(alert, SAME_TEXT_DIFFERENT_LANGUAGE, "/alert[1]/info[2]/instruction[1]");
  }

  public void testInfoParseErrors() {
    Info.Builder info = CapTestUtil.getValidInfoBuilder();
    assertNoReasons(info);
    
    info.clearLanguage();
    assertNoReasons(info);

    for (String lang : new String[] { "en", "EN", "fr-CA", "en-scouse", "i-klingon", "x-pig-latin",
        "de-1901", "sgn-CH-de", "zh-cmn-Hans", "zh-Hans-HK"}) {
      info.setLanguage(lang);
      assertNoReasons(info);
    }

    for (String lang : new String[] { "12345", "toolonglang", "en_US", "??"}) {
      info.setLanguage(lang);
      assertReasons(info, INVALID_LANGUAGE, "/alert[1]/info[1]/language[1]");
    }
    info.clearLanguage();

    info.setWeb("some non full absolute uri");
    assertReasons(info, ReasonType.INVALID_WEB, "/alert[1]/info[1]/web[1]");
    
    info = CapTestUtil.getValidInfoBuilder();
    info.getAreaBuilder(0).clearCircle();
    info.getAreaBuilder(0).clearAltitude();
    assertReasons(info, INVALID_AREA, "/alert[1]/info[1]/area[1]");
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
    assertReasons(area, INVALID_POLYGON, "/alert[1]/info[1]/area[1]/polygon[1]");

    area.clearPolygon()
        .clearCircle()
        .clearAltitude();
    assertReasons(area, INVALID_AREA, "/alert[1]/info[1]/area[1]");

    area.setAltitude(area.getCeiling() + 1);
    assertReasons(area, INVALID_ALTITUDE_CEILING_RANGE, "/alert[1]/info[1]/area[1]/ceiling[1]");
  }

  public void testValidateResourceErrors() {
    Resource.Builder resource = CapTestUtil.getValidResourceBuilder();
    resource.setMimeType("foo-type/bar");
    assertReasons(resource, INVALID_MIME_TYPE, "/alert[1]/info[1]/resource[1]/mimeType[1]");
    
    resource = CapTestUtil.getValidResourceBuilder();
    resource.setDerefUri("foo");
    assertReasons(resource, INVALID_DEREF_URI, "/alert[1]/info[1]/resource[1]/derefUri[1]");
    
    resource = CapTestUtil.getValidResourceBuilder();
    resource.setUri("some invalid uri");
    assertReasons(resource, ReasonType.INVALID_URI, "/alert[1]/info[1]/resource[1]/uri[1]");
    
    resource = CapTestUtil.getValidResourceBuilder();
    resource.setUri("some/relative/path");
    assertReasons(resource, ReasonType.RELATIVE_URI_MISSING_DEREF_URI,
        "/alert[1]/info[1]/resource[1]/uri[1]");
    
    resource = CapTestUtil.getValidResourceBuilder();
    resource.setUri("some/relative/path");
    resource.setDerefUri("aaaa");
    assertNoReasons(resource);
  }
  
  public void testValidateStringFields() {
    Alert.Builder alert = CapTestUtil.getValidAlertBuilder().setNote("a&nbsp;note");
    assertReasons(alert, TEXT_CONTAINS_HTML_ENTITIES, "/alert[1]/note[1]");
    
    alert = CapTestUtil.getValidAlertBuilder().addInfo(1, CapTestUtil.getValidInfoBuilder()
        .setDescription("DHS has set the threat&nbsp;level to ORANGE."));
    assertReasons(alert, TEXT_CONTAINS_HTML_ENTITIES, "/alert[1]/info[2]/description[1]");
    
    alert = CapTestUtil.getValidAlertBuilder().setInfo(0, CapTestUtil.getValidInfoBuilder()
        .setArea(0, CapTestUtil.getValidAreaBuilder().setGeocode(0, ValuePair.newBuilder()
            .setValueName("G&nbsp;1").setValue("V1"))));
    assertReasons(alert,
        TEXT_CONTAINS_HTML_ENTITIES, "/alert[1]/info[1]/area[1]/geocode[1]/valueName[1]");
    
    alert = CapTestUtil.getValidAlertBuilder().setNote("a<b>note</b>");
    assertReasons(alert, TEXT_CONTAINS_HTML_TAGS, "/alert[1]/note[1]");
    
    alert = CapTestUtil.getValidAlertBuilder().addInfo(1, CapTestUtil.getValidInfoBuilder()
        .setDescription("DHS has set the threa level to <strong>ORANGE</strong>."));
    assertReasons(alert, TEXT_CONTAINS_HTML_TAGS, "/alert[1]/info[2]/description[1]");

    alert = CapTestUtil.getValidAlertBuilder().setInfo(0, CapTestUtil.getValidInfoBuilder()
        .setArea(0, CapTestUtil.getValidAreaBuilder().setGeocode(0, ValuePair.newBuilder()
            .setValueName("G<b>1").setValue("V1"))));
    assertReasons(alert,
        TEXT_CONTAINS_HTML_TAGS, "/alert[1]/info[1]/area[1]/geocode[1]/valueName[1]");
  }
  
  public void testGetVersion() {
    assertEquals(10, new CapValidator().getValidateVersion(CapValidator.CAP10_XMLNS));
    assertEquals(11, new CapValidator().getValidateVersion(CapValidator.CAP11_XMLNS));
    assertEquals(12, new CapValidator().getValidateVersion(CapValidator.CAP12_XMLNS));
    assertEquals(12, new CapValidator().getValidateVersion(null));
    assertEquals(12, new CapValidator().getValidateVersion("foo"));
  }

  private void assertReasons(
      AlertOrBuilder alert, ReasonType expectedReasonType, String expectedXPath) {
    
    CapTestUtil.assertReasons(
        new CapValidator().validateAlert(alert),
        new Reason(expectedXPath, expectedReasonType));
  }

  private void assertNoReasons(InfoOrBuilder info) {
    XPath xPath = new XPath();
    xPath.push("alert");
    
    CapTestUtil.assertReasons(new CapValidator().validateInfo(info, xPath));
  }
  
  private void assertNoReasons(ResourceOrBuilder resource) {
    XPath xPath = new XPath();
    xPath.push("alert");
    xPath.push("info");
    
    CapTestUtil.assertReasons(new CapValidator().validateResource(resource, xPath));
  }

  private void assertReasons(
      InfoOrBuilder info, ReasonType expectedReasonType, String expectedXPath) {
    XPath xPath = new XPath();
    xPath.push("alert");
    
    CapTestUtil.assertReasons(
        new CapValidator().validateInfo(info, xPath),
        new Reason(expectedXPath, expectedReasonType));
  }
  
  private void assertReasons(
      AreaOrBuilder area, ReasonType expectedReasonType, String expectedXPath) {
    XPath xPath = new XPath();
    xPath.push("alert");
    xPath.push("info");
    
    CapTestUtil.assertReasons(
        new CapValidator().validateArea(area, xPath),
        new Reason(expectedXPath, expectedReasonType));
  }
  
  private void assertReasons(
      ResourceOrBuilder resource, ReasonType expectedReasonType, String expectedXPath) {
    XPath xPath = new XPath();
    xPath.push("alert");
    xPath.push("info");
    
    CapTestUtil.assertReasons(
        new CapValidator().validateResource(resource, xPath),
        new Reason(expectedXPath, expectedReasonType));
  }
}

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

import static com.google.common.truth.Truth.assertThat;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests for {@link CapJsonBuilder}.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class CapJsonBuilderTest extends TestCase {

  private CapJsonBuilder builder;

  public CapJsonBuilderTest(String s) {
    super(s);
  }

  @Override
  protected void setUp() throws Exception {
    builder = new CapJsonBuilder();
  }

  public void testAlert() throws Exception {
    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP_LATEST_XMLNS)
        .setIdentifier("43b080713727")
        .setSender("hsas@dhs.gov")
        .setSent("2003-04-02T14:39:01-05:00")
        .setStatus(Alert.Status.ACTUAL)
        .setMsgType(Alert.MsgType.ALERT)
        .setSource("a source")
        .setScope(Alert.Scope.PUBLIC)
        .setRestriction("a restriction")
        .setAddresses(Group.newBuilder()
            .addValue("address 1").addValue("address2").build())
        .addCode("abcde")
        .addCode("fghij")
        .setNote("a note")
        .setReferences(Group.newBuilder()
            .addValue("reference1").addValue("reference 2").build())
        .setIncidents(Group.newBuilder()
            .addValue("incident1").addValue("incident2").build())
        .buildPartial();

    JSONObject json = builder.toJSONObject(alert);
    assertEquals("43b080713727", json.get("identifier"));
    assertEquals("hsas@dhs.gov", json.get("sender"));
    assertEquals("2003-04-02T14:39:01-05:00", json.get("sent"));
    assertEquals("Actual", json.get("status"));
    assertEquals("Alert", json.get("msgType"));
    assertEquals("a source", json.get("source"));
    assertEquals("Public", json.get("scope"));
    assertEquals("a restriction", json.get("restriction"));
    assertEquals("\"address 1\" address2", json.get("addresses"));
    assertEquals("abcde", getStringFromArray(json, "code", 0));
    assertEquals("fghij", getStringFromArray(json, "code", 1));
    assertEquals("a note", json.get("note"));
    assertEquals("reference1 \"reference 2\"", json.get("references"));
    assertEquals("incident1 incident2", json.get("incidents"));
  }

  public void testInfo() throws Exception {
    Info info1 = Info.newBuilder()
        .addCategory(Info.Category.SECURITY)
        .addCategory(Info.Category.SAFETY)
        .setEvent("Homeland Security Advisory System Update")
        .setUrgency(Info.Urgency.UNKNOWN_URGENCY)
        .setSeverity(Info.Severity.UNKNOWN_SEVERITY)
        .setCertainty(Info.Certainty.UNKNOWN_CERTAINTY)
        .setSenderName("Department of Homeland Security")
        .setHeadline("Homeland Security Sets Code ORANGE")
        .setDescription("DHS has set the threat level to ORANGE.")
        .setInstruction("Take Protective Measures.")
        .setWeb("http://www.dhs.gov/dhspublic/display?theme=29")
        .addParameter(ValuePair.newBuilder()
            .setValueName("HSAS").setValue("ORANGE").build())
        .addParameter(ValuePair.newBuilder()
            .setValueName("p2").setValue("v2").build())
        .buildPartial();

    Info info2 = Info.newBuilder()
        .setAudience("an audience")
        .setContact("a contact")
        .addEventCode(ValuePair.newBuilder()
            .setValueName("EC").setValue("v1").build())
        .addEventCode(ValuePair.newBuilder()
            .setValueName("EC2").setValue("v2").build())
        .setEffective("2003-04-02T14:39:01-05:00")
        .setOnset("2003-04-02T15:39:01+05:00")
        .setExpires("2003-04-02T16:39:01-00:00")
        .buildPartial();

    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP_LATEST_XMLNS)
        .addInfo(info1)
        .addInfo(info2)
        .buildPartial();

    JSONObject json = builder.toJSONObject(alert);
    JSONObject jsonInfo1 = getJSONObjectFromArray(json, "info", 0);
    JSONObject jsonInfo2 = getJSONObjectFromArray(json, "info", 1);

    assertEquals("Security", getStringFromArray(jsonInfo1, "category", 0));
    assertEquals("Safety", getStringFromArray(jsonInfo1, "category", 1));
    assertEquals("Homeland Security Advisory System Update",
        jsonInfo1.get("event"));
    assertEquals("Unknown", jsonInfo1.get("urgency"));
    assertEquals("Unknown", jsonInfo1.get("severity"));
    assertEquals("Unknown", jsonInfo1.get("certainty"));
    assertEquals("Department of Homeland Security",
        jsonInfo1.get("senderName"));
    assertEquals("Homeland Security Sets Code ORANGE",
        jsonInfo1.get("headline"));
    assertEquals("Take Protective Measures.", jsonInfo1.get("instruction"));
    assertEquals("http://www.dhs.gov/dhspublic/display?theme=29",
        jsonInfo1.get("web"));
    JSONObject param1 = getJSONObjectFromArray(jsonInfo1, "parameter", 0);
    assertEquals("HSAS", param1.get("valueName"));
    assertEquals("ORANGE", param1.get("value"));
    JSONObject param2 = getJSONObjectFromArray(jsonInfo1, "parameter", 1);
    assertEquals("p2", param2.get("valueName"));
    assertEquals("v2", param2.get("value"));

    assertEquals("an audience", jsonInfo2.get("audience"));
    assertEquals("a contact", jsonInfo2.get("contact"));
    JSONObject eventCode1 = getJSONObjectFromArray(jsonInfo2, "eventCode", 0);
    assertEquals("EC", eventCode1.get("valueName"));
    assertEquals("v1", eventCode1.get("value"));
    JSONObject eventCode2 = getJSONObjectFromArray(jsonInfo2, "eventCode", 1);
    assertEquals("EC2", eventCode2.get("valueName"));
    assertEquals("v2", eventCode2.get("value"));
    assertEquals("a contact", jsonInfo2.get("contact"));
    assertEquals("2003-04-02T14:39:01-05:00", jsonInfo2.get("effective"));
    assertEquals("2003-04-02T15:39:01+05:00", jsonInfo2.get("onset"));
    assertEquals("2003-04-02T16:39:01-00:00", jsonInfo2.get("expires"));
  }

  public void testArea() throws Exception {
    Area area1 = Area.newBuilder()
        .addPolygon(Polygon.newBuilder()
            .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
            .addPoint(Point.newBuilder().setLatitude(3).setLongitude(4).build())
            .addPoint(Point.newBuilder().setLatitude(5).setLongitude(6).build())
            .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
            .build())
        .addPolygon(Polygon.newBuilder()
            .addPoint(
                Point.newBuilder().setLatitude(11).setLongitude(12).build())
            .addPoint(
                Point.newBuilder().setLatitude(13).setLongitude(14).build())
            .addPoint(
                Point.newBuilder().setLatitude(15).setLongitude(16).build())
            .addPoint(
                Point.newBuilder().setLatitude(11).setLongitude(12).build())
            .build())
        .addCircle(Circle.newBuilder()
            .setPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
            .setRadius(3).build())
        .addCircle(Circle.newBuilder()
            .setPoint(Point.newBuilder()
              .setLatitude(4).setLongitude(5).build()).setRadius(6).build())
        .addGeocode(ValuePair.newBuilder()
            .setValueName("G1").setValue("v1").build())
        .addGeocode(ValuePair.newBuilder()
            .setValueName("G2").setValue("v2").build())
        .setAltitude(5.5)
        .setCeiling(6.5)
        .buildPartial();
    Area area2 = Area.newBuilder().setAreaDesc("U.S. nationwide").build();
    Info info = Info.newBuilder().addArea(area1).addArea(area2).buildPartial();
    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP_LATEST_XMLNS)
        .addInfo(info)
        .buildPartial();

    JSONObject json = builder.toJSONObject(alert);
    JSONObject jsonInfo = getJSONObjectFromArray(json, "info", 0);
    JSONObject jsonArea1 = getJSONObjectFromArray(jsonInfo, "area", 0);
    JSONObject jsonArea2 = getJSONObjectFromArray(jsonInfo, "area", 1);

    assertEquals("1.0,2.0 3.0,4.0 5.0,6.0 1.0,2.0",
        getStringFromArray(jsonArea1, "polygon", 0));
    assertEquals("11.0,12.0 13.0,14.0 15.0,16.0 11.0,12.0",
        getStringFromArray(jsonArea1, "polygon", 1));
    assertEquals("1.0,2.0 3.0", getStringFromArray(jsonArea1, "circle", 0));
    assertEquals("4.0,5.0 6.0", getStringFromArray(jsonArea1, "circle", 1));
    JSONObject geocode1 = getJSONObjectFromArray(jsonArea1, "geocode", 0);
    assertEquals("G1", geocode1.get("valueName"));
    assertEquals("v1", geocode1.get("value"));
    JSONObject geocode2 = getJSONObjectFromArray(jsonArea1, "geocode", 1);
    assertEquals("G2", geocode2.get("valueName"));
    assertEquals("v2", geocode2.get("value"));
    assertEquals("5.5", jsonArea1.get("altitude"));
    assertEquals("6.5", jsonArea1.get("ceiling"));
    assertEquals("U.S. nationwide", jsonArea2.get("areaDesc"));
  }

  public void testResource() throws Exception {
    Resource resource1 = Resource.newBuilder()
        .setMimeType("image/gif")
        .setSize(123)
        .setDerefUri("a deref uri")
        .setDigest("a digest")
        .buildPartial();
    Resource resource2 = Resource.newBuilder()
        .setResourceDesc("Image file (GIF)")
        .setUri("http://www.dhs.gov/dhspublic/getAdvisoryImage")
        .buildPartial();

    Info info = Info.newBuilder()
        .addResource(resource1)
        .addResource(resource2)
        .buildPartial();
    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP_LATEST_XMLNS)
        .addInfo(info)
        .buildPartial();

    JSONObject json = builder.toJSONObject(alert);
    JSONObject jsonInfo = getJSONObjectFromArray(json, "info", 0);
    JSONObject jsonRes1 = getJSONObjectFromArray(jsonInfo, "resource", 0);
    JSONObject jsonRes2 = getJSONObjectFromArray(jsonInfo, "resource", 1);

    assertEquals("image/gif", jsonRes1.get("mimeType"));
    assertEquals("123", jsonRes1.get("size"));
    assertEquals("a deref uri", jsonRes1.get("derefUri"));
    assertEquals("Image file (GIF)", jsonRes2.get("resourceDesc"));
    assertEquals("http://www.dhs.gov/dhspublic/getAdvisoryImage",
        jsonRes2.get("uri"));
  }

  @SuppressWarnings("deprecation")
  public void testCap10() throws Exception {
    Area area = Area.newBuilder()
        .addGeocode(ValuePair.newBuilder()
            .setValueName("G1").setValue("v1").build())
        .buildPartial();

    Info info = Info.newBuilder()
        .setCertainty(Info.Certainty.VERY_LIKELY)
        .addParameter(ValuePair.newBuilder()
            .setValueName("HSAS").setValue("ORANGE").build())
        .addEventCode(ValuePair.newBuilder()
            .setValueName("EC").setValue("v1").build())
        .addArea(area)
        .buildPartial();
    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP10_XMLNS)
        .setPassword("obsolete")
        .addInfo(info)
        .buildPartial();

    JSONObject json = builder.toJSONObject(alert);
    JSONObject jsonInfo = getJSONObjectFromArray(json, "info", 0);
    JSONObject jsonArea = getJSONObjectFromArray(jsonInfo, "area", 0);

    assertEquals("G1=v1", getStringFromArray(jsonArea, "geocode", 0));
    assertEquals("Very Likely", jsonInfo.get("certainty"));
    assertEquals("HSAS=ORANGE", getStringFromArray(jsonInfo, "parameter", 0));
    assertEquals("EC=v1", getStringFromArray(jsonInfo, "eventCode", 0));
    assertEquals("obsolete", json.get("password"));
  }

  public void testCap11() throws Exception {
    Resource resource = Resource.newBuilder()
        .setDerefUri("deref uri")
        .buildPartial();
    Info info = Info.newBuilder()
        .addCategory(Info.Category.CBRNE)
        .addResponseType(Info.ResponseType.EVACUATE)
        .addResource(resource)
        .buildPartial();
    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP11_XMLNS)
        .setStatus(Alert.Status.DRAFT)
        .setScope(Alert.Scope.PUBLIC)
        .addInfo(info)
        .buildPartial();

    JSONObject json = builder.toJSONObject(alert);
    JSONObject jsonInfo = getJSONObjectFromArray(json, "info", 0);
    JSONObject jsonRes = getJSONObjectFromArray(jsonInfo, "resource", 0);

    assertEquals("Draft", json.get("status"));
    assertEquals("Public", json.get("scope"));
    assertEquals("CBRNE", getStringFromArray(jsonInfo, "category", 0));
    assertEquals("Evacuate", getStringFromArray(jsonInfo, "responseType", 0));
    assertEquals("deref uri", jsonRes.get("derefUri"));
  }

  public void testCap12() throws Exception {
    Info info = Info.newBuilder()
        .addResponseType(Info.ResponseType.AVOID)
        .addResponseType(Info.ResponseType.ALL_CLEAR)
        .buildPartial();
    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP12_XMLNS)
        .addInfo(info)
        .buildPartial();

    JSONObject json = builder.toJSONObject(alert);
    JSONObject jsonInfo = getJSONObjectFromArray(json, "info", 0);

    assertEquals("Avoid", getStringFromArray(jsonInfo, "responseType", 0));
    assertEquals("AllClear", getStringFromArray(jsonInfo, "responseType", 1));
  }

  public void testPrettyPrint() {
    Info info = Info.newBuilder()
        .setEvent("event")
        .setHeadline("headline")
        .buildPartial();
    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP12_XMLNS)
        .setSender("sender")
        .addInfo(info)
        .buildPartial();


    String json = builder.toJson(alert);
    // JSON dict fields can appear in any order
    assertThat(json).contains("{\n");
    assertThat(json).contains("\n  \"sender\": \"sender\"");
    assertThat(json).contains("\n  \"info\": [{");
    assertThat(json).contains("\n    \"headline\": \"headline\"");
    assertThat(json).contains("\n    \"event\": \"event\"");
    assertThat(json).contains("\n  }]");
    assertThat(json).contains("\n}");

    builder = new CapJsonBuilder(0);

    json = builder.toJson(alert);
    // JSON dict fields can appear in any order
    assertThat(json).doesNotContain("\n");
    assertThat(json).doesNotContain(" ");
    assertThat(json).contains("{\"");
    assertThat(json).contains("\"sender\":\"sender\"");
    assertThat(json).contains("\"info\":[{");
    assertThat(json).contains("\"headline\":\"headline\"");
    assertThat(json).contains("\"event\":\"event\"");
    assertThat(json).contains("}]");
  }

  public void testEscaping() {
    String evilNote = "\"',{}[]&<>\u0000";
    Alert alert = Alert.newBuilder().setXmlns(CapValidator.CAP12_XMLNS)
        .setNote(evilNote)
        .buildPartial();

    assertEquals("{\"note\": " + JSONObject.quote(evilNote) + "}",
        builder.toJson(alert));
  }

  private String getStringFromArray(JSONObject parent, String name, int index)
      throws JSONException {
    return (String) ((JSONArray) parent.get(name)).get(index);
  }

  private JSONObject getJSONObjectFromArray(
      JSONObject parent, String name, int index) throws JSONException {
    return (JSONObject) ((JSONArray) parent.get(name)).get(index);
  }
}

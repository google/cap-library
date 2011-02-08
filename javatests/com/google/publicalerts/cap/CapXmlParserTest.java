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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.publicalerts.cap.CapException.Type;
import com.google.publicalerts.cap.CapXmlParser.CapXmlHandler;

import junit.framework.TestCase;

import org.xml.sax.SAXParseException;


/**
 * Tests for {@link CapXmlParser}.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class CapXmlParserTest extends TestCase {

  public CapXmlParserTest(String s) {
    super(s);
  }

  private String getValidAlertPre(String xmlns) {
    return "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
      + "<alert xmlns=\"" + xmlns + "\">\n"
      + "<identifier>43b080713727</identifier>\n"
      + "<sender>hsas@dhs.gov</sender>\n"
      + "<sent>2003-04-02T14:39:01-05:00</sent>\n"
      + "<status>Actual</status>\n"
      + "<msgType>Alert</msgType>\n"
      + "<source>a source</source>\n"
      + "<scope>Private</scope>\n"
      + "<addresses>\"address 1\" address2</addresses>\n"
      + "<code>abcde</code>\n"
      + "<code>fghij</code>\n"
      + "<note>a note</note>\n"
      + "<references>reference1 \"reference 2\"</references>\n"
      + "<incidents>incident1 incident2</incidents>\n";
  }

  private String getValidInfoPre() {
    return "<info>\n"
    + "<category>Security</category>\n"
    + "<category>Safety</category>\n"
    + "<event>Homeland Security Advisory System Update</event>\n"
    + "<urgency>Unknown</urgency>\n"
    + "<severity>Unknown</severity>\n"
    + "<certainty>Unknown</certainty>\n"
    + "<senderName>Department of Homeland Security</senderName>\n"
    + "<headline>Homeland Security Sets Code ORANGE</headline>\n"
    + "<description>DHS has set the threat level to ORANGE.</description>\n"
    + "<instruction>Take Protective Measures.</instruction>\n"
    + "<web>http://www.dhs.gov/dhspublic/display?theme=29</web>\n"
    + "<parameter>\n"
    + "<valueName>HSAS</valueName>\n"
    + "<value>ORANGE</value>\n"
    + "</parameter>\n"
    + "<parameter>\n"
    + "<valueName>p2</valueName>\n"
    + "<value>v2</value>\n"
    + "</parameter>\n";
  }

  public void testParseAlert() throws Exception {
    String alertStr = getValidAlertPre(CapValidator.CAP_LATEST_XMLNS)
        + "</alert>";

    CapXmlParser parser = new CapXmlParser(true);
    Alert alert = parser.parseFrom(alertStr);
    assertEquals(CapValidator.CAP_LATEST_XMLNS, alert.getXmlns());
    assertEquals("43b080713727", alert.getIdentifier());
    assertEquals("hsas@dhs.gov", alert.getSender());
    assertEquals("2003-04-02T14:39:01-05:00", alert.getSent());
    assertEquals(Alert.Status.Actual, alert.getStatus());
    assertEquals(Alert.MsgType.Alert, alert.getMsgType());
    assertEquals("a source", alert.getSource());
    assertEquals(Alert.Scope.Private, alert.getScope());
    assertEquals("address 1", alert.getAddresses().getValue(0));
    assertEquals("address2", alert.getAddresses().getValue(1));
    assertEquals("abcde", alert.getCode(0));
    assertEquals("fghij", alert.getCode(1));
    assertEquals("a note", alert.getNote());
    assertEquals("reference1", alert.getReferences().getValue(0));
    assertEquals("reference 2", alert.getReferences().getValue(1));
    assertEquals("incident1", alert.getIncidents().getValue(0));
    assertEquals("incident2", alert.getIncidents().getValue(1));
  }

  public void testParseInfo() throws Exception {
    String alertStr = getValidAlertPre(CapValidator.CAP_LATEST_XMLNS)
        + getValidInfoPre()
        + "</info>\n"
        + "<info>\n"
        + "<language>fr-CA</language>\n"
        + "<category>Safety</category>\n"
        + "<event>Homeland Security Advisory System Update</event>\n"
        + "<responseType>Evacuate</responseType>\n"
        + "<responseType>Shelter</responseType>\n"
        + "<urgency>Unknown</urgency>\n"
        + "<severity>Unknown</severity>\n"
        + "<certainty>Unknown</certainty>\n"
        + "<audience>an audience</audience>\n"
        + "<eventCode><valueName>EC</valueName><value>v1</value></eventCode>\n"
        + "<eventCode><valueName>EC2</valueName><value>v2</value></eventCode>\n"
        + "<effective>2003-04-02T14:39:01-05:00</effective>\n"
        + "<onset>2003-04-02T15:39:01+05:00</onset>\n"
        + "<expires>2003-04-02T16:39:01-00:00</expires>\n"
        + "<senderName>Department of Homeland Security</senderName>\n"
        + "<contact>a contact</contact>\n"
        + "</info>\n"
        + "</alert>";

    CapXmlParser parser = new CapXmlParser(true);
    Alert alert = parser.parseFrom(alertStr);
    Info info1 = alert.getInfo(0);
    Info info2 = alert.getInfo(1);
    assertEquals(Info.Category.Security, info1.getCategory(0));
    assertEquals(Info.Category.Safety, info1.getCategory(1));
    assertEquals("Homeland Security Advisory System Update", info1.getEvent());
    assertEquals(Info.Urgency.Unknown_Urgency, info1.getUrgency());
    assertEquals(Info.Severity.Unknown_Severity, info1.getSeverity());
    assertEquals(Info.Certainty.Unknown_Certainty, info1.getCertainty());
    assertEquals("Department of Homeland Security", info1.getSenderName());
    assertEquals("Homeland Security Sets Code ORANGE", info1.getHeadline());
    assertEquals("DHS has set the threat level to ORANGE.",
        info1.getDescription());
    assertEquals("Take Protective Measures.", info1.getInstruction());
    assertEquals("http://www.dhs.gov/dhspublic/display?theme=29",
        info1.getWeb());
    assertEquals("HSAS", info1.getParameter(0).getValueName());
    assertEquals("ORANGE", info1.getParameter(0).getValue());
    assertEquals("p2", info1.getParameter(1).getValueName());
    assertEquals("v2", info1.getParameter(1).getValue());
    assertEquals("an audience", info2.getAudience());
    assertEquals("a contact", info2.getContact());
    assertEquals("EC", info2.getEventCode(0).getValueName());
    assertEquals("v1", info2.getEventCode(0).getValue());
    assertEquals("EC2", info2.getEventCode(1).getValueName());
    assertEquals("v2", info2.getEventCode(1).getValue());
    assertEquals("2003-04-02T14:39:01-05:00", info2.getEffective());
    assertEquals("2003-04-02T15:39:01+05:00", info2.getOnset());
    assertEquals("2003-04-02T16:39:01-00:00", info2.getExpires());
  }
  
  public void testParseArea() throws Exception {
    String alertStr = getValidAlertPre(CapValidator.CAP_LATEST_XMLNS)
        + getValidInfoPre()
        + "<area>"
        + "<areaDesc>U.S. nationwide</areaDesc>"
        + "<polygon>1,2 3,4 5,6 1,2</polygon>"
        + "<polygon>11,12 13,14 15,16 11,12</polygon>"
        + "<circle>1,2 3</circle>"
        + "<circle>4,5 6</circle>"
        + "<geocode><valueName>G1</valueName><value>v1</value></geocode>"
        + "<geocode><valueName>G2</valueName><value>v2</value></geocode>"
        + "<altitude>5.5</altitude>"
        + "<ceiling>6.5</ceiling>"
        + "</area>"
        + "<area>"
        + "<areaDesc>U.S. nationwide</areaDesc>"
        + "</area>"
        + "</info>"
        + "</alert>";

    CapXmlParser parser = new CapXmlParser(true);
    Alert alert = parser.parseFrom(alertStr);
    Area area1 = alert.getInfo(0).getArea(0);
    Area area2 = alert.getInfo(0).getArea(1);
    assertEquals(Polygon.newBuilder()
        .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .addPoint(Point.newBuilder().setLatitude(3).setLongitude(4).build())
        .addPoint(Point.newBuilder().setLatitude(5).setLongitude(6).build())
        .addPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .build(),
        area1.getPolygon(0));
    assertEquals(Polygon.newBuilder()
        .addPoint(Point.newBuilder().setLatitude(11).setLongitude(12).build())
        .addPoint(Point.newBuilder().setLatitude(13).setLongitude(14).build())
        .addPoint(Point.newBuilder().setLatitude(15).setLongitude(16).build())
        .addPoint(Point.newBuilder().setLatitude(11).setLongitude(12).build())
        .build(),
        area1.getPolygon(1));
    assertEquals(Circle.newBuilder()
        .setPoint(Point.newBuilder().setLatitude(1).setLongitude(2).build())
        .setRadius(3).build(),
        area1.getCircle(0));
    assertEquals(Circle.newBuilder().setPoint(Point.newBuilder()
        .setLatitude(4).setLongitude(5).build()).setRadius(6).build(),
        area1.getCircle(1));
    assertEquals("G1", area1.getGeocode(0).getValueName());
    assertEquals("v1", area1.getGeocode(0).getValue());
    assertEquals("G2", area1.getGeocode(1).getValueName());
    assertEquals("v2", area1.getGeocode(1).getValue());
    assertEquals(5.5, area1.getAltitude());
    assertEquals(6.5, area1.getCeiling());
    assertEquals("U.S. nationwide", area2.getAreaDesc());
  }

  public void testParseResource() throws Exception {
    String alertStr = getValidAlertPre(CapValidator.CAP_LATEST_XMLNS)
        + getValidInfoPre()
        + "<resource>"
        + "<resourceDesc>Image file (GIF)</resourceDesc>"
        + "<mimeType>image/gif</mimeType>"
        + "<size>123</size>"
        + "<derefUri>a deref uri</derefUri>"
        + "<digest>a digest</digest>"
        + "</resource>"
        + "<resource>"
        + "<resourceDesc>Image file (GIF)</resourceDesc>"
        + "<mimeType>image/jpeg</mimeType>"
        + "<uri>http://www.dhs.gov/dhspublic/getAdvisoryImage</uri>"
        + "</resource>"
        + "</info>"
        + "</alert>";

    CapXmlParser parser = new CapXmlParser(true);
    Alert alert = parser.parseFrom(alertStr);
    Resource resource1 = alert.getInfo(0).getResource(0);
    Resource resource2 = alert.getInfo(0).getResource(1);
    assertEquals("image/gif", resource1.getMimeType());
    assertEquals(123, resource1.getSize());
    assertEquals("a deref uri", resource1.getDerefUri());
    assertEquals("a digest", resource1.getDigest());
    assertEquals("Image file (GIF)", resource2.getResourceDesc());
    assertEquals("http://www.dhs.gov/dhspublic/getAdvisoryImage",
        resource2.getUri());
  }

  @SuppressWarnings("deprecation")
  public void testCap10() throws Exception {
    // Testing deprecated password, certainty, parameter, eventcode, geocode
    String alertStr = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
        + "<alert xmlns=\"" + CapValidator.CAP10_XMLNS + "\">"
        + "<identifier>43b080713727</identifier>\n"
        + "<sender>hsas@dhs.gov</sender>\n"
        + "<sent>2003-04-02T14:39:01-05:00</sent>\n"
        + "<status>Actual</status>\n"
        + "<msgType>Alert</msgType>\n"
        + "<password>obsolete</password>"
        + "<info>"
        + "<category>Safety</category>\n"
        + "<event>Homeland Security Advisory System Update</event>\n"
        + "<urgency>Unknown</urgency>\n"
        + "<severity>Unknown</severity>\n"
        + "<certainty>Very Likely</certainty>"
        + "<eventCode>EC=v1</eventCode>"
        + "<parameter>HSAS=ORANGE</parameter>"
        + "<area>"
        + "<areaDesc>U.S. nationwide</areaDesc>"
        + "<geocode>G1=v1</geocode>"
        + "</area>"
        + "</info>"
        + "</alert>";

    CapXmlParser parser = new CapXmlParser(true);
    Alert alert = parser.parseFrom(alertStr);
    Info info = alert.getInfo(0);
    Area area = info.getArea(0);
    assertEquals(CapValidator.CAP10_XMLNS , alert.getXmlns());
    assertEquals("obsolete", alert.getPassword());
    assertEquals(Info.Certainty.VeryLikely, info.getCertainty());
    assertEquals("EC", info.getEventCode(0).getValueName());
    assertEquals("v1", info.getEventCode(0).getValue());
    assertEquals("HSAS", info.getParameter(0).getValueName());
    assertEquals("ORANGE", info.getParameter(0).getValue());
    assertEquals("G1", area.getGeocode(0).getValueName());
    assertEquals("v1", area.getGeocode(0).getValue());
  }

  public void testCap11() throws Exception {
    // Testing new status Draft, scope required, new category CBRNE,
    // new responseType Evacuate, new derefUri in resource
    String alertStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<alert xmlns=\"" + CapValidator.CAP11_XMLNS + "\">\n"
        + "  <identifier>43b080713727</identifier>\n"
        + "  <sender>hsas@dhs.gov</sender>\n"
        + "  <sent>2003-04-02T14:39:01-05:00</sent>\n"
        + "  <status>Draft</status>\n"
        + "  <msgType>Alert</msgType>\n"
        + "  <scope>Public</scope>\n"
        + "  <info>\n"
        + "    <category>CBRNE</category>\n"
        + "    <event>Homeland Security Advisory System Update</event>\n"
        + "    <responseType>Evacuate</responseType>\n"
        + "    <urgency>Unknown</urgency>\n"
        + "    <severity>Unknown</severity>\n"
        + "    <certainty>Likely</certainty>"
        + "    <resource>\n"
        + "      <resourceDesc>resource desc</resourceDesc>\n"
        + "      <derefUri>deref uri</derefUri>\n"
        + "    </resource>\n"
        + "  </info>\n"
        + "</alert>\n";

    CapXmlParser parser = new CapXmlParser(true);
    Alert alert = parser.parseFrom(alertStr);
    Info info = alert.getInfo(0);
    assertEquals(CapValidator.CAP11_XMLNS, alert.getXmlns());
    assertEquals(Alert.Status.Draft, alert.getStatus());
    assertEquals(Alert.Scope.Public, alert.getScope());
    assertEquals(Info.Category.CBRNE, info.getCategory(0));
    assertEquals(Info.ResponseType.Evacuate, info.getResponseType(0));
    assertEquals("deref uri", info.getResource(0).getDerefUri());
  }

  public void testCap12() throws Exception {
    // Testing new responseType's Avoid and AllClear
    String alertStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<alert xmlns=\"" + CapValidator.CAP12_XMLNS + "\">"
        + "  <identifier>43b080713727</identifier>\n"
        + "  <sender>hsas@dhs.gov</sender>\n"
        + "  <sent>2003-04-02T14:39:01-05:00</sent>\n"
        + "  <status>Actual</status>\n"
        + "  <msgType>Alert</msgType>\n"
        + "  <scope>Public</scope>\n"
        + "  <info>"
        + "    <category>CBRNE</category>\n"
        + "    <event>Homeland Security Advisory System Update</event>\n"
        + "    <responseType>Avoid</responseType>"
        + "    <responseType>AllClear</responseType>"
        + "    <urgency>Unknown</urgency>\n"
        + "    <severity>Unknown</severity>\n"
        + "    <certainty>Likely</certainty>"
        + "  </info>"
        + "</alert>";

    CapXmlParser parser = new CapXmlParser(true);
    Alert alert = parser.parseFrom(alertStr);
    Info info = alert.getInfo(0);
    assertEquals(CapValidator.CAP12_XMLNS, alert.getXmlns());
    assertEquals(Info.ResponseType.Avoid, info.getResponseType(0));
    assertEquals(Info.ResponseType.AllClear, info.getResponseType(1));
  }

  public void testIgnoreDigitalSignature() throws Exception {
    String alertStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<alert xmlns=\"" + CapValidator.CAP_LATEST_XMLNS + "\">"
        + "<identifier>43b080713727</identifier>\n"
        + "<sender>hsas@dhs.gov</sender>\n"
        + "<sent>2003-04-02T14:39:01-05:00</sent>\n"
        + "<status>Actual</status>\n"
        + "<msgType>Alert</msgType>\n"
        + "<scope>Public</scope>\n"
        + "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">"
        + "<SignedInfo>"
        + "<CanonicalizationMethod "
        + "Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments\"/>"
        + "<SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#dsa-sha1\"/>"
        + "<Reference URI=\"\">"
        + "<Transforms>"
        + "<Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>"
        + "</Transforms>"
        + "<DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>"
        + "<DigestValue>uooqbWYa5VCqcJCbuymBKqm17vY=</DigestValue>"
        + "</Reference>"
        + "</SignedInfo>"
        + "<SignatureValue>KedJuTob5gtvYx9qM3k3gm7kbLBwVbEQRl26S2tmXjqNND7MRGtoew==</SignatureValue>"
        + "<KeyInfo>"
        + "<KeyValue>"
        + "<DSAKeyValue>"
        + "<P>"
        + "/KaCzo4Syrom78z3EQ5SbbB4sF7ey80etKII864WF64B81uRpH5t9jQTxeEu0ImbzRMqzVDZkVG9xD7nN1kuFw=="
        + "</P>"
        + "<Q>li7dzDacuo67Jg7mtqEm2TRuOMU=</Q>"
        + "<G>Z4Rxsnqc9E7pGknFFH2xqaryRPBaQ01khpMdLRQnG541Awtx/XPaF5Bpsy4pNWMOHCBiNU0NogpsQW5QvnlMpA=="
        + "</G>"
        + "<Y>qV38IqrWJG0V/mZQvRVi1OHw9Zj84nDC4jO8P0axi1gb6d+475yhMjSc/BrIVC58W3ydbkK+Ri4OKbaRZlYeRA=="
        + "</Y>"
        + "</DSAKeyValue>"
        + "</KeyValue>"
        + "</KeyInfo>"
        + "</Signature>"
        + "</alert>";

    CapXmlParser parser = new CapXmlParser(true);
    Alert alert = parser.parseFrom(alertStr);
    assertEquals(CapValidator.CAP_LATEST_XMLNS, alert.getXmlns());
  }

  public void testParseXmlSyntaxError() throws Exception{
    CapXmlParser parser = new CapXmlParser(true);
    try {
      parser.parseFrom("<alert</alert>");
      fail("Expected SAXParseException");
    } catch (SAXParseException expected) {
      // expected
    }
  }

  public void testParseValidationError() throws Exception {
    String alertStr = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
        + "<alert xmlns=\"" + CapValidator.CAP_LATEST_XMLNS + "\">"
        + "<identifier>43b080713727</identifier>\n"
        + "<sender>hsas@dhs.gov</sender>\n"
        + "<sent>2003-04-02T14:39:01-05:00</sent>\n"
        + "<status>Actual</status>\n"
        + "<msgType>Alert</msgType>\n"
        + "<scope>Invalid</scope>"
        + "</alert>";
    CapXmlParser parser = new CapXmlParser(true);
    try {
      parser.parseFrom(alertStr);
      fail("Expected a CapException");
    } catch (CapException e) {
      assertEquals(2, e.getReasons().size());
      assertEquals(Type.INVALID_ENUM_VALUE, e.getReasons().get(0).getType());
      assertEquals(Type.SCOPE_IS_REQUIRED, e.getReasons().get(1).getType());
    }
  }

  public void testParseRequiredTagError() throws Exception {
    String alertStr = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
        + "<alert xmlns=\"" + CapValidator.CAP_LATEST_XMLNS + "\">"
        + "<identifier>43b080713727</identifier>\n"
        + "<sender>hsas@dhs.gov</sender>\n"
        + "<sent>2003-04-02T14:39:01-05:00</sent>\n"
        + "<status>Actual</status>\n"
        + "<msgType>Alert</msgType>\n"
        + "<scoep>Public</scoep>\n"
        + "</alert>";
    CapXmlParser parser = new CapXmlParser(true);
    try {
      parser.parseFrom(alertStr);
      fail("Expected a CapException");
    } catch (CapException e) {
      assertEquals(2, e.getReasons().size());
      assertEquals(Type.UNSUPPORTED_ELEMENT, e.getReasons().get(0).getType());
      assertEquals(Type.SCOPE_IS_REQUIRED, e.getReasons().get(1).getType());
    }
  }

  public void testParseOptionalTagError() throws Exception {
    String alertStr = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
        + "<alert xmlns=\"" + CapValidator.CAP_LATEST_XMLNS + "\">"
        + "<identifier>43b080713727</identifier>\n"
        + "<sender>hsas@dhs.gov</sender>\n"
        + "<sent>2003-04-02T14:39:01-05:00</sent>\n"
        + "<status>Actual</status>\n"
        + "<msgType>Alert</msgType>\n"
        + "<scope>Public</scope>\n"
        + "<address>blah</address>\n" // should be addresses
        + "</alert>";
    CapXmlParser parser = new CapXmlParser(true);
    try {
      parser.parseFrom(alertStr);
      fail("Expected a CapException");
    } catch (CapException e) {
      assertEquals(1, e.getReasons().size());
      assertEquals(Type.UNSUPPORTED_ELEMENT, e.getReasons().get(0).getType());
    }
  }

  public void testParseInfoOutOfOrderIsInvalid() throws Exception {
    String alertStr = getValidAlertPre(CapValidator.CAP_LATEST_XMLNS)
        + "<info>\n"
        + "<audience>an audience</audience>\n"
        + "<contact>a contact</contact>\n"
        + "<eventCode><valueName>EC</valueName><value>v1</value></eventCode>\n"
        + "<eventCode><valueName>EC2</valueName><value>v2</value></eventCode>\n"
        + "<effective>2003-04-02T14:39:01-05:00</effective>\n"
        + "<onset>2003-04-02T15:39:01+05:00</onset>\n"
        + "<expires>2003-04-02T16:39:01-00:00</expires>\n"
        + "<category>Safety</category>\n"
        + "<event>Homeland Security Advisory System Update</event>\n"
        + "<urgency>Unknown</urgency>\n"
        + "<severity>Unknown</severity>\n"
        + "<certainty>Unknown</certainty>\n"
        + "<senderName>Department of Homeland Security</senderName>\n"
        + "</info>\n"
        + "</alert>";

    CapXmlParser parser = new CapXmlParser(true);

    try {
      parser.parseFrom(alertStr);
      fail("Expected a CapException");
    } catch (CapException e) {
      assertEquals(1, e.getReasons().size());
      assertEquals(Type.INVALID_SEQUENCE, e.getReasons().get(0).getType());
    }
  }

  public void testParseNotCap() throws Exception {
    String alertStr =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<foo></foo>";
    CapXmlParser parser = new CapXmlParser(true);
    try {
      parser.parseFrom(alertStr);
      fail("Expected a CapException");
    } catch (NotCapException expected) {
      // expected
    }

    alertStr = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
        + "<alert xmlns=\"foo\"></alert>";
    try {
      parser.parseFrom(alertStr);
      fail("Expected a CapException");
    } catch (NotCapException expected) {
      // expected
    }
  }

  public void testParseNoValidation() throws Exception {
    String alertStr = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
        + "<alert xmlns=\"" + CapValidator.CAP_LATEST_XMLNS + "\">"
        + "<scope>Invalid</scope>"
        + "</alert>";
    CapXmlParser parser = new CapXmlParser(false);
    parser.parseFrom(alertStr);
  }

  public void testUnescaping() throws Exception {
    String alertStr = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
        + "<alert xmlns=\"" + CapValidator.CAP_LATEST_XMLNS + "\">"
        + "<note>&amp;&quot;&apos;&lt;&gt;</note>"
        + "</alert>";
    CapXmlParser parser = new CapXmlParser(false);
    Alert alert = parser.parseFrom(alertStr);
    assertEquals("&\"'<>", alert.getNote());
  }

  public void testSetOrAdd() {
    CapXmlHandler handler = new CapXmlHandler();
    Alert.Builder alert = Alert.newBuilder();

    // Single field
    FieldDescriptor fd = alert.getDescriptorForType().findFieldByName("xmlns");
    assertFalse(fd.isRepeated());
    handler.setOrAdd(alert, fd, null);
    assertFalse(alert.hasXmlns());
    assertTrue(handler.getParseErrors().isEmpty());
    handler.setOrAdd(alert, fd, "foo");
    assertEquals("foo", alert.getXmlns());
    assertTrue(handler.getParseErrors().isEmpty());

    handler.setOrAdd(alert, fd, "bar");
    assertEquals("foo", alert.getXmlns());
    assertEquals(1, handler.getParseErrors().size());
    assertEquals(Type.DUPLICATE_ELEMENT,
        handler.getParseErrors().get(0).getType());

    // Repeated field
    handler = new CapXmlHandler();
    fd = alert.getDescriptorForType().findFieldByName("code");
    assertTrue(fd.isRepeated());
    handler.setOrAdd(alert, fd, "foo");
    assertEquals("foo", alert.getCode(0));
    assertTrue(handler.getParseErrors().isEmpty());
    handler.setOrAdd(alert, fd, "bar");
    assertEquals("foo", alert.getCode(0));
    assertEquals("bar", alert.getCode(1));
    assertTrue(handler.getParseErrors().isEmpty());
  }

  public void testGetPrimitiveValue() {
    CapXmlHandler handler = new CapXmlHandler();
    Alert.Builder alert = Alert.newBuilder();
    Resource.Builder resource = Resource.newBuilder();
    Point.Builder point = Point.newBuilder();

    FieldDescriptor fd = alert.getDescriptorForType().findFieldByName("xmlns");
    assertEquals(FieldDescriptor.Type.STRING, fd.getType());
    assertEquals("1", handler.getPrimitiveValue(fd, "1"));

    fd = alert.getDescriptorForType().findFieldByName("status");
    assertEquals(FieldDescriptor.Type.ENUM, fd.getType());
    assertEquals(Alert.Status.Actual.getNumber(),
        ((Descriptors.EnumValueDescriptor) handler
            .getPrimitiveValue(fd, "Actual")).getNumber());

    fd = resource.getDescriptorForType().findFieldByName("size");
    assertEquals(FieldDescriptor.Type.INT64, fd.getType());
    assertEquals(1L, handler.getPrimitiveValue(fd, "1"));

    fd = point.getDescriptorForType().findFieldByName("latitude");
    assertEquals(FieldDescriptor.Type.DOUBLE, fd.getType());
    assertEquals(1D, handler.getPrimitiveValue(fd, "1"));
  }

  public void testGetPrimitiveValueErrors() {
    CapXmlHandler handler = new CapXmlHandler();
    Alert.Builder alert = Alert.newBuilder();
    Resource.Builder resource = Resource.newBuilder();
    Point.Builder point = Point.newBuilder();

    int errorCount = 0;

    FieldDescriptor fd = alert.getDescriptorForType().findFieldByName("status");
    assertEquals(FieldDescriptor.Type.ENUM, fd.getType());
    assertNull(handler.getPrimitiveValue(fd, "Foo"));
    assertEquals(errorCount, handler.getParseErrors().size());
    // Capitalization must be correct
    assertNull(handler.getPrimitiveValue(fd, "actual"));
    assertEquals(errorCount, handler.getParseErrors().size());
    assertNull(handler.getPrimitiveValue(fd, ""));
    assertEquals(errorCount, handler.getParseErrors().size());
    assertNull(handler.getPrimitiveValue(fd, null));
    assertEquals(errorCount, handler.getParseErrors().size());

    fd = resource.getDescriptorForType().findFieldByName("size");
    assertEquals(FieldDescriptor.Type.INT64, fd.getType());
    assertNull(handler.getPrimitiveValue(fd, "a"));
    assertEquals(++errorCount, handler.getParseErrors().size());
    assertNull(handler.getPrimitiveValue(fd, ""));
    assertEquals(++errorCount, handler.getParseErrors().size());
    assertNull(handler.getPrimitiveValue(fd, null));
    assertEquals(++errorCount, handler.getParseErrors().size());

    fd = point.getDescriptorForType().findFieldByName("latitude");
    assertEquals(FieldDescriptor.Type.DOUBLE, fd.getType());
    assertNull(handler.getPrimitiveValue(fd, "a"));
    assertEquals(++errorCount, handler.getParseErrors().size());
    assertNull(handler.getPrimitiveValue(fd, ""));
    assertEquals(++errorCount, handler.getParseErrors().size());
    assertNull(handler.getPrimitiveValue(fd, null));
    assertEquals(++errorCount, handler.getParseErrors().size());
  }

  public void testGetComplexValue() {
    CapXmlHandler handler = new CapXmlHandler();
    Point point = Point.newBuilder().setLatitude(1.5).setLongitude(2.5).build();
    Point point2 = Point.newBuilder().setLatitude(-2).setLongitude(-3).build();
    Point point3 = Point.newBuilder().setLatitude(3).setLongitude(4).build();

    assertEquals(Polygon.newBuilder().addPoint(point).addPoint(point2)
        .addPoint(point3).build(),
        handler.getComplexValue(
            Polygon.newBuilder(), "polygon", "1.5,2.5 -2,-3 3,4"));
    assertEquals(Circle.newBuilder().setPoint(point).setRadius(3.5).build(),
        handler.getComplexValue(Circle.newBuilder(), "circle", "1.5,2.5 3.5"));
    assertEquals(Group.newBuilder().addValue("a").addValue("b").build(),
        handler.getComplexValue(Group.newBuilder(), "addresses", "a b"));
    assertEquals(Group.newBuilder().addValue("a").addValue("b").build(),
        handler.getComplexValue(Group.newBuilder(), "references", "a b"));
    assertEquals(Group.newBuilder().addValue("a").addValue("b").build(),
        handler.getComplexValue(Group.newBuilder(), "incidents", "a b"));
  }

  public void testToPolygon() {
    CapXmlHandler handler = new CapXmlHandler();
    Point point = Point.newBuilder().setLatitude(1.5).setLongitude(2.5).build();
    Point point2 = Point.newBuilder().setLatitude(-2).setLongitude(-3).build();
    Point point3 = Point.newBuilder().setLatitude(3).setLongitude(4).build();

    int errorCount = 0;
    assertEquals(Polygon.newBuilder().addPoint(point).addPoint(point2)
        .addPoint(point3).build(), handler.toPolygon("1.5,2.5 -2,-3 3,4"));
    assertEquals(Polygon.newBuilder().addPoint(point).addPoint(point2)
        .addPoint(point3).build(), handler.toPolygon("1.5,2.5 \t -2,-3  3,4"));
    assertNull(handler.toPolygon("invalid"));
    assertEquals(++errorCount, handler.getParseErrors().size());
    assertNull(handler.toPolygon("1,23,4 5,6"));
    assertEquals(++errorCount, handler.getParseErrors().size());
    assertNull(handler.toPolygon(""));
    assertEquals(errorCount, handler.getParseErrors().size());
  }

  public void testToCircle() {
    CapXmlHandler handler = new CapXmlHandler();
    Point point = Point.newBuilder().setLatitude(1).setLongitude(2).build();
    int errorCount = 0;
    assertEquals(Circle.newBuilder().setPoint(point).setRadius(3).build(),
        handler.toCircle("1,2 3"));
    assertEquals(Circle.newBuilder().setPoint(point).setRadius(3.125).build(),
        handler.toCircle("1.00,2.00  \t 3.125"));
    assertNull(handler.toCircle("1, 2 3"));
    assertEquals(++errorCount, handler.getParseErrors().size());
    assertNull(handler.toCircle("invalid"));
    assertEquals(++errorCount, handler.getParseErrors().size());
    assertNull(handler.toCircle(""));
    assertEquals(++errorCount, handler.getParseErrors().size());
  }

  public void testToPoint() {
    CapXmlHandler handler = new CapXmlHandler();
    assertEquals(Point.newBuilder().setLatitude(1).setLongitude(2).build(),
        handler.toPoint("1,2"));
    assertEquals(Point.newBuilder().setLatitude(-1).setLongitude(-2).build(),
        handler.toPoint("-1.0,   -2.0"));
    assertNull(handler.toPoint("1.0, 2.0 a"));
    assertNull(handler.toPoint("invalid"));
    assertNull(handler.toPoint("a 1,2"));
    assertNull(handler.toPoint(""));
  }

  public void testToGroup() {
    CapXmlHandler handler = new CapXmlHandler();
    assertEquals(Group.newBuilder().addValue("a").addValue("b").build(),
        handler.toGroup("a b"));
    assertEquals(Group.newBuilder().addValue("a").addValue("b").build(),
        handler.toGroup("a    b"));
    assertEquals(Group.newBuilder().addValue("a,b").build(),
        handler.toGroup("a,b"));
    assertEquals(Group.newBuilder().addValue("a b").addValue("c").build(),
        handler.toGroup("\"a b\" c"));
    assertNull(handler.toGroup("              "));
    assertNull(handler.toGroup("     \t         "));
    assertNull(handler.toGroup(""));
  }
}

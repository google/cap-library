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

package com.google.publicalerts.cap.edxl.io;

import com.google.publicalerts.cap.edxl.DistributionFeed;
import com.google.publicalerts.cap.edxl.types.ContentObject;
import com.google.publicalerts.cap.edxl.types.TargetArea;
import com.google.publicalerts.cap.feed.CapFeedParser;
import com.google.publicalerts.cap.testing.TestResources;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.io.FeedException;

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.StringReader;
import java.util.List;

/**
 * Tests for {@link Edxlde10Parser}.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public class Edxlde10ParserTest extends TestCase {

  private final Edxlde10Parser parser = new Edxlde10Parser();
  private final CapFeedParser capParser = new CapFeedParser(true);
  private final SAXBuilder builder = new SAXBuilder();

  private final String xml =
    "<edxlde:EDXLDistribution " +
      "xmlns:edxlde=\"urn:oasis:names:tc:emergency:EDXL:DE:1.0\">" +
      "<edxlde:distributionID>RFSuniqueID12345</edxlde:distributionID>" +
      "<edxlde:senderID>webmaster@rfs.nsw.gov.au</edxlde:senderID>" +
      "<edxlde:dateTimeSent>2011-10-17T11:18:00-00:00" +
      "</edxlde:dateTimeSent>" +
      "<edxlde:distributionStatus>Actual</edxlde:distributionStatus>" +
      "<edxlde:distributionType>Report</edxlde:distributionType>" +
      "<edxlde:combinedConfidentiality>UNCLASSIFIED" +
      "</edxlde:combinedConfidentiality></edxlde:EDXLDistribution>";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testNamespaceCreated() throws Exception {
    Namespace ns = parser.getEdxldeNamespace();
    assertEquals(ns.getURI(), parser.getEdxldeUri());
  }

  public void testValidType() throws Exception {
    String feed = TestResources.load("bushfire_valid.edxlde");
    String feedType = capParser.parseFeed(feed).getFeedType();
    assertTrue(feedType.equals(parser.getType()));
  }

  public void testInvalidType() throws Exception {
    String feed = TestResources.load("weather.atom");
    String feedType = capParser.parseFeed(feed).getFeedType();
    assertFalse(feedType.equals(parser.getType()));
  }

  public void testParse() throws Exception {
    String feedStr = TestResources.load("bushfire_valid.edxlde");
    Document doc = builder.build(new StringReader(feedStr));
    WireFeed wireFeed = parser.parse(doc, false);
    assertEquals(wireFeed.getFeedType(), parser.getType());
    if (wireFeed instanceof DistributionFeed) {
      DistributionFeed feed = (DistributionFeed) wireFeed;

      // Verify required fields
      assertEquals(feed.getDistributionId(), "RFSuniqueID12345");
      assertEquals(feed.getSenderId(), "webmaster@rfs.nsw.gov.au");
      assertEquals(feed.getDateTimeSent(), "2011-10-17T11:18:00-00:00");
      assertEquals(feed.getDistributionStatus(), "ACTUAL");
      assertEquals(feed.getDistributionType(), "REPORT");
      assertEquals(feed.getCombinedConfidentiality(), "UNCLASSIFIED");

      // Verify optional fields
      assertEquals(feed.getLanguage(), "en-AU");
      assertEquals(feed.getSenderRoles().size(), 0);
      assertEquals(feed.getRecipientRoles().size(), 0);
      assertEquals(feed.getKeywords().size(), 0);
      assertEquals(feed.getDistributionReferences().size(), 0);
      assertEquals(feed.getExplicitAddresses().size(), 0);

      // Verify Target Area
      assertEquals(feed.getTargetArea().size(), 1);
      TargetArea targetArea = feed.getTargetArea().get(0);
      assertEquals(targetArea.getCircle().size(), 0);
      assertEquals(targetArea.getPolygon().size(), 0);
      assertEquals(targetArea.getCountry().size(), 0);
      assertEquals(targetArea.getSubdivision().size(), 1);
      assertEquals(targetArea.getSubdivision().get(0), "AU-NSW");
      assertEquals(targetArea.getLocCode().size(), 0);

      // Verify Content Objects
      assertEquals(feed.getContentObjects().size(), 7);

      // All content objects in the sample XML are roughly the same, so choose
      // the first one in the list to do a detailed verification.
      ContentObject content = feed.getContentObjects().get(0);
      assertEquals(content.getContentDescription(),
          "Information on One Tree MVA (Unmapped Incident)");
      assertEquals(content.getContentKeywords().size(), 0);
      assertNull(content.getIncidentId());
      assertNull(content.getIncidentDescription());
      assertTrue(content.getOriginatorRoles().isEmpty());
      assertEquals(content.getConsumerRoles().size(), 0);
      assertNull(content.getConfidentiality());
      assertNull(content.getNonXmlContent());

      // Verify XML content
      assertNotNull(content.getXmlContent());
      assertEquals(content.getXmlContent().getKeyXmlContent().size(), 0);
      List<String> xmlContent = content.getXmlContent().getEmbeddedXmlContent();
      assertEquals(xmlContent.size(), 1);
      assertTrue(xmlContent.get(0).contains("<cap:alert"));
    } else {
      fail("Parser did not build a DistributionFeed instance.");
    }
  }

  public void testMissingRequiredFields() throws Exception {
    String distId =
        "<edxlde:distributionID>RFSuniqueID12345</edxlde:distributionID>";
    String senderId =
        "<edxlde:senderID>webmaster@rfs.nsw.gov.au</edxlde:senderID>";
    String dateTime =
        "<edxlde:dateTimeSent>2011-10-17T11:18:00-00:00</edxlde:dateTimeSent>";
    String distStatus =
        "<edxlde:distributionStatus>Actual</edxlde:distributionStatus>";
    String distType =
        "<edxlde:distributionType>Report</edxlde:distributionType>";
    String confidentiality = "<edxlde:combinedConfidentiality>"
        + "UNCLASSIFIED</edxlde:combinedConfidentiality>";

    StringBuilder sb = new StringBuilder(xml);
    removeField("combinedConfidentiality", sb, confidentiality);
    removeField("distributionType", sb, distType);
    removeField("distributionStatus", sb, distStatus);
    removeField("dateTimeSent", sb, dateTime);
    removeField("senderID", sb, senderId);
    removeField("distributionID", sb, distId);
  }

  private void removeField(String name, StringBuilder sb, String tag)
      throws Exception {
    // Ensure the original XML contains the tag, then remove it
    assertTrue(sb.toString().contains(name));
    int start = sb.indexOf(tag);
    sb.delete(start, start + tag.length());
    assertFalse(sb.toString().contains(name));

    // Build a new document with the required tag removed
    Document doc = builder.build(new StringReader(sb.toString()));
    DistributionFeed feed = new DistributionFeed();

    // Parse should now throw an exception
    try {
      parser.parseRequiredFields(doc.getRootElement(), feed);
      fail("FeedException expected.");
    } catch (FeedException e) {
      assertEquals(e.getMessage(), "Missing required tag " + name);
    }
  }
}

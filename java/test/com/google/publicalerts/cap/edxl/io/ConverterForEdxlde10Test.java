// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.edxl.io;

import com.google.publicalerts.cap.edxl.DistributionFeed;
import com.google.publicalerts.cap.edxl.types.ContentObject;
import com.google.publicalerts.cap.testing.TestResources;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import java.io.StringReader;
import java.util.List;

/**
 * Tests for {@link ConverterForEdxlde10}.
 *
 * TODO(anshul): Increase test coverage.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public class ConverterForEdxlde10Test extends TestCase {

  private final ConverterForEdxlde10 converter = new ConverterForEdxlde10();
  private final Edxlde10Parser parser = new Edxlde10Parser();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testCopyInto() throws Exception {
    String feedStr = TestResources.load("bushfire_valid.edxlde");
    Document doc = new SAXBuilder().build(new StringReader(feedStr));
    DistributionFeed feed = (DistributionFeed) parser.parse(doc, false);
    SyndFeed syndFeed = new SyndFeedImpl();
    converter.copyInto(feed, syndFeed);

    assertEquals("RFSuniqueID12345", syndFeed.getUri());
    assertEquals(1318850280000L, syndFeed.getPublishedDate().getTime());
    assertEquals(7, syndFeed.getEntries().size());

    @SuppressWarnings("unchecked")
    List<SyndEntry> syndEntries = syndFeed.getEntries();
    for (int i = 0; i < syndEntries.size(); i++) {
      SyndEntry entry = syndEntries.get(i);
      ContentObject contentObject = feed.getContentObjects().get(i);
      assertEquals(entry.getTitle(), contentObject.getContentDescription());

      @SuppressWarnings("unchecked")
      List<SyndContent> syndContent = entry.getContents();
      for (int j = 0; j < syndContent.size(); j++) {
        assertEquals(syndContent.get(j).getValue(),
            contentObject.getXmlContent().getEmbeddedXmlContent().get(j));
      }
    }
  }
}

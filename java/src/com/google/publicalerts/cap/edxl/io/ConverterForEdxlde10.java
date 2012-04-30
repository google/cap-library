// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.edxl.io;

import com.google.publicalerts.cap.edxl.DistributionFeed;
import com.google.publicalerts.cap.edxl.types.ContentObject;
import com.google.publicalerts.cap.edxl.types.XmlContent;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.module.impl.ModuleUtils;
import com.sun.syndication.feed.synd.Converter;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

/**
 * Converter plugin for EDXL-DE 1.0.
 *
 * TODO(anshul): Populate more of the SyndFeed/SyndEntry fields.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public class ConverterForEdxlde10 implements Converter {
  private String type;

  public ConverterForEdxlde10() {
    this(Edxlde10Parser.TYPE);
  }

  protected ConverterForEdxlde10(String type) {
    this.type = type;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void copyInto(WireFeed feed, SyndFeed syndFeed) {
    DistributionFeed distributionFeed = (DistributionFeed) feed;
    syndFeed.setModules(ModuleUtils.cloneModules(feed.getModules()));
    @SuppressWarnings("unchecked")
    List foreignMarkup = (List) feed.getForeignMarkup();
    if (!foreignMarkup.isEmpty()) {
      syndFeed.setForeignMarkup(feed.getForeignMarkup());
    }
    syndFeed.setEncoding(feed.getEncoding());
    syndFeed.setUri(distributionFeed.getDistributionId());
    syndFeed.setPublishedDate(DatatypeConverter
        .parseDateTime(distributionFeed.getDateTimeSent()).getTime());

    List<ContentObject> contentObjects = distributionFeed.getContentObjects();
    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    for (ContentObject o : contentObjects) {
      SyndEntryImpl syndEntry = new SyndEntryImpl();
      syndEntry.setTitle(o.getContentDescription());
      syndEntry.setContents(createSyndContent(o));
      entries.add(syndEntry);
    }
    syndFeed.setEntries(entries);
  }

  private List<SyndContent> createSyndContent(ContentObject contentObject) {
    List<SyndContent> list = new ArrayList<SyndContent>();
    XmlContent xmlContent = contentObject.getXmlContent();

    if (xmlContent == null) {
      return list;
    }

    for (String xml : xmlContent.getEmbeddedXmlContent()) {
      SyndContentImpl syndContent = new SyndContentImpl();
      syndContent.setValue(xml);
      list.add(syndContent);
    }

    return list;
  }

  @Override
  public WireFeed createRealFeed(SyndFeed syndFeed) {
    // TODO(anshul): Implement this method
    return null;
  }
}
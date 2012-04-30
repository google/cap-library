// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.edxl.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean to hold <xmlContent> data from EDXL-DE feeds.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public class XmlContent {
  private List<String> keyXmlContent = new ArrayList<String>();
  private List<String> embeddedXmlContent = new ArrayList<String>();

  public void setKeyXmlContent(List<String> keyXmlContent) {
    this.keyXmlContent = keyXmlContent;
  }

  public List<String> getKeyXmlContent() {
    return keyXmlContent;
  }

  public void addKeyXmlContent(String value) {
    keyXmlContent.add(value);
  }

  public void setEmbeddedXmlContent(List<String> embeddedXmlContent) {
    this.embeddedXmlContent = embeddedXmlContent;
  }

  public List<String> getEmbeddedXmlContent() {
    return embeddedXmlContent;
  }

  public void addEmbeddedXmlContent(String value) {
    embeddedXmlContent.add(value);
  }
}

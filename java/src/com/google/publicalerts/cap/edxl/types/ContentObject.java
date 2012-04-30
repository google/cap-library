// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.edxl.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean to hold <contentObject> data from EDXL-DE feeds.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public final class ContentObject {
  private String contentDescription;
  private List<ValueList> contentKeywords;
  private String incidentId;
  private String incidentDescription;
  private List<ValueList> originatorRoles;
  private List<ValueList> consumerRoles;
  private String confidentiality;
  private NonXmlContent nonXmlContent;
  private XmlContent xmlContent;

  // TODO(anshul): Populate this field
  // private other;

  public ContentObject() {
    contentKeywords = new ArrayList<ValueList>();
    originatorRoles = new ArrayList<ValueList>();
    consumerRoles = new ArrayList<ValueList>();
  }

  public String getContentDescription() {
    return contentDescription;
  }

  public void setContentDescription(String contentDescription) {
    this.contentDescription = contentDescription;
  }

  public List<ValueList> getContentKeywords() {
    return contentKeywords;
  }

  public void setContentKeywords(List<ValueList> contentKeywords) {
    this.contentKeywords = contentKeywords;
  }

  public String getIncidentId() {
    return incidentId;
  }

  public void setIncidentId(String incidentId) {
    this.incidentId = incidentId;
  }

  public String getIncidentDescription() {
    return incidentDescription;
  }

  public void setIncidentDescription(String incidentDescription) {
    this.incidentDescription = incidentDescription;
  }

  public List<ValueList> getOriginatorRoles() {
    return originatorRoles;
  }

  public void setOriginatorRoles(List<ValueList> originatorRoles) {
    this.originatorRoles = originatorRoles;
  }

  public List<ValueList> getConsumerRoles() {
    return consumerRoles;
  }

  public void setConsumerRoles(List<ValueList> consumerRoles) {
    this.consumerRoles = consumerRoles;
  }

  public String getConfidentiality() {
    return confidentiality;
  }

  public void setConfidentiality(String confidentiality) {
    this.confidentiality = confidentiality;
  }

  public NonXmlContent getNonXmlContent() {
    return nonXmlContent;
  }

  public void setNonXmlContent(NonXmlContent nonXmlContent) {
    this.nonXmlContent = nonXmlContent;
  }

  public XmlContent getXmlContent() {
    return xmlContent;
  }

  public void setXmlContent(XmlContent xmlContent) {
    this.xmlContent = xmlContent;
  }
}

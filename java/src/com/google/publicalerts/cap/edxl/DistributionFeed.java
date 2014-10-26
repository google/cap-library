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

package com.google.publicalerts.cap.edxl;

import com.google.publicalerts.cap.edxl.types.ContentObject;
import com.google.publicalerts.cap.edxl.types.TargetArea;
import com.google.publicalerts.cap.edxl.types.ValueList;

import com.sun.syndication.feed.WireFeed;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean for EDXL-DE feeds.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public final class DistributionFeed extends WireFeed {
  private String distributionId;
  private String senderId;
  private String dateTimeSent;
  private DistributionStatus distributionStatus;
  private DistributionType distributionType;
  private String combinedConfidentiality;
  private String language;
  private List<ValueList> senderRoles = new ArrayList<ValueList>();
  private List<ValueList> recipientRoles = new ArrayList<ValueList>();
  private List<ValueList> keywords = new ArrayList<ValueList>();
  private List<String> distributionReferences = new ArrayList<String>();
  private List<ValueList> explicitAddresses = new ArrayList<ValueList>();
  private List<TargetArea> targetArea = new ArrayList<TargetArea>();
  private List<ContentObject> contentObjects = new ArrayList<ContentObject>();

  private enum DistributionStatus {
    ACTUAL,
    EXERCISE,
    SYSTEM,
    TEST
  }

  private enum DistributionType {
    REPORT,
    UPDATE,
    CANCEL,
    REQUEST,
    RESPONSE,
    DISPATH,
    ACK,
    ERROR,
    SENSORCONFIGURATION,
    SENSORCONTROL,
    SENSORSTATUS,
    SENSORDETECTION
  }

  public DistributionFeed() {
  }

  /**
   * @param type the type of the RSS feed.
   */
  public DistributionFeed(String type) {
    super(type);
  }

  public String getDistributionId() {
    return distributionId;
  }

  public void setDistributionId(String distributionId) {
    this.distributionId = distributionId;
  }

  public String getSenderId() {
    return senderId;
  }

  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  public String getDateTimeSent() {
    return dateTimeSent;
  }

  public void setDateTimeSent(String dateTimeSent) {
    this.dateTimeSent = dateTimeSent;
  }

  public String getCombinedConfidentiality() {
    return combinedConfidentiality;
  }

  public void setCombinedConfidentiality(String combinedConfidentiality) {
    this.combinedConfidentiality = combinedConfidentiality;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public List<String> getDistributionReferences() {
    return distributionReferences;
  }

  public void addDistributionReference(String distributionReference) {
    this.distributionReferences.add(distributionReference);
  }

  public String getDistributionStatus() {
    return distributionStatus.name();
  }

  public void setDistributionStatus(String distributionStatus) {
    this.distributionStatus =
        DistributionStatus.valueOf(distributionStatus.toUpperCase());
  }

  public String getDistributionType() {
    return distributionType.name();
  }

  public void setDistributionType(String distributionType) {
    this.distributionType =
        DistributionType.valueOf(distributionType.toUpperCase());
  }

  public void setSenderRoles(List<ValueList> senderRoles) {
    this.senderRoles = senderRoles;
  }

  public List<ValueList> getSenderRoles() {
    return senderRoles;
  }

  public void setRecipientRoles(List<ValueList> recipientRoles) {
    this.recipientRoles = recipientRoles;
  }

  public List<ValueList> getRecipientRoles() {
    return recipientRoles;
  }

  public void setKeywords(List<ValueList> keywords) {
    this.keywords = keywords;
  }

  public List<ValueList> getKeywords() {
    return keywords;
  }

  public void setExplicitAddresses(List<ValueList> explicitAddresses) {
    this.explicitAddresses = explicitAddresses;
  }

  public List<ValueList> getExplicitAddresses() {
    return explicitAddresses;
  }

  public void addContentObject(ContentObject contentObject) {
    this.contentObjects.add(contentObject);
  }

  public List<ContentObject> getContentObjects() {
    return contentObjects;
  }

  public void addTargetArea(TargetArea targetArea) {
    this.targetArea.add(targetArea);
  }

  public List<TargetArea> getTargetArea() {
    return targetArea;
  }
}

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

package com.google.publicalerts.cap.validator;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.Reasons;
import com.google.publicalerts.cap.Reason.Level;
import com.google.publicalerts.cap.XercesCapExceptionMapper;

import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.ParsingFeedException;

/**
 * Holder for the data to be presented as a result of validation.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class ValidationResult {
  private final String xml;
  private final Set<ValidationMessage> validationMessages;
  private final List<Alert> validAlerts;
  private final Timing timing;

  private LineOffsetParser.LineOffsets lineOffsets;

  /**
   * Creates a new validation result.
   *
   * @param xml the XML of the document being validated
   */
  public ValidationResult(String xml) {
    this.xml = xml;
    this.validationMessages = Sets.newLinkedHashSet();
    this.validAlerts = Lists.newArrayList();
    this.timing = new Timing();
  }

  /**
   * Adds a valid alert parsed from the document to the result.
   *
   * @param alert the valid alert to add
   */
  public void addValidAlert(Alert alert) {
    validAlerts.add(alert);
  }

  /**
   * Adds a validation message to the result and HTML-escapes the message.
   *
   * @param lineNum line number of the message
   * @param level level (i.e., severity) of the message
   * @param unescapedMessage the unescaped text of validation message text
   */
  public void addValidationMessage(
      int lineNum, Level level, String source, String unescapedMessage) {
    validationMessages.add(ValidationMessage.withUnescapedMessage(
        lineNum, level, source, unescapedMessage));
  }

  /**
   * Adds the reasons as validation messages to the result.
   *
   * @param reasons the reasons to add
   */
  public void addValidationMessages(Reasons reasons) {
    for (Reason reason : new XercesCapExceptionMapper().map(reasons)) {
      int lineOffset = getLineOffsets().getXPathLineNumber(reason.getXPath());
      addValidationMessage(lineOffset, reason.getLevel(),
          reason.getSource(), reason.getMessage());
    }
  }
  
  /**
   * Adds the reasons stored in the exception as validation messages to the
   * result.
   * 
   * @param capException the exception storing the reasons to add
   */
  public void addValidationMessages(CapException capException) {
    addValidationMessages(capException.getReasons());
  }  

  /**
   * Adds the message corresponding to the given exception to the result.
   *
   * @param e the exception to add
   */
  public void addValidationMessage(FeedException e) {
    if (e instanceof ParsingFeedException) {
      ParsingFeedException pfe = (ParsingFeedException) e;
      addValidationMessage(pfe.getLineNumber(), Level.ERROR, "XML parser",
          pfe.getLocalizedMessage());
    } else {
      addValidationMessage(1, Level.ERROR, "XML parser",
          e.getLocalizedMessage());
    }
  }

  /**
   * Adds the reasons as validation messages to the result.
   *
   * @param linkUrl the url that generated the messages
   * @param reasons the reasons to add
   */
  public void addValidationMessageForLink(String linkUrl, Reasons reasons) {
    for (Reason reason : new XercesCapExceptionMapper().map(reasons)) {
      addValidationMessageForLink(
          linkUrl, reason.getLevel(), reason.getSource(), reason.getMessage());
    }
  }
  
  /**
   * Adds the string as validation messages to the result.
   *
   * @param linkUrl the url that generated the messages
   * @param level the level of the validation message
   * @param message the free-text message
   */
  public void addValidationMessageForLink(
      String linkUrl, Level level, String source, String message) {
    Integer lineNum = getLineOffsets().getLinkLineNumber(linkUrl);
    lineNum = (lineNum == null) ? 0 : lineNum;
    
    addValidationMessage(lineNum, level, source, message);
  }
  
  /**
   * Logs the current elapsed time for the request, adding the given label
   * @param label the label for the current status of the request
   */
  public void recordTiming(String label) {
    timing.record(label);
  }

  /**
   * @return the document being validated.
   */
  public String getInput() {
    return xml;
  }

  /**
   * @return the list of valid alerts added via {@link #addValidAlert(Alert)}
   */
  public List<Alert> getValidAlerts() {
    return validAlerts;
  }

  /**
   * @return a map of line number to validation messages corresponding to
   * that line
   */
  public Multimap<Integer, ValidationMessage>
      getByLineValidationMessages() {
    Multimap<Integer, ValidationMessage> map =
        TreeMultimap.create();
    for (ValidationMessage validationMessage : validationMessages) {
      map.put(validationMessage.getLineNumber(), validationMessage);
    }
    return map;
  }

  public boolean containsErrors() {
    for (ValidationMessage validationMessage : validationMessages) {
      if (validationMessage.getLevel().equals(Level.ERROR)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * @return the time this result was created, in milliseconds.
   */
  public long getStartTimeMillis() {
    return timing.startTime;
  }

  /**
   * @return a snippet of text describing all the times recorded by
   * {@link #recordTiming(String)}.
   */
  public String getTiming() {
    return timing.get();
  }

  private LineOffsetParser.LineOffsets getLineOffsets() {
    if (lineOffsets == null) {
      lineOffsets = new LineOffsetParser().parse(xml);
    }
    return lineOffsets;
  }

  private static class Timing {
    private final StringBuilder sb;
    private final long startTime;
    private long lastEventTime;

    public Timing() {
      this.sb = new StringBuilder();
      this.startTime = System.currentTimeMillis();
      this.lastEventTime = startTime;
    }

    public void record(String label) {
      long now = System.currentTimeMillis();
      sb.append(label).append(": ").append(now - lastEventTime).append("ms\n");
      lastEventTime = now;
    }

    public String get() {
      return sb.toString() + "Total: "
          + (System.currentTimeMillis() - startTime) + "ms";
    }
  }
}

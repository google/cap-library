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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.XercesCapExceptionMapper;
import com.google.publicalerts.cap.feed.CapFeedException;
import com.google.publicalerts.cap.profile.CapProfile;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.ParsingFeedException;

/**
 * Holder for the data to be presented as a result of validation.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class ValidationResult {
  private final String xml;
  private final List<ValidationError> errors;
  private final List<ValidationError> recommendations;
  private final List<Alert> validAlerts;
  private final Timing timing;

  private LineOffsetParser.LineOffsets lineOffsets;

  /**
   * Creates a new validation result.
   *
   * @param xml the xml of the document being validated
   */
  public ValidationResult(String xml) {
    this.xml = xml;
    this.errors = Lists.newArrayList();
    this.recommendations = Lists.newArrayList();
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
   * Adds a recommendation to the result and HTML-escapes the message.
   *
   * @param lineNum line number of the recommendation
   * @param colNum column number of the recommendation
   * @param recommendation recommendation text
   */
  public void addRecommendation(
      int lineNum, int colNum, String recommendation) {
    recommendations.add(
        ValidationError.unescaped(lineNum, colNum, recommendation));
  }

  /**
   * Adds an error to the result and HTML-escapes the message
   *
   * @param lineNum line number of the error
   * @param colNum column number of the error
   * @param errorMsg error message
   */
  public void addError(int lineNum, int colNum, String errorMsg) {
    errors.add(
        ValidationError.unescaped(lineNum, colNum, errorMsg));
  }

  /**
   * Adds a recommendation to the result. Be careful to escape
   * any user-generated data passed into this method.
   *
   * @param lineNum line number of the recommendation
   * @param colNum column number of the recommendation
   * @param recommendation HTML-escaped recommendation text
   */
  public void addEscapedRecommendation(
      int lineNum, int colNum, String recommendation) {
    recommendations.add(
        ValidationError.escaped(lineNum, colNum, recommendation));
  }

  /**
   * Adds an error to the result. Be careful to escape
   * any user-generated data passed into this method.
   *
   * @param lineNum line number of the error
   * @param colNum column number of the error
   * @param errorMsg HTML-escaped error message
   */
  public void addEscapedError(int lineNum, int colNum, String errorMsg) {
    errors.add(
        ValidationError.escaped(lineNum, colNum, errorMsg));
  }

  /**
   * Adds the reasons in the given exception as errors to the result.
   *
   * @param e the exception to add
   */
  public void addFeedError(FeedException e) {
    if (e instanceof ParsingFeedException) {
      ParsingFeedException pfe = (ParsingFeedException) e;
      addError(pfe.getLineNumber(), pfe.getColumnNumber(),
          pfe.getLocalizedMessage());
    } else {
      addError(1, 1, e.getLocalizedMessage());
    }
  }

  /**
   * Adds the reasons in the given exception as errors to the result.
   *
   * @param e the exception to add
   */
  public void addFeedError(CapFeedException e) {
    for (Reason reason : e.getReasons()) {
      int lineNumber = reason.getLineNumber();
      if (reason.getLineNumber() == -1) {
        lineNumber = getLineOffsets().getXPathLineNumber(reason.getXPath());
      }
      addError(lineNumber, reason.getColumnNumber(), reason.getMessage());
    }
  }

  /**
   * Adds the reasons as recommendations to the result.
   *
   * @param reasons the recommendations to add
   */
  public void addFeedRecommendations(List<Reason> reasons) {
    for (Reason reason : reasons) {
      int lineOffset = getLineOffsets().getXPathLineNumber(reason.getXPath());
      addRecommendation(lineOffset, reason.getColumnNumber(), reason.getMessage());
    }
  }

  /**
   * Adds the reasons in the given exception as errors to the result at the
   * link identified by the given url.
   *
   * @param linkUrl the url that generated the errors
   * @param e the exception to add
   */
  public void addCapLinkError(String linkUrl, CapException e) {
    addCapLinkReasons(linkUrl, e.getReasons(), true);
  }

  /**
   * Adds the given message as an error to the result at the
   * link identified by the given URL.
   *
   * @param linkUrl the url that generated the errors
   * @param errorMsg the message to add
   */
  public void addCapLinkError(String linkUrl, String errorMsg) {
    Integer lineNum = getLineOffsets().getLinkLineNumber(linkUrl);
    lineNum = lineNum == null ? 0 : lineNum;
    addError(lineNum, 1, errorMsg);
  }

  /**
   * Adds the reasons in the given exception as errors to the result.
   *
   * @param e the exception to add
   */
  public void addError(CapException e) {
    addCapContentError(e, -1);
  }

  /**
   * For feeds of "fat" pings.  Adds the reasons in the given exception
   * as errors to the result, where the line numbers of the errors are offset
   * by the location of the entry containing the invalid CAP message.
   *
   * @param e the exception to add
   * @param entryIndex the 0-based index of the entry containing the invalid
   * CAP message
   */
  public void addCapContentError(CapException e, int entryIndex) {
    e = new XercesCapExceptionMapper().map(e);
    int lineOffset = getLineOffsets().getEntryLineNumber(entryIndex);
    String xpath = entryIndex == -1
        ? "" : "/feed/entry[" + entryIndex + "]/content";
    for (Reason reason : e.getReasons()) {
      int lineNumber = reason.getLineNumber() == -1 ?
          getLineOffsets().getXPathLineNumber(xpath + reason.getXPath())
          : reason.getLineNumber() + lineOffset;
      addError(lineNumber, reason.getColumnNumber(),
          reason.getMessage());
    }
  }

  /**
   * Adds the given errors and reasons caused by the given profile
   * to the result at the link identified by the given URL.
   * @param profile the profile generating the errors and recommendations
   * @param linkUrl the URL that generated the errors
   * @param errors the errors generated by the profile
   * @param recommendations the recommendations generated by the profile
   */
  public void addProfileResult(CapProfile profile, String linkUrl,
      List<Reason> errors, List<Reason> recommendations) {
    addCapLinkReasons(linkUrl, errors, true);
    addCapLinkReasons(linkUrl, recommendations, false);
  }

  /**
   * Adds the given errors and reasons caused by the given profile
   * to the result, where the line numbers of the errors are offset
   * by the location of the entry containing the invalid CAP message.
   *
   * @param profile the profile generating the errors and recommendations
   * @param entryIndex the 0-based index of the entry containing the invalid
   * CAP message
   * @param errors the errors generated by the profile
   * @param recommendations the recommendations generated by the profile
   */
  public void addProfileResult(CapProfile profile, int entryIndex,
      List<Reason> errors, List<Reason> recommendations) {
    errors = errors == null ? ImmutableList.<Reason>of() : errors;
    recommendations = recommendations == null
        ? ImmutableList.<Reason>of() : recommendations;

    String xpath = entryIndex == -1
        ? "" : "/feed/entry[" + entryIndex + "]/content";
    for (Reason reason : errors) {
      int line = getLineOffsets().getXPathLineNumber(xpath + reason.getXPath());
      addError(line, reason.getColumnNumber(),
          profile.getCode() + ": " + reason.getMessage());
    }
    for (Reason reason : recommendations) {
      int line = getLineOffsets().getXPathLineNumber(xpath + reason.getXPath());
      addRecommendation(line, reason.getColumnNumber(),
          profile.getCode() + ": " + reason.getMessage());
    }
  }

  private void addCapLinkReasons(
      String linkUrl, List<Reason> reasons, boolean isError) {
    if (reasons == null || reasons.isEmpty()) {
      return;
    }

    Integer lineNum = getLineOffsets().getLinkLineNumber(linkUrl);
    lineNum = lineNum == null ? 0 : lineNum;

    String escapedUrl = StringUtil.htmlEscape(linkUrl);
    StringBuilder sb = new StringBuilder(
        isError ? "Errors" : "Recommendations")
        .append(" for CAP message loaded from: <a href='")
        .append(escapedUrl)
        .append("'>")
        .append(escapedUrl)
        .append("</a><br/>");
    reasons = new XercesCapExceptionMapper().map(reasons);
    for (Reason reason : reasons) {
      sb.append("\u2022 ")
          .append(StringUtil.htmlEscape(reason.getMessage()))
          .append("<br/>");
    }
    if (isError) {
      addEscapedError(lineNum, 1, sb.toString());
    } else {
      addEscapedRecommendation(lineNum, 1, sb.toString());
    }
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
   * @return a map of line number to errors that occurred at that line
   */
  public Multimap<Integer, ValidationError> getByLineErrorMap() {
    Multimap<Integer, ValidationError> errorMap = TreeMultimap.create();
    for (ValidationError error : errors) {
      errorMap.put(error.getLineNumber(), error);
    }
    return errorMap;
  }

  /**
   * @return a map of line number to recommendations for that line
   */
  public Multimap<Integer, ValidationError> getByLineRecommendationMap() {
    Multimap<Integer, ValidationError> recommendationMap = TreeMultimap.create();
    for (ValidationError error : recommendations) {
      recommendationMap.put(error.getLineNumber(), error);
    }
    return recommendationMap;
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

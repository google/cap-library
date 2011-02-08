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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * An exception for CAP alerts.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapException extends Exception {
  private static final long serialVersionUID = 1L;

  /** List of reasons for the exception */
  private final List<Reason> reasons;

  public CapException(Reason...reasons) {
    this(Arrays.asList(reasons));
  }

  public CapException(List<Reason> reasons) {
    super(getMessage(reasons, Locale.ENGLISH));
    this.reasons = Collections.unmodifiableList(
        new ArrayList<Reason>(reasons));
  }

  public List<Reason> getReasons() {
    return reasons;
  }

  @Override
  public String toString() {
    return "CapException[reasons=[" + getMessage() + "]]";
  }
  
  public String getMessage(Locale locale) {
    return getMessage(reasons, locale);
  }
  
  private static String getMessage(List<Reason> reasons, Locale locale) {
    StringBuilder sb = new StringBuilder();
    for (Reason reason : reasons) {
      sb.append(reason.getMessage()).append("; ");
    }
    sb.setLength(sb.length() - 2);
    return sb.toString();
  }

  /**
   * A reason for the exception. This allows a single exception to contain
   * multiple causes.
   */
  public static class Reason {

    /**
     * Type of the reason. Should allow equality comparison to avoid
     * resorting to string comparisons on the exception message.
     */
    private final ReasonType type;

    /**
     * Line number of the reason. Valid only when parsing an alert from text,
     * otherwise -1.
     */
    private int lineNumber;
    
    /**
     * Column number of the reason.  Valid only when parsing an alert from
     * text, otherwise -1.
     */
    private int columnNumber;
    
    /** Message parameters for the reason. */
    private final Object[] messageParams;

    public Reason(ReasonType type, Object...messageParams) {
      this(-1, -1, type, messageParams);
    }

    public Reason(int line, int col, ReasonType type, Object...messageParams) {
      this.type = type;
      this.lineNumber = line;
      this.messageParams = messageParams;
    }

    public ReasonType getType() {
      return type;
    }
    
    public int getMessageParamsCount() {
      return messageParams.length;
    }

    public Object getMessageParam(int i) {
      return getMessageParamsCount() > i ? messageParams[i] : null;
    }
    
    public void setLineNumber(int line) {
      this.lineNumber = line;
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public void setColumnNumber(int col) {
      this.columnNumber = col;
    }

    public int getColumnNumber() {
      return columnNumber;
    }
    
    /**
     * Returns the {@link ReasonType}'s message formatted with the
     * {@link #messageParams}.
     * @return an English human-readable message for this reason 
     */
    public String getMessage() {
      return getLocalizedMessage(Locale.ENGLISH);
    }

    /**
     * Returns the {@link ReasonType}'s message formatted with the
     * {@link #messageParams}.
     * @param locale for the requested message
     * @return a human-readable message for this reason 
     */
    public String getLocalizedMessage(Locale locale) {
      StringBuilder sb = new StringBuilder();
      if (messageParams.length == 0) {
        sb.append(type.getMessage(locale));
      } else {
        sb.append(MessageFormat.format(
            type.getMessage(locale), messageParams));        
      }
      return sb.toString();
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Reason)) {
        return false;
      }
      Reason that = (Reason) other;
      return type == that.type
          && lineNumber == that.lineNumber
          && columnNumber == that.columnNumber
          && Arrays.deepEquals(messageParams, that.messageParams);
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + columnNumber;
      result = 31 * result + lineNumber;
      result = 31 * result + Arrays.hashCode(messageParams);
      result = 31 * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }
  }
  
  /**
   * Interface defining the type of a CapException Reason.
   * This allows for external types to be defined. 
   */
  public static interface ReasonType {

    /**
     * Returns the programmer-facing message for this type
     * @return
     */
    String getMessage(Locale locale);
  }

  /**
   * Type of the exception message. Use {@link Type#OTHER} if your exception
   * doesn't fit one of the listed types.
   * 
   * TODO(shakusa) Localize messages
   */
  public enum Type implements ReasonType {
    // For ADDRESS_*, INFO_*, and RESOURCE_* values, the convention is for
    // the first parameter to contain the sequential index of the Info it 
    // pertains to.
    
    ADDRESSES_SCOPE_MISMATCH(
        "<addresses> should be used only when <scope> is Private"),
    ALERT_ELEMENTS_OUT_OF_SEQUENCE(""),
    AREA_AREA_DESC_IS_REQUIRED("<areaDesc> is required in Info #{0}"),
    AREA_INVALID_CIRCLE("Invalid <circle> \"{1}\" in Info #{0}"),
    AREA_INVALID_CIRCLE_POINT("Invalid <circle> point \"{1}\" in Info #{0}"),
    AREA_INVALID_CIRCLE_RADIUS("Invalid <circle> radius \"{1}\" in Info #{0}"),
    AREA_INVALID_ALTITUDE_CEILING_RANGE("Invalid <area>; altitude must not " +
    		"be greater than ceiling in Info #{0}"),
    AREA_INVALID_CEILING("Invalid <area>; when ceiling is specified " +
        "altitude must also be specified in Info #{0}"),
    AREA_INVALID_POINT_LATITUDE("Invalid latitude \"{1}\" in Info #{0}"),
    AREA_INVALID_POINT_LONGITUDE("Invalid longitude: \"{1}\" in Info #{0}"),
    AREA_INVALID_POLYGON_NUM_POINTS(
        "Invalid <polygon>; must have at least 4 points in Info #{0}"),
    AREA_INVALID_POLYGON_POINT("Invalid <polygon> at point \"{1}\" in "
    		+ "Info #{0}: \"{2}\""),
    AREA_INVALID_POLYGON_START_END("Invalid <polygon> in Info #{0}; "
        + "first and last pairs of coordinates must be the same."),
    DUPLICATE_ELEMENT("Invalid duplicate <{0}>, ignoring \"{1}\""),
    IDENTIFIER_IS_REQUIRED("<identifier> is required"),
    INFO_CATEGORY_IS_REQUIRED("<category> is required in Info #{0}"),
    INFO_CERTAINTY_VERY_LIKELY_DEPRECATED("<certainty> VeryLikely has been " +
    		"deprecated. Use Likely instead in Info #{0}"),
    INFO_INVALID_EFFECTIVE("Invalid <effective>: \"{1}\" in Info #{0}. " +
        "Must be formatted like '2002-05-24T16:49:00-07:00'"),
    INFO_INVALID_EXPIRES("Invalid <expires>: \"{1}\" in Info #{0}. " +
        "Must be formatted like '2002-05-24T16:49:00-07:00'"),
    INFO_INVALID_ONSET("Invalid <onset>: \"{1}\" in Info #{0}. " +
        "Must be formatted like '2002-05-24T16:49:00-07:00'"),
    INFO_INVALID_WEB("Invalid <web>: \"{1}\" in Info #{0}. Must be a full " +
    		"absolute URI"),
    INFO_EVENT_IS_REQUIRED("<event> is required in Info #{0}"),
    INFO_URGENCY_IS_REQUIRED("<urgency> is required in Info #{0}"),
    INFO_SEVERITY_IS_REQUIRED("<severity> is required in Info #{0}"),
    INFO_CERTAINTY_IS_REQUIRED("<certainty> is required in Info #{0}"),
    INVALID_ENUM_VALUE("Invalid enum value <{0}> = \"{1}\". " +
    		"Must be one of {2}"),
    INVALID_IDENTIFIER("Invalid <identifier> \"{0}\". Must not include " +
        "spaces, commas, or restricted characters (< and &)"),
    INVALID_SENDER("Invalid <sender>: \"{0}\". Must not include " +
        "spaces, commas, or restricted characters (< and &)"),
    INVALID_SENT("Invalid <sent>: \"{0}\" " +
        "Must be formatted like '2002-05-24T16:49:00-07:00'"),
    INVALID_SEQUENCE("Elements not in the correct sequence order specified " +
    		"by the official schema. One of {0} expected instead of \"{1}\"."),
    MSGTYPE_IS_REQUIRED("<msgType> is required"),
    OTHER("{0}"),
    PASSWORD_DEPRECATED("<password> has been deprecated"),
    RESOURCE_RESOURCE_DESC_IS_REQUIRED(
        "<resourceDesc> is required in Info #{0}"),
    RESOURCE_MIME_TYPE_IS_REQUIRED("<mimeType> is required> in Info #{0}"),
    RESOURCE_INVALID_SIZE("Invalid size: {1} in Info #{0}"),
    RESOURCE_INVALID_URI("Invalid URI: {1} in Info #{0}"),
    RESTRICTION_SCOPE_MISMATCH(
        "<restriction> should be used only when <scope> is Restricted"),
    SCOPE_IS_REQUIRED("<scope> is required"),
    SENDER_IS_REQUIRED("<sender> is required"),
    SENT_IS_REQUIRED("<sent> is required"),
    STATUS_IS_REQUIRED("<status> is required"),
    XMLNS_IS_REQUIRED("<alert> tag must include the xmlns attribute " +
    		"referencing the CAP URL as the namespace, e.g."
        + "xmlns:cap=\"urn:oasis:names:tc:emergency:cap:1.2\""),
    UNSUPPORTED_ELEMENT("Unsupported element <{0}>"),
    UNSUPPORTED_VALUE("Unsupported value <{0}> = \"{1}\""),
    ;

    private final String message;

    private Type(String message) {
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }
  }
}

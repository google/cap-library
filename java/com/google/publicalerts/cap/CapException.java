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
  private static final long serialVersionUID = -8060028892021203660L;

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
    private final int lineNumber;

    /**
     * Column number of the reason.  Valid only when parsing an alert from
     * text, otherwise -1.
     */
    private final int columnNumber;

    /** Message parameters for the reason. */
    private final Object[] messageParams;

    public static Reason withNewLineNumber(Reason reason, int newLineNumber) {
      return new Reason(newLineNumber, reason.columnNumber,
          reason.type, reason.messageParams);
    }

    public Reason(ReasonType type, Object...messageParams) {
      this(-1, -1, type, messageParams);
    }

    public Reason(
        int line, int col, ReasonType type, Object...messageParams) {
      this.lineNumber = line;
      this.columnNumber = col;
      this.type = type;
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

    public int getLineNumber() {
      return lineNumber;
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
    public String toString() {
      return getMessage();
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
    CERTAINTY_VERY_LIKELY_DEPRECATED("<certainty> VeryLikely has been " +
        "deprecated. Use Likely instead"),
    DUPLICATE_ELEMENT("Invalid duplicate <{0}>, ignoring \"{1}\""),
    INVALID_ALTITUDE_CEILING_RANGE("Invalid <area>; altitude must not " +
    		"be greater than ceiling"),
    INVALID_AREA("Invalid <area>; when ceiling is specified " +
        "altitude must also be specified."),
    INVALID_CHARACTERS("Invalid characters in element \"{0}\""),
    INVALID_CIRCLE("Invalid <circle> \"{1}\". Must be formatted like: " +
        "\"-12.345,67.89 15.2\", which is a [WGS 84] coordinate followed by a " +
        "radius in kilometers"),
    INVALID_DATE("Invalid <{0}>: \"{1}\". " +
        "Must be formatted like \"2002-05-24T16:49:00-07:00\""),
    INVALID_ENUM_VALUE("Invalid enum value <{0}> = \"{1}\". " +
        "Must be one of {2}"),
    INVALID_IDENTIFIER("Invalid <identifier> \"{0}\". Must not include " +
        "spaces, commas, or restricted characters (< and &)"),
    INVALID_POLYGON("Invalid <polygon> \"{1}\". Expect a minimum of 4 " +
    		"[WGS 84] coordinates like: \"12.3,4.2 12.3,4.3 12.4,4.3 12.3,4.3 \", " +
    		"where the first and last coordinates are equal."),
    INVALID_REFERENCES("Invalid <references>: \"{0}\". Must be a " +
    		"space-separated list of sender,identifier,sent triplets."),
    INVALID_RESOURCE_SIZE("Invalid size: \"{0}\""),
    INVALID_RESOURCE_URI("Invalid URI: \"{0}\""),
    INVALID_SENDER("Invalid <sender>: \"{0}\". Must not include " +
        "spaces, commas, or restricted characters (< and &)"),
    INVALID_SEQUENCE("Elements are not in the correct sequence order." +
        "One of {0} expected instead of \"{1}\"."),
    INVALID_VALUE("Unsupported value <{0}> = \"{1}\""),
    INVALID_WEB("Invalid <web>: \"{0}\". Must be a full " +
        "absolute URI"),
    MISSING_REQUIRED_ELEMENT("The content of <{0}> is not complete. One of " +
    	"{1} is required"),
    OTHER("{0}"),
    PASSWORD_DEPRECATED("<password> has been deprecated"),
    RESTRICTION_SCOPE_MISMATCH(
        "<restriction> should be used only when <scope> is Restricted"),
    UNSUPPORTED_ELEMENT("Unsupported element <{0}>"),
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

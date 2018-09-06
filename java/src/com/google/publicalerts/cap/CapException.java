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

import java.util.Locale;

/**
 * An exception for CAP alerts. This class is thread-safe.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapException extends Exception {
  private static final long serialVersionUID = -8060028892021203660L;

  /** Reasons for the exception */
  private final Reasons reasons;

  public CapException(Reason...reasons) {
    this(Reasons.of(reasons));
  }

  public CapException(Reasons reasons) {
    super(getMessage(reasons, Locale.ENGLISH));
    this.reasons = reasons;
  }

  /**
   * @return the reasons for this exception
   */
  public Reasons getReasons() {
    return reasons;
  }

  @Override
  public String toString() {
    return "CapException[reasons=[" + getMessage() + "]]";
  }

  public String getMessage(Locale locale) {
    return getMessage(reasons, locale);
  }

  @SuppressWarnings("unused")
  private static String getMessage(Reasons reasons, Locale locale) {
    StringBuilder sb = new StringBuilder();
    for (Reason reason : reasons) {
      sb.append(reason.getMessage()).append("; ");
    }
    sb.setLength(sb.length() - 2);
    return sb.toString();
  }

  /**
   * Type of the exception message. Use {@link #OTHER} if your exception
   * doesn't fit one of the listed types.
   */
  // TODO(shakusa) Localize messages
  public enum ReasonType implements Reason.Type {
    
    // Errors
    CERTAINTY_VERY_LIKELY_DEPRECATED(
        Reason.Level.ERROR,
        "<certainty> \"Very Likely\" has been deprecated. Use Likely instead."),
    CIRCULAR_REFERENCE(
        Reason.Level.ERROR,
        "Invalid <references>: \"{0}\". Alert cannot reference itself."),
    DUPLICATE_ELEMENT(
        Reason.Level.ERROR,
        "Invalid duplicate <{0}>, ignoring \"{1}\"."),
    INVALID_ALTITUDE_CEILING_RANGE(
        Reason.Level.ERROR,
        "Invalid <area>; ceiling must be greater than altitude."),
    INVALID_AREA(
        Reason.Level.ERROR,
        "Invalid <area>; when ceiling is specified altitude must also be "
            + "specified."),
    INVALID_CHARACTERS(
        Reason.Level.ERROR,
        "Invalid characters in element \"{0}\"."),
    INVALID_CIRCLE(
        Reason.Level.ERROR,
        "Invalid <circle> \"{0}\". Must be formatted like: "
            + "\"-12.345,67.89 15.2\", which is a [WGS 84] coordinate followed "
            + "by a radius in kilometers."),
    INVALID_DATE(
        Reason.Level.ERROR,
        "Invalid <{0}>: \"{1}\". Must be formatted like "
            + "\"2002-05-24T16:49:00-07:00\"."),
    INVALID_DEREF_URI(
        Reason.Level.ERROR,
        "Invalid <derefUri> \"{0}\". Must be base64-encoded."),
    INVALID_ENUM_VALUE(
        Reason.Level.ERROR,
        "Invalid enum value <{0}> = \"{1}\". Must be one of {2}."),
    INVALID_IDENTIFIER(
        Reason.Level.ERROR,
        "Invalid <identifier> \"{0}\". Must not include spaces, commas, or "
            + "restricted characters (< and &)."),
    INVALID_LANGUAGE(
        Reason.Level.ERROR,
        "Invalid <language> \"{0}\". Must follow RFC 3066."),
    INVALID_MIME_TYPE(
        Reason.Level.ERROR,
        "Invalid <mimeType> \"{0}\". Must follow RFC 2046."),
    INVALID_POLYGON(
        Reason.Level.ERROR,
        "Invalid <polygon> \"{0}\". Expect a minimum of four [WGS 84] "
            + "coordinates like: \"12.3,-4.2 12.3,-4.3 12.4,-4.3 12.3,-4.2\", "
            + "where the first and last coordinates are equal."),
    INVALID_POLYGON_SELF_INTERSECTION(
        Reason.Level.ERROR,
        "Invalid <polygon>. The polygon edges must not intersect. {0}"),
    INVALID_REFERENCES(
        Reason.Level.ERROR,
        "Invalid <references>: \"{0}\". Must be a non-empty, space-separated "
           + "list of sender,identifier,sent triplets."),
    INVALID_RESOURCE_SIZE(
        Reason.Level.ERROR,
        "Invalid size: \"{0}\"."),
    INVALID_RESOURCE_URI(
        Reason.Level.ERROR,
        "Invalid URI: \"{0}\"."),
    INVALID_SENDER(
        Reason.Level.ERROR,
        "Invalid <sender>: \"{0}\". Must not include spaces, commas, or "
            + "restricted characters (< and &)."),
    INVALID_SEQUENCE(
        Reason.Level.ERROR,
            "Elements are not in the correct sequence order. "
            + "One of {0} expected instead of \"{1}\"."),
    INVALID_VALUE(
        Reason.Level.ERROR,
        "Unsupported value <{0}> = \"{1}\"."),
    INVALID_URI(
        Reason.Level.ERROR,
        "Invalid <uri>: \"{0}\". Must be a full absolute or relative URI."),
    INVALID_WEB(
        Reason.Level.ERROR,
        "Invalid <web>: \"{0}\". Must be a full absolute URI."),
    MISSING_REQUIRED_ELEMENT(
        Reason.Level.ERROR,
        "The content of <{0}> is not complete. One of {1} is required."),
    OTHER(
        Reason.Level.ERROR,
        "{0}"),
    PASSWORD_DEPRECATED(
        Reason.Level.ERROR,
        "<password> has been deprecated."),
    RELATIVE_URI_MISSING_DEREF_URI(
        Reason.Level.ERROR,
        "Relative <uri> must reference the content of <derefUri>"),
    RESTRICTION_SCOPE_MISMATCH(
        Reason.Level.ERROR,
        "<restriction> should be used only when <scope> is Restricted."),
    UNSUPPORTED_ELEMENT(
        Reason.Level.ERROR,
        "Unsupported element <{0}>."),
        
    // Warnings
    TEXT_CONTAINS_HTML_ENTITIES (
        Reason.Level.WARNING,
        "<{0}> contains HTML entities, which is discouraged as per section "
            + "3.3.3 of the CAP 1.2 specification."),
    TEXT_CONTAINS_HTML_TAGS (
        Reason.Level.WARNING,
        "<{0}> contains HTML tags, which is discouraged."),
    POSTDATED_REFERENCE(
        Reason.Level.WARNING,
        "Invalid <references>: \"{0}\". Alert should not have have post-dated "
            + "references."),
    SAME_TEXT_DIFFERENT_LANGUAGE(
        Reason.Level.WARNING,
        "Text in <{0}> appears in multiple <info> blocks but each specifies a different "
            + "<language> field. Human-readable content in an <info> should be written in the "
            + "same language as specified in the <language> field."),
    ;

    private final Reason.Level defaultLevel;
    private final String message;

    private ReasonType(Reason.Level defaultLevel, String message) {
      this.defaultLevel = defaultLevel;
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }

    @Override
    public Reason.Level getDefaultLevel() {
      return defaultLevel;
    }

    @Override
    public String getSource() {
      return "CAP";
    }
  }
}

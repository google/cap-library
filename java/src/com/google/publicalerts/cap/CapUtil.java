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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.ProtocolMessageEnum;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

/**
 * Utilities for dealing with transforming to and from CAP protos.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapUtil {

  /**
   * From the CAP spec:
   *
   * The date and time is represented in [dateTime] format
   * (e. g., "2002-05-24T16:49:00-07:00" for 24 May 2002 at
   * 16: 49 PDT).  Alphabetic timezone designators such as "Z"
   * MUST NOT be used.  The timezone for UTC MUST be represented
   * as "-00:00" or "+00:00"
   */
  // TODO(andriy): We handle fractional seconds (hundredths or thousands), but
  // because [dateTime] allows an arbitrary number of decimals, it would be
  // better to devise a more robust and flexible solution here.
  private static final Pattern DATE_PATTERN = Pattern.compile(
      "[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]"
          + "(\\.[0-9]{2}([0-9])?)?([\\+|-])([01][0-9]:[0-5][0-9])");

  private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";

  private static final Map<String, String> ENUM_CASING_EXCEPTIONS =
      buildEnumCasingExceptions();

  private static Map<String, String> buildEnumCasingExceptions() {
    Map<String, String> ret = new HashMap<String, String>();
    ret.put("VERY_LIKELY", "Very Likely");
    ret.put("CBRNE", "CBRNE");
    ret.put("UNKNOWN_URGENCY", "Unknown");
    ret.put("UNKNOWN_SEVERITY", "Unknown");
    ret.put("UNKNOWN_CERTAINTY", "Unknown");
    return ret;
  }

  /**
   * Returns a CAP field value for an enum field.
   * <p/>
   * Enum values in proto use C++ scoping rules, so 2 enums
   * of the same message can't have the same name. We work around
   * this limitation by using the name Value_EnumTypeName
   *
   * @param evd the enum value descriptor
   * @return a CAP field name for the enum field.
   */
  public static String getEnumValue(EnumValueDescriptor evd) {
    String value = ENUM_CASING_EXCEPTIONS.get(evd.getName());
    return value == null ? camelCase(evd.getName()) : value;
  }

  /**
   * Returns a CAP field value for an enum field.
   * <p/>
   * Enum values in proto use C++ scoping rules, so 2 enums
   * of the same message can't have the same name. We work around
   * this limitation by using the name Value_EnumTypeName
   *
   * @param e the enum value
   * @return a CAP field name for the enum field.
   */
  public static String getEnumValue(ProtocolMessageEnum e) {
    return getEnumValue(e.getValueDescriptor());
  }

  /**
   * Returns the CAP element name given a proto FieldDescriptor.
   * <p/>
   * This method takes care of differences in casing between CAP and proto.
   *
   * @param fd the field descriptor for which the name is returned
   * @return the element name of the field
   */
  public static String getElementName(FieldDescriptor fd) {
    return javaCase(fd.getName());
  }

  static String javaCase(String s) {
    return toCase(s, false);
  }

  static String camelCase(String s) {
    return toCase(s, true);
  }

  static String toCase(String s, boolean camel) {
    String[] parts = s.split("_");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (part.length() > 0) {
        sb.append(part.substring(0, 1).toUpperCase())
            .append(part.substring(1).toLowerCase());
      }
    }
    if (!camel && sb.length() > 0) {
      sb.replace(0, 1, String.valueOf(Character.toLowerCase(sb.charAt(0))));
    }
    return sb.toString();
  }

  /**
   * Returns a field descriptor for the field of the given builder with the
   * given CAP name. Takes care of differences in casing between CAP and proto.
   *
   * @param builder the builder to search in
   * @param name the CAP name to search for
   * @return the field, or null if it does not exist
   */
  public static FieldDescriptor findFieldByName(Builder builder, String name) {
    return builder == null ? null :
        builder.getDescriptorForType().findFieldByName(
            underscoreCase(name).toLowerCase());
  }

  static String underscoreCase(String s) {
    if (s.isEmpty()) {
      return s;
    }
    StringBuilder sb = new StringBuilder();
    char[] chars = s.toCharArray();
    sb.append(chars[0]);
    for (int i = 1; i < chars.length; i++) {
      char ch = chars[i];
      if (Character.isUpperCase(ch) && Character.isLowerCase(chars[i - 1])) {
        sb.append('_');
      }
      sb.append(ch);
    }
    return sb.toString();
  }

  /**
   * Returns a map of CAP fields to their child fields that may be repeated.
   * <p>
   * For instance, an "info" can have repeated "category" and "msgType" fields,
   * so both of those fields (and others) will be included in the set returned
   * by <code>getRepeatedFieldNames().get("info")</code>.
   *
   * @return a map of CAP fields to their child fields that may be repeated
   */
  public static Map<String, Set<String>> getRepeatedFieldNames() {
    Map<String, Set<String>> map = new HashMap<String, Set<String>>();
    getRepeatedFieldNamesInternal(Alert.getDescriptor(), map);
    return map;
  }

  private static void getRepeatedFieldNamesInternal(
      Descriptor d, Map<String, Set<String>> result) {
    Set<String> repeatedFields = new HashSet<String>();
    for (FieldDescriptor fd : d.getFields()) {
      if (fd.isRepeated()) {
        repeatedFields.add(javaCase(fd.getName()));
      }
      if (fd.getType() == FieldDescriptor.Type.MESSAGE) {
        getRepeatedFieldNamesInternal(fd.getMessageType(), result);
      }
    }
    if (!repeatedFields.isEmpty()) {
      result.put(javaCase(d.getName()), repeatedFields);
    }
  }

  /**
   * Converts a date string in [datetime] format to a java date
   * @param dateStr the string to convert
   * @return the date, or null if the date is invalid
   */
  public static Date toJavaDate(String dateStr) {
    if (!DATE_PATTERN.matcher(dateStr).matches()) {
      return null;
    } else {
      return DatatypeConverter.parseDateTime(dateStr).getTime();
    }
  }

  /**
   * Returns the timezone offset, in minutes, between the given
   * {@code dateStr} and UTC.
   * <p>For example, if {@code dateStr} is 2003-04-02T14:39:01+05:00, this
   * method would return 300. If {@code dateStr} is 2003-04-02T14:39:01-01:29,
   * this method would return -89.
   */
  public static int getTimezoneOffset(String dateStr) {
    Matcher matcher = DATE_PATTERN.matcher(dateStr);
    if (!matcher.matches()) {
      return 0;
    }
    String sign = matcher.group(3);
    String tz = matcher.group(4);
    String[] parts = tz.split(":");
    int hours = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    return "+".equals(sign) ? hours : -hours;
  }

  /**
   * Returns true if the given string is a valid date according to the CAP spec
   * @param dateStr the string to parse
   * @return true if the given string is a valid date according to the CAP spec
   */
  public static boolean isValidDate(String dateStr) {
    return toJavaDate(dateStr) != null;
  }

  /**
   * Returns true if the string is null or {@code s.trim()}
   * returns the empty string.
   *
   * @param s the string to check
   * @return true if the string is empty or whitespace
   */
  public static boolean isEmptyOrWhitespace(String s) {
    return s == null || "".equals(s.trim());
  }

  /**
   * Returns a string legal for use in a CAP {@literal <references>} parameter.
   */
  public static String formatCapReference(
      String capSender, String capIdentifier, Calendar sent) {

    // CAP reference format 'sender,identifier,sent'
    return String.format("%s,%s,%s",
        capSender,
        capIdentifier,
        formatCapDate(sent));
  }

  /**
   * Formats the given date as a [datetime].
   *
   * @param cal the date and time zone to format
   * @return a string of the form "2011-10-28T12:00:01+00:00"
   */
  public static String formatCapDate(Calendar cal) {
    SimpleDateFormat format = new SimpleDateFormat(DATETIME_FORMAT);
    format.setTimeZone(cal.getTimeZone());
    StringBuilder ret = new StringBuilder(format.format(cal.getTime()));
    // SimpleDateFormat doesn't include the colon in the timezone, so add it here
    ret.insert(ret.length() - 2, ':');
    return ret.toString();
  }

  /**
   * Strips the XML preamble, if any, from the start of the given string.
   *
   * @param xmlDocument document to check for {@literal <?xml ..>} preamble.
   *     If a preamble exists, this method expects it to start on the first
   *     character of the given xmlDocument.
   * @return the same String, minus any preamble.
   */
  public static String stripXmlPreamble(String xmlDocument) {
    while (xmlDocument.startsWith("<?xml")) {
      xmlDocument = xmlDocument.substring(xmlDocument.indexOf(">") + 1);
    }
    return xmlDocument;
  }

  private CapUtil() {}
}

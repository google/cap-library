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

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.ProtocolMessageEnum;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.text.ParseException;

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
   *
   * {@link #DATE_FORMATS} below do most of the work of validating this format,
   * but do not support handling the colon in the time zone format
   * and are more lenient in parsing months and dates than the spec specifies.
   */
  // TODO (andriy): We handle fractional seconds (hundredths or thousands), but
  // because [dateTime] allows an arbitrary number of decimals, it would be better
  // to devise a more robust and flexible solution here.
  private static final Pattern DATE_PATTERN = Pattern.compile(
      "[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9](\\.[0-9]{2}([0-9])?)?"
          + "[\\+|-][01][0-9]:[0-5][0-9]");

  // These are the (only) supported date formats; they must be covered by DATE_PATTERN.
  private static final String [] DATE_FORMATS = {"yyyy-MM-dd'T'HH:mm:ssZ",
                                                 "yyyy-MM-dd'T'HH:mm:ss.SSZ",
                                                 "yyyy-MM-dd'T'HH:mm:ss.SSSZ"};

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
    String value = evd.getName();
    String suffixWorkaround = "_" + evd.getType().getName();
    if (value.endsWith(suffixWorkaround)) {
      return value.substring(0, value.length() - suffixWorkaround.length());
    } else if ("VeryLikely".equals(value)) {
      // Special-case for the deprecated CAP 1.0 enum value for <certainty>
      // The proto enum does not support a space, but the CAP XML enum has one.
      return "Very Likely";
    }
    return value;
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
    return camelCase(fd.getName());
  }

  static String camelCase(String s) {
    String[] parts = s.split("_");
    StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
    for (int i = 1; i < parts.length; i++) {
      String part = parts[i];
      sb.append(part.substring(0, 1).toUpperCase())
          .append(part.substring(1).toLowerCase());
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
        builder.getDescriptorForType().findFieldByName(underscoreCase(name));
  }

  static String underscoreCase(String s) {
    StringBuilder sb = new StringBuilder();
    char[] chars = s.toCharArray();
    sb.append(chars[0]);
    for (int i = 1; i < chars.length; i++) {
      char ch = chars[i];
      if (Character.isUpperCase(ch)) {
        sb.append('_').append(Character.toLowerCase(ch));
      } else {
        sb.append(ch);
      }
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
        repeatedFields.add(camelCase(fd.getName()));
      }
      if (fd.getType() == FieldDescriptor.Type.MESSAGE) {
        getRepeatedFieldNamesInternal(fd.getMessageType(), result);
      }
    }
    if (!repeatedFields.isEmpty()) {
      result.put(camelCase(d.getName()), repeatedFields);
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
    }
    int lastColon = dateStr.lastIndexOf(':');
    dateStr = dateStr.substring(0, lastColon)
        + dateStr.substring(lastColon + 1);

    for (String dateFormat : DATE_FORMATS) {
      // Can't be static, SimpleDateFormat is not thread safe.
      SimpleDateFormat format = new SimpleDateFormat(dateFormat);
      try {
        return format.parse(dateStr);
      } catch (ParseException e) {
        // do nothing.  Try the next format...
      }
    }
    return null;
  }

  /**
   * Returns true if the given string is a valid date according to the CAP spec
   * @param dateStr the string to parse
   * @return true if the given string is a valid date according to the CAP spec
   */
  public static boolean isValidDate(String dateStr) {
    return toJavaDate(dateStr) != null;
  }

  private CapUtil() {}
}

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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.ProtocolMessageEnum;

import org.apache.commons.lang3.StringEscapeUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilities for dealing with transforming to and from CAP protos.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapUtil {

  private static final Map<String, String> ENUM_CASING_EXCEPTIONS =
      ImmutableMap.<String, String>builder()
          .put("VERY_LIKELY", "Very Likely")
          .put("CBRNE", "CBRNE")
          .put("UNKNOWN_URGENCY", "Unknown")
          .put("UNKNOWN_SEVERITY", "Unknown")
          .put("UNKNOWN_CERTAINTY", "Unknown")
          .build();

  private static final Pattern BASE_64_PATTERN = Pattern.compile(
      "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
  
  // First group matches greedily, second reluctantly, so that we can match
  // multiple tags one after the other
  private static final Pattern HTML_TAG_PATTERN =
      Pattern.compile("<([^\\s>]+)(.*?)>");
  
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
    Map<String, Set<String>> map = Maps.newHashMap();
    getRepeatedFieldNamesInternal(Alert.getDescriptor(), map);
    return map;
  }

  private static void getRepeatedFieldNamesInternal(
      Descriptor d, Map<String, Set<String>> result) {
    Set<String> repeatedFields = Sets.newHashSet();
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
   * Returns true if the string is null or {@code s.trim()}
   * returns the empty string.
   *
   * @param s the string to check
   * @return true if the string is empty or whitespace
   */
  public static boolean isEmptyOrWhitespace(String s) {
    return Strings.nullToEmpty(s).trim().isEmpty();
  }
  
  /**
   * Returns {@code true} if the string represents base64-encoded content,
   * {@code false} otherwise.
   * 
   * <p>The input string cannot be {@code null}, but can be empty. In this
   * case, the function would return {@code true}.
   * 
   * <p>The new-line character '\n' is stripped.
   */
  public static boolean isBase64(String s) {
    return BASE_64_PATTERN.matcher(s.replace("\n", "")).find();
  }
  
  /**
   * Returns a string legal for use in a CAP {@literal <references>} parameter.
   */
  public static String formatCapReference(
      String capSender, String capIdentifier, Calendar sent) {
    return formatCapReference(
        capSender, capIdentifier, CapDateUtil.formatCapDate(sent));
  }

  /**
   * Returns a string legal for use in a CAP {@literal <references>} parameter.
   */
  public static String formatCapReference(
      String capSender, String capIdentifier, String sent) {
    // CAP reference format 'sender,identifier,sent'
    return String.format("%s,%s,%s", capSender, capIdentifier, sent);
  }

  /**
   * Parses a CAP identifier from a single valid reference in
   * {@literal <references>}.
   *
   * <p>Different parsing rules are applied according to the version of CAP
   * being employed.
   * 
   * <p>If the input string is not compliant with the XSD schema, and parsing it
   * is not possible, {@code null} is returned.
   */
  public static String parseReferenceIdentifier(String s, int capVersion) {
    if (capVersion <= 10) {
      int separatorIndex = s.indexOf('/');
      return (separatorIndex > 0) ? s.substring(0, separatorIndex) : null;
    }

    String[] parts = s.split(",", 3);
    return (parts.length > 1) ? parts[1] : null;
  }
  
  /**
   * Parses the date specified in a single valid reference in
   * {@literal <references>}.
   * 
   * <p>This method should be called only when parsing CAP feeds of version
   * 1.1 or greater.
   *
   * <p>If the input string is not compliant with the XSD schema, and parsing it
   * is not possible, {@code null} is returned.
   */
  public static Date parseReferenceSent(String s) {
    String[] parts = s.split(",", 3);
    return (parts.length > 2) ? CapDateUtil.toJavaDate(parts[2]) : null;
  }
  
  /**
   * Parses a string into a URI, as defined by RFC 2396.
   * 
   * <p>{@code null} is returned if the string does not match the spec.
   */
  public static URI parseUri(String s) {
    try {
      return new URI(s);
    } catch (URISyntaxException e) {
      return null;
    }
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

  /**
   * @return {@true} if the input string contains HTML entities, {@code false}
   * otherwise
   */
  public static boolean containsHtmlEntities(String s) {
    return !StringEscapeUtils.unescapeHtml4(s).equals(s);
  }
  
  /**
   * @return {@true} if the input string contains HTML tags, {@code false}
   * otherwise
   */
  public static boolean containsHtmlTags(String s) {
    return HTML_TAG_PATTERN.matcher(s).find();
  }
  
  private CapUtil() {}
}

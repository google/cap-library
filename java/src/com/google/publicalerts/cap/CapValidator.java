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

import java.util.logging.Logger;

import static com.google.publicalerts.cap.CapException.ReasonType.CIRCULAR_REFERENCE;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_ALTITUDE_CEILING_RANGE;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_AREA;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_DEREF_URI;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_LANGUAGE;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_MIME_TYPE;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_POLYGON;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_URI;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_WEB;
import static com.google.publicalerts.cap.CapException.ReasonType.POSTDATED_REFERENCE;
import static com.google.publicalerts.cap.CapException.ReasonType.RELATIVE_URI_MISSING_DEREF_URI;
import static com.google.publicalerts.cap.CapException.ReasonType.RESTRICTION_SCOPE_MISMATCH;
import static com.google.publicalerts.cap.CapException.ReasonType.SAME_TEXT_DIFFERENT_LANGUAGE;
import static com.google.publicalerts.cap.CapException.ReasonType.TEXT_CONTAINS_HTML_ENTITIES;
import static com.google.publicalerts.cap.CapException.ReasonType.TEXT_CONTAINS_HTML_TAGS;
import static com.google.publicalerts.cap.CapException.ReasonType.INVALID_POLYGON_SELF_INTERSECTION;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import java.awt.geom.Line2D;

import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates portions of the CAP specification that are not possible to express in an XML schema
 * definition file.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidator {
  private static final Logger log =
      Logger.getLogger(CapValidator.class.getName());

  public static final String CAP10_XMLNS = "http://www.incident.com/cap/1.0";
  public static final String CAP11_XMLNS = "urn:oasis:names:tc:emergency:cap:1.1";
  public static final String CAP12_XMLNS = "urn:oasis:names:tc:emergency:cap:1.2";
  public static final String CAP_LATEST_XMLNS = CAP12_XMLNS;

  public static final Set<String> CAP_XML_NAMESPACES = 
      ImmutableSet.of(CAP10_XMLNS, CAP11_XMLNS, CAP12_XMLNS);

  /** See http://www.ietf.org/rfc/rfc3066.txt */
  private static final Pattern RFC_3066_LANGUAGE =
      Pattern.compile("^([a-zA-Z]{1,8})(-[a-zA-Z0-9]{1,8})*$");

  /** See http://www.iana.org/assignments/media-types/media-types.xhtml */
  private static final Set<String> VALID_CONTENT_TYPES = ImmutableSet.of(
      "application", "audio", "image", "message", "model", "multipart", "text", "video");
  
  /**
   * The fields of {@code <info>} that contain human-readable content, for which we want to verify
   * language consistency (i.e., no two fields have the same exact content if they are defined in
   * different {@code <info>} with different {@code <language>}).
   * 
   * <p>Human-readable fields of {@code <info>} that are likely to contain the same content across
   * different languages (e.g., {@code <senderName>}) are omitted from this set.
   */
  private static final Set<FieldDescriptor> HUMAN_READABLE_CONTENT_INFO_FIELDS = ImmutableSet.of(
      Info.getDescriptor().findFieldByNumber(Info.DESCRIPTION_FIELD_NUMBER),
      Info.getDescriptor().findFieldByNumber(Info.HEADLINE_FIELD_NUMBER),
      Info.getDescriptor().findFieldByNumber(Info.INSTRUCTION_FIELD_NUMBER),
      Info.getDescriptor().findFieldByNumber(Info.EVENT_FIELD_NUMBER));
  
  /**
   * The row of the table is the field where the text was seen, the column is the text itself,
   * the cell value is the primary subtag of the language in which the text was written.
   */
  private Table<FieldDescriptor, String, String> humanReadableText = HashBasedTable.create();
  
  /**
   * Validates a CAP alert.
   *
   * @param alert the alert to validate
   * @return a collection of errors, warnings, recommendations, or infos about the alert
   */
  public Reasons validateAlert(AlertOrBuilder alert) {
    int version = getValidateVersion(alert.getXmlns());
    Reasons.Builder reasons = Reasons.newBuilder();
    
    XPath xPath = new XPath();
    xPath.push("alert");
    
    // Validate restriction
    if (alert.hasRestriction() && !CapUtil.isEmptyOrWhitespace(alert.getRestriction())
        && alert.getScope() != Alert.Scope.RESTRICTED) {
      xPath.push("restriction");
      reasons.add(xPath.toString(), RESTRICTION_SCOPE_MISMATCH);
      xPath.pop();
    }

    // Validate references
    reasons.addAll(validateReferences(alert, xPath, version));
    
    // Validate infos, and the nested objects
    for (InfoOrBuilder infoOrBuilder : alert.getInfoOrBuilderList()) {
      reasons.addAll(validateInfo(infoOrBuilder, xPath));
    }

    xPath.pop();
    
    // Do another pass to validate all the String fields
    xPath = new XPath();
    xPath.push("alert");
    reasons.addAll(validateAllStringFields(alert, xPath));
    xPath.pop();
    
    return reasons.build();
  }

  private Reasons validateReferences(AlertOrBuilder alert, XPath xPath, int version) {
    Reasons.Builder reasons = Reasons.newBuilder();

    if (alert.hasReferences()) {
      xPath.push("references");
      
      String alertIdentifier = alert.getIdentifier();
      Date alertSent = CapDateUtil.toJavaDate(alert.getSent());
      
      for (String reference : alert.getReferences().getValueList()) {
        String referenceIdentifier =
            CapUtil.parseReferenceIdentifier(reference, version);
        
        if (referenceIdentifier == null) {
          continue;
        }
        
        if (alertIdentifier.equals(referenceIdentifier)) {
          reasons.add(xPath.toString(), CIRCULAR_REFERENCE, reference);
        }
        
        if (version > 10) {
          Date referenceSent = CapUtil.parseReferenceSent(reference);
          
          if (alertSent != null && referenceSent != null && referenceSent.after(alertSent)) {
            reasons.add(xPath.toString(), POSTDATED_REFERENCE, reference);
          }
        }
      }
      
      xPath.pop();
    }
    
    return reasons.build();
  }
  
  Reasons validateInfo(InfoOrBuilder info, XPath xPath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    xPath.push("info");

    // Validate language
    if (info.hasLanguage()) {
      String language = info.getLanguage().trim();
      Matcher matcher =  RFC_3066_LANGUAGE.matcher(language);
      
      if (!CapUtil.isEmptyOrWhitespace(language) && !matcher.matches()) {
        xPath.push("language");
        reasons.add(xPath.toString(), INVALID_LANGUAGE, language);
        xPath.pop();
      } else {
        reasons.addAll(
            validateHumanReadableContent(info, matcher.group(0), xPath));
      } 
    }

    // Validate areas
    for (AreaOrBuilder areaOrBuilder : info.getAreaOrBuilderList()) {
      reasons.addAll(validateArea(areaOrBuilder, xPath));
    }
    
    // Validate resources
    for (ResourceOrBuilder resourceOrBuilder : info.getResourceOrBuilderList()){
      reasons.addAll(validateResource(resourceOrBuilder, xPath));
    }
    
    // Validate web
    if (info.hasWeb()) {
      reasons.addAll(validateWeb(info.getWeb(), xPath));
    }
    
    xPath.pop();
    return reasons.build();
  }
  
  /**
   * Within this method, {@code language} refers to a  RFC 3066 primary-subtag, as we don't want to
   * distinguish between languages with a large overlap (e.g., en-US and en-GB).
   */
  private Reasons validateHumanReadableContent(InfoOrBuilder info, String language, XPath xPath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    
    for (FieldDescriptor humanReadableField : HUMAN_READABLE_CONTENT_INFO_FIELDS) {
      if (!info.hasField(humanReadableField)) {
        continue;
      }
      String fieldName = humanReadableField.getName();
      
      String fieldValue = (String) info.getField(humanReadableField);
      xPath.push(fieldName);
      
      String previousLanguage = humanReadableText.get(humanReadableField, fieldValue);
 
      if (previousLanguage != null) {
        if (!language.equals(previousLanguage)) {
          reasons.add(xPath.toString(), SAME_TEXT_DIFFERENT_LANGUAGE, fieldName);
        }
      } else {
        humanReadableText.put(humanReadableField, fieldValue, fieldName);
      }
      
      xPath.pop();
    }
    
    return reasons.build();
  }
  
  Reasons validateArea(AreaOrBuilder area, XPath xPath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    xPath.push("area");

    // Validate polygons
    for (PolygonOrBuilder polygonOrBuilder : area.getPolygonOrBuilderList()) {
      reasons.addAll(validatePolygon(polygonOrBuilder, xPath));
    }

    if (area.hasCeiling() && !area.hasAltitude()) {
      reasons.add(xPath.toString(), INVALID_AREA);
    }

    if (area.hasAltitude() && area.hasCeiling() && area.getAltitude() > area.getCeiling()) {
      xPath.push("ceiling");
      reasons.add(xPath.toString(), INVALID_ALTITUDE_CEILING_RANGE);
      xPath.pop();
    }

    xPath.pop();
    return reasons.build();
  }

  private Reasons validatePolygon(PolygonOrBuilder polygon, XPath xPath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    xPath.push("polygon");
    
    if (!polygon.getPoint(0).equals(polygon.getPoint(polygon.getPointCount() - 1))) {
      reasons.add(xPath.toString(), INVALID_POLYGON);
    }

    String intersectionDescription = getIntersectionDescription(polygon);
    if (intersectionDescription != null) {
      reasons.add(xPath.toString(), INVALID_POLYGON_SELF_INTERSECTION, intersectionDescription);
    }
    
    xPath.pop();
    return reasons.build();
  }

  Reasons validateResource(ResourceOrBuilder resource, XPath xPath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    xPath.push("resource");
    
    if (resource.hasMimeType()) {
      xPath.push("mimeType");
      String mimeType = resource.getMimeType(); // formatted as: type/subtype
      
      if (!mimeType.contains("/")
          || !VALID_CONTENT_TYPES.contains(mimeType.substring(0, mimeType.indexOf('/')))) {
        reasons.add(xPath.toString(), INVALID_MIME_TYPE, mimeType);
      }
      
      xPath.pop();
    }
    
    if (resource.hasUri()) {
      xPath.push("uri");
      URI uri = CapUtil.parseUri(resource.getUri());
      
      if (uri == null) {
        reasons.add(xPath.toString(), INVALID_URI, resource.getUri());
      } else if (!uri.isAbsolute() && !resource.hasDerefUri()) {
        reasons.add(xPath.toString(), RELATIVE_URI_MISSING_DEREF_URI);
      }
          
      xPath.pop();
    }
    
    if (resource.hasDerefUri() && !CapUtil.isBase64(resource.getDerefUri())) {
      xPath.push("derefUri");
      reasons.add(xPath.toString(), INVALID_DEREF_URI, resource.getDerefUri());
      xPath.pop();
    }

    xPath.pop();
    return reasons.build();
  }

  private Reasons validateWeb(String web, XPath xPath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    xPath.push("web");

    URI uri = CapUtil.parseUri(web);
    
    if (uri == null || !uri.isAbsolute()) {
      reasons.add(xPath.toString(), INVALID_WEB, web);
    }
    
    xPath.pop();
    return reasons.build();
  }
  
  int getValidateVersion(String xmlns) {
    if (CAP10_XMLNS.equals(xmlns)) {
      return 10;
    } else if (CAP11_XMLNS.equals(xmlns)) {
      return 11;
    }
    return 12;
  }
  
  /**
   * Performs a generic validation over all String values in the message.
   * 
   * @see #validateStringField
   */
  private Reasons validateAllStringFields(MessageOrBuilder message, XPath xPath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    
    for (FieldDescriptor fieldDescriptor : message.getDescriptorForType().getFields()) {
      FieldDescriptor.Type type = fieldDescriptor.getType();
      boolean isRepeated = fieldDescriptor.isRepeated();
      String fieldName = CapUtil.javaCase(fieldDescriptor.getName());

      if (type.equals(FieldDescriptor.Type.STRING)) {
        if (isRepeated) {
          for (int i = 0; i < message.getRepeatedFieldCount(fieldDescriptor); i++) {
            String fieldValue = (String) message.getRepeatedField(fieldDescriptor, i);
            reasons.addAll(validateStringField(fieldName, fieldValue, xPath));
          }
        } else if (message.hasField(fieldDescriptor)) {
          String fieldValue = (String) message.getField(fieldDescriptor);
          reasons.addAll(validateStringField(fieldName, fieldValue, xPath));
        }
      } else if (type.equals(FieldDescriptor.Type.MESSAGE)) {
        if (isRepeated) {
          for (int i = 0; i < message.getRepeatedFieldCount(fieldDescriptor); i++) {
            xPath.push(fieldName);
            reasons.addAll(validateAllStringFields(
                (Message) message.getRepeatedField(fieldDescriptor, i), xPath));
            xPath.pop();
          }
        } else if (message.hasField(fieldDescriptor)){
          xPath.push(fieldName);
          reasons.addAll(validateAllStringFields(
              (Message) message.getField(fieldDescriptor), xPath));
          xPath.pop();
        }
      }
    }
    
    return reasons.build();
  }
  
  /**
   * Validates a generic String value found in a message.
   */
  private Reasons validateStringField(String fieldName, String fieldValue, XPath xPath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    xPath.push(fieldName);
    
    /*
     * A proto field as parsed from CapXmlParser can have HTML entities if the HTML entities were
     * originally in a CDATA block.
     * 
     * Input XML <contact><![CDATA[a &lt; b]]></contact> is parsed in the proto to
     * fieldName="contact" fieldValue="a &lt; b"
     */
    if (CapUtil.containsHtmlEntities(fieldValue)) {
      reasons.add(xPath.toString(), TEXT_CONTAINS_HTML_ENTITIES, fieldName);
    }
    
    /* A proto field as parsed from CapXmlParser can have HTML tags from two cases:
     * 
     * 1. The HTML tags were originally encoded with HTML entities.
     *      Input XML <description>&lt;b&gt;WATCH OUT!&lt;/b&gt;</description> is parsed
     *      in the proto to fieldName="description" fieldValue="<b>WATCH OUT!</b>"
     * 2. The HTML tags were originally in a CDATA block
     *      Input XML <contact><![CDATA[<b>foobar</b>]]></contact> is parsed in the proto to
     *      fieldName="contact" fieldValue="<b>foobar</b>"
     */
    if (CapUtil.containsHtmlTags(fieldValue)) {
      reasons.add(xPath.toString(), TEXT_CONTAINS_HTML_TAGS, fieldName);
    }
    
    xPath.pop();
    return reasons.build();
  }

  // This method checks for self intersections in a polygon, under these assumptions:
  // 1. The polygon doesn't cover the North/South pole.
  // 2. Using lat-lon coordinates as cartesian coordinates and mapping onto a 2d plane is a
  // sufficent approximation.
  // 3. The last point of the polygon is identical to the first point.
  //
  // Returns null if no intersections were found, returns a description of an intersection else.
  private String getIntersectionDescription(PolygonOrBuilder polygon) {
    // Naive O(n^2) algorithm. If performance becomes an issue, consider using Bentley–Ottmann
    // algorithm, which is O(n*logn)
    
    // if the first point is duplicated, skip to the last duplicate.
    int start;
    for(start = 0; start < polygon.getPointCount() - 1; start++) {
      if (!polygon.getPoint(start).equals(polygon.getPoint(start + 1))) {
        break;
      }
    }

    // if the last point is duplicate, skip to the first duplicate
    int lastIndex;
    for (lastIndex = polygon.getPointCount() - 1; lastIndex > 0; lastIndex--) {
      if (!polygon.getPoint(lastIndex).equals(polygon.getPoint(lastIndex - 1))) {
        break;
      }
    }

    for (int i = start; i < lastIndex - 1; i++) {
      for (int j = i + 2; j < lastIndex; j++) {
        // The first and last lines share the same point, so their intersection is ok.
        if (i == start && j == lastIndex - 1) {
          continue;
        }
        Point p0 = polygon.getPoint(i);
        Point p1 = polygon.getPoint(i + 1);
        Point p2 = polygon.getPoint(j);
        Point p3 = polygon.getPoint(j + 1);

        // Duplicate point - ignore the intersection of the two lines from this point.
        if (p1.equals(p2)) {
          continue;
        }

        // Duplicate point - in that case line1 or line2 isn't a line, but a single point.
        if (p0.equals(p1) || p2.equals(p3)) {
          continue;
        }

        Line2D line1 =
            new Line2D.Double(
                p0.getLatitude(),
                toLegalLongitude(p0.getLongitude()),
                p1.getLatitude(),
                toLegalLongitude(p1.getLongitude()));
        Line2D line2 =
            new Line2D.Double(
                p2.getLatitude(),
                toLegalLongitude(p2.getLongitude()),
                p3.getLatitude(),
                toLegalLongitude(p3.getLongitude()));

        if (line2.intersectsLine(line1)) {
          return "Detected intersection in the following lines:\n" 
              + String.format("line1: %s,%s  -->  %s,%s\n", p0.getLatitude(), p0.getLongitude(),
                  p1.getLatitude(), p1.getLongitude()) 
              + String.format("line2: %s,%s  -->  %s,%s",
                  p2.getLatitude(), p2.getLongitude(),
                  p3.getLatitude(), p3.getLongitude());
        }
      }
    }
    return null;
  }

  // returns longitude in the range of [-180, 180]
  private Double toLegalLongitude(Double longitude) {
    if (longitude > 180) {
      return longitude - 360;
    }
    if (longitude < -180) {
      return longitude + 360;
    }
    return longitude;
  }
}

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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.MessageOrBuilder;
import com.google.publicalerts.cap.CapException.ReasonType;

import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates portions of the CAP spec that are not possible to
 * express in an XML schema definition file.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidator {

  public static final String CAP10_XMLNS =
      "http://www.incident.com/cap/1.0";
  public static final String CAP11_XMLNS =
      "urn:oasis:names:tc:emergency:cap:1.1";
  public static final String CAP12_XMLNS =
      "urn:oasis:names:tc:emergency:cap:1.2";
  public static final String CAP_LATEST_XMLNS = CAP12_XMLNS;

  public static final Set<String> CAP_XML_NAMESPACES = 
      ImmutableSet.of(CAP10_XMLNS, CAP11_XMLNS, CAP12_XMLNS);

  /** See http://www.ietf.org/rfc/rfc3066.txt */
  private static final Pattern RFC_3066_LANGUAGE =
      Pattern.compile("^[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*$");

  /** See http://www.iana.org/assignments/media-types/media-types.xhtml */
  private static final Set<String> VALID_CONTENT_TYPES =
      ImmutableSet.<String>builder()
          .add("application")
          .add("audio")
          .add("image")
          .add("message")
          .add("model")
          .add("multipart")
          .add("text")
          .add("video")
          .build();
      
  /**
   * Validates the given message.
   *
   * @param message the message to validate
   * @param xmlns xmlns of the version whose validation rules are to be applied
   * @param visitChildren true to also validate child messages
   * @return a collection of reasons storing errors, warnings or
   * recommendations
   */
  public Reasons validate(MessageOrBuilder message, String xmlns,
      String xpath, boolean visitChildren) {
    int version = getValidateVersion(xmlns);

    if (message instanceof AlertOrBuilder) {
      return validateAlert((AlertOrBuilder) message, visitChildren);
    } else if (message instanceof InfoOrBuilder) {
      return validateInfo(
          (InfoOrBuilder) message, xpath, version, visitChildren);
    } else if (message instanceof AreaOrBuilder) {
      return validateArea(
          (AreaOrBuilder) message, xpath, version, visitChildren);
    } else if (message instanceof ResourceOrBuilder) {
      return validateResource(
          (ResourceOrBuilder) message, xpath, version);
    } else if (message instanceof PolygonOrBuilder) {
      return validatePolygon((PolygonOrBuilder) message, xpath);
    } else if (message instanceof ValuePairOrBuilder
        || message instanceof GroupOrBuilder
        || message instanceof CircleOrBuilder
        || message instanceof PointOrBuilder) {
      return Reasons.EMPTY;
    }

    throw new IllegalArgumentException("Unsupported message: " + message);
  }

  /**
   * Validates a CAP alert.
   *
   * @param alert the alert to validate
   * @throws CapException if there are validation errors
   */
  public void validateAlert(AlertOrBuilder alert) throws CapException {
    Reasons reasons = validateAlert(alert, true);
    
    if (reasons.containsWithLevelOrHigher(Reason.Level.ERROR)) {
      throw new CapException(reasons);
    }
  }

  Reasons validateAlert(AlertOrBuilder alert, boolean visitChildren) {
    Reasons.Builder reasons = Reasons.newBuilder();
    
    if (alert.hasRestriction()
        && !CapUtil.isEmptyOrWhitespace(alert.getRestriction())
        && alert.getScope() != Alert.Scope.RESTRICTED) {
      reasons.add(new Reason("/alert[1]/restriction[1]",
          ReasonType.RESTRICTION_SCOPE_MISMATCH));
    }

    reasons.addAll(validateReferences(alert));
    
    if (visitChildren) {
      int version = getValidateVersion(alert.getXmlns());
      for (int i = 0; i < alert.getInfoOrBuilderList().size(); ++i) {
        reasons.addAll(validateInfo(alert.getInfoOrBuilderList().get(i),
            "/alert[1]/info[" + (i + 1) + "]", version, visitChildren));
      }
    }

    return reasons.build();
  }

  private Reasons validateReferences(AlertOrBuilder alert) {
    Reasons.Builder reasons = Reasons.newBuilder();

    int version = getValidateVersion(alert.getXmlns());
    
    if (alert.hasReferences()) {
      String alertIdentifier = alert.getIdentifier();
      Date alertSent = CapDateUtil.toJavaDate(alert.getSent());
      
      for (String reference : alert.getReferences().getValueList()) {
        String referenceIdentifier =
            CapUtil.parseReferenceIdentifier(reference, version);
        
        if (referenceIdentifier == null) {
          continue;
        }
        
        if (alertIdentifier.equals(referenceIdentifier)) {
          reasons.add(new Reason("/alert[1]/references[1]",
              ReasonType.CIRCULAR_REFERENCE, reference));
        }
        
        if (version > 10) {
          Date referenceSent = CapUtil.parseReferenceSent(reference);
          
          if (alertSent != null && referenceSent != null
              && referenceSent.after(alertSent)) {
            reasons.add(new Reason("/alert[1]/references[1]",
                ReasonType.POSTDATED_REFERENCE, reference));
          }
        }
      }
    }
    
    return reasons.build();
  }
  
  Reasons validateInfo(InfoOrBuilder info, String xpath,
      int version, boolean visitChildren) {
    Reasons.Builder reasons = Reasons.newBuilder();

    if (info.hasLanguage()) {
      reasons.addAll(validateLanguage(info.getLanguage(), xpath, version));
    }

    if (visitChildren) {
      for (int i = 0; i < info.getAreaOrBuilderList().size(); ++i) {
        reasons.addAll(validateArea(info.getAreaOrBuilderList().get(i),
            xpath + "/area[" + (i + 1) + "]", version, visitChildren));
      }

      for (int i = 0; i < info.getResourceOrBuilderList().size(); ++i) {
        reasons.addAll(validateResource(info.getResourceOrBuilderList().get(i),
            xpath + "/resource[" + (i + 1) + "]", version));
      }
    }
    
    return reasons.build();
  }

  @SuppressWarnings("unused")
  Reasons validateLanguage(String language, String xpath, int version) {
    if (CapUtil.isEmptyOrWhitespace(language)) {
      return Reasons.EMPTY;
    }

    language = language.trim();

    if (RFC_3066_LANGUAGE.matcher(language).matches()) {
      return Reasons.EMPTY;
    }

    return Reasons.of(new Reason(xpath + "/language[1]",
        ReasonType.INVALID_LANGUAGE, language));
  }

  @SuppressWarnings("unused")
  Reasons validateArea(
      AreaOrBuilder area, String xpath, int version, boolean visitChildren) {
    Reasons.Builder reasons = Reasons.newBuilder();

    if (visitChildren) {
      for (int i = 0; i < area.getPolygonOrBuilderList().size(); i++) {
        reasons.addAll(validatePolygon(
            area.getPolygonOrBuilder(i), xpath + "/polygon[" + (i + 1) + "]"));
      }
    }

    if (area.hasCeiling() && !area.hasAltitude()) {
      reasons.add(new Reason(xpath, ReasonType.INVALID_AREA));
    }

    if (area.hasAltitude() && area.hasCeiling()) {
      if (area.getAltitude() > area.getCeiling()) {
        reasons.add(new Reason(xpath + "/ceiling[1]",
            ReasonType.INVALID_ALTITUDE_CEILING_RANGE));
      }
    }

    return reasons.build();
  }

  Reasons validatePolygon(PolygonOrBuilder polygon, String xpath) {
    Reasons.Builder reasons = Reasons.newBuilder();
    
    if (!polygon.getPoint(0).equals(
        polygon.getPoint(polygon.getPointCount() - 1))) {
      reasons.add(new Reason(xpath, ReasonType.INVALID_POLYGON));
    }
    
    return reasons.build();
  }

  @SuppressWarnings("unused")
  Reasons validateResource(
      ResourceOrBuilder resource, String xpath, int version) {
    Reasons.Builder reasons = Reasons.newBuilder();
    
    if (resource.hasMimeType()) {
      String mimeType = resource.getMimeType(); // formatted as: type/subtype
      
      if (!mimeType.contains("/") || !VALID_CONTENT_TYPES.contains(
          mimeType.substring(0, mimeType.indexOf('/')))) {
        reasons.add(new Reason(
            xpath + "/mimeType[1]", ReasonType.INVALID_MIME_TYPE, mimeType));
      }    
    }
    
    if (resource.hasDerefUri() && !CapUtil.isBased64(resource.getDerefUri())) {
      reasons.add(new Reason(xpath + "/derefUri[1]",
          ReasonType.INVALID_DEREF_URI, resource.getDerefUri()));
    }

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
}

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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.protobuf.MessageOrBuilder;
import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.CapException.Type;

/**
 * Validates a CAP alert.
 * Supports CAP 1.0, 1.1, and 1.2.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidator {

  public static final String CAP10_XMLNS = "http://www.incident.com/cap/1.0";
  public static final String CAP11_XMLNS =
      "urn:oasis:names:tc:emergency:cap:1.1";
  public static final String CAP12_XMLNS =
      "urn:oasis:names:tc:emergency:cap:1.2";
  public static final String CAP_LATEST_XMLNS = CAP12_XMLNS;

  public static final Set<String> CAP_XML_NAMESPACES = new HashSet<String>();
  static {
    CAP_XML_NAMESPACES.add(CAP10_XMLNS);
    CAP_XML_NAMESPACES.add(CAP11_XMLNS);
    CAP_XML_NAMESPACES.add(CAP12_XMLNS);
  }

  // Spec says the identifer and sender
  // "MUST NOT include spaces, commas or restricted characters (< and &)"
  private static final Pattern ID_SENDER_ILLEGAL_CHARS =
      Pattern.compile(".*[\\s,&<].*");

  /**
   * Validates the given message.
   *
   * @param message the message to validate
   * @param xmlns xmlns of the version whose validation rules are to be applied
   * @param visitChildren true to also validate child messages
   * @return a list of reasons the message is invalid, empty if valid
   */
  public List<Reason> validate(MessageOrBuilder message, String xmlns,
      int infoSeqNum, boolean visitChildren) {
    int version = getValidateVersion(xmlns);

    if (message instanceof AlertOrBuilder) {
      return validateAlert((AlertOrBuilder) message, visitChildren);
    } else if (message instanceof InfoOrBuilder) {
      return validateInfo(
          (InfoOrBuilder) message, infoSeqNum, version, visitChildren);
    } else if (message instanceof AreaOrBuilder) {
      return validateArea(
          (AreaOrBuilder) message, infoSeqNum, version, visitChildren);
    } else if (message instanceof ResourceOrBuilder) {
      return validateResource((ResourceOrBuilder) message, infoSeqNum, version);
    } else if (message instanceof ValuePairOrBuilder) {
      return Collections.<Reason>emptyList();
    } else if (message instanceof GroupOrBuilder) {
      return Collections.<Reason>emptyList();
    } else if (message instanceof CircleOrBuilder) {
      return validateCircle((CircleOrBuilder) message, infoSeqNum);
    } else if (message instanceof PolygonOrBuilder) {
      return validatePolygon((PolygonOrBuilder) message, infoSeqNum);
    } else if (message instanceof PointOrBuilder) {
      return validatePoint((PointOrBuilder) message, infoSeqNum);
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
  	List<Reason> reasons = validateAlert(alert, true);
    if (!reasons.isEmpty()) {
      throw new CapException(reasons);
    }
  }

  @SuppressWarnings("deprecation")
  private List<Reason> validateAlert(
      AlertOrBuilder alert, boolean visitChildren) {
    int version = getValidateVersion(alert.getXmlns());

    List<Reason> reasons = new ArrayList<Reason>();

    if (!alert.hasXmlns()) {
      reasons.add(new Reason(Type.XMLNS_IS_REQUIRED));
    }

    if (!alert.hasIdentifier() || "".equals(alert.getIdentifier())) {
      reasons.add(
          new Reason(Type.IDENTIFIER_IS_REQUIRED, alert.getIdentifier()));
    } else if (ID_SENDER_ILLEGAL_CHARS.matcher(
        alert.getIdentifier()).matches()) {
      reasons.add(new Reason(Type.INVALID_IDENTIFIER));
    }

    if (alert.hasPassword() && version > 10) {
      reasons.add(new Reason(Type.PASSWORD_DEPRECATED));
    }

    if (!alert.hasSender() || "".equals(alert.getSender())) {
      reasons.add(new Reason(Type.SENDER_IS_REQUIRED));
    } else if (ID_SENDER_ILLEGAL_CHARS.matcher(alert.getSender()).matches()) {
      reasons.add(new Reason(Type.INVALID_SENDER, alert.getSender()));
    }

    if (!alert.hasSent() || "".equals(alert.getSent())) {
      reasons.add(new Reason(Type.SENT_IS_REQUIRED));
    } else if (!CapUtil.isValidDate(alert.getSent())) {
      reasons.add(new Reason(Type.INVALID_SENT, alert.getSent()));
    }

    if (!alert.hasStatus()) {
      reasons.add(new Reason(Type.STATUS_IS_REQUIRED));
    }

    if (!alert.hasMsgType()) {
      reasons.add(new Reason(Type.MSGTYPE_IS_REQUIRED));
    }

    // Scope made required in CAP 1.1
    if (!alert.hasScope() && !CAP10_XMLNS.equals(alert.getXmlns())) {
      reasons.add(new Reason(Type.SCOPE_IS_REQUIRED));
    }

    if (alert.hasRestriction() && !"".equals(alert.getRestriction())
        && alert.getScope() != Alert.Scope.Restricted) {
      reasons.add(new Reason(Type.RESTRICTION_SCOPE_MISMATCH));
    }

    if (alert.hasAddresses() && alert.getAddresses().getValueCount() > 0
        && alert.getScope() != Alert.Scope.Private) {
      reasons.add(new Reason(Type.ADDRESSES_SCOPE_MISMATCH));
    }

    if (visitChildren) {
      for (int i = 0; i < alert.getInfoOrBuilderList().size(); ++i) {
        reasons.addAll(validateInfo(
            alert.getInfoOrBuilderList().get(i), i, version, visitChildren));
      }
    }

    return reasons;
  }

  List<Reason> validateInfo(InfoOrBuilder info, int infoSeqNum,
      int version, boolean visitChildren) {
    List<Reason> reasons = new ArrayList<Reason>();

    // TODO(shakusa) Validate language is valid RFC 3066?

    // Category made required in CAP 1.1
    if (info.getCategoryCount() == 0  && version >= 11) {
      reasons.add(new Reason(Type.INFO_CATEGORY_IS_REQUIRED, infoSeqNum));
    }

    if (!info.hasEvent()) {
      reasons.add(new Reason(Type.INFO_EVENT_IS_REQUIRED, infoSeqNum));
    }

    if (!info.hasUrgency()) {
      reasons.add(new Reason(Type.INFO_URGENCY_IS_REQUIRED, infoSeqNum));
    }

    if (!info.hasSeverity()) {
      reasons.add(new Reason(Type.INFO_SEVERITY_IS_REQUIRED, infoSeqNum));
    }

    if (!info.hasCertainty()) {
      reasons.add(new Reason(Type.INFO_CERTAINTY_IS_REQUIRED, infoSeqNum));
    }
    if (info.getCertainty() == Info.Certainty.VeryLikely && version > 10) {
      reasons.add(new Reason(
          Type.INFO_CERTAINTY_VERY_LIKELY_DEPRECATED, infoSeqNum));
    }

    if (info.hasEffective() && !CapUtil.isValidDate(info.getEffective())) {
      reasons.add(new Reason(
          Type.INFO_INVALID_EFFECTIVE, infoSeqNum, info.getEffective()));
    }

    if (info.hasOnset() && !CapUtil.isValidDate(info.getOnset())) {
      reasons.add(new Reason(
          Type.INFO_INVALID_ONSET, infoSeqNum, info.getOnset()));
    }

    if (info.hasExpires() && !CapUtil.isValidDate(info.getExpires())) {
      reasons.add(new Reason(
          Type.INFO_INVALID_EXPIRES, infoSeqNum, info.getExpires()));
    }

    if (info.hasWeb() && !isAbsoluteUri(info.getWeb())) {
      reasons.add(new Reason(
          Type.INFO_INVALID_WEB, infoSeqNum, info.getWeb()));
    }

    if (visitChildren) {
      for (int i = 0; i < info.getAreaOrBuilderList().size(); ++i) {
        reasons.addAll(validateArea(info.getAreaOrBuilderList().get(i),
            i, version, visitChildren));
      }

      for (int i = 0; i < info.getResourceList().size(); ++i) {
        reasons.addAll(validateResource(info.getResourceOrBuilderList().get(i),
            i, version));
      }
    }
    return reasons;
  }

  List<Reason> validateArea(AreaOrBuilder area, int infoSeqNum,
                            int version, boolean visitChildren) {
    List<Reason> reasons = new ArrayList<Reason>();

    if (!area.hasAreaDesc()) {
      reasons.add(new Reason(Type.AREA_AREA_DESC_IS_REQUIRED, infoSeqNum));
    }

    for (PolygonOrBuilder polygon : area.getPolygonOrBuilderList()) {
    	reasons.addAll(validatePolygon(polygon, infoSeqNum));
    }

    for (CircleOrBuilder circle : area.getCircleOrBuilderList()) {
    	reasons.addAll(validateCircle(circle, infoSeqNum));
    }

    if (area.hasCeiling() && !area.hasAltitude()) {
      reasons.add(new Reason(Type.AREA_INVALID_CEILING, infoSeqNum));
    }

    if (area.hasAltitude() && area.hasCeiling()) {
      if (area.getAltitude() > area.getCeiling()) {
        reasons.add(new Reason(
            Type.AREA_INVALID_ALTITUDE_CEILING_RANGE, infoSeqNum));
      }
    }

    return reasons;
  }

  List<Reason> validateCircle(CircleOrBuilder circle, int infoSeqNum) {
    List<Reason> reasons = new ArrayList<Reason>();
  	reasons.addAll(validatePoint(circle.getPoint(), infoSeqNum));

    if (circle.getRadius() < 0) {
      reasons.add(new Reason(Type.AREA_INVALID_CIRCLE_RADIUS, infoSeqNum,
          circle.getRadius()));
    }
    return reasons;
  }

  List<Reason> validatePolygon(PolygonOrBuilder polygon, int infoSeqNum) {
    List<Reason> reasons = new ArrayList<Reason>();
    if (polygon.getPointCount() < 4) {
      reasons.add(new Reason(Type.AREA_INVALID_POLYGON_NUM_POINTS, infoSeqNum));
    }

    if (!polygon.getPoint(0).equals(
        polygon.getPoint(polygon.getPointCount() - 1))) {
      reasons.add(new Reason(Type.AREA_INVALID_POLYGON_START_END, infoSeqNum));
    }

    for (PointOrBuilder point : polygon.getPointOrBuilderList()) {
      reasons.addAll(validatePoint(point, infoSeqNum));
    }
    return reasons;
  }

  List<Reason> validatePoint(PointOrBuilder point, int infoSeqNum) {
    List<Reason> reasons = new ArrayList<Reason>();

    if (point.getLatitude() < -90 || point.getLatitude() > 90) {
      reasons.add(new Reason(Type.AREA_INVALID_POINT_LATITUDE, infoSeqNum,
          point.getLatitude()));
    }

    if (point.getLongitude() < -180 || point.getLongitude() > 180) {
      reasons.add(new Reason(Type.AREA_INVALID_POINT_LONGITUDE, infoSeqNum,
          point.getLongitude()));
    }

    return reasons;
  }

  List<Reason> validateResource(
      ResourceOrBuilder resource, int infoSeqNum, int version) {
    List<Reason> reasons = new ArrayList<Reason>();

    if (!resource.hasResourceDesc() || "".equals(resource.getResourceDesc())) {
      reasons.add(new Reason(
          Type.RESOURCE_RESOURCE_DESC_IS_REQUIRED, infoSeqNum));
    }

    if (!resource.hasMimeType() && version >= 12) {
      reasons.add(new Reason(Type.RESOURCE_MIME_TYPE_IS_REQUIRED, infoSeqNum));
    }

    if (resource.hasSize() && resource.getSize() < 0) {
      reasons.add(new Reason(
          Type.RESOURCE_INVALID_SIZE, infoSeqNum, resource.getSize()));
    }

    // TODO(shakusa) Check if mime type is valid RFC 2046?

    if (resource.hasUri() && !isAbsoluteUri(resource.getUri())) {
      reasons.add(new Reason(
          Type.RESOURCE_INVALID_URI, infoSeqNum, resource.getUri()));
    }

    // TODO(shakusa) Check if derefUri is base-64 encoded?

    return reasons;
  }

  int getValidateVersion(String xmlns) {
    if (CAP10_XMLNS.equals(xmlns)) {
      return 10;
    } else if (CAP11_XMLNS.equals(xmlns)) {
      return 11;
    }
    return 12;
  }

  boolean isAbsoluteUri(String uri) {
    try {
      return new URI(uri).isAbsolute();
    } catch (URISyntaxException e) {
      return false;
    }
  }
}

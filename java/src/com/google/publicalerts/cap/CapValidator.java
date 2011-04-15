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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.protobuf.MessageOrBuilder;
import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.CapException.Type;

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

  public static final Set<String> CAP_XML_NAMESPACES = new HashSet<String>();
  static {
    CAP_XML_NAMESPACES.add(CAP10_XMLNS);
    CAP_XML_NAMESPACES.add(CAP11_XMLNS);
    CAP_XML_NAMESPACES.add(CAP12_XMLNS);
  }

  /**
   * Validates the given message.
   *
   * @param message the message to validate
   * @param xmlns xmlns of the version whose validation rules are to be applied
   * @param visitChildren true to also validate child messages
   * @return a list of reasons the message is invalid, empty if valid
   */
  public List<Reason> validate(MessageOrBuilder message, String xmlns,
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
      return Collections.emptyList();
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
    List<Reason> reasons = new ArrayList<Reason>();

    if (alert.hasRestriction()
        && !CapUtil.isEmptyOrWhitespace(alert.getRestriction())
        && alert.getScope() != Alert.Scope.Restricted) {
      reasons.add(new Reason("/alert", Type.RESTRICTION_SCOPE_MISMATCH));
    }

    if (visitChildren) {
      int version = getValidateVersion(alert.getXmlns());
      for (int i = 0; i < alert.getInfoOrBuilderList().size(); ++i) {
        reasons.addAll(validateInfo(alert.getInfoOrBuilderList().get(i),
            "/alert/info[" + i + "]", version, visitChildren));
      }
    }

    return reasons;
  }

  List<Reason> validateInfo(InfoOrBuilder info, String xpath,
      int version, boolean visitChildren) {
    List<Reason> reasons = new ArrayList<Reason>();

    if (visitChildren) {
      for (int i = 0; i < info.getAreaOrBuilderList().size(); ++i) {
        reasons.addAll(validateArea(info.getAreaOrBuilderList().get(i),
            xpath + "/area[" + i + "]", version, visitChildren));
      }

      for (int i = 0; i < info.getResourceOrBuilderList().size(); ++i) {
        reasons.addAll(validateResource(info.getResourceOrBuilderList().get(i),
            xpath + "/resource[" + i + "]", version));
      }
    }
    return reasons;
  }

  List<Reason> validateArea(AreaOrBuilder area, String xpath,
                            int version, boolean visitChildren) {
    List<Reason> reasons = new ArrayList<Reason>();

    if (visitChildren) {
      for (int i = 0; i < area.getPolygonOrBuilderList().size(); i++) {
      	reasons.addAll(validatePolygon(
      	    area.getPolygonOrBuilder(i), xpath + "/polygon[" + i + "]"));
      }
    }

    if (area.hasCeiling() && !area.hasAltitude()) {
      reasons.add(new Reason(xpath + "/ceiling", Type.INVALID_AREA));
    }

    if (area.hasAltitude() && area.hasCeiling()) {
      if (area.getAltitude() > area.getCeiling()) {
        reasons.add(new Reason(xpath + "/ceiling",
            Type.INVALID_ALTITUDE_CEILING_RANGE));
      }
    }

    return reasons;
  }

  List<Reason> validatePolygon(PolygonOrBuilder polygon, String xpath) {
    List<Reason> reasons = new ArrayList<Reason>();
    if (!polygon.getPoint(0).equals(
        polygon.getPoint(polygon.getPointCount() - 1))) {
      reasons.add(new Reason(xpath, Type.INVALID_POLYGON));
    }
    return reasons;
  }

  List<Reason> validateResource(
      ResourceOrBuilder resource, String xpath, int version) {

    // TODO(shakusa) Check if mime type is valid RFC 2046?
    // TODO(shakusa) Check if derefUri is base-64 encoded?

    return Collections.<Reason>emptyList();
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

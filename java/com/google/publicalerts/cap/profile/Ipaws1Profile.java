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

package com.google.publicalerts.cap.profile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.AlertOrBuilder;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.CapException.ReasonType;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.ValuePair;

/**
 * US FEMA Integrated Public Alert and Warning System
 * CAP Profile, version 1.
 * <p>
 * Most of these checks are not possible to represent with an
 * xsd schema.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class Ipaws1Profile extends AbstractCapProfile {

  public Ipaws1Profile() {
    super();
  }

  public Ipaws1Profile(boolean strictXsdValidation) {
    super(strictXsdValidation);
  }

  @Override
  public String getName() {
    return "US IPAWS Profile v1.0";
  }

  @Override
  public String getCode() {
    return "IPAWSv1.0";
  }

  @Override
  public String getDocumentationUrl() {
    return "http://docs.oasis-open.org/emergency/cap/v1.2/ipaws-profile/v1.0/cap-v1.2-ipaws-profile-v1.0.pdf";
  }

  @Override
  public List<Reason> checkForErrors(AlertOrBuilder alert) {
    List<Reason> reasons = new ArrayList<Reason>();

    // sent SHALL include the timezone offset
    checkZeroTimezone(reasons, alert.getSent(), "/alert/sent",
        ErrorType.SENT_INCLUDE_TIMEZONE_OFFSET);

    // code SHALL include the string "IPAWSv1.0" to indicate the profile
    // version in use.
    boolean hasVersionCode = false;
    for (String code : alert.getCodeList()) {
      if (getCode().equals(code)) {
        hasVersionCode = true;
      }
    }
    if (!hasVersionCode) {
      reasons.add(new Reason(
          "/alert", ErrorType.VERSION_CODE_REQUIRED, getCode()));
    }

    // if Update or Cancel, references should contain all
    // non-expired messages
    if ((alert.getMsgType() == Alert.MsgType.Update
        || alert.getMsgType() == Alert.MsgType.Cancel)
        && alert.getReferences().getValueCount() == 0) {
      reasons.add(new Reason(
          "/alert/msgType", ErrorType.UPDATE_OR_CANCEL_MUST_REFERENCE));
    }

    Set<ValuePair> eventCodes = null;
    Set<Info.Category> categories = null;
    for (int i = 0; i < alert.getInfoCount(); i++) {
      String xpath = "/alert/info[" + i + "]";
      // All info blocks in a single alert MUST have the same category and
      // eventCode values.
      Info info = alert.getInfo(i);
      Set<ValuePair> ecs = new HashSet<ValuePair>();
      ecs.addAll(info.getEventCodeList());
      Set<Info.Category> cats = new HashSet<Info.Category>();
      cats.addAll(info.getCategoryList());
      if (eventCodes == null) {
        eventCodes = ecs;
        categories = cats;
      } else {
        if (!eventCodes.equals(ecs)) {
          reasons.add(new Reason(xpath, ErrorType.EVENT_CODES_MUST_MATCH));
        }
        if (!categories.equals(cats)) {
          reasons.add(new Reason(xpath, ErrorType.CATEGORIES_MUST_MATCH));
        }
      }

      // expires is required.  The value MUST include the timezone offset.
      if (CapUtil.isEmptyOrWhitespace(info.getExpires())) {
        reasons.add(new Reason(xpath, ErrorType.EXPIRES_IS_REQUIRED));
      } else {
        checkZeroTimezone(reasons, info.getExpires(), xpath + "/expires",
            ErrorType.EXPIRES_INCLUDE_TIMEZONE_OFFSET);
      }

      // MUST have one and only one eventCode with valueName of "SAME"
      // and a SAME-standard three-letter value.
      int sameCodeCount = 0;
      for (int j = 0; j < info.getEventCodeCount(); j++) {
        ValuePair eventCode = info.getEventCode(j);
        if ("SAME".equals(eventCode.getValueName())) {
          sameCodeCount++;
          if (eventCode.getValue().length() != 3) {
            sameCodeCount = 0;
            break;
          }
        }
      }
      if (sameCodeCount != 1) {
        reasons.add(new Reason(
            xpath, ErrorType.SAME_EVENT_CODE_REQUIRED));
      }

      // At least one <area> block MUST be present.
      if (info.getAreaCount() == 0) {
        reasons.add(new Reason(xpath, ErrorType.AREA_IS_REQUIRED));
      }

      // TODO(shakusa) Do we want to check for this? Does that mean
      // separate profiles for EAS / HazCollect and CMAS?
      // An alert intended for EAS and/or HazCollect dissemination MUST
      // include a parameter with a valueName of "EAS-ORG" with a value of
      // SAME ORG code.
    }

    return reasons;
  }


  @Override
  public List<Reason> checkForRecommendations(AlertOrBuilder alert) {
    List<Reason> reasons = new ArrayList<Reason>();

    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert/info[" + i + "]";

      // effective, onset are ignored if present.
      if (!CapUtil.isEmptyOrWhitespace(info.getEffective())) {
        reasons.add(new Reason(xpath + "/effective",
            RecommendationType.INFO_EFFECTIVE_IS_IGNORED));
      }
      if (!CapUtil.isEmptyOrWhitespace(info.getOnset())) {
        reasons.add(new Reason(xpath + "/onset",
            RecommendationType.INFO_ONSET_IS_IGNORED));
      }

      // description should be there
      if (CapUtil.isEmptyOrWhitespace(info.getDescription())) {
        reasons.add(new Reason(xpath,
            RecommendationType.INFO_DESCRIPTION_RECOMMENDED));
      }

      // instruction should be there
      if (CapUtil.isEmptyOrWhitespace(info.getInstruction())) {
        reasons.add(new Reason(xpath,
            RecommendationType.INFO_INSTRUCTION_RECOMMENDED));
      }

      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        // At least one <geocode> required with a valueName of "SAME" and
        // value of a SAME 6-digit location code (extended FIPS).
        boolean hasSameGeocode = false;
        for (ValuePair geocode : area.getGeocodeList()) {
          if ("SAME".equals(geocode.getValueName())) {
            if (geocode.getValue().length() == 6) {
              hasSameGeocode = true;
              break;
            }
          }
        }
        if (!hasSameGeocode) {
          reasons.add(new Reason(xpath + "/area[" + j + "]",
              RecommendationType.AREA_SAME_GEOCODE_RECOMMENDED));
        }
      }
    }

    return reasons;
  }

  // TODO(shakusa) Localize messages
  public enum ErrorType implements CapException.ReasonType {
    SENT_INCLUDE_TIMEZONE_OFFSET("<sent> should include the timezone offset." +
        "An offset of 0 is unlikely for US alerts."),
    EXPIRES_INCLUDE_TIMEZONE_OFFSET("<expires> should include the timezone" +
        "offset. An offset of 0 is unlikely for US alerts."),
    VERSION_CODE_REQUIRED("<code>{0}</code> required"),
    UPDATE_OR_CANCEL_MUST_REFERENCE("All related messages that have not yet " +
    "expired MUST be referenced for \"Update\" and \"Cancel\" messages."),
    EVENT_CODES_MUST_MATCH(
        "All <info> blocks must contain the same <eventCode>s"),
    CATEGORIES_MUST_MATCH(
        "All <info> blocks must contain the same <category>s"),
    SAME_EVENT_CODE_REQUIRED("Messages intended for EAS, CMAS and " +
        "HazCollect dissemination MUST include one and only one instance of " +
        "<eventCode> with a <valueName> of \"SAME\" and using a" +
        "SAME-standard three-letter value."),
    EXPIRES_IS_REQUIRED("<expires> is required"),
    AREA_IS_REQUIRED("At least one <area> must be present"),
    ;

    private final String message;

    private ErrorType(String message) {
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }
  }

  // TODO(shakusa) Localize messages
  public enum RecommendationType implements CapException.ReasonType {
    INFO_EFFECTIVE_IS_IGNORED("<effective> is ignored if present.  Alerts " +
        "SHALL be effective upon issuance."),
    INFO_ONSET_IS_IGNORED("<onset> is ignored if present.  Alerts " +
        "SHALL be effective upon issuance."),
    INFO_DESCRIPTION_RECOMMENDED("Messages should have meaningful values " +
        "for the <description>."),
    INFO_INSTRUCTION_RECOMMENDED("Messages should have meaningful values " +
        "for the <instruction>."),
    AREA_SAME_GEOCODE_RECOMMENDED("At least one instance of <geocode> with " +
        "a <valueName> of \"SAME\" and a value of a SAME 6-digit location " +
        "(extended FIPS) SHOULD be used."),
    ;

    private final String message;

    private RecommendationType(String message) {
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }
  }
}

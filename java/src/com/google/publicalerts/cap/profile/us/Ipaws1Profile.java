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

package com.google.publicalerts.cap.profile.us;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.publicalerts.cap.Reason.Level.ERROR;
import static com.google.publicalerts.cap.Reason.Level.RECOMMENDATION;

import com.google.common.collect.Sets;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.AlertOrBuilder;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.Reason.Level;
import com.google.publicalerts.cap.Reasons;
import com.google.publicalerts.cap.ValuePair;
import com.google.publicalerts.cap.profile.AbstractCapProfile;

import java.util.Locale;
import java.util.Set;

/**
 * US FEMA Integrated Public Alert and Warning System CAP Profile, version 1.
 * 
 * <p>Most of these checks are not possible to represent with an XSD schema.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class Ipaws1Profile extends AbstractCapProfile {

  static final String IPAWS1_PROFILE_CODE = "IPAWSv1.0";
  
  public Ipaws1Profile() {
    super();
  }

  /**
   * @param strictXsdValidation if {@code true}, perform by-the-spec XSD schema validation, which
   * does not check a number of properties specified elsewhere in the spec. If {@code false}
   * (the default), attempt to do extra validation to conform to the text of the spec.
   */
  public Ipaws1Profile(boolean strictXsdValidation) {
    super(strictXsdValidation);
  }

  @Override
  public String getName() {
    return "US IPAWS Profile v1.0";
  }

  @Override
  public String getCode() {
    return IPAWS1_PROFILE_CODE;
  }

  @Override
  public String getDocumentationUrl() {
    return "http://docs.oasis-open.org/emergency/cap/v1.2/ipaws-profile/v1.0/cap-v1.2-ipaws-profile-v1.0.pdf";
  }

  @Override
  public String toString() {
    return getCode();
  }
  
  @Override
  public Reasons validate(AlertOrBuilder alert) {
    Reasons.Builder reasons = Reasons.newBuilder();
    
    checkForErrors(alert, reasons);
    checkForRecommendations(alert, reasons);
    
    return reasons.build();
  }
  
  /**
   * Checks the Alert for errors and populates the collection provided as input.
   */
  private void checkForErrors(AlertOrBuilder alert, Reasons.Builder reasons) {
    // sent SHALL include the timezone offset
    checkZeroTimezone(reasons, alert.getSent(), "/alert[1]/sent[1]",
        ReasonType.SENT_INCLUDE_TIMEZONE_OFFSET);

    // code SHALL include the string "IPAWSv1.0" to indicate the profile version in use.
    boolean hasVersionCode = false;
    for (String code : alert.getCodeList()) {
      if (getCode().equals(code)) {
        hasVersionCode = true;
      }
    }
    if (!hasVersionCode) {
      reasons.add("/alert[1]", ReasonType.VERSION_CODE_REQUIRED, getCode());
    }

    // if Update or Cancel, references should contain all non-expired messages
    if ((alert.getMsgType() == Alert.MsgType.UPDATE || alert.getMsgType() == Alert.MsgType.CANCEL)
        && alert.getReferences().getValueCount() == 0) {
      reasons.add("/alert[1]/msgType[1]", ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE);
    }

    Set<ValuePair> eventCodes = null;
    Set<Info.Category> categories = null;
    for (int i = 0; i < alert.getInfoCount(); i++) {
      String xpath = "/alert[1]/info[" + (i + 1) + "]";
      // All info blocks in a single alert MUST have the same category and eventCode values.
      Info info = alert.getInfo(i);
      Set<ValuePair> ecs = Sets.newHashSet();
      ecs.addAll(info.getEventCodeList());
      Set<Info.Category> cats = Sets.newHashSet();
      cats.addAll(info.getCategoryList());
      if (eventCodes == null) {
        eventCodes = ecs;
        categories = cats;
      } else {
        if (!eventCodes.equals(ecs)) {
          reasons.add(xpath, ReasonType.EVENT_CODES_MUST_MATCH);
        }
        if (!categories.equals(cats)) {
          reasons.add(xpath, ReasonType.CATEGORIES_MUST_MATCH);
        }
      }

      // expires is required.  The value MUST include the timezone offset.
      if (CapUtil.isEmptyOrWhitespace(info.getExpires())) {
        reasons.add(xpath, ReasonType.EXPIRES_IS_REQUIRED);
      } else {
        checkZeroTimezone(reasons, info.getExpires(), xpath + "/expires[1]",
            ReasonType.EXPIRES_INCLUDE_TIMEZONE_OFFSET);
      }

      // MUST have one and only one eventCode with valueName of "SAME" and a SAME-standard
      // three-letter value.
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
        reasons.add(xpath, ReasonType.SAME_EVENT_CODE_REQUIRED);
      }

      // At least one <area> block MUST be present.
      if (info.getAreaCount() == 0) {
        reasons.add(xpath, ReasonType.AREA_IS_REQUIRED);
      }

      // TODO(shakusa) Do we want to check for this? Does that mean separate profiles for
      // EAS / HazCollect and CMAS?
      // An alert intended for EAS and/or HazCollect dissemination MUST include a parameter with a
      // valueName of "EAS-ORG" with a value of SAME ORG code.
    }
  }

  /**
   * Checks the Alert for recommendations and populates the collection provided
   * as input.
   */
  private void checkForRecommendations(
      AlertOrBuilder alert, Reasons.Builder reasons) {

    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert[1]/info[" + (i + 1) + "]";

      // effective, onset are ignored if present.
      if (!CapUtil.isEmptyOrWhitespace(info.getEffective())) {
        reasons.add(xpath + "/effective[1]", ReasonType.INFO_EFFECTIVE_IS_IGNORED);
      }
      if (!CapUtil.isEmptyOrWhitespace(info.getOnset())) {
        reasons.add(xpath + "/onset[1]", ReasonType.INFO_ONSET_IS_IGNORED);
      }

      // description should be there
      if (CapUtil.isEmptyOrWhitespace(info.getDescription())) {
        reasons.add(xpath, ReasonType.INFO_DESCRIPTION_RECOMMENDED);
      }

      // instruction should be there
      if (CapUtil.isEmptyOrWhitespace(info.getInstruction())) {
        reasons.add(xpath, ReasonType.INFO_INSTRUCTION_RECOMMENDED);
      }

      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        // At least one <geocode> required with a valueName of "SAME" and value of a SAME 6-digit
        // location code (extended FIPS).
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
          reasons.add(xpath + "/area[" + (j + 1) + "]", ReasonType.AREA_SAME_GEOCODE_RECOMMENDED);
        }
      }
    }
  }

  // TODO(shakusa) Localize messages
  enum ReasonType implements Reason.Type {
    
    // Errors
    SENT_INCLUDE_TIMEZONE_OFFSET(
        ERROR,
        "<sent> should include the timezone offset. An offset of 0 is unlikely for US alerts."),
    EXPIRES_INCLUDE_TIMEZONE_OFFSET(
        ERROR,
        "<expires> should include the timezone offset. An offset of 0 is unlikely for US alerts."),
    VERSION_CODE_REQUIRED(
        ERROR,
        "<code>{0}</code> required."),
    UPDATE_OR_CANCEL_MUST_REFERENCE(
        ERROR,
        "All related messages that have not yet expired MUST be referenced for \"Update\" and "
            + "\"Cancel\" messages."),
    EVENT_CODES_MUST_MATCH(
        ERROR,
        "All <info> blocks must contain the same <eventCode>s."),
    CATEGORIES_MUST_MATCH(
        ERROR,
        "All <info> blocks must contain the same <category>s."),
    SAME_EVENT_CODE_REQUIRED(
        ERROR,
        "Messages intended for EAS, CMAS and HazCollect dissemination MUST include one and only "
            + "one instance of <eventCode> with a <valueName> of \"SAME\" and using a "
            + "SAME-standard three-letter value."),
    EXPIRES_IS_REQUIRED(
        ERROR,
        "<expires> is required."),
    AREA_IS_REQUIRED(
        ERROR,
        "At least one <area> must be present."),

    // Recommendations
    INFO_EFFECTIVE_IS_IGNORED(
        RECOMMENDATION,
        "<effective> is ignored if present. Alerts SHALL be effective upon issuance."),
    INFO_ONSET_IS_IGNORED(
        RECOMMENDATION,
        "<onset> is ignored if present. Alerts SHALL be effective upon issuance."),
    INFO_DESCRIPTION_RECOMMENDED(
        RECOMMENDATION,
        "Messages should have meaningful values for the <description>."),
    INFO_INSTRUCTION_RECOMMENDED(
        RECOMMENDATION,
        "Messages should have meaningful values for the <instruction>."),
    AREA_SAME_GEOCODE_RECOMMENDED(
        RECOMMENDATION,
        "At least one instance of <geocode> with a <valueName> of \"SAME\" and a value of a SAME "
            + "6-digit location (extended FIPS) SHOULD be used."),
    ;

    private final Level defaultLevel;
    private final String message;

    private ReasonType(Level defaultLevel, String message) {
      this.defaultLevel = checkNotNull(defaultLevel);
      this.message = checkNotNull(message);
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }

    @Override
    public Level getDefaultLevel() {
      return defaultLevel;
    }
    
    @Override
    public String getSource() {
      return IPAWS1_PROFILE_CODE;
    }
  }
}

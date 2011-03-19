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
 * <a href="http://capan.ca/uploads/CAP-CP/CAP-CP_Intro_Rules_Beta_0.4.pdf">
 * Canadian CAP profile, version 1</a>.
 *
 * This class does not currently validate location codes, event codes, or
 * event names.
 * <p>
 * Most of these checks are not possible to represent with an
 * xsd schema.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CanadianProfile extends AbstractCapProfile {

  static final String CAPCP_LOCATION = "layer:EC:1.0:CLC";
  static final String CAPCP_EVENT = "profile:CAP-CP:Event:";

  public CanadianProfile() {
    super();
  }

  /**
   * @param strictXsdValidation if true, perform by-the-spec xsd schema
   * validation, which does not check a number of properties specified
   * elsewhere in the spec. If false (the default), attempt to do extra
   * validation to conform to the text of the spec.
   */
  public CanadianProfile(boolean strictXsdValidation) {
    super(strictXsdValidation);
  }

  @Override
  public String getName() {
    return "CAP Canadian Profile v1.0";
  }

  @Override
  public String getCode() {
    return "profile:CAP-CP:1.0";
  }

  @Override
  public String getDocumentationUrl() {
    return "http://capan.ca/uploads/CAP-CP/CAP-CP_Intro_Rules_Beta_0.4.pdf";
  }

  @Override
  public String toString() {
    return getCode();
  }

  @Override
  public List<Reason> checkForErrors(AlertOrBuilder alert) {
    List<Reason> reasons = new ArrayList<Reason>();

    // Note: numbers in the comments refer to the corresponding numbers
    // in the profile documentation.

    // 3. The CAP-CP version for an alert message must be identified
    boolean hasVersionCode = false;
    for (String code : alert.getCodeList()) {
      if (code.startsWith("profile:CAP-CP:")) {
        hasVersionCode = true;
      }
    }
    if (!hasVersionCode) {
      reasons.add(new Reason("/alert",
          ErrorType.VERSION_CODE_REQUIRED, getCode()));
    }

    // 5. Alert messages intended for public distribution must include
    // an <info> block
    if (alert.getMsgType() != Alert.MsgType.Ack
        && alert.getMsgType() != Alert.MsgType.Error
        && alert.getInfoCount() == 0) {
      reasons.add(new Reason("/alert", ErrorType.IS_REQUIRED));
    }

    // 12. An Update or Cancel message should minimally include
    // references to all active messages
    if ((alert.getMsgType() == Alert.MsgType.Update
        || alert.getMsgType() == Alert.MsgType.Cancel)
        && alert.getReferences().getValueCount() == 0) {
      reasons.add(new Reason("/alert/msgType",
          ErrorType.UPDATE_OR_CANCEL_MUST_REFERENCE));
    }

    Set<ValuePair> eventCodes = null;
    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert/info[" + i + "]";

      // 2. Constraint of one subject event per alert message
      Set<ValuePair> ecs = new HashSet<ValuePair>();
      ecs.addAll(info.getEventCodeList());
      if (eventCodes == null) {
        eventCodes = ecs;
      } else if (!eventCodes.equals(ecs)) {
        reasons.add(new Reason(xpath, ErrorType.EVENT_CODES_MUST_MATCH));
      }

      // 8. A recognized <eventCode> must be used
      boolean hasRecognizedEventCode = false;
      for (ValuePair eventCode : info.getEventCodeList()) {
        if (eventCode.getValueName().startsWith(CAPCP_EVENT)) {
          // TODO(shakusa) Validate event code
          hasRecognizedEventCode = true;
          break;
        }
      }
      if (!hasRecognizedEventCode) {
        reasons.add(new Reason(xpath,
            ErrorType.RECOGNIZED_EVENT_CODE_REQUIRED));
      }

      // 10. <area> blocks are required
      if (info.getAreaCount() == 0) {
        reasons.add(new Reason(xpath, ErrorType.AREA_IS_REQUIRED));
      }

      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        boolean hasValidGeocode = false;
        // 9. A recognized <geocode> must be used
        for (ValuePair geocode : area.getGeocodeList()) {
          if (geocode.getValueName().startsWith(CAPCP_LOCATION)) {
            // TODO(shakusa) Validate value according to
            // http://www.statcan.gc.ca/subjects-sujets/standard-norme/sgc-cgt/geography-geographie-eng.htm
            // CAP_CP Location References document
            hasValidGeocode = true;
            break;
          }
        }
        if (!hasValidGeocode) {
          reasons.add(new Reason(xpath + "/area[" + j + "]",
              ErrorType.AREA_GEOCODE_IS_REQUIRED));
        }
      }
    }

    return reasons;
  }

  @Override
  public List<Reason> checkForRecommendations(AlertOrBuilder alert) {
    List<Reason> reasons = new ArrayList<Reason>();

    // 7. Use established <event> values
    // TODO(shakusa) Lookup established event values?

    // 4. Time zone field must be included in all time values
    checkZeroTimezone(reasons, alert.getSent(), "/alert/sent",
        RecommendationType.SENT_INCLUDE_TIMEZONE_OFFSET);

    Set<String> languages = new HashSet<String>();
    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert/info[" + i + "]";

      languages.add(info.getLanguage());

      // 13. An <expires> value is strongly recommended
      if (!info.hasExpires()
          || CapUtil.isEmptyOrWhitespace(info.getExpires())) {
        reasons.add(new Reason(xpath,
            RecommendationType.EXPIRES_STRONGLY_RECOMMENDED));
      }

      // 4. Time zone field must be included in all time values
      checkZeroTimezone(reasons, info.getEffective(), xpath + "/effective",
          RecommendationType.EFFECTIVE_INCLUDE_TIMEZONE_OFFSET);
      checkZeroTimezone(reasons, info.getOnset(), xpath + "/onset",
          RecommendationType.ONSET_INCLUDE_TIMEZONE_OFFSET);
      checkZeroTimezone(reasons, info.getExpires(), xpath + "/expires",
          RecommendationType.EXPIRES_INCLUDE_TIMEZONE_OFFSET);

      // 14. A <senderName> is strongly recommended
      if (CapUtil.isEmptyOrWhitespace(info.getSenderName())) {
        reasons.add(new Reason(xpath,
            RecommendationType.SENDER_NAME_STRONGLY_RECOMMENDED));
      }

      // 15. <responseType> is strongly recommended, when applicable,
      // along with a corresponding <instruction> value
      if (info.getResponseTypeCount() == 0) {
        reasons.add(new Reason(xpath,
            RecommendationType.RESPONSE_TYPE_STRONGLY_RECOMMENDED));
      }
      if (CapUtil.isEmptyOrWhitespace(info.getInstruction())) {
        reasons.add(new Reason(xpath,
            RecommendationType.INSTRUCTION_STRONGLY_RECOMMENDED));
      }

      // 16. Indicate when an update message contains non-substantive
      // content changes.
      // TODO(shakusa) How to recommend MinorChange ?
      // Indicate when Update message contains non-substantive content changes

      // 17. Indicate automated translation of free form text
      // TODO(shakusa) How to recommend AutoTranslated ?

      // 18. Preferential treatment of <polygon> and <circle>
      boolean hasPolygonOrCircle = false;
      int polygonCircleGeocodeAreaIndex = -1;
      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        if (area.getCircleCount() != 0 || area.getPolygonCount() != 0) {
          hasPolygonOrCircle = true;
          if (area.getGeocodeCount() != 0) {
            polygonCircleGeocodeAreaIndex = j;
          }
        }
      }
      if (!hasPolygonOrCircle && info.getAreaCount() > 0) {
        reasons.add(new Reason(xpath + "/area[0]",
            RecommendationType.CIRCLE_POLYGON_ENCOURAGED));
      }
    }

    // 6. <info> blocks must specify the content language
    if ((!languages.contains("en-US") && !languages.contains("en-CA"))
        || !languages.contains("fr-CA")) {
      reasons.add(new Reason("/alert",
          RecommendationType.ENGLISH_AND_FRENCH));
    }

    return reasons;
  }

  // TODO(shakusa) Localize messages
  public enum ErrorType implements CapException.ReasonType {
    // Errors
    VERSION_CODE_REQUIRED("<code>{0}</code> required"),
    UPDATE_OR_CANCEL_MUST_REFERENCE("All related messages that have not yet " +
    "expired MUST be referenced for \"Update\" and \"Cancel\" messages."),
    EVENT_CODES_MUST_MATCH(
        "All <info> blocks must contain the same <eventCode>s"),
    RECOGNIZED_EVENT_CODE_REQUIRED("The CAP-CP requires the use of an " +
        "<eventCode> value from the CAP-CP Event References document that " +
        "should match the corresponding <event> value"),
    IS_REQUIRED("At least one <info> must be present"),
    AREA_IS_REQUIRED("At least one <area> must be present"),
    AREA_GEOCODE_IS_REQUIRED("At least one <geocode> value from the CAP-CP" +
        "Location References document for messages that describe areas " +
        "within Canada is required."),
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
    ENGLISH_AND_FRENCH("Consider alerts with 2 <info> blocks, one each for " +
        "English and French"),
    EXPIRES_STRONGLY_RECOMMENDED("<expires> is strongly recommended."),
    SENDER_NAME_STRONGLY_RECOMMENDED(
        "<senderName> is strongly recommended."),
    RESPONSE_TYPE_STRONGLY_RECOMMENDED(
        "<responseType> is strongly recommended."),
    INSTRUCTION_STRONGLY_RECOMMENDED(
        "<instruction> is strongly recommended."),
    CIRCLE_POLYGON_ENCOURAGED("<polygon> and <circle>, while optional, are " +
        "encouraged as more accurate representations of <geocode> values"),
    CIRCLE_POLYGON_PREFERRED("When <polygon> or <circle> values are present " +
        "in an area block, the combination of <polygon> and <circle> values " +
        "is the more accurate representation of the alert area. This is " +
        "contrary to what is currently defined in CAP, which recognizes the " +
        "area as the combination of the <geocode>, <polygon> and <circle> " +
        "values"),
    SENT_INCLUDE_TIMEZONE_OFFSET("Time zone field must be included in " +
        "<sent>. An offset of 0 is unlikely for Canadian alerts."),
    EFFECTIVE_INCLUDE_TIMEZONE_OFFSET("Time zone field must be included in " +
        "<effective>. An offset of 0 is unlikely for Canadian alerts."),
    ONSET_INCLUDE_TIMEZONE_OFFSET("Time zone field must be included in " +
        "<onset>. An offset of 0 is unlikely for Canadian alerts."),
    EXPIRES_INCLUDE_TIMEZONE_OFFSET("Time zone field must be included in " +
        "<expires>. An offset of 0 is unlikely for Canadian alerts."),
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

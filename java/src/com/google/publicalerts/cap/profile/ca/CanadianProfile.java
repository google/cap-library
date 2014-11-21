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

package com.google.publicalerts.cap.profile.ca;

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
 * <a href="http://capan.ca/uploads/CAP-CP/CAP-CP_Intro_Rules_Beta_0.4.pdf"> Canadian CAP profile,
 * version 1</a>.
 *
 * This class does not currently validate location codes, event codes, or event names.
 * 
 * <p>Most of these checks are not possible to represent with an XSD schema.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CanadianProfile extends AbstractCapProfile {

  /**
   * Alerts following CAP-CP prefix geocode.valueName with this string literal for
   * officially-supported geocodes.
   */
  public static final String CAPCP_LOCATION = "profile:CAP-CP:Location";

  /**
   * Alerts following CAP-CP prefix eventCode.valueName with this string literal for
   * officially-supported event codes.
   */
  public static final String CAPCP_EVENT = "profile:CAP-CP:Event:";

  /**
   * Parameter value name for the "Parent URI" parameter.
   */
  public static final String EC_MSC_SMC_PARENT_URI = "layer:EC-MSC-SMC:1.0:Parent_URI";

  static final String CANADIAN_PROFILE_CODE = "profile:CAP-CP:1.0";
  
  public CanadianProfile() {
    super();
  }

  /**
   * @param strictXsdValidation if {@code true}, perform by-the-spec XSD schema validation, which
   * does not check a number of properties specified elsewhere in the spec. If {@code false}
   * (the default), attempt to do extra validation to conform to the text of the spec.
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
    return CANADIAN_PROFILE_CODE;
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
    // Note: numbers in the comments refer to the corresponding numbers in the profile
    // documentation.

    // 3. The CAP-CP version for an alert message must be identified
    boolean hasVersionCode = false;
    for (String code : alert.getCodeList()) {
      if (code.startsWith("profile:CAP-CP:")) {
        hasVersionCode = true;
      }
    }
    if (!hasVersionCode) {
      reasons.add("/alert[1]", ReasonType.VERSION_CODE_REQUIRED, getCode());
    }

    // 5. Alert messages intended for public distribution must include an <info> block
    if (alert.getMsgType() != Alert.MsgType.ACK && alert.getMsgType() != Alert.MsgType.ERROR
        && alert.getInfoCount() == 0) {
      reasons.add("/alert[1]", ReasonType.IS_REQUIRED);
    }

    // 12. An Update or Cancel message should minimally include references to all active messages
    if ((alert.getMsgType() == Alert.MsgType.UPDATE || alert.getMsgType() == Alert.MsgType.CANCEL)
        && alert.getReferences().getValueCount() == 0) {
      reasons.add("/alert[1]/msgType[1]", ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE);
    }

    Set<ValuePair> eventCodes = null;
    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert[1]/info[" + (i + 1) + "]";

      // 2. Constraint of one subject event per alert message
      Set<ValuePair> ecs = Sets.newHashSet();
      ecs.addAll(info.getEventCodeList());
      if (eventCodes == null) {
        eventCodes = ecs;
      } else if (!eventCodes.equals(ecs)) {
        reasons.add(xpath, ReasonType.EVENT_CODES_MUST_MATCH);
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
        reasons.add(xpath, ReasonType.RECOGNIZED_EVENT_CODE_REQUIRED);
      }

      // 10. <area> blocks are required
      if (info.getAreaCount() == 0) {
        reasons.add(xpath, ReasonType.AREA_IS_REQUIRED);
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
          reasons.add(xpath + "/area[" + (j + 1) + "]", ReasonType.AREA_GEOCODE_IS_REQUIRED);
        }
      }
    }
  }
  
  /**
   * Checks the Alert for recommendations and populates the collection provided as input.
   */
  private void checkForRecommendations(
      AlertOrBuilder alert, Reasons.Builder reasons) {
    
    // 7. Use established <event> values
    // TODO(shakusa) Lookup established event values?

    Set<String> languages = Sets.newHashSet();
    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert[1]/info[" + (i + 1) + "]";

      languages.add(info.getLanguage());

      // 13. An <expires> value is strongly recommended
      if (!info.hasExpires() || CapUtil.isEmptyOrWhitespace(info.getExpires())) {
        reasons.add(xpath, ReasonType.EXPIRES_STRONGLY_RECOMMENDED);
      }

      // 14. A <senderName> is strongly recommended
      if (CapUtil.isEmptyOrWhitespace(info.getSenderName())) {
        reasons.add(xpath, ReasonType.SENDER_NAME_STRONGLY_RECOMMENDED);
      }

      // 15. <responseType> is strongly recommended, when applicable, along with a corresponding 
      // <instruction> value
      if (info.getResponseTypeCount() == 0) {
        reasons.add(xpath, ReasonType.RESPONSE_TYPE_STRONGLY_RECOMMENDED);
      }
      if (CapUtil.isEmptyOrWhitespace(info.getInstruction())) {
        reasons.add(xpath, ReasonType.INSTRUCTION_STRONGLY_RECOMMENDED);
      }

      // 16. Indicate when an update message contains non-substantive content changes.
      // TODO(shakusa) How to recommend MinorChange ?
      // Indicate when Update message contains non-substantive content changes

      // 17. Indicate automated translation of free form text
      // TODO(shakusa) How to recommend AutoTranslated ?

      // 18. Preferential treatment of <polygon> and <circle>
      boolean hasPolygonOrCircle = false;
      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        if (area.getCircleCount() != 0 || area.getPolygonCount() != 0) {
          hasPolygonOrCircle = true;
        }
      }
      if (!hasPolygonOrCircle && info.getAreaCount() > 0) {
        reasons.add(xpath + "/area[1]", ReasonType.CIRCLE_POLYGON_ENCOURAGED);
      }
    }

    // 6. <info> blocks must specify the content language
    if ((!languages.contains("en-US") && !languages.contains("en-CA"))
        || !languages.contains("fr-CA")) {
      reasons.add("/alert[1]", ReasonType.ENGLISH_AND_FRENCH);
    }
  }

  // TODO(shakusa) Localize messages
  enum ReasonType implements Reason.Type {
    // Errors
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
    RECOGNIZED_EVENT_CODE_REQUIRED(
        ERROR,
        "The CAP-CP requires the use of an <eventCode> value from the CAP-CP Event References "
            + "document that should match the corresponding <event> value."),
    IS_REQUIRED(
        ERROR,
        "At least one <info> must be present."),
    AREA_IS_REQUIRED(
        ERROR,
        "At least one <area> must be present."),
    AREA_GEOCODE_IS_REQUIRED(
        ERROR,
        "At least one <geocode> value from the CAP-CP Location References document for messages "
            + "that describe areas within Canada is required."),

    // Recommendations
    ENGLISH_AND_FRENCH(
        RECOMMENDATION,
        "Consider alerts with 2 <info> blocks, one each for English and French."),
    EXPIRES_STRONGLY_RECOMMENDED(
        RECOMMENDATION,
        "<expires> is strongly recommended."),
    SENDER_NAME_STRONGLY_RECOMMENDED(
        RECOMMENDATION,
        "<senderName> is strongly recommended."),
    RESPONSE_TYPE_STRONGLY_RECOMMENDED(
        RECOMMENDATION,
        "<responseType> is strongly recommended."),
    INSTRUCTION_STRONGLY_RECOMMENDED(
        RECOMMENDATION,
        "<instruction> is strongly recommended."),
    CIRCLE_POLYGON_ENCOURAGED(
        RECOMMENDATION,
        "<polygon> and <circle>, while optional, are encouraged as more accurate representations "
            + "of <geocode> values."),
    CIRCLE_POLYGON_PREFERRED(
        RECOMMENDATION,
        "When <polygon> or <circle> values are present in an area block, the combination of "
            + "<polygon> and <circle> values is the more accurate representation of the alert "
            + "area. This is contrary to what is currently defined in CAP, which recognizes the "
            + "area as the combination of the <geocode>, <polygon> and <circle> values"),
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
      return CANADIAN_PROFILE_CODE;
    }
  }
}

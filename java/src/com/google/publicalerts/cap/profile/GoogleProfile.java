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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.publicalerts.cap.Reason.Level.ERROR;
import static com.google.publicalerts.cap.Reason.Level.INFO;
import static com.google.publicalerts.cap.Reason.Level.RECOMMENDATION;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.AlertOrBuilder;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.CapDateUtil;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.Circle;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.Reason.Level;
import com.google.publicalerts.cap.Reasons;
import com.google.publicalerts.cap.ValuePair;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A CAP profile for alerts intended for the Google Public Alerts platform.
 *
 * <p>Based on http://goo.gl/yb0tC
 * 
 * <p>Most of these checks are not possible to represent with an XSD schema.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class GoogleProfile extends AbstractCapProfile {
  static final String GOOGLE_PROFILE_CODE = "google";
  
  public GoogleProfile() {
    super();
  }

  /**
   * @param strictXsdValidation if {@code true}, perform by-the-spec XSD schema validation, which
   * does not check a number of properties specified elsewhere in the spec. If {@code false}
   * (the default), attempt to do extra validation to conform to the text of the spec.
   */
  public GoogleProfile(boolean strictXsdValidation) {
    super(strictXsdValidation);
  }

  @Override
  public String getName() {
    return "Google Public Alerts CAP v1.0";
  }

  @Override
  public String getCode() {
    return GOOGLE_PROFILE_CODE;
  }

  @Override
  public String getDocumentationUrl() {
    return "http://goo.gl/yb0tC";
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
    // An Update or Cancel message should minimally include references to all active messages
    if ((alert.getMsgType() == Alert.MsgType.UPDATE || alert.getMsgType() == Alert.MsgType.CANCEL)
        && alert.getReferences().getValueCount() == 0) {
      reasons.add("/alert[1]/msgType[1]", ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE);
    }

    // Alert messages intended for public distribution must include an <info> block
    if (alert.getInfoCount() == 0) {
      reasons.add("/alert[1]", ReasonType.INFO_IS_REQUIRED);
    }

    Set<Info.Category> categories = null;
    Set<ValuePair> eventCodes = null;
    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert[1]/info[" + (i + 1) + "]";

      // All infos must have same <category> and <eventCode> values
      Set<Info.Category> cats = Sets.newHashSet();
      cats.addAll(info.getCategoryList());
      if (categories == null) {
        categories = cats;
      } else if (!categories.equals(cats)) {
        reasons.add(xpath + "/category[1]", ReasonType.CATEGORIES_MUST_MATCH);
      }

      Set<ValuePair> ecs = Sets.newHashSet();
      ecs.addAll(info.getEventCodeList());
      if (eventCodes == null) {
        eventCodes = ecs;
      } else if (!eventCodes.equals(ecs)) {
        reasons.add(xpath + "/eventCode[1]", ReasonType.EVENT_CODES_MUST_MATCH);
      }

      // <description> is required
      if (!info.hasDescription() || CapUtil.isEmptyOrWhitespace(info.getDescription())) {
        reasons.add(xpath, ReasonType.DESCRIPTION_IS_REQUIRED);
      }

      // <effective> should be before <expires>
      if (info.hasExpires()) {
        String effective = info.hasEffective() ? info.getEffective() : alert.getSent();
        
        // The following variables are nullable, as their corresponding dates could be not parsable.
        // If this happens, an error has already been thrown.
        Date effectiveDate = CapDateUtil.toJavaDate(effective);
        Date expiresDate = CapDateUtil.toJavaDate(info.getExpires());

        if (effectiveDate != null && expiresDate != null && effectiveDate.after(expiresDate)) {
          reasons.add(xpath + "/effective[1]", ReasonType.EFFECTIVE_NOT_AFTER_EXPIRES);
        }
      }

      // <web> is required
      if (!info.hasWeb() || CapUtil.isEmptyOrWhitespace(info.getWeb())) {
        reasons.add(xpath, ReasonType.WEB_IS_REQUIRED);
      }

      // An <expires> value is required
      if (!info.hasExpires() || CapUtil.isEmptyOrWhitespace(info.getExpires())) {
        reasons.add(xpath, ReasonType.EXPIRES_IS_REQUIRED);
      }

      // <area> blocks are required
      if (info.getAreaCount() == 0) {
        reasons.add(xpath, ReasonType.AREA_IS_REQUIRED);
      }

      if (!info.hasUrgency()) {
        reasons.add(xpath, ReasonType.URGENCY_IS_REQUIRED);
      }
      if (!info.hasSeverity()) {
        reasons.add(xpath, ReasonType.SEVERITY_IS_REQUIRED);
      }
      if (!info.hasCertainty()) {
        reasons.add(xpath, ReasonType.CERTAINTY_IS_REQUIRED);
      }

      // <area> blocks must have at least one <circle> <polygon> or <geocode>
      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        if (area.getGeocodeCount() == 0 && area.getCircleCount() == 0
            && area.getPolygonCount() == 0) {
          reasons.add(xpath + "/area[" + (j + 1) + "]",
              ReasonType.CIRCLE_POLYGON_OR_GEOCODE_IS_REQUIRED);
        }
      }
    }
  }

  /**
   * Checks the Alert for recommendations and populates the collection provided as input.
   */
  private void checkForRecommendations(
      AlertOrBuilder alert, Reasons.Builder reasons) {
    // Annotate all time values if they refer to the UTC time zone, in case this is an oversight
    checkZeroTimezone(reasons, alert.getSent(), "/alert[1]/sent[1]",
        ReasonType.SENT_INCLUDES_UTC_TIMEZONE);

    // Alerts with status Test are discouraged 
    if (alert.hasStatus() && alert.getStatus() == Alert.Status.TEST) {
      reasons.add("/alert[1]/status[1]", ReasonType.TEST_ALERT_DISCOURAGED);
    }

    Map<String, String> eventByLanguage = Maps.newHashMap();
    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert[1]/info[" + (i + 1) + "]";

      // Annotate all time values if they refer to the UTC time zone, in case this is an oversight
      checkZeroTimezone(reasons, info.getEffective(), xpath + "/effective[1]",
          ReasonType.EFFECTIVE_INCLUDES_UTC_TIMEZONE);
      checkZeroTimezone(reasons, info.getOnset(), xpath + "/onset[1]",
          ReasonType.ONSET_INCLUDES_UTC_TIMEZONE);
      checkZeroTimezone(reasons, info.getExpires(), xpath + "/expires[1]",
          ReasonType.EXPIRES_INCLUDES_UTC_TIMEZONE);

      // A <senderName> is strongly recommended
      if (CapUtil.isEmptyOrWhitespace(info.getSenderName())) {
        reasons.add(xpath, ReasonType.SENDER_NAME_STRONGLY_RECOMMENDED);
      }

      // <responseType> is strongly recommended, when applicable,
      // along with a corresponding <instruction> value
      if (info.getResponseTypeCount() == 0) {
        reasons.add(xpath, ReasonType.RESPONSE_TYPE_STRONGLY_RECOMMENDED);
      }
      if (CapUtil.isEmptyOrWhitespace(info.getInstruction())) {
        reasons.add(xpath, ReasonType.INSTRUCTION_STRONGLY_RECOMMENDED);
      }

      // Headline should be < 140 chars
      if (info.hasHeadline() && info.getHeadline().length() > 140) {
        reasons.add(xpath + "/headline[1]", ReasonType.HEADLINE_TOO_LONG);
      }

      if (info.getDescription().equals(info.getHeadline())) {
        reasons.add(xpath + "/headline[1]", ReasonType.HEADLINE_AND_DESCRIPTION_SHOULD_DIFFER);
      }

      if (info.hasInstruction()
          && !CapUtil.isEmptyOrWhitespace(info.getInstruction())
          && info.getDescription().equals(info.getInstruction())) {
        reasons.add(xpath + "/description[1]",
            ReasonType.DESCRIPTION_AND_INSTRUCTION_SHOULD_DIFFER);
      }

      if (info.getUrgency() == Info.Urgency.UNKNOWN_URGENCY) {
        reasons.add(xpath + "/urgency[1]", ReasonType.UNKNOWN_URGENCY_DISCOURAGED);
      }
      if (info.getSeverity() == Info.Severity.UNKNOWN_SEVERITY) {
        reasons.add(xpath + "/severity[1]", ReasonType.UNKNOWN_SEVERITY_DISCOURAGED);
      }
      if (info.getCertainty() == Info.Certainty.UNKNOWN_CERTAINTY) {
        reasons.add(xpath + "/certainty[1]", ReasonType.UNKNOWN_CERTAINTY_DISCOURAGED);
      }

      if (!info.hasContact()
          || CapUtil.isEmptyOrWhitespace(info.getContact())) {
        reasons.add(xpath, ReasonType.CONTACT_IS_RECOMMENDED);
      }

      // 18. Preferential treatment of <polygon> and <circle>
      boolean hasPolygonOrCircle = false;
      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        if (area.getCircleCount() != 0 || area.getPolygonCount() != 0) {
          hasPolygonOrCircle = true;
        }
        for (int k = 0; k < area.getCircleCount(); k++) {
          Circle circle = area.getCircle(k);
          if (circle.getRadius() == 0) {
            reasons.add(xpath + "/area[" + (j + 1) + "]/circle[" + (k + 1) + "]",
                ReasonType.NONZERO_CIRCLE_RADIUS_RECOMMENDED);
          }
        }
      }
      if (!hasPolygonOrCircle && info.getAreaCount() > 0) {
        reasons.add(xpath + "/area[1]", ReasonType.CIRCLE_POLYGON_ENCOURAGED);
      }

      if (eventByLanguage.containsKey(info.getLanguage())) {
        if (!info.getEvent().equals(eventByLanguage.get(info.getLanguage()))) {
          reasons.add(xpath + "/event[1]", ReasonType.EVENTS_IN_SAME_LANGUAGE_SHOULD_MATCH);
        }
      } else {
        eventByLanguage.put(info.getLanguage(), info.getEvent());
      }

      // Check for empty values in geocode <valueName> or <value> field
      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        for (int k = 0; k < area.getGeocodeCount(); k++) {
          ValuePair geocode = area.getGeocode(k);
          if ("".equals(geocode.getValueName()) || "".equals(geocode.getValue())) {
            reasons.add(xpath + "/area[" + (j + 1) + "]/geocode[" + (k + 1) + "]",
                ReasonType.EMPTY_GEOCODE_FIELD);
          }
        }
      }
    }
  }
  
  // TODO(shakusa) Localize messages
  enum ReasonType implements Reason.Type {
    
    // Errors
    UPDATE_OR_CANCEL_MUST_REFERENCE(
        ERROR,
        "All related messages that have not yet expired must be referenced when an \"Update\" or "
            + "\"Cancel\" is issued. This ensures that an \"Update\" or \"Cancel\" applies to at "
            + "least one non-expired alert."),
    CATEGORIES_MUST_MATCH(
        ERROR,
        "All <info> blocks must contain the same <category>s."),
    EVENT_CODES_MUST_MATCH(
        ERROR,
        "All <info> blocks must contain the same <eventCode>s."),
    INFO_IS_REQUIRED(
        ERROR,
        "At least one <info> must be present."),
    DESCRIPTION_IS_REQUIRED(
        ERROR,
        "<description> must be present."),
    WEB_IS_REQUIRED(
        ERROR,
        "<web> must be present."),
    EXPIRES_IS_REQUIRED(
       ERROR,
       "<expires> must be present."),
    EFFECTIVE_NOT_AFTER_EXPIRES(
       ERROR,
       "<effective> should not come after <expires>."),
    URGENCY_IS_REQUIRED(
       ERROR,
       "<urgency> must be present."),
    SEVERITY_IS_REQUIRED(
       ERROR,
       "<severity> must be present."),
    CERTAINTY_IS_REQUIRED(
       ERROR,
       "<certainty> must be present."),
    AREA_IS_REQUIRED(ERROR,
       "At least one <area> must be present."),
    CIRCLE_POLYGON_OR_GEOCODE_IS_REQUIRED(
       ERROR,
       "Each <area> must have at least one <circle>, <polygon> or <geocode>."),

   // Recommendations
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
       "<polygon> and <circle>, while optional, are encouraged as more accurate representations of "
           + "<geocode> values."),
    TEST_ALERT_DISCOURAGED(
       RECOMMENDATION,
       "<status>Test</status> alerts are excluded from appearing in google public alerts."),
    HEADLINE_TOO_LONG(
       RECOMMENDATION,
       "Headline should be less than 140 characters."),
    HEADLINE_AND_DESCRIPTION_SHOULD_DIFFER(
       RECOMMENDATION,
       "Description should provide more detail than the headline and should not be identical."),
    DESCRIPTION_AND_INSTRUCTION_SHOULD_DIFFER(
       RECOMMENDATION,
       "Description should describe the hazard while instruction should provide human-readable "
           + "instructions. They should not be identical."),
    UNKNOWN_URGENCY_DISCOURAGED(
       RECOMMENDATION,
       "Unknown <urgency> is discouraged."),
    UNKNOWN_SEVERITY_DISCOURAGED(
       RECOMMENDATION,
       "Unknown <severity> is discouraged."),
    UNKNOWN_CERTAINTY_DISCOURAGED(
       RECOMMENDATION,
       "Unknown <certainty> is discouraged."),
    CONTACT_IS_RECOMMENDED(
       RECOMMENDATION,
       "<contact> is recommended to give users a way to provide feedback and respond to the"
           + "alert."),
    NONZERO_CIRCLE_RADIUS_RECOMMENDED(
       RECOMMENDATION,
       "A CAP <area> defines the area inside which people should be alerted, not the area of the "
           + "event causing the alert. This area should normally have nonzero radius."),
    EVENTS_IN_SAME_LANGUAGE_SHOULD_MATCH(
       RECOMMENDATION,
       "All <info> blocks with the same <language> should contain the same <event>. An exception "
           + "is made for variations in severity on the same event, such as tsunami warning and "
           + "tsunami advisory."),
    EMPTY_GEOCODE_FIELD(
       RECOMMENDATION,
       "A <geocode> should contain non-empty <valueName> <value> fields."),
    
    // Infos
    SENT_INCLUDES_UTC_TIMEZONE(
        INFO,
        "<sent> refers to the UTC time zone. If this is not your local time zone, consider using "
            + "a local time zone instead."),
    EFFECTIVE_INCLUDES_UTC_TIMEZONE(
        INFO,
        "<effective> refers to the UTC time zone. If this is not your local time zone, consider "
            + "using a local time zone instead."),
    ONSET_INCLUDES_UTC_TIMEZONE(
        INFO,
        "<onset> refers to the UTC time zone. If this is not your local time zone, consider using "
            + "a local time zone instead."),
    EXPIRES_INCLUDES_UTC_TIMEZONE(
        INFO,
        "<expires> refers to the UTC time zone. If this is not your local time zone, consider "
            + "using a local time zone instead."),
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
      return GOOGLE_PROFILE_CODE;
    }
  }
}

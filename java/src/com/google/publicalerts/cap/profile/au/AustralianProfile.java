/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.publicalerts.cap.profile.au;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.AlertOrBuilder;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.ValuePair;
import com.google.publicalerts.cap.profile.AbstractCapProfile;
import com.google.publicalerts.cap.profile.CsvFileIterator;
import com.google.publicalerts.cap.profile.CsvFileIterator.CsvRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <a href="https://govshare.gov.au/xmlui/handle/10772/6494">CAP
 * v1.2 Australian profile, CAP-AU-STD, version 1.0, Committee
 * Specification 02</a>.
 *
 * This class does not currently validate location codes, event codes, or
 * event names.
 * <p>
 * Most of these checks are not possible to represent with an
 * xsd schema.
 *
 * TODO(shakusa) CAP-AU-STD has an XSD (
 * https://govshare.gov.au/xmlui/handle/10772/6498), validate against that
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class AustralianProfile extends AbstractCapProfile {

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "[\\w\\.\\-\\+_]+\\@[\\w\\.\\-]+", Pattern.CASE_INSENSITIVE);

  static final String CAP_AU_CODE =
      "urn:oasis:names:tc:emergency:cap:1.2:profile:CAP-AU:1.0";

  static final String CAP_AU_EVENT_CODE_VALUE_NAME =
      "https://govshare.gov.au/xmlui/handle/10772/6495";

  private static final Map<String, EventListEntry> EVENT_BY_EVENT_CODE_MAP =
      loadEventList("AUeventLIST1.0.csv");

  private static final Set<String> RECOGNIZED_GEOCODE_VALUES = Collections.unmodifiableSet(
      new HashSet<String>(Arrays.asList(new String[] {
          "http://www.psma.com.au/?product=g-naf",
          "http://www.iso.org/iso/country_codes.html",
          "http://www.ga.gov.au/place-name/",
          "http://www.psma.com.au/?product=postcode-boundaries"})));

  public AustralianProfile() {
    super();
  }

  /**
   * @param strictXsdValidation if true, perform by-the-spec xsd schema
   * validation, which does not check a number of properties specified
   * elsewhere in the spec. If false (the default), attempt to do extra
   * validation to conform to the text of the spec.
   */
  public AustralianProfile(boolean strictXsdValidation) {
    super(strictXsdValidation);
  }

  @Override
  public String getName() {
    return "CAP Australian Profile v3.0, Committee Specification 02";
  }

  @Override
  public String getCode() {
    return CAP_AU_CODE;
  }

  @Override
  public String getDocumentationUrl() {
    return "https://govshare.gov.au/xmlui/handle/10772/6494";
  }

  @Override
  public String toString() {
    return getCode();
  }

  @Override
  public List<Reason> checkForErrors(AlertOrBuilder alert) {
    List<Reason> reasons = new ArrayList<Reason>();

    // The CAP-AU version code is required
    boolean hasVersionCode = false;
    for (String code : alert.getCodeList()) {
      if (getCode().equals(code)) {
        hasVersionCode = true;
      }
    }
    if (!hasVersionCode) {
      reasons.add(new Reason("/alert",
          ErrorType.VERSION_CODE_REQUIRED, getCode()));
    }

    // For alert messages intended for public distribution, a <msgType> of
    // "Alert", "Update" or "Cancel" affects the message state, and an
    // <info> element is REQUIRED.
    if (alert.getMsgType() != Alert.MsgType.ACK
        && alert.getMsgType() != Alert.MsgType.ERROR
        && alert.getInfoCount() == 0) {
      reasons.add(new Reason("/alert", ErrorType.INFO_IS_REQUIRED));
    }

    // An Update or Cancel message should minimally include
    // references to all active messages
    if ((alert.getMsgType() == Alert.MsgType.UPDATE
        || alert.getMsgType() == Alert.MsgType.CANCEL)
        && alert.getReferences().getValueCount() == 0) {
      reasons.add(new Reason("/alert/msgType",
          ErrorType.UPDATE_OR_CANCEL_MUST_REFERENCE));
    }

    Set<Info.Category> categories = null;
    Set<ValuePair> eventCodes = null;
    ValuePair authorizedEventCode = null;
    Map<String, String> eventByLanguage = new HashMap<String, String>();
    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert/info[" + i + "]";

      // All infos must have same <category> and <eventCode> values
      Set<Info.Category> cats = new HashSet<Info.Category>();
      cats.addAll(info.getCategoryList());
      if (categories == null) {
        categories = cats;
      } else if (!categories.equals(cats)) {
        reasons.add(new Reason(xpath, ErrorType.CATEGORIES_MUST_MATCH));
      }

      Set<ValuePair> ecs = new HashSet<ValuePair>();
      ecs.addAll(info.getEventCodeList());
      if (eventCodes == null) {
        eventCodes = ecs;
      } else if (!eventCodes.equals(ecs)) {
        reasons.add(new Reason(xpath, ErrorType.EVENT_CODES_MUST_MATCH));
      }

      for (ValuePair eventCode : info.getEventCodeList()) {
        if (CAP_AU_EVENT_CODE_VALUE_NAME.equals(eventCode.getValueName())) {
          if (authorizedEventCode == null) {
            authorizedEventCode = eventCode;
          } else if (!authorizedEventCode.equals(eventCode)) {
            reasons.add(new Reason(xpath,
                ErrorType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT));
          }
        }
      }

      if (authorizedEventCode == null) {
        reasons.add(new Reason(xpath,
            ErrorType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT));
      } else {
        EventListEntry eventListEntry =
            EVENT_BY_EVENT_CODE_MAP.get(authorizedEventCode.getValue());
        if (eventListEntry == null) {
          reasons.add(new Reason(xpath,
              ErrorType.UNRECOGNIZED_EVENT_CODE,
              authorizedEventCode.getValue()));
        } else if (!info.getEvent().equals(eventListEntry.tierIEvent)
            && !info.getEvent().equals(eventListEntry.tierIIEvent)) {
          reasons.add(new Reason(xpath,
              ErrorType.EVENT_AND_EVENT_CODE_MUST_MATCH,
              authorizedEventCode.getValue(), info.getEvent(),
              eventListEntry.tierIEvent + ("".equals(eventListEntry.tierIIEvent)
                  ? "" : " or " + eventListEntry.tierIIEvent)));
        }
      }

      if (eventByLanguage.containsKey(info.getLanguage())) {
        if (!info.getEvent().equals(eventByLanguage.get(info.getLanguage()))) {
          reasons.add(new Reason(xpath,
              ErrorType.EVENTS_IN_SAME_LANGUAGE_MUST_MATCH));
        }
      } else {
        eventByLanguage.put(info.getLanguage(), info.getEvent());
      }

      // Do not use <effective> when <msgType> is Cancel
      if (alert.getMsgType() == Alert.MsgType.CANCEL && info.hasEffective()) {
        reasons.add(new Reason(xpath,
            ErrorType.DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL));
      }

      // <area> blocks are required
      if (info.getAreaCount() == 0) {
        reasons.add(new Reason(xpath, ErrorType.AREA_IS_REQUIRED));
      }
    }

    return reasons;
  }

  @Override
  public List<Reason> checkForRecommendations(AlertOrBuilder alert) {
    List<Reason> reasons = new ArrayList<Reason>();

    // <sender> RECOMMENDED that a valid address in the format
    // example@domain that identifies the agency that assembled the message,
    // or another agency that originated the message be used.
    // Use of Third Level Domain (example@bom.gov.au) or Fourth Level Domain
    // (example.ses.sa.gov.au) acceptable.
    if (!EMAIL_PATTERN.matcher(alert.getSender()).matches()) {
      reasons.add(new Reason("/alert/sender",
          RecommendationType.SENDER_SHOULD_BE_EMAIL));
    }

    // <status> "Test" treated as log-only event and not be broadcast as a
    // valid alert
    if (alert.getStatus() == Alert.Status.TEST) {
      reasons.add(new Reason("/alert/status",
          RecommendationType.TEST_ALERT_WILL_NOT_BE_BROADCAST));
    }

    // TODO(shakusa) Timezones should be local to the area of the alert
    // unless the area crosses multiple timezones (then use UTC).
    // Is there a way we can recommend based on this?

    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert/info[" + i + "]";

      // Note when a recognized <eventCode> is not used
      boolean hasRecognizedEventCode = false;
      for (ValuePair eventCode : info.getEventCodeList()) {
        if (CAP_AU_EVENT_CODE_VALUE_NAME.equals(eventCode.getValueName())) {
          hasRecognizedEventCode = true;
          break;
        }
      }
      if (!hasRecognizedEventCode) {
        reasons.add(new Reason(xpath,
            RecommendationType.RECOGNIZED_EVENT_CODE_NOT_USED));
      }

      // An <expires> value is strongly recommended
      if (!info.hasExpires()
          || CapUtil.isEmptyOrWhitespace(info.getExpires())) {
        reasons.add(new Reason(xpath,
            RecommendationType.EXPIRES_STRONGLY_RECOMMENDED));
      }

      // <senderName> is strongly recommended to be populated as
      // publicly-recognisable name of the agency issuing the alert. It is
      // expected to be used for presentation purposes.
      if (CapUtil.isEmptyOrWhitespace(info.getSenderName())) {
        reasons.add(new Reason(xpath,
            RecommendationType.SENDER_NAME_STRONGLY_RECOMMENDED));
      }

      // <responseType> is recommended along with corresponding <instruction>
      // Allows actions to be available when instructions are not available or
      // not available in all languages.
      if (info.getResponseTypeCount() == 0) {
        reasons.add(new Reason(xpath,
            RecommendationType.RESPONSE_TYPE_STRONGLY_RECOMMENDED));
      }

      if (CapUtil.isEmptyOrWhitespace(info.getInstruction())) {
        reasons.add(new Reason(xpath,
            RecommendationType.INSTRUCTION_STRONGLY_RECOMMENDED));
      }

      // Indicate when an update message contains non-substantive
      // content changes.
      // TODO(shakusa) How to recommend MinorChange ?

      // Preferential treatment of <polygon> and <circle>
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
        boolean hasValidGeocode = false;
        // 2.4. area should include a recognised <geocode> value.
        for (ValuePair geocode : area.getGeocodeList()) {
          if (RECOGNIZED_GEOCODE_VALUES.contains(geocode.getValueName())) {
            hasValidGeocode = true;
            break;
          }
        }
        if (!hasValidGeocode) {
          reasons.add(new Reason(xpath + "/area[" + j + "]",
              RecommendationType.AREA_GEOCODE_IS_RECOMMENDED));
        }
      }

      if (!hasPolygonOrCircle && info.getAreaCount() > 0) {
        reasons.add(new Reason(xpath + "/area[0]",
            RecommendationType.CIRCLE_POLYGON_ENCOURAGED));
      }
    }

    return reasons;
  }

  private static Map<String, EventListEntry> loadEventList(String fileName) {
    Iterator<CsvRow> itr = new CsvFileIterator(
        AustralianProfile.class.getResourceAsStream(fileName));

    Map<String, EventListEntry> map = new HashMap<String, EventListEntry>();
    while (itr.hasNext()) {
      CsvRow row = itr.next();

      String serialNumberStr = row.getAt(0, "-1");
      if ("CAP-AU #".equals(serialNumberStr)) {
        // Header
        continue;
      }
      int serialNumber = Integer.parseInt(serialNumberStr);

      String tierIEvent = row.getAt(1, "");
      String tierIIEvent = row.getAt(2, "");
      String eventCode = row.getAt(3, "");
      String categoriesStr = row.getAt(4, "");
      String authoritiesToModifyStr = row.getAt(5, "");

      Set<Info.Category> categories = new HashSet<Info.Category>();
      for (String cat : categoriesStr.split(",")) {
        categories.add(Info.Category.valueOf(cat.trim().toUpperCase()));
      }

      Set<Integer> authoritiesToModify = new HashSet<Integer>();
      for (String auth : authoritiesToModifyStr.split(",")) {
        if (!"".equals(auth)) {
          authoritiesToModify.add(Integer.parseInt(auth));
        }
      }

      if (!"".equals(eventCode.trim())) {
        map.put(eventCode, new EventListEntry(serialNumber, tierIEvent,
            tierIIEvent, eventCode, categories, authoritiesToModify));
      }
    }
    return Collections.unmodifiableMap(map);
  }

  static class EventListEntry {
    final int serialNumber;
    final String tierIEvent;
    final String tierIIEvent;
    final String eventCode;
    final Set<Info.Category> categories;
    final Set<Integer> authoritiesToModify;

    EventListEntry(int serialNumber, String tierIEvent, String tierIIEvent,
        String eventCode, Set<Info.Category> categories,
        Set<Integer> authoritiesToModify) {
      this.serialNumber = serialNumber;
      this.tierIEvent = tierIEvent;
      this.tierIIEvent = tierIIEvent;
      this.eventCode = eventCode;
      this.categories = categories;
      this.authoritiesToModify = authoritiesToModify;
    }
  }

  // TODO(shakusa) Localize messages
  public enum ErrorType implements CapException.ReasonType {
    VERSION_CODE_REQUIRED("<code>{0}</code> required"),
    UPDATE_OR_CANCEL_MUST_REFERENCE("All related messages that have not yet " +
    "expired MUST be referenced for \"Update\" and \"Cancel\" messages."),
    CATEGORIES_MUST_MATCH(
        "All <info> blocks must contain the same <category>s"),
    EVENT_CODES_MUST_MATCH(
        "All <info> blocks must contain the same <eventCode>s"),
    ONE_AUTHORIZED_EVENT_CODE_PER_ALERT(
        "Each alert message shall contain one single value from an "
            + "authorised <eventCode> list in order to avoid any potential "
            + "confusion or difficulty having a single alert message "
            + "containing multiple events. Each such <eventCode> should have "
            + "<valueName> " + CAP_AU_EVENT_CODE_VALUE_NAME),
    EVENTS_IN_SAME_LANGUAGE_MUST_MATCH("All <info> blocks with the same " +
        "<langauge> must contain the same <event>"),
    UNRECOGNIZED_EVENT_CODE("<eventCode> {0} does not appear in " +
        "AUeventLIST1.0"),
    EVENT_AND_EVENT_CODE_MUST_MATCH("<eventCode> {0} does not match " +
        "<event> {1}. Expected {2}"),
    INFO_IS_REQUIRED("At least one <info> must be present"),
    DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL("Do not use <effective> " +
        "when <msgType> is 'Cancel'"),
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
    SENDER_SHOULD_BE_EMAIL("<sender> should be a valid address in the " +
        "format example@domain"),
    TEST_ALERT_WILL_NOT_BE_BROADCAST("Alerts with Test <status> are treated " +
        "as log-only event and not be broadcast as a valid alert"),
    RECOGNIZED_EVENT_CODE_NOT_USED("Note AUeventLIST codes not being used. " +
        "No <eventCode> with <valueName> " +
        CAP_AU_EVENT_CODE_VALUE_NAME),
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
    AREA_GEOCODE_IS_RECOMMENDED("At least one <geocode> value is " +
        "recommended but is not mandatory, either G-NAF (recommended), " +
        "ISO3166-2, Gazeetteer, or postcode, with one of the following " +
        "<valueName>s: " + RECOGNIZED_GEOCODE_VALUES + ". If a value is " +
        "used, the Producer should ensure the consumer system is able to " +
        "interpret the value selected."),
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

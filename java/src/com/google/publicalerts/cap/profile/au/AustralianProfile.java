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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.publicalerts.cap.Reason.Level.ERROR;
import static com.google.publicalerts.cap.Reason.Level.RECOMMENDATION;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
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
import com.google.publicalerts.cap.profile.CsvFileIterator;
import com.google.publicalerts.cap.profile.CsvFileIterator.CsvRow;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <a href="https://govshare.gov.au/xmlui/handle/10772/6494">CAP v1.2 Australian profile,
 * CAP-AU-STD, version 1.0, Committee Specification 02</a>.
 *
 * This class does not currently validate location codes, event codes, or event names.
 * 
 * <p>Most of these checks are not possible to represent with an XSD schema.
 *
 * TODO(shakusa) CAP-AU-STD has an XSD (https://govshare.gov.au/xmlui/handle/10772/6498),
 * validate against that
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class AustralianProfile extends AbstractCapProfile {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("[\\w\\.\\-\\+_]+\\@[\\w\\.\\-]+", Pattern.CASE_INSENSITIVE);

  private static final String CAP_AU_CODE =
      "urn:oasis:names:tc:emergency:cap:1.2:profile:CAP-AU:1.0";

  static final String CAP_AU_EVENT_CODE_VALUE_NAME =
      "https://govshare.gov.au/xmlui/handle/10772/6495";

  private static final Map<String, EventListEntry> EVENT_BY_EVENT_CODE_MAP =
      loadEventList("AUeventLIST1.0.csv");

  static final Set<String> RECOGNIZED_GEOCODE_VALUES = ImmutableSet.of(
      "http://www.psma.com.au/?product=g-naf",
      "http://www.iso.org/iso/country_codes.html",
      "http://www.ga.gov.au/place-name/",
      "http://www.psma.com.au/?product=postcode-boundaries");

  public AustralianProfile() {
    super();
  }

  /**
   * @param strictXsdValidation if {@code true}, perform by-the-spec XSD schema validation, which
   * does not check a number of properties specified elsewhere in the spec. If {@code false}
   * (the default), attempt to do extra validation to conform to the text of the spec.
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
    // The CAP-AU version code is required
    boolean hasVersionCode = false;
    for (String code : alert.getCodeList()) {
      if (getCode().equals(code)) {
        hasVersionCode = true;
      }
    }
    if (!hasVersionCode) {
      reasons.add("/alert[1]", ReasonType.VERSION_CODE_REQUIRED, getCode());
    }

    // For alert messages intended for public distribution, a <msgType> of "Alert", "Update" or
    // "Cancel" affects the message state, and an <info> element is REQUIRED.
    if (alert.getMsgType() != Alert.MsgType.ACK && alert.getMsgType() != Alert.MsgType.ERROR
        && alert.getInfoCount() == 0) {
      reasons.add("/alert[1]", ReasonType.INFO_IS_REQUIRED);
    }

    // An Update or Cancel message should minimally include references to all active messages
    if ((alert.getMsgType() == Alert.MsgType.UPDATE || alert.getMsgType() == Alert.MsgType.CANCEL)
        && alert.getReferences().getValueCount() == 0) {
      reasons.add("/alert[1]/msgType[1]", ReasonType.UPDATE_OR_CANCEL_MUST_REFERENCE);
    }

    Set<Info.Category> categories = null;
    Set<ValuePair> eventCodes = null;
    ValuePair authorizedEventCode = null;
    Map<String, String> eventByLanguage = Maps.newHashMap();
    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert[1]/info[" + (i + 1) + "]";

      // All infos must have same <category> and <eventCode> values
      Set<Info.Category> cats = Sets.newHashSet();
      cats.addAll(info.getCategoryList());
      if (categories == null) {
        categories = cats;
      } else if (!categories.equals(cats)) {
        reasons.add(xpath, ReasonType.CATEGORIES_MUST_MATCH);
      }

      Set<ValuePair> ecs = Sets.newHashSet();
      ecs.addAll(info.getEventCodeList());
      if (eventCodes == null) {
        eventCodes = ecs;
      } else if (!eventCodes.equals(ecs)) {
        reasons.add(xpath, ReasonType.EVENT_CODES_MUST_MATCH);
      }

      for (ValuePair eventCode : info.getEventCodeList()) {
        if (CAP_AU_EVENT_CODE_VALUE_NAME.equals(eventCode.getValueName())) {
          if (authorizedEventCode == null) {
            authorizedEventCode = eventCode;
          } else if (!authorizedEventCode.equals(eventCode)) {
            reasons.add(xpath, ReasonType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT);
          }
        }
      }

      if (authorizedEventCode == null) {
        reasons.add(xpath, ReasonType.ONE_AUTHORIZED_EVENT_CODE_PER_ALERT);
      } else {
        EventListEntry eventListEntry =
            EVENT_BY_EVENT_CODE_MAP.get(authorizedEventCode.getValue());
        if (eventListEntry == null) {
          reasons.add(xpath, ReasonType.UNRECOGNIZED_EVENT_CODE, authorizedEventCode.getValue());
        } else if (!info.getEvent().equals(eventListEntry.tierIEvent)
            && !info.getEvent().equals(eventListEntry.tierIIEvent)) {
          reasons.add(xpath, ReasonType.EVENT_AND_EVENT_CODE_MUST_MATCH,
              authorizedEventCode.getValue(), info.getEvent(),
              eventListEntry.tierIEvent + ("".equals(eventListEntry.tierIIEvent)
                  ? "" : " or " + eventListEntry.tierIIEvent));
        }
      }

      if (eventByLanguage.containsKey(info.getLanguage())) {
        if (!info.getEvent().equals(eventByLanguage.get(info.getLanguage()))) {
          reasons.add(xpath, ReasonType.EVENTS_IN_SAME_LANGUAGE_MUST_MATCH);
        }
      } else {
        eventByLanguage.put(info.getLanguage(), info.getEvent());
      }

      // Do not use <effective> when <msgType> is Cancel
      if (alert.getMsgType() == Alert.MsgType.CANCEL && info.hasEffective()) {
        reasons.add(xpath, ReasonType.DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL);
      }

      // <area> blocks are required
      if (info.getAreaCount() == 0) {
        reasons.add(xpath, ReasonType.AREA_IS_REQUIRED);
      }
    }
  }

  /**
   * Checks the Alert for recommendations and populates the collection provided as input.
   */
  private void checkForRecommendations(
      AlertOrBuilder alert, Reasons.Builder reasons) {
    
    // <sender> RECOMMENDED that a valid address in the format example@domain that identifies the
    // agency that assembled the message, or another agency that originated the message be used.
    // Use of Third Level Domain (example@bom.gov.au) or Fourth Level Domain (example.ses.sa.gov.au)
    // acceptable.
    if (!EMAIL_PATTERN.matcher(alert.getSender()).matches()) {
      reasons.add("/alert[1]/sender[1]", ReasonType.SENDER_SHOULD_BE_EMAIL);
    }

    // <status> "Test" treated as log-only event and not be broadcast as a valid alert
    if (alert.getStatus() == Alert.Status.TEST) {
      reasons.add("/alert[1]/status[1]", ReasonType.TEST_ALERT_WILL_NOT_BE_BROADCAST);
    }

    // TODO(shakusa) Timezones should be local to the area of the alert unless the area crosses
    // multiple timezones (then use UTC).
    // Is there a way we can recommend based on this?

    for (int i = 0; i < alert.getInfoCount(); i++) {
      Info info = alert.getInfo(i);
      String xpath = "/alert[1]/info[" + (i + 1) + "]";

      // Note when a recognized <eventCode> is not used
      boolean hasRecognizedEventCode = false;
      for (ValuePair eventCode : info.getEventCodeList()) {
        if (CAP_AU_EVENT_CODE_VALUE_NAME.equals(eventCode.getValueName())) {
          hasRecognizedEventCode = true;
          break;
        }
      }
      if (!hasRecognizedEventCode) {
        reasons.add(xpath, ReasonType.RECOGNIZED_EVENT_CODE_NOT_USED);
      }

      // An <expires> value is strongly recommended
      if (!info.hasExpires() || CapUtil.isEmptyOrWhitespace(info.getExpires())) {
        reasons.add(xpath, ReasonType.EXPIRES_STRONGLY_RECOMMENDED);
      }

      // <senderName> is strongly recommended to be populated as publicly-recognisable name of the
      // agency issuing the alert. It is expected to be used for presentation purposes.
      if (CapUtil.isEmptyOrWhitespace(info.getSenderName())) {
        reasons.add(xpath, ReasonType.SENDER_NAME_STRONGLY_RECOMMENDED);
      }

      // <responseType> is recommended along with corresponding <instruction>
      // Allows actions to be available when instructions are not available or not available in all
      // languages.
      if (info.getResponseTypeCount() == 0) {
        reasons.add(xpath, ReasonType.RESPONSE_TYPE_STRONGLY_RECOMMENDED);
      }

      if (CapUtil.isEmptyOrWhitespace(info.getInstruction())) {
        reasons.add(xpath, ReasonType.INSTRUCTION_STRONGLY_RECOMMENDED);
      }

      // Indicate when an update message contains non-substantive content changes.
      // TODO(shakusa) How to recommend MinorChange ?

      // Preferential treatment of <polygon> and <circle>
      boolean hasPolygonOrCircle = false;
      for (int j = 0; j < info.getAreaCount(); j++) {
        Area area = info.getArea(j);
        if (area.getCircleCount() != 0 || area.getPolygonCount() != 0) {
          hasPolygonOrCircle = true;
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
          reasons.add(xpath + "/area[" + (j + 1) + "]", ReasonType.AREA_GEOCODE_IS_RECOMMENDED);
        }
      }

      if (!hasPolygonOrCircle && info.getAreaCount() > 0) {
        reasons.add(xpath + "/area[1]", ReasonType.CIRCLE_POLYGON_ENCOURAGED);
      }
    }
  }

  private static Map<String, EventListEntry> loadEventList(String fileName) {
    Iterator<CsvRow> itr =
        new CsvFileIterator(AustralianProfile.class.getResourceAsStream(fileName));

    ImmutableMap.Builder<String, EventListEntry> mapBuilder = ImmutableMap.builder();
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

      Set<Info.Category> categories = Sets.newHashSet();
      for (String cat : categoriesStr.split(",")) {
        categories.add(Info.Category.valueOf(cat.trim().toUpperCase()));
      }

      Set<Integer> authoritiesToModify = Sets.newHashSet();
      for (String auth : authoritiesToModifyStr.split(",")) {
        if (!"".equals(auth)) {
          authoritiesToModify.add(Integer.parseInt(auth));
        }
      }

      if (!"".equals(eventCode.trim())) {
        mapBuilder.put(eventCode, new EventListEntry(serialNumber, tierIEvent,
            tierIIEvent, eventCode, categories, authoritiesToModify));
      }
    }
    return mapBuilder.build();
  }

  static class EventListEntry {
    final int serialNumber;
    final String tierIEvent;
    final String tierIIEvent;
    final String eventCode;
    final Set<Info.Category> categories;
    final Set<Integer> authoritiesToModify;

    EventListEntry(int serialNumber, String tierIEvent, String tierIIEvent, String eventCode,
        Set<Info.Category> categories, Set<Integer> authoritiesToModify) {
      this.serialNumber = serialNumber;
      this.tierIEvent = tierIEvent;
      this.tierIIEvent = tierIIEvent;
      this.eventCode = eventCode;
      this.categories = categories;
      this.authoritiesToModify = authoritiesToModify;
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
    CATEGORIES_MUST_MATCH(
        ERROR,
        "All <info> blocks must contain the same <category>s."),
    EVENT_CODES_MUST_MATCH(
        ERROR,
        "All <info> blocks must contain the same <eventCode>s."),
    ONE_AUTHORIZED_EVENT_CODE_PER_ALERT(
        ERROR,
        "Each alert message shall contain one single value from an authorised <eventCode> list in "
            + "order to avoid any potential confusion or difficulty having a single alert message "
            + "containing multiple events. Each such <eventCode> should have <valueName> "
            + CAP_AU_EVENT_CODE_VALUE_NAME + "."),
    EVENTS_IN_SAME_LANGUAGE_MUST_MATCH(
        ERROR,
        "All <info> blocks with the same <langauge> must contain the same <event>"),
    UNRECOGNIZED_EVENT_CODE(
        ERROR,
        "<eventCode> {0} does not appear in AUeventLIST1.0."),
    EVENT_AND_EVENT_CODE_MUST_MATCH(
        ERROR,
        "<eventCode> {0} does not match <event> {1}. Expected {2}."),
    INFO_IS_REQUIRED(
        ERROR,
        "At least one <info> must be present."),
    DO_NOT_USE_EFFECTIVE_WITH_MSGTYPE_CANCEL(
        ERROR,
        "Do not use <effective> when <msgType> is 'Cancel'."),
    AREA_IS_REQUIRED(
        ERROR,
        "At least one <area> must be present."),

    // Recommendations
    SENDER_SHOULD_BE_EMAIL(
        RECOMMENDATION,
        "<sender> should be a valid address in the format example@domain."),
    TEST_ALERT_WILL_NOT_BE_BROADCAST(
        RECOMMENDATION,
        "Alerts with Test <status> are treated as log-only event and not be broadcast as a valid "
            + "alert"),
    RECOGNIZED_EVENT_CODE_NOT_USED(
        RECOMMENDATION,
        "Note AUeventLIST codes not being used. No <eventCode> with <valueName> "
            + CAP_AU_EVENT_CODE_VALUE_NAME + "."),
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
    AREA_GEOCODE_IS_RECOMMENDED(
        RECOMMENDATION,
        "At least one <geocode> value is recommended but is not mandatory, either G-NAF "
            + "(recommended), ISO3166-2, Gazeetteer, or postcode, with one of the following "
            + "<valueName>s:" + RECOGNIZED_GEOCODE_VALUES + ". If a value is used, the Producer "
            + "should ensure the consumer system is able to interpret the value selected."),
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
      return CAP_AU_CODE;
    }
  }
}

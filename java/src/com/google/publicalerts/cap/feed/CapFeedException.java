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

package com.google.publicalerts.cap.feed;

import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.Reasons;

import java.util.Locale;

/**
 * An exception for CAP feeds.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapFeedException extends CapException {
  private static final long serialVersionUID = 3488384603825778076L;

  public CapFeedException(Reason...reasons) {
    super(reasons);
  }

  public CapFeedException(Reasons reasons) {
    super(reasons);
  }

  /**
   * Errors that can occur when validating a feed, or recommendations for
   * improving the utility of a feed.
   */
  enum ReasonType implements Reason.Type {
    // Errors
    ATOM_ID_IS_REQUIRED(
        Reason.Level.ERROR,
        "Feeds must have a non-empty <id> element."),
    ATOM_TITLE_IS_REQUIRED(
        Reason.Level.ERROR,
        "Feeds must have a non-empty <title> element."),
    ATOM_UPDATED_IS_REQUIRED(
        Reason.Level.ERROR,
        "Feeds must have a non-empty <updated> element."),
    ATOM_ENTRY_ID_IS_REQUIRED(
        Reason.Level.ERROR,
        "Entries must have a non-empty <id> element."),
    ATOM_ENTRY_TITLE_IS_REQUIRED(
        Reason.Level.ERROR,
        "Entries must have a non-empty <title> element."),
    ATOM_ENTRY_UPDATED_IS_REQUIRED(
        Reason.Level.ERROR,
        "Entries must have a non-empty <updated> element."),
    ATOM_ENTRY_MISSING_CAP_LINK(
        Reason.Level.ERROR,
        "Entries that do not embed CAP must contain a link to a CAP document, "
            + "normally found with type='" + CapFeedParser.CAP_MIME_TYPE
            + "' or type='" + CapFeedParser.ALTERNATE_CAP_MIME_TYPE + "'."),
    ATOM_ENTRY_NON_UNIQUE_IDS(
        Reason.Level.ERROR,
        "Entries must have unique <id> elements, '{0}' is repeated."),
    RSS_ITEM_TITLE_OR_DESCRIPTION_IS_REQUIRED(
        Reason.Level.ERROR,
        "Items must have a non-empty <title> or <description>."),
    RSS_ITEM_MISSING_CAP_LINK(
        Reason.Level.ERROR,
        "Item must contain a link to a CAP document."),
    EDXLDE_CONTENT_OBJECT_IS_REQUIRED(
        Reason.Level.ERROR,
        "Feeds must have a non-empty <contentObject> element."),
    EDXLDE_NO_CAP_IN_CONTENT_OBJECT(
        Reason.Level.ERROR,
        "Feeds must contain an xmlContent element containing a CAP alert, "
            + "or a nonXMLContent element pointing to a CAP alert."),
    OTHER(
        Reason.Level.ERROR,
        "{0}"),

    // Recommendations
    RSS_PUBDATE_IS_RECOMMENDED(
        Reason.Level.RECOMMENDATION,
        "Feeds should contain a <pubDate>."),
    RSS_ITEM_GUID_IS_RECOMMENDED(
        Reason.Level.RECOMMENDATION,
        "Items should contain a <guid>."),
    ;

    private final Reason.Level defaultLevel;
    private final String message;

    private ReasonType(Reason.Level defaultLevel, String message) {
      this.defaultLevel = defaultLevel;
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }

    @Override
    public Reason.Level getDefaultLevel() {
      return defaultLevel;
    }

    @Override
    public String getSource() {
      return "Feed";
    }
  }
}

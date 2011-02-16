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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.google.publicalerts.cap.CapException;

/**
 * An exception for CAP feeds.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapFeedException extends CapException {
  private static final long serialVersionUID = 3488384603825778076L;

  public CapFeedException(Reason...reasons) {
    this(Arrays.asList(reasons));
  }

  public CapFeedException(List<Reason> reasons) {
    super(reasons);
  }

  public enum FeedErrorType implements CapException.ReasonType {
    // Errors
    ATOM_ID_IS_REQUIRED("Feeds must have a non-empty <id> element"),
    ATOM_TITLE_IS_REQUIRED("Feeds must have a non-empty <title> element"),
    ATOM_UPDATED_IS_REQUIRED("Feeds must have a non-empty <updated> element"),
    ATOM_ENTRY_ID_IS_REQUIRED("Entries must have a non-empty <id> element"),
    ATOM_ENTRY_TITLE_IS_REQUIRED("Entries must have a non-empty <title> element"),
    ATOM_ENTRY_UPDATED_IS_REQUIRED("Entries must have a non-empty <updated> element"),
    ATOM_ENTRY_MISSING_CAP_LINK("Entries that do not embed CAP must contain a link to a CAP document"),
    RSS_ITEM_TITLE_OR_DESCRIPTION_IS_REQUIRED("Items must have a non-empty <title> or <description>"),
    RSS_ITEM_MISSING_CAP_LINK("Item ust contain a link to a CAP document"),
    OTHER("{0}"),
    ;

    private final String message;

    private FeedErrorType(String message) {
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }
  }

  public enum FeedRecommendationType implements CapException.ReasonType {
    RSS_PUBDATE_IS_RECOMMENDED("Feeds should contain a <pubDate>"),
    RSS_ITEM_GUID_IS_RECOMMENDED("Items should contain a <guid>"),
    ;

    private final String message;

    private FeedRecommendationType(String message) {
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }
  }
}
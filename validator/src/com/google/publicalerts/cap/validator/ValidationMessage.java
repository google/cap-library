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

package com.google.publicalerts.cap.validator;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.publicalerts.cap.Reason.Level;

/**
 * Represents a validation message (e.g., error, warning), ideally, referring a
 * specific line and column numbers.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class ValidationMessage implements Comparable<ValidationMessage> {
  private final int lineNumber;
  private final Level level;
  private final String escapedMessage;

  static ValidationMessage withUnescapedMessage(
      int lineNumber, Level level, String unescapedMessage) {
    return new ValidationMessage(lineNumber, level,
        StringUtil.lineBreaksBr(StringUtil.htmlEscape(unescapedMessage)));
  }

  static ValidationMessage withEscapedMessage(
      int lineNumber, Level level, String escapedMessage) {
    return new ValidationMessage(lineNumber, level, escapedMessage);
  }

  private ValidationMessage(
      int lineNumber, Level level, String escapedMessage) {
    this.lineNumber = lineNumber;
    this.level = checkNotNull(level);
    this.escapedMessage = checkNotNull(escapedMessage);
  }

  /**
   * @return the line number of this message, -1 if it has none
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * @return the level (i.e., severity) of this message
   */
  public Level getLevel() {
    return level;
  }
  
  /**
   * @return the error message
   */
  public String getEscapedMessage() {
    return escapedMessage;
  }

  @Override
  public int compareTo(ValidationMessage other) {
    // First, sort by line number
    int compareLineNumber = Integer.compare(lineNumber, other.lineNumber);
    if (compareLineNumber != 0) {
      return compareLineNumber;
    }

    // Then, the inverse of level (higher goes first)
    int compareLevel = - level.compareTo(other.getLevel());
    if (compareLevel != 0) {
      return compareLevel;
    }
    
    // Finally, use the string message
    return escapedMessage.compareTo(other.escapedMessage);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(level.toString());
    
    if (lineNumber >= 0) {
      builder.append(" [").append("l. ").append(lineNumber).append(']');
    }
    
    builder.append(": ").append(escapedMessage);
    
    return builder.toString();
  }
}

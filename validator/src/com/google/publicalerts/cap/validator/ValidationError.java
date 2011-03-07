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

import com.google.common.base.Preconditions;

/**
 * Represents a validation error, ideally with line and column numbers.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class ValidationError implements Comparable<ValidationError> {
  private final int lineNumber;
  private final int columnNumber;
  private final String escapedMessage;

  public static ValidationError unescaped(
    int lineNumber, int columnNumber, String unescapedMessage) {
    return new ValidationError(lineNumber, columnNumber,
      StringUtil.lineBreaksBr(StringUtil.htmlEscape(unescapedMessage)));
  }

  public static ValidationError escaped(
    int lineNumber, int columnNumber, String escapedMessage) {
    return new ValidationError(lineNumber, columnNumber, escapedMessage);
  }

  private ValidationError(int lineNumber, int columnNumber, String message) {
    Preconditions.checkNotNull(message);
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
    this.escapedMessage = message;
  }

  /**
   * @return the line number of this error, -1 if it has none
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * @return the column number of this error, -1 if it has none
   */
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
   * @return the error message
   */
  public String getEscapedMessage() {
    return escapedMessage;
  }

  @Override
  public int compareTo(ValidationError other) {
    int lineDiff = lineNumber - other.lineNumber;
    if (lineDiff != 0) {
    return lineDiff;
    }
    int colDiff = columnNumber - other.columnNumber;
    if (colDiff != 0) {
    return colDiff;
    }
    return escapedMessage.compareTo(other.escapedMessage);
  }

  @Override
  public String toString() {
    return lineNumber + ", " + columnNumber + ": " + escapedMessage;
  }
}

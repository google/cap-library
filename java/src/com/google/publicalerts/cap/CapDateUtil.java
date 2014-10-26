/*
 * Copyright (C) 2014 Google Inc.
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

package com.google.publicalerts.cap;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

/**
 * Utilities for dealing with transforming to and from the CAP timestamp format.
 */
public class CapDateUtil {

  /**
   * From the CAP spec:
   *
   * The date and time is represented in [dateTime] format
   * (e. g., "2002-05-24T16:49:00-07:00" for 24 May 2002 at
   * 16: 49 PDT).  Alphabetic timezone designators such as "Z"
   * MUST NOT be used.  The timezone for UTC MUST be represented
   * as "-00:00" or "+00:00"
   */
  // TODO(andriy): We handle fractional seconds (hundredths or thousands), but
  // because [dateTime] allows an arbitrary number of decimals, it would be
  // better to devise a more robust and flexible solution here.
  private static final Pattern DATE_PATTERN = Pattern.compile(
      "[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]"
          + "(\\.[0-9]{2}([0-9])?)?([\\+|-])([01][0-9]:[0-5][0-9])");

  private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";

  /**
   * Converts a date string in [datetime] format to a java date
   * @param dateStr the string to convert
   * @return the date, or null if the date is invalid
   */
  public static Date toJavaDate(String dateStr) {
    if (!DATE_PATTERN.matcher(dateStr).matches()) {
      return null;
    }
    try {
      return DatatypeConverter.parseDateTime(dateStr).getTime();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Formats the given date as a [datetime].
   *
   * @param cal the date and time zone to format
   * @return a string of the form "2011-10-28T12:00:01+00:00"
   */
  public static String formatCapDate(Calendar cal) {
    SimpleDateFormat format = new SimpleDateFormat(DATETIME_FORMAT);
    format.setTimeZone(cal.getTimeZone());
    StringBuilder ret = new StringBuilder(format.format(cal.getTime()));
    // SimpleDateFormat doesn't include the colon in the timezone, so add it
    // here
    ret.insert(ret.length() - 2, ':');
    return ret.toString();
  }

  /**
   * Returns the timezone offset, in minutes, between the given
   * {@code dateStr} and UTC.
   * <p>For example, if {@code dateStr} is 2003-04-02T14:39:01+05:00, this
   * method would return 300. If {@code dateStr} is 2003-04-02T14:39:01-01:29,
   * this method would return -89.
   */
  public static int getTimezoneOffset(String dateStr) {
    Matcher matcher = DATE_PATTERN.matcher(dateStr);
    if (!matcher.matches()) {
      return 0;
    }
    String sign = matcher.group(3);
    String tz = matcher.group(4);
    String[] parts = tz.split(":");
    int hours = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    return "+".equals(sign) ? hours : -hours;
  }

  /**
   * Returns true if the given string is a valid date according to the CAP spec
   * @param dateStr the string to parse
   * @return true if the given string is a valid date according to the CAP spec
   */
  public static boolean isValidDate(String dateStr) {
    return toJavaDate(dateStr) != null;
  }
}

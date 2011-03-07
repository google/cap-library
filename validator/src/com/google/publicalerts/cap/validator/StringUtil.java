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

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * String utils that for some reason aren't in
 * com.google.common.collect.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class StringUtil {

  private static final Map<Character, String> HTML_ESCAPE_MAP =
      new ImmutableMap.Builder<Character, String>()
          .put('"', "&quot;")
          .put('\'', "&#39;")
          .put('&', "&amp;")
          .put('<', "&lt;")
          .put('>', "&gt;")
          .put('\u2022', "&bull;")
          .build();

  /**
   * Escapes a string for insertion into HTML; mainly
   * to prevent XSS attacks.
   *
   * @param s the string to escape
   * @return the escaped string
   */
  public static String htmlEscape(String s) {
    StringBuilder sb = new StringBuilder();
    int slen = s.length();
    for (int i = 0; i < slen; i++) {
      String repl = HTML_ESCAPE_MAP.get(s.charAt(i));
      if (repl == null) {
        sb.append(s.charAt(i));
      } else {
        sb.append(repl);
      }
    }
    return sb.toString();
  }

  /**
   * Converts newlines to html br tags.
   * @param s the string to convert
   * @return the converted string
   */
  public static String lineBreaksBr(String s) {
    return s.replace("\n", "<br>");
  }

  private StringUtil() {}
}

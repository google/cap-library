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

package com.google.publicalerts.cap.validator;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Posts to a given URL.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class HttpPoster {
  private static final Logger log =
      Logger.getLogger(HttpPoster.class.getName());

  /**
   * Posts to the given URL with the given params.
   *
   * @param postUrl the URL to send the POST
   * @param params the URL params to POST
   * @return the http status code from the post
   * @throws IOException on error
   */
  public int post(String postUrl, Map<String, String> params)
      throws IOException {
    URL url = new URL(postUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setUseCaches(false);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Language", "en-US");  
    connection.setRequestProperty(
        "Content-Type", "application/x-www-form-urlencoded");

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (sb.length() > 0) {
        sb.append('&');
      }
      sb.append(entry.getKey() + "=" +
          URLEncoder.encode(entry.getValue(), "UTF-8"));
    }
    
    String body = sb.toString();
    connection.setRequestProperty(
        "Content-Length", Integer.toString(body.getBytes().length));

    log.info(postUrl + " : " + body);

    OutputStreamWriter writer = new OutputStreamWriter(
        connection.getOutputStream());
    writer.write(body);
    writer.close();
  
    return connection.getResponseCode();
  }
}

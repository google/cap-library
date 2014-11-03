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

package com.google.publicalerts.cap.testing;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Test utilities.
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class TestResources {

  public static String load(String filename) throws IOException {
    InputStream stream = TestResources.class.getResourceAsStream("testdata/" + filename);
    if (stream == null) {
      throw new FileNotFoundException(filename);
    }
    BufferedReader br = new BufferedReader(
        new InputStreamReader(stream, Charset.forName("UTF-8")));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line).append('\n');
    }
    return sb.toString();
  }

  public static byte[] loadBytes(String filename) throws IOException {
    InputStream stream = TestResources.class.getResourceAsStream("testdata/" + filename);
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int bytesRead = -1;
    while ((bytesRead = stream.read(chunk)) > 0) {
      buffer.write(chunk, 0, bytesRead);
    }
    stream.close();
    return buffer.toByteArray();
  }

  private TestResources() { }
}

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

import static com.google.common.truth.Truth.assertThat;

import junit.framework.TestCase;

/**
 * Tests for {@link XPath}.
 * 
 * @author sschiavoni@google.com (Stefano Schiavoni)
 */
public class XPathTest extends TestCase {

  public void testToString() {
    XPath xPath = new XPath();
    assertThat(xPath.toString()).isEqualTo("/");

    xPath.push("a");
    assertThat(xPath.toString()).isEqualTo("/a[1]");
    
    xPath.push("b");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]");
    
    xPath.push("c");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]");
    
    xPath.push("c");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[2]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]");
    
    xPath.push("c");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[3]");
    
    xPath.push("d");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[3]/d[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[3]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]");
    
    xPath.push("c");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]");
    
    xPath.push("e");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]/e[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]");
    
    xPath.push("f");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]/f[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]");
    
    xPath.push("f");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]/f[2]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]");
    
    xPath.push("g");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]/g[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[4]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]");
    
    xPath.push("c");
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]/c[5]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]/b[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/a[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/");
  }
  
  /**
   * Tests the case of an XML document that allows multiple repeated fields with the same name in
   * different parts of the document.
   */
  public void testToString_multipleRepeatedFieldsWithSameName() {
    XPath xPath = new XPath();
    assertThat(xPath.toString()).isEqualTo("/");

    xPath.push("rss");
    assertThat(xPath.toString()).isEqualTo("/rss[1]");

    xPath.push("channel");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]");

    xPath.push("link");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/link[1]");

    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]");

    xPath.push("link");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/link[2]");

    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]");

    xPath.push("item");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[1]");
    
    xPath.push("link");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[1]/link[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[1]");

    xPath.push("link");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[1]/link[2]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[1]");

    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]");

    xPath.push("item");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[2]");
    
    xPath.push("link");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[2]/link[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[2]");

    xPath.push("link");
    assertThat(xPath.toString()).isEqualTo("/rss[1]/channel[1]/item[2]/link[2]");
  }
  
  /**
   * Tests the case of an XML document that allows an element to contain itself, like
   * 
   *  <pre>
   *  <feed>
   *    <feed></feed>
   *  </feed>
   *  </pre>
   */
  public void testToString_nestedElementsWithSameName() {
    XPath xPath = new XPath();

    assertThat(xPath.toString()).isEqualTo("/");
    
    xPath.push("feed");
    assertThat(xPath.toString()).isEqualTo("/feed[1]");
    
    xPath.push("feed");
    assertThat(xPath.toString()).isEqualTo("/feed[1]/feed[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/feed[1]");
    
    xPath.pop();
    assertThat(xPath.toString()).isEqualTo("/");
  }
}

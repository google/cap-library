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

import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

/**
 * Tests for {@link XPath}.
 * 
 * @author sschiavoni@google.com (Stefano Schiavoni)
 */
public class XPathTest extends TestCase {

  public void testGetXPaths() {
    XPath xpath = new XPath(ImmutableSet.of("A", "B"));
    assertEquals("/", xpath.toString());
    assertEquals("/", xpath.getNonPredicatedXPath());

    xpath.push("a");
    assertEquals("/a", xpath.toString());
    assertEquals("/a", xpath.getNonPredicatedXPath());
    
    xpath.push("b");
    assertEquals("/a/b", xpath.toString());
    assertEquals("/a/b", xpath.getNonPredicatedXPath());
    
    xpath.push("A");
    assertEquals("/a/b/A[0]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b", xpath.toString());
    assertEquals("/a/b", xpath.getNonPredicatedXPath());
    
    xpath.push("A");
    assertEquals("/a/b/A[1]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    xpath.push("A");
    assertEquals("/a/b/A[2]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.push("c");
    assertEquals("/a/b/A[2]/c", xpath.toString());
    assertEquals("/a/b/A[2]/c", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[2]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b", xpath.toString());
    assertEquals("/a/b", xpath.getNonPredicatedXPath());
    
    xpath.push("A");
    assertEquals("/a/b/A[3]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.push("c");
    assertEquals("/a/b/A[3]/c", xpath.toString());
    assertEquals("/a/b/A[3]/c", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[3]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.push("B");
    assertEquals("/a/b/A[3]/B[0]", xpath.toString());
    assertEquals("/a/b/A[3]/B", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[3]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.push("B");
    assertEquals("/a/b/A[3]/B[1]", xpath.toString());
    assertEquals("/a/b/A[3]/B", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[3]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.push("d");
    assertEquals("/a/b/A[3]/d", xpath.toString());
    assertEquals("/a/b/A[3]/d", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[3]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b", xpath.toString());
    assertEquals("/a/b", xpath.getNonPredicatedXPath());
    
    xpath.push("A");
    assertEquals("/a/b/A[4]", xpath.toString());
    assertEquals("/a/b/A", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a/b", xpath.toString());
    assertEquals("/a/b", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/a", xpath.toString());
    assertEquals("/a", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/", xpath.toString());
    assertEquals("/", xpath.getNonPredicatedXPath());
  }
  
  /**
   * Tests the case of an XML document that allows multiple repeated fields
   * with the same name in different parts of the document.
   */
  public void testGetXPaths_multipleRepeatedFieldsWithSameName() {
    XPath xpath = new XPath(ImmutableSet.of("link", "item"));
    assertEquals("/", xpath.toString());
    assertEquals("/", xpath.getNonPredicatedXPath());

    xpath.push("rss");
    assertEquals("/rss", xpath.toString());
    assertEquals("/rss", xpath.getNonPredicatedXPath());

    xpath.push("channel");
    assertEquals("/rss/channel", xpath.toString());
    assertEquals("/rss/channel", xpath.getNonPredicatedXPath());

    xpath.push("link");
    assertEquals("/rss/channel/link[0]", xpath.toString());
    assertEquals("/rss/channel/link", xpath.getNonPredicatedXPath());

    xpath.pop();
    assertEquals("/rss/channel", xpath.toString());
    assertEquals("/rss/channel", xpath.getNonPredicatedXPath());

    xpath.push("link");
    assertEquals("/rss/channel/link[1]", xpath.toString());
    assertEquals("/rss/channel/link", xpath.getNonPredicatedXPath());

    xpath.pop();
    assertEquals("/rss/channel", xpath.toString());
    assertEquals("/rss/channel", xpath.getNonPredicatedXPath());

    xpath.push("item");
    assertEquals("/rss/channel/item[0]", xpath.toString());
    assertEquals("/rss/channel/item", xpath.getNonPredicatedXPath());
    
    xpath.push("link");
    assertEquals("/rss/channel/item[0]/link[0]", xpath.toString());
    assertEquals("/rss/channel/item[0]/link", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/rss/channel/item[0]", xpath.toString());
    assertEquals("/rss/channel/item", xpath.getNonPredicatedXPath());

    xpath.push("link");
    assertEquals("/rss/channel/item[0]/link[1]", xpath.toString());
    assertEquals("/rss/channel/item[0]/link", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/rss/channel/item[0]", xpath.toString());
    assertEquals("/rss/channel/item", xpath.getNonPredicatedXPath());

    xpath.pop();
    assertEquals("/rss/channel", xpath.toString());
    assertEquals("/rss/channel", xpath.getNonPredicatedXPath());

    xpath.push("item");
    assertEquals("/rss/channel/item[1]", xpath.toString());
    assertEquals("/rss/channel/item", xpath.getNonPredicatedXPath());
    
    xpath.push("link");
    assertEquals("/rss/channel/item[1]/link[0]", xpath.toString());
    assertEquals("/rss/channel/item[1]/link", xpath.getNonPredicatedXPath());
    
    xpath.pop();
    assertEquals("/rss/channel/item[1]", xpath.toString());
    assertEquals("/rss/channel/item", xpath.getNonPredicatedXPath());

    xpath.push("link");
    assertEquals("/rss/channel/item[1]/link[1]", xpath.toString());
    assertEquals("/rss/channel/item[1]/link", xpath.getNonPredicatedXPath());
  }
}

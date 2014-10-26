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
    assertEquals("/", xpath.getComplexXPath());
    assertEquals("/", xpath.getSimpleXPath());

    xpath.push("a");
    assertEquals("/a", xpath.getComplexXPath());
    assertEquals("/a", xpath.getSimpleXPath());
    
    xpath.push("b");
    assertEquals("/a/b", xpath.getComplexXPath());
    assertEquals("/a/b", xpath.getSimpleXPath());
    
    xpath.push("A");
    assertEquals("/a/b/A[0]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b", xpath.getComplexXPath());
    assertEquals("/a/b", xpath.getSimpleXPath());
    
    xpath.push("A");
    assertEquals("/a/b/A[1]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.pop();
    xpath.push("A");
    assertEquals("/a/b/A[2]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.push("c");
    assertEquals("/a/b/A[2]/c", xpath.getComplexXPath());
    assertEquals("/a/b/A[2]/c", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[2]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b", xpath.getComplexXPath());
    assertEquals("/a/b", xpath.getSimpleXPath());
    
    xpath.push("A");
    assertEquals("/a/b/A[3]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.push("c");
    assertEquals("/a/b/A[3]/c", xpath.getComplexXPath());
    assertEquals("/a/b/A[3]/c", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[3]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.push("B");
    assertEquals("/a/b/A[3]/B[0]", xpath.getComplexXPath());
    assertEquals("/a/b/A[3]/B", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[3]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.push("B");
    assertEquals("/a/b/A[3]/B[1]", xpath.getComplexXPath());
    assertEquals("/a/b/A[3]/B", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[3]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.push("d");
    assertEquals("/a/b/A[3]/d", xpath.getComplexXPath());
    assertEquals("/a/b/A[3]/d", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b/A[3]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b", xpath.getComplexXPath());
    assertEquals("/a/b", xpath.getSimpleXPath());
    
    xpath.push("A");
    assertEquals("/a/b/A[4]", xpath.getComplexXPath());
    assertEquals("/a/b/A", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a/b", xpath.getComplexXPath());
    assertEquals("/a/b", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/a", xpath.getComplexXPath());
    assertEquals("/a", xpath.getSimpleXPath());
    
    xpath.pop();
    assertEquals("/", xpath.getComplexXPath());
    assertEquals("/", xpath.getSimpleXPath());
  }
  
  /**
   * Tests the case of an XML document that allows multiple repeated fields
   * with the same name in different parts of the documents
   */
  public void testGetXPaths_multipleRepeatedFieldsWithSameName() {
    XPath xpath = new XPath(ImmutableSet.of("link", "item"));
    assertEquals("/", xpath.getComplexXPath());
    assertEquals("/", xpath.getSimpleXPath());

    xpath.push("rss");
    xpath.push("channel");
    assertEquals("/rss/channel", xpath.getComplexXPath());
    assertEquals("/rss/channel", xpath.getSimpleXPath());
    
    xpath.push("item");
    assertEquals("/rss/channel/item[0]", xpath.getComplexXPath());
    assertEquals("/rss/channel/item", xpath.getSimpleXPath());
    
    xpath.push("link");
    assertEquals("/rss/channel/item[0]/link[0]", xpath.getComplexXPath());
    assertEquals("/rss/channel/item[0]/link", xpath.getSimpleXPath());
    
    xpath.pop();
    xpath.push("link");
    assertEquals("/rss/channel/item[0]/link[1]", xpath.getComplexXPath());
    assertEquals("/rss/channel/item[0]/link", xpath.getSimpleXPath());
    
    xpath.pop();
    xpath.pop();
    xpath.push("item");
    assertEquals("/rss/channel/item[1]", xpath.getComplexXPath());
    assertEquals("/rss/channel/item", xpath.getSimpleXPath());
    
    xpath.push("link");
    assertEquals("/rss/channel/item[1]/link[0]", xpath.getComplexXPath());
    assertEquals("/rss/channel/item[1]/link", xpath.getSimpleXPath());
    
    xpath.pop();
    xpath.push("link");
    assertEquals("/rss/channel/item[1]/link[1]", xpath.getComplexXPath());
    assertEquals("/rss/channel/item[1]/link", xpath.getSimpleXPath());
  }
}

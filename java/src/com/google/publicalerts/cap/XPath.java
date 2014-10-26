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

package com.google.publicalerts.cap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A utility class to deal with the current XPath during XML parsing.
 * 
 * <p>Throughout this class we use the terms <em>simple XPath</em> and
 * <em>complex XPath</em>. The two differ in how repeated elements are
 * represented.
 * 
 * <p>A <strong>complex XPath</strong> enumerates all the occurrences of
 * multiple instances of the same repeated tag. An example of complex XPath is
 * 
 * <pre>
 * /rss/channel/item[1]/link[2]
 * </pre>
 * 
 * <p>A <strong>simple XPath</strong> does not enumerate the occurrences of
 * multiple instances of the last repeated tag. The simple XPath corresponding
 * to the previous example is
 * 
 * <pre>
 * /rss/channel/item[1]/link
 * </pre>
 * 
 * @author sschiavoni@google.com (Stefano Schiavoni)
 */
public class XPath {
  private final Set<String> repeatedFields;
  private final Stack<String> elements;
  private Map<String, Integer> indexMap; // Key is a simple XPath
  private String lastSimpleXPath;
  
  public XPath(Iterable<String> repeatedFields) {
    this.repeatedFields = ImmutableSet.copyOf(repeatedFields);
    this.elements = new Stack<String>();
    this.indexMap = Maps.newHashMap();
    this.lastSimpleXPath = null;
  }

  public void push(String element) {
    if (elements.isEmpty() || !element.equals(elements.peek())) {
      elements.push(element);
    }
    
    String currentSimpleXPath = getSimpleXPath();
    lastSimpleXPath = null;
    
    if (indexMap.containsKey(currentSimpleXPath)) {
      indexMap.put(currentSimpleXPath,
          indexMap.get(currentSimpleXPath) + 1);
    } else {
      indexMap.put(currentSimpleXPath, 0);
    }
  }

  public void pop() {
    if (!elements.peek().equals(lastSimpleXPath)) {
      indexMap.remove(lastSimpleXPath);
    }

    lastSimpleXPath = getSimpleXPath();
    elements.pop();
  }

  String getSimpleXPath() {
    String complexXPath = getComplexXPath();
    
    // Strip trailing square brackets, if any 
    return complexXPath.replaceFirst("\\[\\d+\\]$", "");
  }
  
  String getComplexXPath() {
    StringBuilder complexXPath = new StringBuilder("/");
    
    for (int i = 0; i < elements.size(); i++) {
      String currentElement = elements.get(i);

      if (i != 0) {
        complexXPath.append("/");
      }
      
      complexXPath.append(currentElement);
      
      Integer currentIndex = indexMap.get(complexXPath.toString());
      
      if (currentIndex != null && repeatedFields.contains(currentElement)) {
        complexXPath.append("[").append(currentIndex).append("]");
      } 
    }
    
    return complexXPath.toString();
  }
  
  @Override
  public String toString() {
    return getComplexXPath();
  }
}

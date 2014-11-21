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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Stack;

/**
 * A utility class to deal with the current XPath during XML parsing.
 *
 * @author sschiavoni@google.com (Stefano Schiavoni)
 */
public class XPath {
  private final Stack<String> elements;
  private Map<String, Integer> indexMap; // Key is a non-predicated XPath
  
  public XPath() {
    this.elements = new Stack<String>();
    this.indexMap = Maps.newHashMap();
  }

  public void push(String element) {
    String currentXPath = toString();
    String currentNonPredicatedXPath = currentXPath
        + (currentXPath.equals("/") ? "" : "/")
        + element;
    
    if (indexMap.containsKey(currentNonPredicatedXPath)) {
      indexMap.put(currentNonPredicatedXPath,
          indexMap.get(currentNonPredicatedXPath) + 1);
    } else {
      indexMap.put(currentNonPredicatedXPath, 1); // XPaths are 1-based
    }
    
    elements.push(element);
  }

  public void pop() {
    elements.pop();
  }
  
  @Override
  public String toString() {
    StringBuilder xPath = new StringBuilder("/");
    
    for (int i = 0; i < elements.size(); i++) {
      String currentElement = elements.get(i);

      if (i != 0) {
        xPath.append("/");
      }
      
      xPath.append(currentElement);
      
      Integer currentIndex = checkNotNull(indexMap.get(xPath.toString()));
      xPath.append("[").append(currentIndex).append("]");
    }
    
    return xPath.toString();
  }
}

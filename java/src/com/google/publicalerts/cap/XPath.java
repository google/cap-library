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
 * <p>Throughout this class we use the term <em>non-predicated XPath</em>. Take
 * the following example XPath:
 * 
 * <pre>
 * /rss/channel/item[1]/link[2]
 * </pre>
 * 
 * <p>A <em>non-predicated XPath</em> is a version that does not enumerate the occurrences
 * of multiple instances of the last repeated tag. The non-predicated XPath
 * corresponding to the previous example is
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
  private Map<String, Integer> indexMap; // Key is a non-predicated XPath
  
  public XPath(Iterable<String> repeatedFields) {
    this.repeatedFields = ImmutableSet.copyOf(repeatedFields);
    this.elements = new Stack<String>();
    this.indexMap = Maps.newHashMap();
  }

  public void push(String element) {
    elements.push(element);
    
    String currentNonPredicatedXPath = getNonPredicatedXPath();
    
    if (indexMap.containsKey(currentNonPredicatedXPath)) {
      indexMap.put(currentNonPredicatedXPath,
          indexMap.get(currentNonPredicatedXPath) + 1);
    } else {
      indexMap.put(currentNonPredicatedXPath, 0);
    }
  }

  public void pop() {
    elements.pop();
  }

  String getNonPredicatedXPath() {
    String xPath = toString();
    
    // Strip trailing XML predicate
    return xPath.replaceFirst("\\[\\d+\\]$", "");
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
      
      Integer currentIndex = indexMap.get(xPath.toString());
      
      if (currentIndex != null && repeatedFields.contains(currentElement)) {
        xPath.append("[").append(currentIndex).append("]");
      } 
    }
    
    return xPath.toString();
  }
}

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

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.publicalerts.cap.CapUtil;

/**
 * Parses a given XML document to maintain mappings
 * from elements to their given line number, which is helpful
 * to know where to display in-line error messages.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class LineOffsetParser {

  private static final Set<String> REPEATED_ELEMENTS = getRepeatedElements();

  private static Set<String> getRepeatedElements() {
    // Start with repeated fields from Atom and RSS
    Set<String> elements = Sets.newHashSet(
        "entry", "author", "category", "contributor", "link", "item");
    // Add repeated fields from CAP
    for (Set<String> repeated : CapUtil.getRepeatedFieldNames().values()) {
      elements.addAll(repeated);
    }
    return elements;
  }

  /**
   * Container object for line offsets based on either xpath epxressions or
   * link href's in the parsed xml document.
   */
  public static class LineOffsets {
    private final Map<String, Integer> linkLineNumbers = Maps.newHashMap();
    private final Map<String, Integer> xpathLineNumbers = Maps.newHashMap();

    /**
     * @param the 0-based index of the entry in the xml document
     * @return the line number where the &lt;entry&gt; tag starts
     */
    public int getEntryLineNumber(int index) {
      int lineNumber = getXPathLineNumber("/feed/entry[" + index + "]");
      if (lineNumber == 0) {
        lineNumber = getXPathLineNumber("/channel/item[" + index + "]");
      }
      return lineNumber;
    }

    /**
     * @param xpath the xpath expression to return a line number for
     * @param the line number of the start of the element referred to by the
     * xpath, or 0 if the xpath doesn't exist
     */
    public int getXPathLineNumber(String xpath) {
      Integer lineNumber = xpathLineNumbers.get(xpath);
      return lineNumber == null ? 0 : lineNumber;
    }

    /**
     * @param href a link href
     * @return the line number of the last link element with the href,
     * or 0 if the xpath doesn't exist
     */
    public int getLinkLineNumber(String href) {
      Integer lineNumber = linkLineNumbers.get(href);
      return lineNumber == null ? 0 : lineNumber;
    }
  }

  /**
   * Parse the given xml and return an object that allows you to fetch
   * line numbers of the elements in the document.
   * @param xml the xml to parse
   * @return an object that allows you to fetch line numbers of the elements
   * in the document
   * @throws IllegalArgumentException if the XML is invalid
   */
  public LineOffsets parse(String xml) {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(true);
    Handler handler = new Handler();
    try {
      factory.newSAXParser().parse(
          new InputSource(new StringReader(xml)), handler);
    } catch (SAXException e) {
      throw new IllegalArgumentException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    return handler.lineOffsets;
  }

  static class Handler extends DefaultHandler {
    Locator locator;
    LineOffsets lineOffsets = new LineOffsets();
    Xpath xpath = new Xpath();
    int linkLine;
    String linkHref;
    StringBuilder characters = new StringBuilder();

    @Override
    public void setDocumentLocator (Locator locator) {
      this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) {
      characters.setLength(0);
      linkLine = -1;
      linkHref = null;

      xpath.push(localName, REPEATED_ELEMENTS.contains(localName));
      lineOffsets.xpathLineNumbers.put(
          xpath.toString(), locator.getLineNumber());
      if ("link".equals(localName)) {
        linkLine = locator.getLineNumber();
        linkHref = attributes.getValue("href");
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      xpath.pop();
      if ("link".equals(localName)) {
        String href = linkHref == null ? characters.toString() : linkHref;
        lineOffsets.linkLineNumbers.put(href, linkLine);
      }
    }

    @Override
    public void characters(char ch[], int start, int length) {
      characters.append(ch, start, length);
    }

    private static class Xpath {
      Stack<String> elements;
      Stack<Integer> indexes;
      Map<String, Integer> indexCounts;
      String lastElement;

      public Xpath() {
        this.elements = new Stack<String>();
        this.indexes = new Stack<Integer>();
        this.indexCounts = new HashMap<String, Integer>();
      }

      public void push(String element, boolean repeated) {
        elements.push(element);
        indexes.push(repeated ?
            element.equals(lastElement) ? indexCounts.get(lastElement) + 1 : 0
            : null);
      }

      public void pop() {
        lastElement = elements.pop();
        indexCounts.put(lastElement, indexes.pop());
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
          sb.append("/").append(elements.get(i));
          if (indexes.get(i) != null) {
            sb.append("[").append(indexes.get(i)).append("]");
          }
        }
        return sb.toString();
      }
    }
  }
}

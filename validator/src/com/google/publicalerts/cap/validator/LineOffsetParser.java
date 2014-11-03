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
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.XPath;
import com.google.publicalerts.cap.feed.CapFeedParser;

/**
 * Parses a given XML document to maintain mappings from elements to their given
 * line number, which is helpful to know where to display in-line error
 * messages.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class LineOffsetParser {
  private static final Set<String> REPEATED_ELEMENTS =
      ImmutableSet.<String>builder()
          .addAll(CapFeedParser.FEED_REPEATED_ELEMENTS)
          .addAll(CapUtil.getRepeatedFieldNamesFlatSet()).build();

  /**
   * Container object for line offsets based on either xPath expressions or
   * link href's in the parsed XML document.
   */
  public static class LineOffsets {
    private final Map<String, Integer> linkLineNumbers = Maps.newHashMap();
    private final Map<String, Integer> xpathLineNumbers = Maps.newHashMap();

    /**
     * @param index the 0-based index of the entry in the XML document
     * @return the line number where the &lt;entry&gt; or &lt;item&gt; tag
     * starts
     * 
     * TODO(sschiavoni): rewrite this method
     */
    public int getEntryLineNumber(int index) {
      int lineNumber = getXPathLineNumber("/feed/entry[" + index + "]");
      if (lineNumber == 0) {
        lineNumber = getXPathLineNumber("/rss/channel/item[" + index + "]");
      }
      
      return lineNumber;
    }

    /**
     * @param xPath the xPath expression to return a line number for
     * @return the line number of the start of the element referred to by the
     * xPath, or 0 if the xPath doesn't exist
     */
    public int getXPathLineNumber(String xPath) {
      Integer lineNumber = xpathLineNumbers.get(xPath);
      return (lineNumber == null) ? 0 : lineNumber;
    }

    /**
     * @param href a link href
     * @return the line number of the last link element with the href,
     * or 0 if the xPath doesn't exist
     */
    public int getLinkLineNumber(String href) {
      Integer lineNumber = linkLineNumbers.get(href);
      return (lineNumber == null) ? 0 : lineNumber;
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

  private static class Handler extends DefaultHandler {
    private Locator locator;
    private LineOffsets lineOffsets = new LineOffsets();
    private XPath xPath = new XPath(REPEATED_ELEMENTS);
    private Integer linkLine;
    private String linkHref;
    private StringBuilder characters = new StringBuilder();

    @Override
    public void setDocumentLocator(Locator locator) {
      this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) {
      xPath.push(localName);
      lineOffsets.xpathLineNumbers
          .put(xPath.toString(), locator.getLineNumber());
      
      characters.setLength(0);
      linkLine = null;
      linkHref = null;

      if ("link".equals(qName)) {
        linkLine = locator.getLineNumber();
        linkHref = attributes.getValue("href");
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      xPath.pop();
      
      if ("link".equals(localName)) {
        String href = (linkHref == null) ? characters.toString() : linkHref;
        lineOffsets.linkLineNumbers.put(href, linkLine);
      }
    }

    @Override
    public void characters(char ch[], int start, int length) {
      characters.append(ch, start, length);
    }
  }
}

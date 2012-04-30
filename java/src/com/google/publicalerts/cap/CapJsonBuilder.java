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

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * Writes an Alert to a JSON object.
 * Supports CAP 1.0, 1.1, and 1.2.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapJsonBuilder {

  public static final int DEFAULT_INDENT = 2;

  private final int indent;

  public CapJsonBuilder() {
    this(DEFAULT_INDENT);
  }

  public CapJsonBuilder(int indent) {
    this.indent = indent;
  }

  /**
   * Writes the given alert to an JSON string.
   *
   * @param alert the alert to convert
   * @return the alert as an JSON string
   */
  public String toJson(AlertOrBuilder alert) {
    StringWriter writer = new StringWriter();
    try {
      toJson(alert, writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  /**
   * Writes the given alert to JSON in the given writer.
   *
   * @param alert the alert to convert
   * @param writer the writer to write out the JSON string
   * @throws IOException if there are troubles writing to the writer
   */
  public void toJson(AlertOrBuilder alert, Writer writer)
      throws JSONException, IOException {
    JSONObject json = toJSONObject(alert);
    if (indent > 0) {
      writer.write(json.toString(indent));
    } else {
      writer.write(json.toString());
    }
  }

  /**
   * Converts the given alert to a JSON object.
   *
   * @param alert the alert to convert
   * @return a JSON object
   * @throws JSONException on conversion error
   */
  public JSONObject toJSONObject(AlertOrBuilder alert) throws JSONException {
    CapXmlBuilder xmlBuilder = new CapXmlBuilder();
    Document document = xmlBuilder.toXmlDocument(alert);
    return documentToJsonObject(document);
  }

  private JSONObject documentToJsonObject(Document document)
      throws JSONException {
    JSONObject object = new JSONObject();
    Map<String, Set<String>> repeatedFields = CapUtil.getRepeatedFieldNames();
    toJsonObjectInner((Element) document.getElementsByTagName("alert").item(0),
        object, repeatedFields);
    return object;
  }

  private void toJsonObjectInner(Element element, JSONObject object,
      Map<String, Set<String>> repeatedFieldsMap) throws JSONException {
    NodeList nl = element.getChildNodes();
    Set<String> repeatedFields = repeatedFieldsMap.get(element.getNodeName());
    for (int i = 0; i < nl.getLength(); i++) {
      Element child = (Element) nl.item(i);
      int gcLength = child.getChildNodes().getLength();
      if (gcLength == 0) {
        continue;
      }
      String nodeName = child.getNodeName();
      if (repeatedFields != null && repeatedFields.contains(nodeName)) {
        if (gcLength == 1
            && child.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
          object.append(nodeName, child.getTextContent());
        } else {
          JSONObject childObj = new JSONObject();
          toJsonObjectInner(child, childObj, repeatedFieldsMap);
          object.append(nodeName, childObj);
        }
      } else {
        if (gcLength == 1
            && child.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
          object.put(child.getNodeName(), child.getTextContent());
        } else {
          JSONObject childObj = new JSONObject();
          toJsonObjectInner(child, childObj, repeatedFieldsMap);
          object.put(child.getNodeName(), childObj);
        }
      }
    }
  }
}

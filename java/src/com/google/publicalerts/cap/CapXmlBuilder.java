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

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.MessageOrBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;

/**
 * Writes an Alert to an XML document.
 * Supports CAP 1.0, 1.1, and 1.2.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapXmlBuilder {

  public static final int DEFAULT_INDENT = 2;

  private final Integer indent;

  /**
   * Creates a new XML builder that prints with newlines and 2-space indent.
   */
  public CapXmlBuilder() {
    this(DEFAULT_INDENT);
  }

  /**
   * Creates a new XML builder with the given printing options.
   *
   * @param indent number of spaces to indent each element.
   * If null, do not indent.
   */
  public CapXmlBuilder(Integer indent) {
    this.indent = indent;
  }

  /**
   * Writes the given alert to an XML string.
   *
   * @param alert the alert to convert
   * @return the alert as an XML string
   */
  public String toXml(AlertOrBuilder alert) {
    StringWriter writer = new StringWriter();
    try {
      toXml(alert, writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  /**
   * Writes the given alert to XML in the given writer.
   *
   * @param alert the alert to convert
   * @param writer the writer to write out the XML string
   * @throws IOException if there are troubles writing to the writer
   */
  public void toXml(AlertOrBuilder alert, Writer writer) throws IOException {
    Document document = toXmlDocument(alert);
    XmlUtil.writeDocument(document, writer, indent);
  }

  /**
   * Converts the alert to a document.
   *
   * @param alert the alert to convert
   * @return the alert as a Document
   */
  public Document toXmlDocument(AlertOrBuilder alert) {
    DocumentBuilder builder = XmlUtil.newDocumentBuilder();
    Document document = builder.newDocument();
    document.appendChild(toXmlDocument(alert, document));
    return document;
  }

  /**
   * Converts the alert to an XML element tree rooted at the return node.
   * @param alert the alert to convert
   * @param document the document used to create the nodes for the element tree
   */
  public Element toXmlDocument(AlertOrBuilder alert, Document document) {
    Element alertElement = document.createElement("alert");
    createDocument(document, alertElement, alert.getXmlns(), alert);
    return alertElement;
  }

  private void createDocument(Document document, Element element,
      String namespace, MessageOrBuilder message) {

    if (message instanceof GroupOrBuilder) {
      element.setTextContent(groupToString((GroupOrBuilder) message));
    } else if (message instanceof PolygonOrBuilder) {
      element.setTextContent(polygonToString((PolygonOrBuilder) message));
    } else if (message instanceof CircleOrBuilder) {
      element.setTextContent(circleToString((CircleOrBuilder) message));
    } else {
      for (FieldDescriptor fd : message.getAllFields().keySet()) {
        if (message instanceof AlertOrBuilder && "xmlns".equals(fd.getName())) {
          element.setAttribute("xmlns", ((AlertOrBuilder) message).getXmlns());
          continue;
        }
        if (fd.isRepeated()) {
          int count = message.getRepeatedFieldCount(fd);
          for (int i = 0; i < count; i++) {
            handleElement(document, element, namespace, fd,
                message.getRepeatedField(fd, i));
          }
        } else {
          handleElement(
              document, element, namespace, fd, message.getField(fd));
        }
      }
    }
  }

  private void handleElement(Document document, Element element,
      String namespace, FieldDescriptor fd, Object value) {
    String name = CapUtil.getElementName(fd);
    switch(fd.getType()) {
      case MESSAGE:
        if (CapValidator.CAP10_XMLNS.equals(namespace)
            && value instanceof ValuePairOrBuilder) {
          String textValue = cap10ValuePairToString((ValuePairOrBuilder) value);
          appendElement(document, element, name, textValue);
        } else {
          Element child = document.createElement(name);
          element.appendChild(child);
          createDocument(document, child, namespace, (MessageOrBuilder) value);
        }
        break;
      case ENUM:
        EnumValueDescriptor evd = (EnumValueDescriptor) value;
        appendElement(document, element, name, CapUtil.getEnumValue(evd));
        break;
      case BYTES:
        String byteVal = ((ByteString) value).toStringUtf8();
        appendElement(document, element, name, byteVal);
        break;
      default:
        appendElement(document, element, name, String.valueOf(value));
    }
  }

  private void appendElement(
      Document document, Element parent, String name, String value) {
    Element child = document.createElement(name);
    child.setTextContent(value);
    parent.appendChild(child);
  }

  private String cap10ValuePairToString(ValuePairOrBuilder valuePair) {
    return valuePair.getValueName() + "=" + valuePair.getValue();
  }

  private String circleToString(CircleOrBuilder circle) {
    StringBuilder sb = new StringBuilder();
    appendPoint(sb, circle.getPoint());
    sb.append(" ");
    sb.append(circle.getRadius());
    return sb.toString();
  }

  private String polygonToString(PolygonOrBuilder polygon) {
    StringBuilder sb = new StringBuilder();
    for (PointOrBuilder point : polygon.getPointOrBuilderList()) {
      appendPoint(sb, point);
      sb.append(" ");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }

  void appendPoint(StringBuilder sb, PointOrBuilder point) {
    sb.append(point.getLatitude()).append(',').append(point.getLongitude());
  }

  private String groupToString(GroupOrBuilder group) {
    StringBuilder sb = new StringBuilder();
    for (String value : group.getValueList()) {
      sb.append(maybeQuote(value)).append(" ");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }

  String maybeQuote(String s) {
    return s.matches(".*\\s.*") ? "\"" + s + "\"" : s;
  }
}

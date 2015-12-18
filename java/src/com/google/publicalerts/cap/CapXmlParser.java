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

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.google.publicalerts.cap.CapException.ReasonType;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * Parses CAP XML, optionally validating.
 * Supports CAP 1.0, 1.1, and 1.2, but all alerts in the same document
 * must have the same schema.
 *
 * Ignores enveloped digital signature.  To validate the signature, use
 * {@link XmlSignatureValidator}.
 *
 * <p>TODO(shakusa) Support input from javax.xml.transform.Source ?
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapXmlParser {
  private static final Map<String, Schema> EXTENDED_SCHEMA_MAP = initSchemaMap(
      new String[] {"cap10_extended.xsd", "cap11_extended.xsd",
          "cap12_extended.xsd"});

  private static final Map<String, Schema> STRICT_SCHEMA_MAP = initSchemaMap(
      new String[] {"cap10.xsd", "cap11.xsd", "cap12.xsd"});

  private static Map<String, Schema> initSchemaMap(String[] xsds) {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(
        XMLConstants.W3C_XML_SCHEMA_NS_URI);
    String[] xmlns = new String[] {CapValidator.CAP10_XMLNS,
        CapValidator.CAP11_XMLNS, CapValidator.CAP12_XMLNS};

    ImmutableMap.Builder<String, Schema> schemas = ImmutableMap.builder();
    for (int i = 0; i < xsds.length; i++) {
      StreamSource cap = new StreamSource(CapXmlParser.class
          .getResourceAsStream("schema/" + xsds[i]));
      try {
        schemas.put(xmlns[i], schemaFactory.newSchema(new Source[] { cap }));
      } catch (SAXException e) {
        throw new RuntimeException(e);
      }
    }
    return schemas.build();
  }

  private final boolean validate;
  private final Map<String, Schema> schemaMap;

  /**
   * Creates a new parser.
   *
   * @param validate if true, the {@code parseFrom} methods throw a
   * {@link CapException} if given invalid CAP XML. If false, no
   * {@link CapException} will be thrown, though a {@link SAXParseException}
   * or {@link NotCapException} could still be thrown if the XML is not
   * valid or not CAP. Instead, if there are errors parsing a field,
   * it is left as null.
   */
  public CapXmlParser(boolean validate) {
    this(validate, false);
  }

  /**
   * Creates a new parser.
   *
   * @param validate if true, the {@code parseFrom} methods throw a
   * {@link CapException} if given invalid CAP XML. If false, no
   * {@link CapException} will be thrown, though a {@link SAXParseException}
   * or {@link NotCapException} could still be thrown if the XML is not
   * valid or not CAP. Instead, if there are errors parsing a field,
   * it is left as null.
   * @param strictXsdValidation if true, perform by-the-spec xsd schema
   * validation, which does not check a number of properties specified
   * elsewhere in the spec. If false (the default), attempt to do extra
   * validation to conform to the text of the spec.
   */
  public CapXmlParser(boolean validate, boolean strictXsdValidation) {
    this.validate = validate;
    this.schemaMap = strictXsdValidation
        ? STRICT_SCHEMA_MAP : EXTENDED_SCHEMA_MAP;
  }

  /**
   * Parses a CAP circle area from a string.
   *
   * @param str A string encoding a circle, as described in the CAP schema.
   * It should be a coordinate pair, followed by a radius.
   * @return A CAP Circle with the same center and radius, or null if the
   * string failed to parse.
   */
  public static Circle toCircle(String str) {
    if (str == null) {
      return null;
    }
    String[] pointRadius = str.trim().split("\\s+");
    if (pointRadius.length == 0) {
      return null;
    }
    if (pointRadius.length != 2) {
      return null;
    }
    Point point = toPoint(pointRadius[0]);
    if (point == null) {
      return null;
    }

    float radius;
    try {
      radius = Float.parseFloat(pointRadius[1]);
    } catch (NumberFormatException e) {
      return null;
    }

    return Circle.newBuilder().setPoint(point).setRadius(radius)
        .buildPartial();
  }

  /**
   * Parses the given string, which should be a comma-separated pair
   * of doubles, as a Point. Returns null if it could not be parsed.
   */
  static Point toPoint(String point) {
    String[] latlng = point.split(",");
    if (latlng.length != 2) {
      return null;
    }
    double latitude;
    double longitude;
    try {
      latitude = Double.parseDouble(latlng[0]);
      longitude = Double.parseDouble(latlng[1]);
    } catch (NumberFormatException e) {
      return null;
    }
    return Point.newBuilder().setLatitude(latitude).setLongitude(longitude)
        .buildPartial();
  }

  /**
   * Parses a CAP circle area from a string.
   *
   * @param str A string encoding a polygon, as described in the CAP schema.
   * It should be a space-separated list of points, each of which is a
   * comma-separated list of doubles.
   * @return A CAP Polygon with the same points, or null if the
   * string failed to parse.
   */
  public static Polygon toPolygon(String str) {
    if (str == null) {
      return null;
    }
    String[] pointStrs = str.trim().split("\\s+");
    if (CapUtil.isEmptyOrWhitespace(str) || pointStrs.length == 0) {
      return null;
    }
    Polygon.Builder polygon = Polygon.newBuilder();
    for (String pointStr : pointStrs) {
      Point point = toPoint(pointStr);
      if (point == null) {
        return null;
      } else {
        polygon.addPoint(point);
      }
    }
    return polygon.buildPartial();
  }

  /**
   * Parses a CAP Polygon, returning null if it has fewer than 4 points.
   *
   * Like CapXmlParser.toPolygon, but additionally returns null if
   * the polygon has too few (less than four) points.
   * @param str A string encoding a polygon, as described in the CAP schema.
   * It should be a space-separated list of points, each of which is a
   * comma-separated list of doubles.
   * @return A CAP Polygon with the same points, or null if the
   * string failed to parse, or contains fewer than 4 points.
   */
  public static Polygon toPolygonNullIfTooFew(String str) {
    Polygon polygon = toPolygon(str);
    if (polygon != null && polygon.getPointCount() < 4) {
      return null;
    }
    return polygon;
  }

  /**
   * Parse the given alert.
   *
   * @param str the CAP XML to parse, as a UTF-8 string
   * @return the parsed alert
   * @throws CapException if validate is true and there are parse-related
   * or validation errors
   * @throws NotCapException if the XML is not CAP XML
   * @throws SAXParseException on XML parsing error
   */
  public final Alert parseFrom(String str)
      throws CapException, NotCapException, SAXParseException {
    return parseFromInternal(new CachedSaxInputSource(str));
  }

  /**
   * Parse the given alert.
   *
   * @param str the CAP XML to parse, as a UTF-8 string
   * @param reasons a collection to which to add any non-fatal errors,
   * warnings or recommendations during parsing
   * @return the parsed alert
   * @throws NotCapException if the XML is not CAP XML
   * @throws SAXParseException on XML parsing error
   */
  public final Alert parseFrom(String str, Reasons.Builder reasons)
      throws NotCapException, SAXParseException {
    return parseFromInternal(new CachedSaxInputSource(str), reasons);
  }

  /**
   * Parse the given alert.
   *
   * @param reader the reader to read the CAP XML to parse
   * @return the parsed alert
   * @throws CapException if validate is true and there are parse-related
   * or validation errors
   * @throws NotCapException if the XML is not CAP XML
   * @throws SAXParseException on XML parsing error
   */
  public final Alert parseFrom(Reader reader)
      throws CapException, NotCapException, SAXParseException {
    return parseFromInternal(new CachedSaxInputSource(reader));
  }

  /**
   * Parse the given alert.
   *
   * @param reader the reader to read the CAP XML to parse
   * @param reasons a collection to which to add any non-fatal errors,
   * warnings or recommendations during parsing
   * @throws NotCapException if the XML is not CAP XML
   * @throws SAXParseException on XML parsing error
   */
  public final Alert parseFrom(Reader reader, Reasons.Builder reasons)
      throws NotCapException, SAXParseException {
    return parseFromInternal(new CachedSaxInputSource(reader), reasons);
  }

  /**
   * Parse the given alert.
   *
   * @param is the input source to read the CAP XML to parse
   * @return the parsed alert
   * @throws CapException if validate is true and there are parse-related
   * or validation errors
   * @throws NotCapException if the XML is not CAP XML
   * @throws SAXParseException on XML parsing error
   */
  public final Alert parseFrom(InputSource is)
    throws CapException, NotCapException, SAXParseException {
    return parseFromInternal(new CachedSaxInputSource(is));
  }

  /**
   * Parse the given alert.
   *
   * @param is the input source to read the CAP XML to parse
   * @param reasons a collection to which to add any non-fatal errors,
   * warnings or recommendations during parsing
   * @return the parsed alert
   * @throws NotCapException if the XML is not CAP XML
   * @throws SAXParseException on XML parsing error
   */
  public final Alert parseFrom(InputSource is, Reasons.Builder reasons)
      throws NotCapException, SAXParseException {
    return parseFromInternal(new CachedSaxInputSource(is), reasons);
  }

  private Alert parseFromInternal(CachedSaxInputSource is)
      throws CapException, NotCapException, SAXParseException {
    Reasons.Builder reasonsBuilder = Reasons.newBuilder();
    Alert alert = parseFromInternal(is, reasonsBuilder);
    Reasons reasons = reasonsBuilder.build();
    
    if (validate && reasons.containsWithLevelOrHigher(Reason.Level.ERROR)) {
      throw new CapException(reasons);
    }
    
    return alert;
  }

  protected Alert parseFromInternal(CachedSaxInputSource is,
      Reasons.Builder reasons) throws NotCapException, SAXParseException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);

    CapXmlHandler handler = new CapXmlHandler();
    try {
      String xmlns = getXmlns(is);
      if (!schemaMap.containsKey(xmlns)) {
        throw new NotCapException("Unsupported xmlns:" + xmlns);
      }
      is.reset(); // Reset input streams so we can now parse entire document.
      factory.setSchema(schemaMap.get(xmlns));
      XMLReader reader = XmlUtil.getXMLReader(factory);
      reader.setContentHandler(handler);
      reader.setErrorHandler(handler);
      reader.parse(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      if (e instanceof SAXParseException) {
        throw (SAXParseException) e;
      }
      throw new RuntimeException(e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    reasons.addAll(handler.getReasons());
    
    Alert alert = handler.getAlert();
    
    if (schemaMap != STRICT_SCHEMA_MAP) {
      reasons.addAll(new CapValidator().validateAlert(alert));
    }
    
    return alert;
  }

  private String getXmlns(CachedSaxInputSource is)
      throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    XMLReader reader = XmlUtil.getXMLReader(factory);
    reader.setContentHandler(new XmlnsHandler());
    try {
      reader.parse(is);
    } catch (AbortXmlnsParseException e) {
      return e.xmlns;
    }
    return null;
  }

  /**
   * Exception to break out of the parse routine that determines the XML
   * namespace of the message.
   */
  static class AbortXmlnsParseException extends RuntimeException {
    private static final long serialVersionUID = 6359526632284475695L;

    private final String xmlns;

    public AbortXmlnsParseException(String xmlns) {
      super(xmlns);
      this.xmlns = xmlns;
    }
  }

  /**
   * Simple handler for determining the XML namespace of an alert document,
   * then aborting by throwing an {@link AbortXmlnsParseException}.
   */
  static class XmlnsHandler extends DefaultHandler {
    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {
      if (!"alert".equals(localName)
          || !CapValidator.CAP_XML_NAMESPACES.contains(uri)) {
        throw new NotCapException();
      }
      throw new AbortXmlnsParseException(uri);
    }
  }

  /**
   * SAX handler for parsing CAP XML.
   */
  static class CapXmlHandler extends DefaultHandler {
    private final StringBuilder characters;
    private final Stack<Builder> builderStack;
    private final Stack<String> builderNameStack;
    private final Reasons.Builder reasons;
    private final XPath xPath;
    private boolean inSignature;
    private Alert.Builder alertBuilder;
    private Alert alert;

    private String localName;

    public CapXmlHandler() {
      this.characters = new StringBuilder();
      this.builderStack = new Stack<Builder>();
      this.builderNameStack = new Stack<String>();
      this.reasons = Reasons.newBuilder();
      this.xPath = new XPath();
    }

    public Alert getAlert() {
      return alert;
    }

    public Reasons getReasons() {
      return reasons.build();
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      reasons.add(new Reason(
          xPath.toString(),
          ReasonType.OTHER,
          e.getMessage(),
          localName,
          characters.toString()));
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      error(e);
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      error(e);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) {
      this.localName = localName;
      characters.setLength(0);

      // Do not bother parsing components of the digital signature
      if (inSignature) {
        return;
      }

      FieldDescriptor fd = getField(localName);
      if (builderStack.isEmpty()) {
        // Must be the first element, which must be an <alert>
        if (!"alert".equals(localName)
            || !CapValidator.CAP_XML_NAMESPACES.contains(uri)) {
          throw new NotCapException();
        }
        alertBuilder = Alert.newBuilder();
        alertBuilder.setXmlns(uri);
        builderStack.push(alertBuilder);
        builderNameStack.push(localName);
        xPath.push("alert");
      } else if ("Signature".equals(localName)) {
        inSignature = true;
      } else if (fd != null) {
        if (fd.getType() == FieldDescriptor.Type.MESSAGE) {
          // Start a new complex child element
          builderStack.push(builderStack.peek().newBuilderForField(fd));
          builderNameStack.push(localName);
        }
        xPath.push(localName);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      // Do not bother parsing components of the digital signature
      this.localName = localName;
      if (inSignature) {
        if ("Signature".equals(localName)) {
          inSignature = false;
        }
        return;
      }

      FieldDescriptor fd = getField(localName);
      if (fd == null) {
        // We are either finishing up a complex type, or
        // dealing with a bad tag
        if (localName.equals(builderNameStack.peek())) {
          // tag must be ok
          if (builderStack.size() == 1) {
            // Must be the end of the <alert>
            builderNameStack.pop();
            Builder builder = builderStack.pop();
            if (builder != null) {
              alert = (Alert) builder.buildPartial();
            }
          } else {
            // Must be the end of a complex child element
            builderNameStack.pop();
            Builder finishedBuilder = builderStack.pop();
            fd = getField(localName);
            if (fd != null) {
              setOrAdd(fd,
                  getComplexValue(finishedBuilder, characters.toString()));
              characters.setLength(0);
              xPath.pop();
            }
          }
        }
        return;
      }

      setOrAdd(fd, getPrimitiveValue(fd, characters.toString()));
      characters.setLength(0);
      if (fd != null) {
        xPath.pop();
      }
    }

    @Override
    public void characters(char ch[], int start, int length) {
      characters.append(ch, start, length);
    }

    void setOrAdd(FieldDescriptor fd, Object value) {
      setOrAdd(builderStack.peek(), fd, value);
    }

    void setOrAdd(Builder builder, FieldDescriptor fd, Object value) {
      if (value == null) {
        return;
      }
      if (fd.isRepeated()) {
        builder.addRepeatedField(fd, value);
      } else {
        if (!builder.hasField(fd)) {
          builder.setField(fd, value);
        }
      }
    }

    private FieldDescriptor getField(String localName) {
      return builderStack.isEmpty() ? null :
          CapUtil.findFieldByName(builderStack.peek(), localName);
    }

    /**
     * Returns the appropriate value by looking at the field descriptor.
     *
     * TODO(shakusa) It would be more correct to use the package-private
     * methods in com.google.protobuf.TextFormat. Any way to do that?
     *
     * @param fd the field descriptor
     * @param val the object from which to extract the value
     * @return returns the value to be assigned to the message field.
     */
    Object getPrimitiveValue(FieldDescriptor fd, String val) {
      switch(fd.getType()) {
        case INT32:
        case SINT32:
        case SFIXED32:
          try {
            return Integer.parseInt(val);
          } catch (NumberFormatException e) {
            break;
          } catch (NullPointerException e) {
            break;
          }
        case UINT32:
        case FIXED32:
        case INT64:
        case SINT64:
        case SFIXED64:
          try {
            return Long.parseLong(val);
          } catch (NumberFormatException e) {
            break;
          } catch (NullPointerException e) {
            break;
          }
        case DOUBLE:
          try {
            return Double.parseDouble(val);
          } catch (NumberFormatException e) {
            break;
          } catch (NullPointerException e) {
            break;
          }
        case BOOL:
          return Boolean.parseBoolean(val);
        case STRING:
          return val;
        case FLOAT:
          try {
            return Float.parseFloat(val);
          } catch (NumberFormatException e) {
            break;
          } catch (NullPointerException e) {
            break;
          }
        case BYTES:
          return ByteString.copyFromUtf8(val);
        case ENUM:
          // Special-case the only valid space character
          if ("Very Likely".equals(val)) {
            val = "VERY_LIKELY";
          } else if (val != null) {
            val = CapUtil.underscoreCase(val).toUpperCase();
          }
          Descriptors.EnumDescriptor enumType = fd.getEnumType();
          Descriptors.EnumValueDescriptor evd = enumType.findValueByName(val);
          if (evd == null) {
            // Enum values in proto use C++ scoping rules, so 2 enums
            // of the same message can't have the same name. We work around
            // this limitation by using the name Value_EnumTypeName
            evd = enumType.findValueByName(val + "_" +
                enumType.getName().toUpperCase());
          }
          if (evd == null) {
            // Error will be added by the XSD validation
            return null;
          }
          return evd;
        case UINT64:
        case FIXED64:
        case MESSAGE:
        case GROUP:
        default:
          throw new IllegalArgumentException(
              fd.getName() + " has unsupported type " + fd.getType());
      }
      return null;
    }

    MessageOrBuilder getComplexValue(Builder builder, String str) {
      MessageOrBuilder message;
      if (builder instanceof Polygon.Builder) {
        message = toPolygonWithErrors(str);
      } else if (builder instanceof Circle.Builder) {
        message = toCircle(str);
      } else if (builder instanceof Group.Builder) {
        message = toGroup(str);
      } else if (builder instanceof ValuePair.Builder
          && CapValidator.CAP10_XMLNS.equals(alertBuilder.getXmlns())) {
        message = toCap10ValuePair(str);
      } else {
        message = builder.buildPartial();
      }
      return message;
    }

    Polygon toPolygonWithErrors(String str) {
      Polygon polygon = toPolygon(str);
      if (polygon == null) {
        // Would like to handle this in the xsd, but the pattern can
        // cause stack overflow if the number of points is large
        reasons.add(new Reason(
            xPath.toString(),
            ReasonType.INVALID_POLYGON,
            (str.length() > 50) ? str.substring(0, 47) + "..." : str));
        return null;
      }
      if (polygon.getPointCount() < 4) {
        // Would like to handle this in the xsd, but the pattern can
        // cause stack overflow if the number of points is large
        reasons.add(new Reason(
            xPath.toString(),
            ReasonType.INVALID_POLYGON,
            (str.length() > 50) ? str.substring(0, 47) + "..." : str));
      }
      return polygon;
    }

    /**
     * Parses a group from a string. From the spec:
     * Multiple space-delimited values MAY be included. Values including
     * whitespace MUST be enclosed in double-quotes.
     *
     * @param str string to parse into a group
     * @return the parsed group
     */
    Group toGroup(String str) {
      Group.Builder group = Group.newBuilder();
      boolean inQuotes = false;
      boolean lastWasEscape = false;
      char[] chars = str.toCharArray();
      StringBuilder sb = new StringBuilder();
      for (char ch : chars) {
        boolean unescapedQuote = false;
        if (ch == '"' && !lastWasEscape) {
          inQuotes = !inQuotes;
          unescapedQuote = true;
        }
        if (!inQuotes && Character.isWhitespace(ch)) {
          if (sb.length() > 0) {
            group.addValue(sb.toString());
            sb.setLength(0);
          }
        } else if (!unescapedQuote) {
          sb.append(ch);
        }
        lastWasEscape = (ch == '/');
      }
      if (sb.length() > 0) {
        group.addValue(sb.toString());
      }
      if (group.getValueCount() == 0) {
        return null;
      }
      return group.buildPartial();
    }

    ValuePair toCap10ValuePair(String str) {
      String[] nameValue = str.split("=");
      if (nameValue.length != 2) {
        return null;
      }
      return ValuePair.newBuilder().setValueName(nameValue[0].trim())
          .setValue(nameValue[1].trim()).build();
    }
  }
}

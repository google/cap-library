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

package com.google.publicalerts.cap.feed;

import java.net.URL;
import java.lang.Thread;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.CapXmlParser;
import com.google.publicalerts.cap.NotCapException;
import com.google.publicalerts.cap.Reason;
import com.google.publicalerts.cap.Reasons;
import com.google.publicalerts.cap.XPath;
import com.google.publicalerts.cap.XmlSignatureValidator;
import com.google.publicalerts.cap.XmlUtil;
import com.google.publicalerts.cap.edxl.DistributionFeed;
import com.google.publicalerts.cap.feed.CapFeedException.ReasonType;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.ParsingFeedException;
import com.sun.syndication.io.SAXBuilder;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.WireFeedInput;
import com.thaiopensource.validation.Constants;

import org.jdom.Document;
import org.jdom.input.JDOMParseException;
import org.jdom.transform.JDOMSource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * Parses feeds of alerts, the entries in those feeds, and the CAP alerts in
 * those entries.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapFeedParser {
  private static final Logger log = Logger.getLogger(
      CapFeedParser.class.getName());

  /**
   * Both of the following constants have been requested as MIME types
   * for CAP. See
   * <a href="http://tools.ietf.org/html/draft-barnes-atoca-cap-mime-01"
   *     >http://tools.ietf.org/html/draft-barnes-atoca-cap-mime-01</a>.
   * <p>However, official status of the type is unclear. Neither are currently
   * listed at <a href="http://www.iana.org/assignments/media-types/"
   *     >http://www.iana.org/assignments/media-types/</a>
   */
  public static final String CAP_MIME_TYPE = "application/cap+xml";
  public static final String ALTERNATE_CAP_MIME_TYPE =
      "application/common-alerting-protocol+xml";

  private static final Schema ATOM_RELAX_NG_SCHEMA =
      loadRelaxNgSchema("atom_rfc4287.rng");
  private static final Schema RSS2_XSD =
      loadXsd("rss-2_0.xsd");
  private static final Schema EDXLDE_SCHEMA =
      loadXsd("edxlde-1_0.xsd");

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private static Schema loadRelaxNgSchema(String schemaFile) {
    try {
      SchemaFactory factory = SchemaFactory.newInstance(
          Constants.RELAXNG_COMPACT_URI);
      StreamSource atomRng = new StreamSource(
          CapFeedParser.class.getResourceAsStream(
              "/com/google/publicalerts/cap/schema/feed/" + schemaFile));
      return factory.newSchema(new Source[] { atomRng });
    } catch (SAXException e) {
      log.log(Level.WARNING, "No schema factory available for " +
          Constants.RELAXNG_COMPACT_URI, e);
      return null;
    } catch (IllegalArgumentException e) {
      log.log(Level.WARNING, "No schema factory available for " +
          Constants.RELAXNG_COMPACT_URI, e);
      return null;
    }
  }

  private static final Schema loadXsd(String schemaFile) {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(
        XMLConstants.W3C_XML_SCHEMA_NS_URI);
    StreamSource source = new StreamSource(
        CapFeedParser.class.getResourceAsStream(
            "/com/google/publicalerts/cap/schema/feed/" + schemaFile));
    try {
      return schemaFactory.newSchema(new Source[] { source });
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

  /** True to validate feeds and alerts that are parsed. */
  private boolean validate;

  /**
   * Used to validate XML signatures on alerts, if {@code validate} is true.
   * If null, no signature validation is done.
   */
  private XmlSignatureValidator xmlSignatureValidator;

  /**
   * Creates a new parser with no XML signature validation.
   *
   * @param validate true to validate the feed as it is parsed
   */
  public CapFeedParser(boolean validate) {
    this(validate, null);
  }

  /**
   * Creates a new parser.
   *
   * @param validate true to validate the feed as it is parsed
   * @param validator Used to validate XML signatures on alerts, if
   * {@code validate} is true. If null, no signature validation is done.
   */
  public CapFeedParser(boolean validate, XmlSignatureValidator validator) {
    this.validate = validate;
    this.xmlSignatureValidator = validator;
  }

  /**
   * Sets whether or not this parser is validating input feeds and alerts
   * @param validate true if this parser is validating input feeds and alerts
   */
  public void setValidate(boolean validate) {
    this.validate = validate;
  }

  /**
   * Returns true if this parser is validating input feeds and alerts
   *
   * @return true if this parser is validating input feeds and alerts
   */
  public boolean isValidate() {
    return validate;
  }

  /**
   * Sets the validator used to validate XML signatures on alerts,
   if {@code validate} is true. If null, no signature validation is done.
   * @param validator the new validator
   */
  public void setXmlSignatureValidator(XmlSignatureValidator validator) {
    this.xmlSignatureValidator = validator;
  }

  /**
   * Returns the validator used to validate XML signatures on alerts.
   *
   * @return the validator used to validate XML signatures on alerts
   */
  public XmlSignatureValidator getXmlSignatureValidator() {
    return xmlSignatureValidator;
  }

  /**
   * Parses the given ATOM or RSS feed, provided as a Reader.
   *
   * @param reader reader for the given feed.
   * @return the parsed Rome SyndFeed
   * @throws FeedException if the XML could not be parsed
   * @throws IllegalArgumentException if XML could be parsed, but is not a
   * feed type supported by Rome
   * @throws CapFeedException if {@code validate} is true and the feed
   * is not valid
   */
  public SyndFeed parseFeed(Reader reader)
      throws FeedException, CapFeedException, IllegalArgumentException {
    return parseFeed(new InputSource(reader));
  }

  /**
   * Parses the given ATOM or RSS feed, provided as an InputSource.
   *
   * @param source source for the given feed.
   * @return the parsed Rome SyndFeed
   * @throws FeedException if the feed could not be parsed
   * @throws IllegalArgumentException if XML could be parsed, but is not a
   * feed type supported by Rome
   * @throws CapFeedException if {@code validate} is true and the feed
   * is not valid
   */
  public SyndFeed parseFeed(InputSource source)
      throws FeedException, CapFeedException, IllegalArgumentException {
    return parseFeedInternal(source, true);
  }

  public SyndFeed parseFeed(String feed)
      throws FeedException, CapFeedException, IllegalArgumentException {
    return parseFeed(new InputSource(new StringReader(feed)));
  }

  private SyndFeed parseFeedInternal(
      InputSource reader, boolean validateSchema)
      throws FeedException, CapFeedException, IllegalArgumentException {
    SyndFeedInput syndFeedInput = new SyndFeedInput(validate);
    syndFeedInput.setPreserveWireFeed(true);

    Document doc = DocBuilder.buildDocument(reader);
    SyndFeed syndFeed = syndFeedInput.build(doc);

    if (validate) {
      CapFeedValidator validator = new CapFeedValidator();
      Reasons reasons = validator.validate(syndFeed);

      if (reasons.containsWithLevelOrHigher(Reason.Level.ERROR)) {
        throw new CapFeedException(reasons);
      }

      if (validateSchema) {
        if (syndFeed.originalWireFeed() instanceof Feed) {
          validate(ATOM_RELAX_NG_SCHEMA, new JDOMSource(doc).getInputSource());
        } else if (syndFeed.originalWireFeed() instanceof Channel) {
          validate(RSS2_XSD, new JDOMSource(doc).getInputSource());
        } else if (syndFeed.originalWireFeed() instanceof DistributionFeed) {
          validate(EDXLDE_SCHEMA, new JDOMSource(doc).getInputSource());
        }
      }
    }
    return syndFeed;
  }

  private void validate(Schema schema, InputSource inputSource)
      throws CapFeedException, IllegalArgumentException {
    if (schema == null) {
      return;
    }

    FeedHandler handler = null;

    try {
      handler = new FeedHandler();
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setSchema(schema);
      factory.setNamespaceAware(true);
      factory.setFeature(
          "http://apache.org/xml/features/validation/dynamic", true);
      XMLReader reader = XmlUtil.getXMLReader(factory);
      reader.setContentHandler(handler);
      reader.setErrorHandler(handler);

      reader.parse(inputSource);
    } catch (SAXException e) {
      // Shouldn't happen; Rome parsing should have already caught this
      throw new CapFeedException(new Reason("",
          ReasonType.OTHER, e.getMessage()));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    Reasons reasons = handler.reasons.build();
    if (reasons.containsWithLevelOrHigher(Reason.Level.ERROR)) {
      throw new CapFeedException(reasons);
    }
  }

  /**
   * Parses the CAP alerts assumed to be in the &lt;content&gt; bodies of the
   * entries of the given feed.
   *
   * @param feed the feed to process
   * @return a list of CAP alerts
   * @throws CapException if any of the alerts are invalid
   * @throws NotCapException if the feed has no &lt;entry&gt;, or any of the
   * &lt;content&gt; do not contain a CAP alert
   */
  public List<Alert> parseAlerts(SyndFeed feed)
      throws CapException, NotCapException {
    @SuppressWarnings("unchecked")
    List<SyndEntry> entries = feed.getEntries();
    if (entries.isEmpty()) {
      throw new NotCapException();
    }

    List<Alert> alerts = Lists.newArrayList();
    for (SyndEntry entry : entries) {
      alerts.add(parseAlert(entry));
    }
    return alerts;
  }

  /**
   * Parses the CAP alert assumed to be in the body of the first &lt;content&gt;
   * entry.
   *
   * @param entry the entry to process
   * @return the CAP alert in the first &lt;content&gt; body
   * @throws CapException if any of the alerts are invalid
   * @throws NotCapException if any of the &lt;content&gt;s do not contain a
   * CAP alert
   */
  public Alert parseAlert(SyndEntry entry)
      throws CapException, NotCapException {
    @SuppressWarnings("unchecked")
    List<SyndContent> contents = entry.getContents();

    if (contents.isEmpty()) {
      throw new NotCapException();
    }
    return parseAlert(contents.get(0).getValue());
  }

  /**
   * Parses the CAP alert assumed to be in the body of the first &lt;content&gt;
   * entry.
   *
   * @param entry the entry to process
   * @param reasons a collection to which to add any non-fatal errors,
   * warnings or recommendations during parsing
   * @return the CAP alert in the first &lt;content&gt; body
   * @throws CapException if the alert is unparseable as XML
   * @throws NotCapException if any of the &lt;content&gt;s do not contain a
   * CAP alert
   */
  public Alert parseAlert(SyndEntry entry, Reasons.Builder reasons)
      throws CapException, NotCapException {
    @SuppressWarnings("unchecked")
    List<SyndContent> contents = entry.getContents();

    if (contents.isEmpty()) {
      throw new NotCapException();
    }
    return parseAlert(contents.get(0).getValue(), reasons);
  }

  /**
   * Parses the given CAP alert.
   *
   * @param entryPayload the alert, as a UTF-8 XML string
   * @return the parsed alert
   * @throws CapException if the alert is invalid
   * @throws NotCapException if the string is not a CAP alert
   */
  public Alert parseAlert(String entryPayload)
      throws CapException, NotCapException {
    return parseAlert(entryPayload.getBytes(UTF_8));
  }

  /**
   * Parses the given CAP alert.
   *
   * @param entryPayload the alert, as bytes
   * @return the parsed alert
   * @throws CapException if the alert is invalid
   * @throws com.google.publicalerts.cap.NotCapException if the string is not
   * a CAP alert
   */
  public Alert parseAlert(byte[] entryPayload)
      throws CapException, NotCapException {
    Reasons.Builder reasonsBuilder = Reasons.newBuilder();
    Alert alert = parseAlert(entryPayload, reasonsBuilder);
    Reasons reasons = reasonsBuilder.build();
    if (validate && reasons.containsWithLevelOrHigher(Reason.Level.ERROR)) {
      throw new CapException(reasons);
    }
    return alert;
  }

  /**
   * Parses the given CAP alert.
   *
   * @param entryPayload the alert, as a UTF-8 XML string
   * @param reasons a collection to which to add any non-fatal errors,
   * warnings or recommendations during parsing
   * @throws CapException if the alert is unparseable as XML
   * @throws com.google.publicalerts.cap.NotCapException if the string is not
   * a CAP alert
   */
  public Alert parseAlert(String entryPayload, Reasons.Builder reasons)
      throws CapException, NotCapException {
    if (CapUtil.isEmptyOrWhitespace(entryPayload)) {
      throw new NotCapException();
    }
    return parseAlert(entryPayload.getBytes(UTF_8), reasons);
  }

  /**
   * Parses the given CAP alert.
   *
   * @param entryPayload the alert, as bytes
   * @param reasons a collection to which to add any non-fatal errors,
   * warnings or recommendations during parsing
   * @throws CapException if the alert is unparseable as XML
   * @throws com.google.publicalerts.cap.NotCapException if the string is not
   * a CAP alert
   */
  public Alert parseAlert(byte[] entryPayload, Reasons.Builder reasons)
      throws CapException, NotCapException {
    if (entryPayload.length == 0) {
      throw new NotCapException();
    }
    CapXmlParser parser = new CapXmlParser(validate);
    Alert alert;
    try {
      alert = parser.parseFrom(
          new InputSource(new ByteArrayInputStream(entryPayload)), reasons);

      if (validate && xmlSignatureValidator != null) {
        XmlSignatureValidator.Result result = xmlSignatureValidator.validate(
            new InputSource(new ByteArrayInputStream(entryPayload)));
        if (!result.isSignatureValid()) {
          reasons.add(new Reason("/alert[1]/Signature[1]", ReasonType.OTHER,
              "Signature failed validation - " + result.details() + " - "
                  + Arrays.toString(entryPayload)));
        }
      }
    } catch (SAXParseException e) {
      throw new CapException(new Reason("/alert[1]", ReasonType.OTHER,
          "Invalid XML: " + new String(entryPayload, UTF_8)));
    }

    return alert;
  }

  /**
   * Returns the link in the entry that most likely points to the full version
   * of CAP.
   * <p>For RSS:
   *   If there is a non-empty link, it is returned, otherwise null
   * <p>For Atom:
   *   If there is only one link in the entry, and it has no a type, it is
   *   assumed to be the CAP URL and is returned. If there are multiple links,
   *   the link with type "application/cap+xml" is returned.
   *   null is returned otherwise.
   * @param entry the entry to extract a URL from
   * @return the URL, or null if no suitable URL is found
   */
  public String getCapUrl(SyndEntry entry) throws FeedException {
    String link = internalGetCapUrl(entry);
    try {
      URL url = new URL(link);
      if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
        throw new FeedException("Invalid URL protocol in link attribute: " + url.getProtocol());
      }
    } catch (java.net.MalformedURLException e) {
      return null;
    }
    return link;
  }

  private String internalGetCapUrl(SyndEntry entry) {
    @SuppressWarnings("unchecked") // SyndEntry.getLinks returns raw unparameterized list
    List<SyndLink> links = entry.getLinks();

    // RSS has only one link, while Atom supports separate sets of "alternate",
    // "enclosure", "other" links.  Links is not used for RSS, so we have to
    // check link.
    if (links.isEmpty()) {
      String link = entry.getLink();
      if (link != null && link.length() > 0) {
        return link;
      }
    }

    SyndLink noTypeSyndLink = null;

    for (SyndLink link : links) {
      if (CAP_MIME_TYPE.equalsIgnoreCase(link.getType()) ||
          ALTERNATE_CAP_MIME_TYPE.equalsIgnoreCase(link.getType())) {
        return link.getHref();
      } else if (CapUtil.isEmptyOrWhitespace(link.getType())
          || link.getType().contains("xml")) {
        noTypeSyndLink = link;
      }
    }

    if (noTypeSyndLink != null) {
      log.warning("@@@@@@@@@@@ noTypeSyndLink" + noTypeSyndLink.getHref());
      return noTypeSyndLink.getHref();
    }

    return null;
  }

  /** Pulled out of Rome to take advantage of their XEE prevention */
  public static class DocBuilder extends WireFeedInput {
    public static Document buildDocument(InputSource reader)
        throws IllegalArgumentException, FeedException {
      SAXBuilder saxBuilder = new DocBuilder().createSAXBuilder();
      try {
        return saxBuilder.build(reader);
      } catch (JDOMParseException ex) {
        throw new ParsingFeedException("Invalid XML: " + ex.getMessage(), ex);
      } catch (IllegalArgumentException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new ParsingFeedException("Invalid XML", ex);
      }
    }
  }

  private static class FeedHandler extends DefaultHandler {
    private static final Pattern ATOM_UNEXPECTED_ELEMENT_PATTERN =
        Pattern.compile("^element \"(.*)\" not allowed here;.*");
    private static final Pattern EDXL_RSS_UNEXPECTED_ELEMENT_PATTERN =
        Pattern.compile("^cvc-complex-type\\.2\\.4\\.a: Invalid content was found "
            + "starting with element '(.*)'\\..*");
    private static final Set<Pattern> UNEXPECTED_ELEMENT_PATTERNS =
        ImmutableSet.of(
            ATOM_UNEXPECTED_ELEMENT_PATTERN,
            EDXL_RSS_UNEXPECTED_ELEMENT_PATTERN);

    private XPath xPath = new XPath();
    private Reasons.Builder reasons;

    private FeedHandler() {
      this.reasons = Reasons.newBuilder();
    }

    @Override
    public void error(SAXParseException e) {
      reasons.add(translate(e));
    }

    @Override
    public void fatalError(SAXParseException e) {
      error(e);
    }

    @Override
    public void warning(SAXParseException e) {
      error(e);
    }

    private static final Map<String, ReasonType> REASON_MAP =
        ImmutableMap.<String, ReasonType>builder()
            .put("element \"entry\" not allowed yet; missing required element "
                + "\"id\"", ReasonType.ATOM_ID_IS_REQUIRED)
            .put("element \"entry\" not allowed yet; missing required element "
                + "\"title\"", ReasonType.ATOM_TITLE_IS_REQUIRED)
            .put("element \"entry\" not allowed yet; missing required element "
                + "\"updated\"", ReasonType.ATOM_UPDATED_IS_REQUIRED)
            .put("element \"entry\" incomplete; missing required element "
                + "\"id\"", ReasonType.ATOM_ENTRY_ID_IS_REQUIRED)
            .put("element \"entry\" incomplete; missing required element "
                + "\"title\"", ReasonType.ATOM_ENTRY_TITLE_IS_REQUIRED)
            .put("element \"entry\" incomplete; missing required element "
                + "\"updated\"", ReasonType.ATOM_ENTRY_UPDATED_IS_REQUIRED)
            .build();

    private Reason translate(SAXParseException e) {
      String xPathString = xPath.toString();

      ReasonType type = REASON_MAP.get(e.getMessage());
      if (type == null) {
        type = ReasonType.OTHER;

        // Try matching the exception message for "unexpected elements"
        for (Pattern pattern : UNEXPECTED_ELEMENT_PATTERNS) {
          Matcher unexpectedElementMatcher = pattern.matcher(e.getMessage());

          if (unexpectedElementMatcher.matches()) {
            // It is unspecified whether error/warning/fatalError methods are
            // called before or after startElement, so the xPath might be
            // updated already.
            String unexpectedElement = unexpectedElementMatcher.group(1);
            String xPathPostfix = "/" + unexpectedElement + "[1]";

            if (!xPathString.endsWith(xPathPostfix)) {
              xPathString += xPathPostfix;
            }
          }
        }
      }

      return new Reason(xPathString, type, e.getMessage());
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes atts) throws SAXException {
      xPath.push(localName);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
      xPath.pop();
    }
  }
}

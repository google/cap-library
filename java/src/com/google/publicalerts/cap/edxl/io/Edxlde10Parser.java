// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.edxl.io;

import com.google.publicalerts.cap.edxl.DistributionFeed;
import com.google.publicalerts.cap.edxl.types.ContentObject;
import com.google.publicalerts.cap.edxl.types.NonXmlContent;
import com.google.publicalerts.cap.edxl.types.TargetArea;
import com.google.publicalerts.cap.edxl.types.ValueList;
import com.google.publicalerts.cap.edxl.types.XmlContent;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.impl.BaseWireFeedParser;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

/**
 * Parser for EDXL-DE 1.0.
 *
 * TODO(anshul): This class should be made thread-safe.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public class Edxlde10Parser extends BaseWireFeedParser {
  private static final String EDXL_DE_10_URI =
      "urn:oasis:names:tc:emergency:EDXL:DE:1.0";
  private static final Namespace EDXL_DE_10_NS =
      Namespace.getNamespace(EDXL_DE_10_URI);
  static final String TYPE = "edxlde_1.0";

  public Edxlde10Parser() {
    this(TYPE);
  }

  protected Edxlde10Parser(String type) {
    super(type, EDXL_DE_10_NS);
  }

  protected Namespace getEdxldeNamespace() {
    return EDXL_DE_10_NS;
  }

  protected String getEdxldeUri() {
    return EDXL_DE_10_URI;
  }

  @Override
  public boolean isMyType(Document document) {
    Element root = document.getRootElement();
    Namespace defaultNs = root.getNamespace();
    return (defaultNs != null) && (defaultNs.equals(getEdxldeNamespace()));
  }

  @Override
  public WireFeed parse(Document document, boolean validate)
      throws IllegalArgumentException, FeedException {
    if (validate) {
      validateFeed(document);
    }
    Element root = document.getRootElement();
    return parseDistributionFeed(root);
  }

  // Leaving this method unimplemented, to follow convention with existing
  // RSS and ATOM parsers.
  protected void validateFeed(Document document) {
  }

  /**
   * Builds a java object from the root of the XML document.
   *
   * @throws FeedException
   */
  protected WireFeed parseDistributionFeed(Element root) throws FeedException {
    DistributionFeed feed = new DistributionFeed(getType());

    parseRequiredFields(root, feed);

    feed.setLanguage(getChildText(root, "language"));
    feed.setSenderRoles(parseValues(root, "senderRole"));
    feed.setRecipientRoles(parseValues(root, "recipientRole"));
    feed.setKeywords(parseValues(root, "keyword"));

    @SuppressWarnings("unchecked")
    List<Element> distributionReferenceNodes =
        root.getChildren("distributionReference", getEdxldeNamespace());
    for (Element distributionReferenceNode : distributionReferenceNodes) {
      feed.addDistributionReference(distributionReferenceNode.getText());
    }

    feed.setExplicitAddresses(parseExplicitAddress(root));

    @SuppressWarnings("unchecked")
    List<Element> targetAreaNodes =
        root.getChildren("targetArea", getEdxldeNamespace());
    for (Element targetAreaNode : targetAreaNodes) {
      feed.addTargetArea(parseTargetArea(targetAreaNode));
    }

    @SuppressWarnings("unchecked")
    List<Element> contentObjectNodes =
        root.getChildren("contentObject", getEdxldeNamespace());
    for (Element contentObjectNode : contentObjectNodes) {
      feed.addContentObject(parseContentObject(contentObjectNode));
    }

    return feed;
  }

  /**
   * Parses and fills in the required fields of the feed.
   *
   * @param root the root element for the entire XML document
   * @param feed object bean to fill in
   * @throws FeedException if any of the required fields are missing.
   */
  protected void parseRequiredFields(Element root, DistributionFeed feed)
      throws FeedException {
    feed.setDistributionId(getRequiredText(root, "distributionID"));
    feed.setSenderId(getRequiredText(root, "senderID"));
    feed.setDateTimeSent(getRequiredText(root, "dateTimeSent"));
    feed.setDistributionStatus(getRequiredText(root, "distributionStatus"));
    feed.setDistributionType(getRequiredText(root, "distributionType"));
    feed.setCombinedConfidentiality(
        getRequiredText(root, "combinedConfidentiality"));
  }

  /**
   * Parses the <targetArea> tag in the XML document.
   *
   * @param node XML element of type <targetArea>
   * @return object containing location information as specified in the XML
   */
  protected TargetArea parseTargetArea(Element node) {
    TargetArea targetArea = new TargetArea();
    @SuppressWarnings("unchecked")
    List<Element> children = node.getChildren();
    for (Element child : children) {
      String tag = child.getName();
      if (tag.equals("circle")) {
        targetArea.addCircleValue(child.getText());
      } else if (tag.equals("polygon")) {
        targetArea.addPolygonValue(child.getText());
      } else if (tag.equals("country")) {
        targetArea.addCountryValue(child.getText());
      } else if (tag.equals("subdivision")) {
        targetArea.addSubdivisionValue(child.getText());
      } else if (tag.equals("locCodeUN")) {
        targetArea.addLocCodeValue(child.getText());
      }
    }
    return targetArea;
  }

  /**
   * Parses the <contentObject> tag in the XML document.
   *
   * @param node XML element of type <contentObject>
   * @return object containing message data and content.
   * @throws FeedException if any required sub-elements are not found
   */
  protected ContentObject parseContentObject(Element node)
      throws FeedException {
    ContentObject content = new ContentObject();

    content.setContentKeywords(parseValues(node, "contentKeyword"));
    content.setOriginatorRoles(parseValues(node, "originatorRole"));
    content.setConsumerRoles(parseValues(node, "consumerRole"));

    Element e = node.getChild("contentDescription", getEdxldeNamespace());
    if (e != null) {
      content.setContentDescription(e.getText());
    }

    e = node.getChild("incidentID", getEdxldeNamespace());
    if (e != null) {
      content.setIncidentId(e.getText());
    }

    e = node.getChild("incidentDescription", getEdxldeNamespace());
    if (e != null) {
      content.setIncidentDescription(e.getText());
    }

    e = node.getChild("confidentiality", getEdxldeNamespace());
    if (e != null) {
      content.setConfidentiality(e.getText());
    }

    //TODO(anshul): Parse and store "other" element

    Element nonXmlContentElem =
        node.getChild("nonXmlContent", getEdxldeNamespace());
    Element xmlContentElem = node.getChild("xmlContent", getEdxldeNamespace());
    if (nonXmlContentElem == null && xmlContentElem == null) {
      throw new FeedException("Did not find either xml or non-xml content.");
    }

    // If both content types exist, both will be stored in the DistributionFeed.
    if (nonXmlContentElem != null) {
      content.setNonXmlContent(parseNonXmlContent(nonXmlContentElem));
    }

    if (xmlContentElem != null) {
      content.setXmlContent(parseXmlContent(xmlContentElem));
    }

    return content;
  }

  /**
   * Parses the fields in the <nonXmlContent> element. If any sub-elements exist
   * more than once, only the first one is parsed.
   *
   * @param node XML element of type <nonXmlContent>
   * @return object containing data provided in an non-XML MIME type format
   * @throws FeedException if any required fields are missing
   */
  protected NonXmlContent parseNonXmlContent(Element node)
      throws FeedException {
    NonXmlContent nonXmlContent =
        new NonXmlContent(getRequiredText(node, "mimeType"));

    Element e = node.getChild("size");
    if (e != null) {
      nonXmlContent.setSize(Integer.parseInt(e.getText()));
    }

    e = node.getChild("digest");
    if (e != null) {
      nonXmlContent.setDigest(e.getText());
    }

    e = node.getChild("uri");
    if (e != null) {
      nonXmlContent.setUri(e.getText());
    }

    e = node.getChild("contentData");
    if (e != null) {
      nonXmlContent.setContentData(
          DatatypeConverter.parseBase64Binary(e.getText()));
    }

    return nonXmlContent;
  }

  /**
   * Parses the fields in the <xmlContent> element.
   *
   * @param node XML element of type <xmlContent>
   * @return object containing valid-namespaced XML data
   */
  protected XmlContent parseXmlContent(Element node) {
    XmlContent xmlContent = new XmlContent();

    @SuppressWarnings("unchecked")
    List<Element> keyXmlContent =
        node.getChildren("keyXMLContent", getEdxldeNamespace());
    for (Element content : keyXmlContent) {
      xmlContent.addKeyXmlContent(content.getText());
    }

    @SuppressWarnings("unchecked")
    List<Element> embeddedContent =
        node.getChildren("embeddedXMLContent", getEdxldeNamespace());
    XMLOutputter outputter = new XMLOutputter();
    for (Element content : embeddedContent) {
      xmlContent.addEmbeddedXmlContent(
          outputter.outputString(content.getChildren()));
    }

    return xmlContent;
  }


  /**
   * Parses elements that contain multiple {@link ValueList} objects.
   *
   * @param root XML parent node containing nodes named "name"
   * @param name name of the tag to parse
   * @return a list containing parsed objects, or an empty list if none exist
   */
  private List<ValueList> parseValues(Element root, String name) {
    return parseValueList(root, name, "valueListUrn", "values");
  }

  /**
   * Parses the <explicitAddress> values from the XML document.
   *
   * @param root XML parent node containing nodes named explicitAddress
   * @return list of objects representing the XML content
   * @throws FeedException if any required sub-elements are missing or empty
   */
  private List<ValueList> parseExplicitAddress(Element root)
      throws FeedException {
    List<ValueList> result =
        parseValueList(root, "explicitAddress", "explicitAddressScheme",
            "explicitAddressValue");

    for (ValueList v : result) {
      if (v.getSpecifier() == null || v.getSpecifier().isEmpty()) {
        throw new FeedException(
            "Explicit Address contains no distribution addressing scheme. "
            + "Missing explicitAddressScheme tag.");
      } else if (v.getValues().isEmpty()) {
        throw new FeedException(
            "Explict Address contains no addressees value. "
            + "Missing explicitAddressValue tag.");
      }
    }
    return result;
  }

  /**
   *
   * @param root XML parent node containing nodes named "name"
   * @param name name of the tag to parse
   * @param specifierTag name of the tag containing the urn, address scheme, etc
   * @param valueTag name of the tag containing the actual values
   * @return properly parsed list of {@link ValueList} objects
   */
  private List<ValueList> parseValueList(
      Element root, String name, String specifierTag, String valueTag) {
    @SuppressWarnings("unchecked")
    List<Element> nodes = root.getChildren(name, getEdxldeNamespace());
    List<ValueList> values = new ArrayList<ValueList>();

    for (Element node : nodes) {
      ValueList valueList = new ValueList();
      @SuppressWarnings("unchecked")
      Iterator<Element> iter = node.getChildren().iterator();
      while (iter.hasNext()) {
        Element e = iter.next();
        if (e.getName().equalsIgnoreCase(specifierTag)) {
          valueList.setSpecifier(e.getTextNormalize());
          valueList.addValue(e.getTextNormalize());
        } else if (e.getName().equalsIgnoreCase(valueTag)) {
        }
      }
      values.add(valueList);
    }
    return values;
  }

  /**
   * Retrieves the textual content of the specified tag. This method should be
   * used to parse required tags.
   *
   * @param node XML parent node that should contain a tag named "name"
   * @param name the name of the tag to retrieve
   * @return textual content contained in the tag
   * @throws FeedException if there is no such tag.
   */
  private String getRequiredText(Element node, String name)
      throws FeedException {
    String text = node.getChildTextNormalize(name, getEdxldeNamespace());
    if (text == null) {
      throw new FeedException("Missing required tag " + name);
    }
    return text;
  }

  /**
   * Returns the textual content of the child element, using the EDXL-DE
   * namespace.
   *
   * @param root XML parent node that should contain a tag named "name"
   * @param name the name of the tag to retrieve
   * @return textual content, or null if there is no such child
   */
  private String getChildText(Element root, String name) {
    return root.getChildText(name, getEdxldeNamespace());
  }
}

// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.edxl.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic bean to hold XML parsed entries of the form:
 * <p>
 * <fooElement>
 *   <valueListUrn>valueListUrn</valueListUrn>
 *   <value>value</value>
 * </fooElement>
 * <p>
 * where the content of <valueListUrn> is the Uniform Resource
 * Name of a published list of values and definitions, and the content of
 * <value> is a string (which may represent a number) denoting the value itself.
 * Multiple instances of the <value> may occur with a single <valueListUrn>
 * within the <fooElement> container.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public class ValueList {
  private String specifier;
  private List<String> values;

  public ValueList() {
    values = new ArrayList<String>();
  }

  public ValueList(String specifier, List<String> values) {
    this.specifier = specifier;
    this.values = values;
  }

  public void setSpecifier(String specifier) {
    this.specifier = specifier;
  }

  public String getSpecifier() {
    return specifier;
  }

  public void addValue(String value) {
    values.add(value);
  }

  public List<String> getValues() {
    return values;
  }
}

// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.edxl.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean to hold <targetArea> data from EDXL-DE feeds.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public class TargetArea {
  private List<String> circle = new ArrayList<String>();
  private List<String> polygon = new ArrayList<String>();
  private List<String> country = new ArrayList<String>();
  private List<String> subdivision = new ArrayList<String>();
  private List<String> locCode = new ArrayList<String>();

  public List<String> getCircle() {
    return circle;
  }

  public void addCircleValue(String value) {
    circle.add(value);
  }

  public List<String> getPolygon() {
    return polygon;
  }

  public void addPolygonValue(String value) {
    polygon.add(value);
  }

  public List<String> getCountry() {
    return country;
  }

  public void addCountryValue(String value) {
    country.add(value);
  }

  public List<String> getSubdivision() {
    return subdivision;
  }

  public void addSubdivisionValue(String value) {
    subdivision.add(value);
  }

  public List<String> getLocCode() {
    return locCode;
  }

  public void addLocCodeValue(String value) {
    locCode.add(value);
  }
}

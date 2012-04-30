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

import java.util.List;

import org.json.JSONArray;

import junit.framework.TestCase;

import com.google.common.collect.Lists;
import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.testing.CapTestUtil;

/**
 * Tests for {@link MapVisualizer}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class MapVisualizerTest extends TestCase {

  private MapVisualizer viz;
  private List<Alert> alerts;

  public MapVisualizerTest(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.alerts = Lists.newArrayList();

    for (int i = 0; i < 3; i++) {
      Alert.Builder alert = CapTestUtil.getValidAlertBuilder();
      alert.setIdentifier(String.valueOf(i));
      alerts.add(alert.build());
    }
    this.viz = new MapVisualizer(alerts);
  }

  public void testGetAlertJs() throws Exception {
    JSONArray alerts = viz.getAlertsJs();
    assertEquals(3, alerts.length());
    assertNotNull(alerts.getJSONObject(0).getJSONArray("centroid"));
    assertNotNull(alerts.getJSONObject(0).get("infoWindowContent"));
    assertNotNull(alerts.getJSONObject(0).getJSONArray("polygons"));
  }

  public void testGetAbbreviatedAlertJs() throws Exception {
    JSONArray alerts = viz.getAlertsJs(true);
    assertEquals(3, alerts.length());
    assertNotNull(alerts.getJSONObject(0).getJSONArray("centroid"));
    assertNotNull(alerts.getJSONObject(0).get("infoWindowContent"));
    assertFalse(alerts.getJSONObject(0).has("polygons"));
  }

  public void testGetCentroid() throws Exception {
    JSONArray polygons = new JSONArray();
    assertCentroid(0, 0, viz.getCentroid(polygons));

    polygons.put(makePolygon(0, 0, 0, 1, 1, 1, 1, 0, 0, 0));
    assertCentroid(0.5, 0.5, viz.getCentroid(polygons));

    polygons.put(makePolygon(0, 0, 0, -1, -1, -1, -1, 0, 0, 0));
    assertCentroid(0.0, 0.0, viz.getCentroid(polygons));
  }

  private JSONArray makePolygon(double...coords) throws Exception {
    JSONArray polygon = new JSONArray();
    for (int i = 0; i < coords.length; i += 2) {
      double lat = coords[i];
      double lng = coords[i + 1];
      polygon.put(new JSONArray().put(lat).put(lng));
    }
    return polygon;
  }

  private void assertCentroid(double lat, double lng, JSONArray centroid)
      throws Exception {
    assertEquals(lat, centroid.getDouble(0));
    assertEquals(lng, centroid.getDouble(1));
  }
}

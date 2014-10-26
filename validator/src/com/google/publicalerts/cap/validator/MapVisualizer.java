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
import org.json.JSONException;
import org.json.JSONObject;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.Area;
import com.google.publicalerts.cap.Circle;
import com.google.publicalerts.cap.Info;
import com.google.publicalerts.cap.Point;
import com.google.publicalerts.cap.Polygon;

/**
 * Prepares a JSON object used to render alerts on a Google Maps API map.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class MapVisualizer {
  private static final double MIN_RADIUS_KM = 0.1;
  private static final double EARTH_RADIUS_KM = 6378.1;
  private static final double DEG_TO_RAD = Math.PI / 180.0;
  private static final double RAD_TO_DEG = 180.0 / Math.PI;

  private List<Alert> alerts;

  public MapVisualizer(List<Alert> alerts) {
    this.alerts = alerts;
  }

  /**
   * Returns a JSON array of JSONObjects, one per renderable alert.
   * An alert is renderable if the code can parse area information from it.
   *
   * @return a JSON array of renderable alerts, or null if there are no
   * renderable alerts
   */
  public JSONArray getAlertsJs() {
    try {
      return getAlertsJs(alerts.size() > 50);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a JSON array of JSONObjects, one per renderable alert.
   * An alert is renderable if the code can parse area information from it.
   *
   * @param abbreviated true to return abbreviated results
   * (no polygons and smaller info window content)
   * @return a JSON array of renderable alerts, or null if there are no
   * renderable alerts
   * @throws JSONException
   */
  public JSONArray getAlertsJs(boolean abbreviated)
      throws JSONException {
    JSONArray array = new JSONArray();

    for (Alert alert : alerts) {
      JSONArray polygons = buildPolygons(alert);
      if (polygons.length() == 0) {
        continue;
      }

      StringBuilder sb = new StringBuilder("<table class='infowindow'>");
      Info info = findFirstEnglishInfo(alert);
      if (abbreviated) {
        append(sb, "Headline", info.getHeadline());
        append(sb, "Identifier", alert.getIdentifier());
      } else {
        append(sb, "Event", info.getEvent());
        append(sb, "Sent", alert.getSent());
        append(sb, "Expires", info.getExpires());
        append(sb, "Area", compileAreas(info));
        append(sb, "Web", info.getWeb(), true);
        append(sb, "Sender", alert.getSender());
        append(sb, "Identifier", alert.getIdentifier());
      }
      sb.append("</table>");

      JSONObject alertJs = new JSONObject();
      alertJs.put("centroid", getCentroid(polygons));
      alertJs.put("infoWindowContent", sb.toString());
      if (!abbreviated) {
        alertJs.put("polygons", polygons);
      }

      array.put(alertJs);
    }
    return array.length() == 0 ? null : array;
  }

  private Info findFirstEnglishInfo(Alert alert) {
    for (Info info : alert.getInfoList()) {
      if (info.getLanguage().startsWith("en")) {
        return info;
      }
    }
    return alert.getInfo(0);
  }

  private String compileAreas(Info info) {
    StringBuilder sb = new StringBuilder();
    for (Area area : info.getAreaList()) {
      if (sb.length() > 0) {
        sb.append("; ");
      }
      sb.append(area.getAreaDesc());
    }
    return sb.toString();
  }

  private JSONArray buildPolygons(Alert alert) throws JSONException {
    JSONArray polygons = new JSONArray();
    for (Info info : alert.getInfoList()) {
      for (Area area : info.getAreaList()) {
        for (Polygon polygon : area.getPolygonList()) {
          polygons.put(buildPolygon(polygon));
        }
        for (Circle circle : area.getCircleList()) {
          polygons.put(buildCircle(circle.getPoint(), circle.getRadius()));
        }
        // TODO(shakusa) If there are no circles or polygons,
        // consider trying to translate well-known geocodes
      }
    }
    return polygons;
  }

  private JSONArray buildPolygon(Polygon polygon) throws JSONException {
    JSONArray jsPolygon = new JSONArray();
    for (Point point : polygon.getPointList()) {
      jsPolygon.put(
          toJsPoint(point.getLatitude(), point.getLongitude()));
    }
    return jsPolygon;
  }

  private JSONArray buildCircle(Point center, double radiusInKm)
      throws JSONException {
    if (radiusInKm == 0) {
      radiusInKm = MIN_RADIUS_KM;
    }
    JSONArray points = new JSONArray();

    int numPoints = 20;
    for (int i = 0; i < numPoints; i++) {
        double heading = i * (2 * Math.PI / numPoints);
        points.put(getRadialEndpoint(
            heading, radiusInKm, center.getLatitude(), center.getLongitude()));
      }
    points.put(points.get(0));  // polygon must end with starting point
    return points;
  }

  /**
   * Computes the end point of an arc of a great circle given a starting point
   * and arc length.
   * See http://williams.best.vwh.net/avform.htm#LL
   *
   * @param heading the direction of the arc, in radians
   * @param arcLength the length of the arc, in kilometers
   * @param latitude latitude of the starting point of the arc
   * @param longitude longitude of the starting point of the arc
   * @return the end point
   */
  private JSONArray getRadialEndpoint(
      double heading, double arcLength, double latitude, double longitude)
      throws JSONException {
    // the angle of the great circle arc
    double earthAngle = arcLength / EARTH_RADIUS_KM;
    // convert decimal degrees to radians for the calculation
    double lat = latitude * DEG_TO_RAD;
    double lng = Math.abs(longitude) * DEG_TO_RAD;
    double asinArg = Math.sin(lat) * Math.cos(earthAngle) +
        Math.cos(lat) * Math.sin(earthAngle) * Math.cos(heading);

    double destLat = Math.asin(asinArg);
    double y = Math.sin(heading) * Math.sin(earthAngle) * Math.cos(lat);
    double x = Math.cos(earthAngle) - Math.sin(lat) * Math.sin(destLat);
    double destLng =
        (lng - Math.atan2(y, x) + Math.PI) % (2 * Math.PI) - Math.PI;

    return toJsPoint(
        destLat * RAD_TO_DEG,
        destLng * RAD_TO_DEG * (longitude < 0 ? -1 : 1));
  }

  private JSONArray toJsPoint(double lat, double lng) throws JSONException {
    return new JSONArray().put(lat).put(lng);
  }

  JSONArray getCentroid(JSONArray polygons) throws JSONException {
    double centroidX = 0;
    double centroidY = 0;
    for (int i = 0; i < polygons.length(); i++) {
      JSONArray polygon = polygons.getJSONArray(i);
      double cx = 0;
      double cy = 0;
      double a = 0;
      for (int j = 0; j < polygon.length() - 1; j++) {
        JSONArray p0 = polygon.getJSONArray(j);
        JSONArray p1 = polygon.getJSONArray(j + 1);
        double p0x = p0.getDouble(0);
        double p0y = p0.getDouble(1);
        double p1x = p1.getDouble(0);
        double p1y = p1.getDouble(1);
        double aj = (p0x * p1y - p1x * p0y);
        cx += (p0x + p1x) * aj;
        cy += (p0y + p1y) * aj;
        a += aj;
      }
      a /= 2;
      if (a != 0) {
        cx /= 6 * a;
        cy /= 6 * a;
        centroidX += cx;
        centroidY += cy;
      }
    }
    int denom = polygons.length() == 0 ? 1 : polygons.length();
    return toJsPoint(centroidX / denom, centroidY / denom);
  }

  private void append(StringBuilder sb, String key, String value) {
    append(sb, key, value, false);
  }

  private void append(
      StringBuilder sb, String key, String value, boolean isUrl) {
    sb.append("<tr><td align='right' valign='top'><b>")
        .append(key)
        .append(":</b></td><td>");
    if (isUrl) {
      sb.append("<a href='")
          .append(StringUtil.htmlEscape(value))
          .append("'>")
          .append(StringUtil.htmlEscape(value))
          .append("</a>");
    } else {
      sb.append(StringUtil.htmlEscape(value));
    }
    sb.append("</td></tr>");
  }
}

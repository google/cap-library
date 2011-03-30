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

(function() {

var activeAlert;

function setupMap(mapEl) {
  if (!mapEl) {
    return;
  }
  mapEl.style.width = "700px";
  mapEl.style.height = "400px";
  var map = new google.maps.Map(mapEl, {
    zoom: 3,
    center: new google.maps.LatLng(40.044437,-97.734375),
    mapTypeId: google.maps.MapTypeId.ROADMAP,
    mapTypeControl: true,
    scaleControl: true,
    navigationControl: true,
    navigationControlOptions: {style: google.maps.NavigationControlStyle.SMALL}
  });

  var bounds = [90, 180, -90, -180];
  for (var i = 0; i < alerts.length; i++) {
    var alert = alerts[i];
    if (alert.polygons) {
      alert.gmPolygons = [];
      for (var j = 0; j < alert.polygons.length; j++) {
        var polygon = alert.polygons[j];
        var path = [];
        for (var k = 0; k < polygon.length; k++) {
          var point = polygon[k];
          checkBounds(bounds, point);
          path.push(new google.maps.LatLng(point[0], point[1]));
        }
        if (path.length > 0) {
          alert.gmPolygons.push(
              new google.maps.Polygon({
                  path: path,
                  fillColor: "#FF0000",
                  fillOpacity: 0.1,
                  strokeColor: "#FF0000",
                  strokeOpacity: 1.0,
                  strokeWeight: 2,
                  map: map
              }));
        }
      }
    }
    if (alert.centroid) {
      checkBounds(bounds, alert.centroid);
      var position = new google.maps.LatLng(alert.centroid[0], alert.centroid[1]);
      alert.gmMarker = new google.maps.Marker({
        position: position,
        map: map
      });
      if (alert.infoWindowContent) {
        google.maps.event.addListener(alert.gmMarker, 'click',
            open(map, alert.gmMarker, alert));
      }      
    }
  }

  if (bounds[0] != 90) {
    map.fitBounds(new google.maps.LatLngBounds(
        new google.maps.LatLng(bounds[0], bounds[1]),
        new google.maps.LatLng(bounds[2], bounds[3])));
  }
  
  if (alerts.length == 1 && alerts[0].gmMarker) {
    window.setTimeout(open(map, alerts[0].gmMarker, alerts[0]), 500);
  }
}

function checkBounds(bounds, point) {
  bounds[0] = Math.min(bounds[0], point[0]);
  bounds[1] = Math.min(bounds[1], point[1]);
  bounds[2] = Math.max(bounds[2], point[0]);
  bounds[3] = Math.max(bounds[3], point[1]);
}

function open(map, marker, alert) {
  return function () {
    if (activeAlert) {
      if (activeAlert.infoWindow) {
        activeAlert.infoWindow.close();
        activeAlert.infoWindow = null;
      }
      if (activeAlert.gmPolygons) {
        for (var i = 0; i < activeAlert.gmPolygons.length; i++) {
          activeAlert.gmPolygons[i].setOptions({
              fillOpacity: 0.1,
              fillColor: "#FF0000",
              strokeColor: "#FF0000",
              zindex: 100
          });
        }
      }
      if (activeAlert.gmMarker) {
        activeAlert.gmMarker.setOptions({
            icon: 'http://www.google.com/intl/en_us/mapfiles/ms/micons/red-dot.png',
            zIndex: 100
        });
      }
    }
    activeAlert = alert;
    if (activeAlert.gmPolygons) {
      for (var i = 0; i < activeAlert.gmPolygons.length; i++) {
        activeAlert.gmPolygons[i].setOptions({
          fillOpacity: 0.2,
          fillColor: "#0000FF",
          strokeColor: "#0000FF",
          zindex: 200
        });
      }
    }
    if (activeAlert.gmMarker) {
      activeAlert.gmMarker.setOptions({
          icon: 'http://www.google.com/intl/en_us/mapfiles/ms/micons/blue-dot.png',
          zIndex: 200
      });
    }
    if (activeAlert.infoWindowContent) {
      activeAlert.infoWindow = new google.maps.InfoWindow({
          content: activeAlert.infoWindowContent
      });
      activeAlert.infoWindow.open(map, marker);
    }
  };
}

function onload() {
  setupMap(document.getElementById('map'));
}
window.onload = onload;

})();

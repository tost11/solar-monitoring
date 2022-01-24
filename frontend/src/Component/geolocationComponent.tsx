import React, {useState} from "react";

export default function geolocationComponent() {
let latitude;
let longitude;
let altitude;
let geoinfo;

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      function (position) {
        latitude = position.coords.latitude;
        longitude = position.coords.longitude;
        if (position.coords.altitude) {
          altitude = position.coords.altitude;
        } else {
          altitude = ' Keine Höhenangaben vorhanden ';
        }
        geoinfo = 'Latitude ' + latitude + 'Longitude'+longitude;
      });
  } else {
    geoinfo = 'Dieser Browser unterstützt die Abfrage der Geolocation nicht.';
  }
  return<div>
    <h1>{geoinfo}</h1>
  </div>
}

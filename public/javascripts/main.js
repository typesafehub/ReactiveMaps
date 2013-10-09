
$(function() {

    var map = L.map("map").setView([-25, 135], 4);

    var osmUrl="http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
    var osmAttrib="Map data © OpenStreetMap contributors";
    var osm = new L.TileLayer(osmUrl, {minZoom: 1, maxZoom: 12, attribution: osmAttrib}).addTo(map);

    var markers = {}

    // When the map zooms, we want to immediately move all the markers to where they should be, not have them
    // slowly transition
    map.on("zoomstart", function() {
        for (var id in markers) {
            var marker = markers[id];
            resetTransition(marker._icon);
            resetTransition(marker._shadow);
        }
    });

    var ws = new WebSocket("ws://localhost:9000/stream");

    ws.onmessage = function(event) {
        var json = JSON.parse(event.data);

        console.log(json.features.length);

        json.features.forEach(function(feature) {
            var marker = markers[feature.id];
            var coordinates = feature.geometry.coordinates;
            var latLng = new L.LatLng(coordinates[0], coordinates[1]);
            if (marker) {
                marker.setLatLng(latLng);
                // Set the transition time to be equal to since we last saw an update
                var lastUpdate = marker.feature.properties.timestamp;
                var updated = feature.properties.timestamp;
                var time = (updated - lastUpdate);
                if (time > 0) {
                    transition(marker._icon, time);
                    transition(marker._shadow, time);
                }
                marker.feature = feature;
            } else {
                marker = new L.Marker(latLng, {title: feature.id}).addTo(map);
                marker.feature = feature;
                markers[feature.id] = marker;
            }
        });

    };

    function resetTransition(element) {
        function updateTransition(element, prefix) {
            element.style[prefix + "transition"] = "";
        }
        updateTransition(element, "-webkit-");
        updateTransition(element, "-moz-");
        updateTransition(element, "-o-");
        updateTransition(element, "");
    }

    function transition(element, time) {
        function updateTransition(element, prefix) {
            element.style[prefix + "transition"] = prefix + "transform " + time + "ms linear";
        }
        updateTransition(element, "-webkit-");
        updateTransition(element, "-moz-");
        updateTransition(element, "-o-");
        updateTransition(element, "");
    }

});

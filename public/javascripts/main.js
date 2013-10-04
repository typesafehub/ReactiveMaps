
$(function() {

    var map = new OpenLayers.Map('map');

    var wms = new OpenLayers.Layer.WMS(
        "OpenLayers WMS",
        "http://vmap0.tiles.osgeo.org/wms/vmap0",
        {'layers':'basic'} );

    map.addLayer(wms);

    var markers = new OpenLayers.Layer.Markers("Markers");
    map.addLayer(markers);

    var size = new OpenLayers.Size(21,25);
    var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);
    var icon = new OpenLayers.Icon("http://www.openlayers.org/dev/img/marker.png", size, offset);
    var marker = new OpenLayers.Marker(new OpenLayers.LonLat(0, 0), icon);
    markers.addMarker(marker);

    map.zoomToMaxExtent();

    var ws = new WebSocket("ws://localhost:9000/stream");

    ws.onmessage = function(event) {
        var coords = JSON.parse(event.data);

        var lat = coords[0];
        var lon = coords[1];

        $("#coords .lat").text(lat);
        $("#coords .lon").text(lon);

        var newLonLat = new OpenLayers.LonLat(lon, lat);
        var newPx = map.getLayerPxFromLonLat(newLonLat);
        marker.moveTo(newPx);
    };
});

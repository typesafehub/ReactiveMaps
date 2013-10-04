
$(function() {
    var ws = new WebSocket("ws://localhost:9000/stream");
    ws.onmessage = function(event) {
        var coords = JSON.parse(event.data);
        $("#coords .lat").text(coords[0]);
        $("#coords .lon").text(coords[1]);
    };
});

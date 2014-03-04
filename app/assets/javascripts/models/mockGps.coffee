#
# The mock GPS interface.  This is provided by providing a second map where you can
# position a marker to fake a GPS location.
#
# Used to manually specify your position if you are not using a GPS enabled device.
#
define ["leaflet"], (Leaflet) ->
  class MockGps
    constructor: (ws) ->
      @ws = ws

      @map = Leaflet.map("mockGps")
      new Leaflet.TileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        minZoom: 1
        maxZoom: 16
        attribution: "Map data © OpenStreetMap contributors"
      ).addTo(@map)

      position
      if localStorage.lastGps
        try
          position = JSON.parse localStorage.lastGps
        catch e
          localStorage.removeItem("lastGps")
          position = [0, 0]
      else
        position = [0, 0]
      @map.setView(position, 4)

      @marker = new Leaflet.Marker(position,
        draggable: true
      ).addTo(@map)

      @marker.on "dragend", =>
        @sendPosition()

      @sendPosition()

    sendPosition: ->
      position = @marker.getLatLng()
      localStorage.lastGps = JSON.stringify position
      @ws.send(JSON.stringify
        event: "user-moved"
        position:
          type: "Point"
          coordinates: [position.lng, position.lat]
      )


    destroy: ->
      try
        @map.remove()
      catch e

  return MockGps
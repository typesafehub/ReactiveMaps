#
# The main map.  Manages displaying markers on the map, as well as responding to the user moving around and zooming
# on the map.
#
define ["marker", "storage", "leaflet"], (Marker, Storage, Leaflet) ->

  class Map
    constructor: (ws) ->
      # the map itself
      @map = Leaflet.map("map")
      new Leaflet.TileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        minZoom: 1
        maxZoom: 16
        attribution: "Map data © OpenStreetMap contributors"
      ).addTo(@map)

      # Focus on the last area that was viewed
      lastArea = Storage.lastArea()
      if (lastArea)
        try
          @map.setView(lastArea.center, lastArea.zoom)
        catch e
          @map.setView([0, 0], 2)
      else
        @map.setView([0, 0], 2)

      # the websocket
      @ws = ws

      # the markers on the map
      @markers = {}

      # When zooming, the markers are likely to all change ids due to clustering, which means until they expire,
      # the screen is going to have too much data on it.  So after zooming, we want to clear them off the screen,
      # but not before we've got at least some data to display, so we hold the ones that existed before zooming
      # in this map.
      @preZoomMarkers = {}

      # the sendArea timeout id
      @sendArea = null

      # When zooming starts or ends, we want to snap the markers to their proper place, so that the marker
      # animation doesn't interfere with the zoom animation.
      @map.on "zoomstart", =>
        @snapMarkers()
      @map.on "zoomend", =>
        @snapMarkers()
        # Move all the markers to the preZoomMarkers
        for id of @markers
          @preZoomMarkers[id] = @markers[id]
        @markers = {}
        # Tell the server about our new viewing area
        @updatePosition()

      @map.on "moveend", =>
        # Tell the server about our new viewing area
        @updatePosition()

      # The clean up task for removing markers that haven't been updated in 20 seconds
      @intervalId = setInterval(=>
        time = new Date().getTime()
        for id of @markers
          marker = @markers[id]
          if time - marker.lastSeen > 20000
            delete @markers[id]
            marker.remove()
      , 5000)

      @updatePosition()

    updatePosition: () ->
      # If we're moving around a lot, we don't want to overwhelm the server with viewing
      # area updates.  So, we wait 500ms before sending the update, and if no further
      # updates happen, then we do it.
      clearTimeout @sendArea if @sendArea
      @sendArea = setTimeout(=>
        @doUpdatePosition()
      , 500)

    doUpdatePosition: () ->
      @sendArea = null
      bounds = @map.getBounds()

      # Update the last area that was viewed in the local storage so we can load it next time.
      localStorage.lastArea = Storage.setLastArea {
        center: bounds.getCenter().wrap(-180, 180)
        zoom: @map.getZoom()
      }

      # Create the event
      event =
        event: "viewing-area"
        area:
          type: "Polygon"
          coordinates: [[[bounds.getSouthWest().lng, bounds.getSouthWest().lat],
                        [bounds.getNorthWest().lng, bounds.getNorthWest().lat],
                        [bounds.getNorthEast().lng, bounds.getNorthEast().lat],
                        [bounds.getSouthEast().lng, bounds.getSouthEast().lat],
                        [bounds.getSouthWest().lng, bounds.getSouthWest().lat]]]
          bbox: [bounds.getWest(), bounds.getSouth(), bounds.getEast(), bounds.getNorth()]

      # Send the viewing area upate to the server
      @ws.send(JSON.stringify(event))

    # Update the given marker positions
    updateMarkers: (features) ->
      for id of features
        feature = features[id]

        # If the marker was in the pre zoom markers, then we can promote it to the markers map
        marker = if @preZoomMarkers[feature.id]
          marker = @preZoomMarkers[feature.id]
          @markers[feature.id] = marker
          delete @preZoomMarkers[feature.id]
          marker
        else
          # Otherwise, just get it from the normal markers map
          @markers[feature.id]

        # Get the LatLng for the marker
        coordinates = feature.geometry.coordinates
        latLng = @wrapForMap(new Leaflet.LatLng(coordinates[1], coordinates[0]))

        # If the marker is already on the map
        if marker
          # Update it
          marker.update(feature, latLng)
        else
          # Otherwise create a new one
          marker = new Marker(@map, feature, latLng)
          @markers[feature.id] = marker

      # Clear any remaining pre zoom markers
      for id of @preZoomMarkers
        @preZoomMarkers[id].remove()
      @preZoomMarkers = {}

    # When the map stops zooming, we want to stop the animations of all the markers, otherwise they will very
    # slowly move to their new position on the zoomed map
    snapMarkers: ->
      for id of @markers
        @markers[id].snap()

    # Destroy the map
    destroy: ->
      try
        @map.remove()
        clearInterval(@intervalId)
      catch e

      # Handles when the user scrolls beyond the bounds of -180 and 180
    wrapForMap: (latLng) ->
      center = @map.getBounds().getCenter()
      offset = center.lng - center.wrap(-180, 180).lng
      if (offset != 0)
        return new Leaflet.LatLng(latLng.lat, latLng.lng + offset)
      else
        return latLng

  return Map

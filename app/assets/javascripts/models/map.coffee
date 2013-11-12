#
# The main map.  Manages displaying markers on the map, as well as responding to the user moving around and zooming
# on the map.
#
define ["md5.min", "webjars!leaflet.js"], (md5) ->

  class Map
    constructor: (ws) ->
      self = @

      # the map itself
      @map = L.map("map")
      new L.TileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        minZoom: 1
        maxZoom: 16
        attribution: "Map data © OpenStreetMap contributors"
      ).addTo(@map)

      # Focus on the last area that was viewed
      if (localStorage.lastArea)
        try
          lastArea = JSON.parse localStorage.lastArea
          @map.setView(lastArea.center, lastArea.zoom)
        catch e
          localStorage.removeItem("lastArea")
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
      @map.on "zoomstart", ->
        self.snapMarkers()
      @map.on "zoomend", ->
        self.snapMarkers()
        # Move all the markers to the preZoomMarkers
        for id of self.markers
          self.preZoomMarkers[id] = self.markers[id]
        self.markers = {}
        # Tell the server about our new viewing area
        self.updatePosition()

      @map.on "moveend", ->
        # Tell the server about our new viewing area
        self.updatePosition()

      # The clean up task for removing markers that haven't been updated in 20 seconds
      @intervalId = setInterval(->
        time = new Date().getTime()
        for id of self.markers
          marker = self.markers[id]
          if time - marker.lastSeen > 20000
            delete self.markers[id]
            self.map.removeLayer(marker)
      , 5000)

      @updatePosition()

    updatePosition: () ->
      # If we're moving around a lot, we don't want to overwhelm the server with viewing
      # area updates.  So, we wait 500ms before sending the update, and if no further
      # updates happen, then we do it.
      clearTimeout @sendArea if @sendArea
      self = @
      @sendArea = setTimeout(->
        self.doUpdatePosition()
      , 500)

    doUpdatePosition: () ->
      @sendArea = null
      bounds = @map.getBounds()

      # Update the last area that was viewed in the local storage so we can load it next time.
      localStorage.lastArea = JSON.stringify {
        center: bounds.getCenter().wrap(-180, 180)
        zoom: @map.getZoom()
      }

      # Create the event
      event =
        event: "viewing-area"
        area:
          type: "Polygon"
          coordinates: [[bounds.getSouthWest().lng, bounds.getSouthWest().lat],
                        [bounds.getNorthWest().lng, bounds.getNorthWest().lat],
                        [bounds.getNorthEast().lng, bounds.getNorthEast().lat],
                        [bounds.getSouthEast().lng, bounds.getSouthEast().lat],
                        [bounds.getSouthWest().lng, bounds.getSouthWest().lat]]
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
        latLng = @wrapForMap(new L.LatLng(coordinates[1], coordinates[0]))

        # If the marker is already on the map
        if marker
          # Update it
          @updateMarker(marker, feature, latLng)
        else
          # Otherwise create a new one
          marker = if feature.properties.count
            @createClusterMarker(feature, latLng)
          else
            @createUserMarker(feature, latLng)
          marker.addTo(@map)
          @markers[feature.id] = marker

        # Set the marker attributes
        marker.feature = feature
        marker.lastSeen = new Date().getTime()

      # Clear any remaining pre zoom markers
      for id of @preZoomMarkers
        @map.removeLayer(@preZoomMarkers[id])
      @preZoomMarkers = {}

    # Update an existing marker on the map
    updateMarker: (marker, feature, latLng) ->
      # Update the position
      marker.setLatLng(latLng)

      # If it's a cluster, check if the size of the cluster has changed
      if feature.properties.count
        if feature.properties.count != marker.feature.properties.count
          marker.setIcon(@createClusterMarkerIcon(marker.feature.properties.count))

      # Animate the marker - calculate how long it took to get from its last position
      # to current, and then set the CSS3 transition time to equal that
      lastUpdate = marker.feature.properties.timestamp
      updated = feature.properties.timestamp
      time = (updated - lastUpdate)
      if time > 0
        if time > 10000
          time = 10000
        @transition(marker._icon, time)
        @transition(marker._shadow, time) if marker._shadow
      marker.feature = feature

    # Create a user marker
    createUserMarker: (feature, latLng) ->
      userId = feature.id
      marker = new L.Marker(latLng,
        title: feature.id
      )

      # The popup should contain the gravatar of the user and their id
      marker.bindPopup("<p><img src='http://www.gravatar.com/avatar/" +
        md5(userId.toLowerCase()) + "'/></p><p>" + @escapeHtml(userId) + "</p>")
      return marker

    # Create a cluster marker
    createClusterMarker: (feature, latLng) ->
      marker = new L.Marker(latLng,
        icon: @createClusterMarkerIcon(feature.properties.count)
      )
      return marker

    # Create the icon for the cluster marker
    createClusterMarkerIcon: (count) ->
      # Style according to the number of users in the cluster
      className = if count < 10
        "cluster-marker-small"
      else if count < 100
        "cluster-marker-medium"
      else
        "cluster-marker-large"
      return new L.DivIcon(
        html: "<div><span>" + count + "</span></div>"
        className: "cluster-marker " + className
        iconSize: new L.Point(40, 40)
      )

    # When the map stops zooming, we want to stop the animations of all the markers, otherwise they will very
    # slowly move to their new position on the zoomed map
    snapMarkers: ->
      for id of @markers
        marker = @markers[id]
        @resetTransition marker._icon
        @resetTransition marker._shadow if marker._shadow

    # Reset the transition properties for the given element so that it doesn't animate
    resetTransition: (element) ->
      updateTransition = (element, prefix) ->
        element.style[prefix + "transition"] = ""
      updateTransition element, "-webkit-"
      updateTransition element, "-moz-"
      updateTransition element, "-o-"
      updateTransition element, ""

    # Reset the transition properties for the given element so that it animates when it moves
    transition: (element, time) ->
      updateTransition = (element, prefix) ->
        element.style[prefix + "transition"] = prefix + "transform " + time + "ms linear"
      updateTransition element, "-webkit-"
      updateTransition element, "-moz-"
      updateTransition element, "-o-"
      updateTransition element, ""

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
        return new L.LatLng(latLng.lat, latLng.lng + offset)
      else
        return latLng

    # Escape the given unsafe user input
    escapeHtml: (unsafe) ->
      return unsafe.replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;")

  return Map
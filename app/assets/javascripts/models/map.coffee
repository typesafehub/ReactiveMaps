#
# The main map.  Manages displaying markers on the map, as well as responding to the user moving around and zooming
# on the map.
#
define ["md5.min", "webjars!leaflet.js"], (md5) ->

  escapeHtml = (unsafe) ->
    return unsafe.replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;")

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

      @map.on "zoomstart", ->
        self.snapMarkers()
      @map.on "zoomend", ->
        self.snapMarkers()
        # merge in case there's already some preZoomMarkers there
        for id of self.markers
          self.preZoomMarkers[id] = self.markers[id]
        self.markers = {}
        self.updatePosition()
      @map.on "moveend", ->
        self.updatePosition()

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
      clearTimeout @sendArea if @sendArea
      self = @
      @sendArea = setTimeout(->
        self.doUpdatePosition()
      , 500)

    doUpdatePosition: () ->
      @sendArea = null
      bounds = @map.getBounds()
      localStorage.lastArea = JSON.stringify {
        center: bounds.getCenter().wrap(-180, 180)
        zoom: @map.getZoom()
      }
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

      @ws.send(JSON.stringify(event))

    updateMarkers: (features) ->
      for id of features
        feature = features[id]
        marker = if @preZoomMarkers[feature.id]
          marker = @preZoomMarkers[feature.id]
          @markers[feature.id] = marker
          delete @preZoomMarkers[feature.id]
          marker
        else
          @markers[feature.id]
        coordinates = feature.geometry.coordinates
        latLng = @wrapForMap(new L.LatLng(coordinates[1], coordinates[0]))
        if marker
          marker.setLatLng(latLng)
          lastUpdate = marker.feature.properties.timestamp
          updated = feature.properties.timestamp
          time = (updated - lastUpdate)
          if feature.properties.count
            # handle size change
            if feature.properties.count != marker.feature.properties.count
              marker.setIcon(@createClusterMarkerIcon(marker.feature.properties.count))
          if time > 0
            if time > 10000
              time = 10000
            @transition(marker._icon, time)
            @transition(marker._shadow, time) if marker._shadow
          marker.feature = feature
          marker.lastSeen = new Date().getTime()
        else
          marker = if feature.properties.count
            @createClusterMarker(feature, latLng)
          else
            @createUserMarker(feature, latLng)
          marker.addTo(@map)
          marker.feature = feature
          marker.lastSeen = new Date().getTime()
          @markers[feature.id] = marker
      # Clear any remaining pre zoom markers
      for id of @preZoomMarkers
        @map.removeLayer(@preZoomMarkers[id])
      @preZoomMarkers = {}

    createUserMarker: (feature, latLng) ->
      userId = feature.id
      marker = new L.Marker(latLng,
        title: feature.id
      )
      marker.bindPopup("<p><img src='http://www.gravatar.com/avatar/" + md5(userId.toLowerCase()) + "'/></p><p>" + escapeHtml(userId) + "</p>")
      return marker

    createClusterMarker: (feature, latLng) ->
      marker = new L.Marker(latLng,
        icon: @createClusterMarkerIcon(feature.properties.count)
      )
      return marker

    createClusterMarkerIcon: (count) ->
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

    # reset the transition properties for the given element so that it doesn't animate
    resetTransition: (element) ->
      updateTransition = (element, prefix) ->
        element.style[prefix + "transition"] = ""
      updateTransition element, "-webkit-"
      updateTransition element, "-moz-"
      updateTransition element, "-o-"
      updateTransition element, ""

    # reset the transition properties for the given element so that it animates when it moves
    transition: (element, time) ->
      updateTransition = (element, prefix) ->
        element.style[prefix + "transition"] = prefix + "transform " + time + "ms linear"
      updateTransition element, "-webkit-"
      updateTransition element, "-moz-"
      updateTransition element, "-o-"
      updateTransition element, ""

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

  return Map
require(["webjars!knockout.js", "webjars!bootstrap.js"], (ko) ->

  escapeHtml = (unsafe) ->
    return unsafe.replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;")

  class MainPageModel
    constructor: () ->
      self = @

      # the current user
      @email = ko.observable()

      @connecting = ko.observable()
      @disconnected = ko.observable(true)

      @mockGps = ko.observable()
      @gps = ko.observable()

      @closing = false

      if localStorage.email
        @email(localStorage.email)
        @connect()

    submitEmail: ->
      localStorage.email = @email()
      @connect()

    connect: ->
      self = @
      email = @email()
      @connecting("Connecting...")
      @disconnected(null)

      @ws = new WebSocket($("meta[name='websocketurl']").attr("content") + email);

      @ws.onopen = (event) ->
        self.connecting(null)
        self.map = new Map(self.ws)
        self.gps(new Gps(self.ws))

      @ws.onclose = (event) ->
        if (!event.wasClean && !self.closing)
          self.connect()
          self.connecting("Reconnecting...")
        else
          self.disconnected(true)
        self.closing = false
        self.map.destroy()
        self.mockGps().destroy() if self.mockGps()
        self.gps().destroy() if self.gps()

      # Handle the stream of feature updates
      @ws.onmessage = (event) ->
        json = JSON.parse(event.data)
        if json.event == "user-positions"
          self.map.updateMarkers(json.positions.features)

    disconnect: ->
      @closing = true
      @ws.close()

    toggleMockGps: ->
      if @mockGps()
        @mockGps().destroy()
        @mockGps(null)
        @gps(new Gps(@ws))
      else
        @gps().destroy() if @gps()
        @gps(null)
        @mockGps(new MockGps(@ws))

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

      # the sendArea timeout id
      @sendArea = null

      @map.on "zoomstart", ->
        self.snapMarkers()
      @map.on "zoomend", ->
        self.snapMarkers()
        self.updatePosition()
      @map.on "moveend", ->
        self.updatePosition()

      @intervalId = setInterval(->
        time = new Date().getTime()
        for id of self.markers
          marker = self.markers[id]
          if time - marker.feature.properties.timestamp > 30000
            delete self.markers[id]
            self.map.removeLayer(marker)
      , 30000)

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
        center: bounds.getCenter()
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
        marker = @markers[feature.id]
        coordinates = feature.geometry.coordinates
        latLng = new L.LatLng(coordinates[1], coordinates[0])
        if marker
          marker.setLatLng(latLng)
          lastUpdate = marker.feature.properties.timestamp
          updated = feature.properties.timestamp
          time = (updated - lastUpdate)
          if time > 0
            if time > 10000
              time = 10000
            @transition(marker._icon, time)
            @transition(marker._shadow, time)
          if feature.properties.count
            # handle size change
            if feature.properties.count != marker.feature.properties.count
              marker.setIcon(@createClusterMarkerIcon(marker.feature.properties.count))
          marker.feature = feature
        else
          marker = if feature["count"]
            @createClusterMarker(feature, latLng)
          else
            @createUserMarker(feature, latLng)
          marker.addTo(@map)
          marker.feature = feature
          @markers[feature.id] = marker

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
        html: "<div><span class='count'>" + count + "</span></div>"
        className: "cluster-marker " + className
        iconSize: new L.Point(40, 40)
      )


    # When the map stops zooming, we want to stop the animations of all the markers, otherwise they will very
    # slowly move to their new position on the zoomed map
    snapMarkers: ->
      for id of @markers
        marker = @markers[id]
        @resetTransition marker._icon
        @resetTransition marker._shadow

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

  class Gps
    constructor: (ws) ->
      @ws = ws
      @lastSent = 0
      @lastPosition = null
      self = @
      # Send position no more than every 2 seconds, no less than every 10 seconds
      @intervalId = setInterval(->
        self.sendPosition(self.lastPosition) if self.lastPosition
      , 10000)
      @watchId = navigator.geolocation.watchPosition((position) ->
        self.sendPosition(position)
      )

    sendPosition: (position) ->
      @lastPosition = position
      time = new Date().getTime()
      if time - @lastSent > 2000
        @lastSent = time
        @ws.send(JSON.stringify
          event: "user-moved"
          position:
            type: "Point"
            coordinates: [position.coords.longitude, position.coords.latitude]
        )

    destroy: ->
      navigator.geolocation.clearWatch(@watchId)
      clearInterval(@intervalId)

  # Used to manually specify your position if you are not using a GPS enabled device
  class MockGps
    constructor: (ws) ->
      self = @

      @ws = ws

      @map = L.map("mockGps")
      new L.TileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
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

      @marker = new L.Marker(position,
        draggable: true
      ).addTo(@map)

      @marker.on "dragend", ->
        self.sendPosition()

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


  model = new MainPageModel
  ko.applyBindings(model)

)


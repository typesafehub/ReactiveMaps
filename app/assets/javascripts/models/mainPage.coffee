#
# The main page.
#
# This class handles most of the user interactions with the buttons/menus/forms on the page, as well as manages
# the WebSocket connection.  It delegates to other classes to manage everything else.
#
define ["webjars!knockout.js", "./map", "./gps", "./mockGps"], (ko, Map, Gps, MockGps) ->

  class MainPageModel
    constructor: () ->
      self = @

      # the current user
      @email = ko.observable()

      # Contains a message to say that we're either connecting or reconnecting
      @connecting = ko.observable()
      @disconnected = ko.observable(true)

      # The MockGps model
      @mockGps = ko.observable()
      # The GPS model
      @gps = ko.observable()

      # If we're closing
      @closing = false

      # Load the previously entered email if set
      if localStorage.email
        @email(localStorage.email)
        @connect()

    # The user clicked connect
    submitEmail: ->
      localStorage.email = @email()
      @connect()

    # Connect function. Connects to the websocket, and sets up callbacks.
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
        # Need to handle reconnects in case of errors
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

  return MainPageModel


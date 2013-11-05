#
# The GPS interface.  Uses the HTML5 location API to watch the devices current position,
# and sends updates to the server.
#
define () ->
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

  return Gps
#
# The GPS interface.  Uses the HTML5 location API to watch the devices current position,
# and sends updates to the server.
#
define () ->
  class Gps
    # @ws The WebSocket to send updates to
    constructor: (ws) ->
      @ws = ws

      # When we last sent our position
      @lastSent = 0

      # The last position that we saw
      @lastPosition = null

      # Schedule a task to send the last position every 10 seconds
      @intervalId = setInterval(=>
        @sendPosition(@lastPosition) if @lastPosition
      , 10000)

      # Watch our position using the HTML5 geo location APIs
      @watchId = navigator.geolocation.watchPosition((position) =>
        @sendPosition(position)
      )

    # Send the given position
    sendPosition: (position) ->
      @lastPosition = position
      time = new Date().getTime()

      # Only send our position if we haven't sent a position update for 2 seconds
      if time - @lastSent > 2000
        @lastSent = time

        # Send the position update through the WebSocket
        @ws.send(JSON.stringify
          event: "user-moved"
          position:
            type: "Point"
            coordinates: [position.coords.longitude, position.coords.latitude]
        )

    # Stop sending our position and stop watching for position updates
    destroy: ->
      navigator.geolocation.clearWatch(@watchId)
      clearInterval(@intervalId)

  return Gps
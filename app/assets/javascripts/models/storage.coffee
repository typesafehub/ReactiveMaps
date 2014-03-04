#
# Reactive maps client side storage
#
define () ->
  return {

  # Get the last viewed area
  lastArea: ->
    if (localStorage.lastArea)
      try
        lastArea = JSON.parse localStorage.lastArea
        return lastArea
      catch e
        localStorage.removeItem("lastArea")

  # Set the last viewed area
  setLastArea: (lastArea) ->
    localStorage.lastArea = JSON.stringify lastArea

  }
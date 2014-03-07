#
# Handles actually rendering a marker, including DOM and CSS
#
define ["leaflet", "md5", "jquery"], (Leaflet, md5) ->
  # Escape the given unsafe user input
  escapeHtml = (unsafe) ->
    return unsafe.replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;")

  {
  # Render the popup for the given user
  renderPopup: (userId, distance) ->
    popup = "<p><img src='http://www.gravatar.com/avatar/" +
      md5(userId.toLowerCase()) + "'/></p><p>" + escapeHtml(userId) + "</p>"
    if (distance)
      popup + "<p>Travelled: " + Math.floor(distance) + "m</p>"
    else
      popup

  # Create the cluster marker icon
  createClusterMarkerIcon: (count) ->
    # Style according to the number of users in the cluster
    className = if count < 10
      "cluster-marker-small"
    else if count < 100
      "cluster-marker-medium"
    else
      "cluster-marker-large"
    return new Leaflet.DivIcon(
      html: "<div><span>" + count + "</span></div>"
      className: "cluster-marker " + className
      iconSize: new Leaflet.Point(40, 40)
    )


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

  }
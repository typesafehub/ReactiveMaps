#
# A marker class
#
define ["md5.min", "webjars!leaflet.js"], (md5) ->

  class Marker
    constructor: (map, feature, latLng) ->
      @map = map
      @feature = feature

      # If it has a count, it's a cluster
      if feature.properties.count
        @marker = new L.Marker(latLng,
          icon: createClusterMarkerIcon(feature.properties.count)
        )
      # Otherwise it's a user
      else
        userId = feature.id
        @marker = new L.Marker(latLng,
          title: feature.id
        )

        # The popup should contain the gravatar of the user and their id
        @marker.bindPopup("<p><img src='http://www.gravatar.com/avatar/" +
          md5(userId.toLowerCase()) + "'/></p><p>" + escapeHtml(userId) + "</p>")

      @lastSeen = new Date().getTime()
      @marker.addTo(map)

    # Update a marker with the given feature and latLng coordinates
    update: (feature, latLng) ->
      # Update the position
      @marker.setLatLng(latLng)

      # If it's a cluster, check if the size of the cluster has changed
      if feature.properties.count
        if feature.properties.count != @feature.properties.count
          @marker.setIcon(createClusterMarkerIcon(feature.properties.count))

      # Animate the marker - calculate how long it took to get from its last position
      # to current, and then set the CSS3 transition time to equal that
      lastUpdate = @feature.properties.timestamp
      updated = feature.properties.timestamp
      time = (updated - lastUpdate)
      if time > 0
        if time > 10000
          time = 10000
        transition(@marker._icon, time)
        transition(@marker._shadow, time) if @marker._shadow

      # Finally update feature
      @feature = feature
      @lastSeen = new Date().getTime()

    # Snap the marker to where it should be, ie stop animating
    snap: ->
      resetTransition @marker._icon
      resetTransition @marker._shadow if @marker._shadow

    # Remove the marker from the map
    remove: ->
      @map.removeLayer(@marker)

    # Create the icon for the cluster marker
    createClusterMarkerIcon = (count) ->
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

    # Reset the transition properties for the given element so that it doesn't animate
    resetTransition = (element) ->
      updateTransition = (element, prefix) ->
        element.style[prefix + "transition"] = ""
      updateTransition element, "-webkit-"
      updateTransition element, "-moz-"
      updateTransition element, "-o-"
      updateTransition element, ""

    # Reset the transition properties for the given element so that it animates when it moves
    transition = (element, time) ->
      updateTransition = (element, prefix) ->
        element.style[prefix + "transition"] = prefix + "transform " + time + "ms linear"
      updateTransition element, "-webkit-"
      updateTransition element, "-moz-"
      updateTransition element, "-o-"
      updateTransition element, ""

    # Escape the given unsafe user input
    escapeHtml = (unsafe) ->
      return unsafe.replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;")


  return Marker


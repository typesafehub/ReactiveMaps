# Mocks
class LatLng
  constructor: (lat, lng) ->
    @lat = lat
    @lng = lng
  wrap: () ->
    this

class MockLeaflet
  constructor: () ->
    self = @
    @TileLayer = class
      addTo: (map) ->
        self._addedTo = map

  _map: {
    markers: {}
    setView: (center, zoom) ->
      @_center = center
      @_zoom = zoom
    on: (event, fn) ->
    remove: () ->
    getBounds: () ->
      center = @_center
      {
        getCenter: () ->
          new LatLng(center[0], center[1])
      }
  }

  map: ->
    @_map

  LatLng: LatLng

class MockStorage
  area: null
  lastArea: ->
    @area
  setLastArea: (area) ->
    @area = area

MM = () ->
  class MockMarker
    constructor: (map, feature, latLng) ->
      map.markers[feature.id] = this
      @feature = feature
      @latLng = latLng
    update: (feature, latLng) ->
      @feature = feature
      @latLng = latLng

# Tests
testMap = (test) ->
  (done) ->

    # Create mocks
    leaflet = new MockLeaflet()
    storage = new MockStorage()

    # Mockout require js environment
    new Squire()
      .mock("marker", MM)
      .mock("storage", storage)
      .mock("leaflet", leaflet)
      .require ["javascripts/map/map"], (Map) ->
        test(leaflet, storage, Map, done)

describe "Map", ->

  # Mock features
  a =
    id: "a"
    geometry:
      coordinates: [1, 2]
  b =
    id: "b"
    geometry:
      coordinates: [3, 4]
  aUpdated =
    id: "a"
    geometry:
      coordinates: [5, 6]

  it "should create a tile layer", testMap (leaflet, storage, Map, done) ->
    new Map()
    assert.equal leaflet._map, leaflet._addedTo
    done()

  it "should initialise the map view", testMap (leaflet, storage, Map, done) ->
    new Map()
    assert.equal 0, leaflet._map._center[0]
    assert.equal 0, leaflet._map._center[1]
    assert.equal 2, leaflet._map._zoom
    done()

  it "should initialise the map view to the last stored area", testMap (leaflet, storage, Map, done) ->
    storage.setLastArea({center: [1, 2], zoom: 3})
    new Map()
    assert.equal 1, leaflet._map._center[0]
    assert.equal 2, leaflet._map._center[1]
    assert.equal 3, leaflet._map._zoom
    done()

  it "should create new markers for features", testMap (leaflet, storage, Map, done) ->
    map = new Map()
    map.updateMarkers [a, b]

    assert.equal a, leaflet._map.markers["a"].feature
    assert.equal 2, leaflet._map.markers["a"].latLng.lat
    assert.equal 1, leaflet._map.markers["a"].latLng.lng
    assert.equal b, leaflet._map.markers["b"].feature
    done()

  it "should update existing markers for features", testMap (leaflet, storage, Map, done) ->
    map = new Map()
    map.updateMarkers [a, b]
    map.updateMarkers [aUpdated, b]

    assert.equal aUpdated, leaflet._map.markers["a"].feature
    assert.equal 6, leaflet._map.markers["a"].latLng.lat
    assert.equal 5, leaflet._map.markers["a"].latLng.lng
    done()

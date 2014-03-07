#
# The main entry point into the client side. Creates a new main page model and binds it to the page.
#
require.config {
  paths: {
    mainPage: "./models/mainPage"
    map: "./map/map"
    marker: "./map/marker"
    markerRenderer: "./map/markerRenderer"
    gps: "./services/gps"
    mockGps: "./services/mockGps"
    storage: "./services/storage"
    md5: "./md5.min"
  }
  map: {
    "*": {
      knockout: "webjars!knockout.js"
      bootstrap: "webjars!bootstrap.js"
      leaflet: "webjars!leaflet.js"
      jquery: "webjars!jquery.js"
    }
  }
}

require ["knockout", "mainPage", "bootstrap"], (ko, MainPageModel) ->

  model = new MainPageModel
  ko.applyBindings(model)


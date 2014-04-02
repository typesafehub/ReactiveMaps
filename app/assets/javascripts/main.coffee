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
    bootstrap: "../lib/bootstrap/js/bootstrap"
    jquery: "../lib/jquery/jquery"
    knockout: "../lib/knockout/knockout"
    leaflet: "../lib/leaflet/leaflet"
  }
  shim: {
    bootstrap: {
      deps: ["jquery"],
      exports: "$"
    }
    jquery: {
      exports: "$"
    }
    knockout: {
      exports: "ko"
    }
  }
}

require ["knockout", "mainPage", "bootstrap"], (ko, MainPageModel) ->

  model = new MainPageModel
  ko.applyBindings(model)


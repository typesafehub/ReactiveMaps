#
# The main entry point into the client side. Creates a new main page model and binds it to the page.
#
require ["webjars!knockout.js", "./models/mainPage", "webjars!bootstrap.js"], (ko, MainPageModel) ->

  model = new MainPageModel
  ko.applyBindings(model)


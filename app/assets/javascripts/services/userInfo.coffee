define ["jquery"], ->
  {
    get: (email) ->
      $.getJSON("/user/" + email)
  }

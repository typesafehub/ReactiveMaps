define ["webjars!jquery.js"], () ->
    {
        putUserInfo: (email, data) ->
            $.ajax {
                method: "PUT"
                url: "/user/" + email
                data: JSON.stringify(data)
                contentType: "application/json"
            }
        getUserInfo: (email) ->
            $.getJSON("/user/" + email)
    }
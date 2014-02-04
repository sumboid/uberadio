@()(implicit r: RequestHeader)

$(function() {

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket

    var chatSocket = new WS("@routes.Application.chatSocket().webSocketURL()")

    var nickname = 'Anonymous'

    var sendMessage = function() {
        chatSocket.send(JSON.stringify(
            {
                name: nickname,
                text: $("#talk").val(),
                type: "message"
            }
        ))
        $("#talk").val('')
    }
    
    var ping = function() {
        chatSocket.send(JSON.stringify(
            {
                type: "ping"
            }
        ))
    }

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        $('#chat-messages').append('<li><div class="header"><b>' + 
                                    data.user + 
                                    '</b><small class="pull-right text-muted"><span class="glyphicon glyphicon-time"></span> ' + 
                                    data.time + 
                                    '</small></div><p>' +
                                    data.message + 
                                    '</p></li>')
        var element = $("#chat ul")[0];
        element.scrollTop = element.scrollHeight;
    }

    var handleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            e.preventDefault()
            if($("#talk").val().search("/nick") == 0) {
                nickname = $("#talk").val().slice(5)
                $("#talk").val('')
            } else {
                sendMessage()
            }
        }
    }

    $("#talk").keypress(handleReturnKey)

    chatSocket.onmessage = receiveEvent

    window.setInterval(function(){
        ping()
    }, 10000);
})
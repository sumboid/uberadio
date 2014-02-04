@()(implicit r: RequestHeader)

$(function() {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var playlistSocket = new WS("@routes.Application.playlistSocket().webSocketURL()")

    var ping = function() {
        playlistSocket.send(JSON.stringify(
            {
                type: "ping"
            }
        ))
    }

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        if(data.type == "playlist") {
            var playlist = ""
            $.each(data.playlist, function(ind, val) {
                var artist = val.artist
                var title = val.title
                var minutes = Math.floor(val.time / 60)
                var seconds = val.time % 60
                if(seconds < 10) seconds = "0" + seconds
                playlist = playlist + '<tr><td>' + artist + '</td><td>' + title + '</td><td>' + minutes + ":" + seconds + '</td></tr>'
            })
            $('#playlist-data tr').remove()
            $('#playlist-data').append(playlist)
        }
        else if(data.type = "currentsong") {
            var pos = data.track.position
            $('#playlist-data tr').attr('class', '')
            $('#playlist-data tr').eq(pos).attr('class', 'active')
        }
    }

    playlistSocket.onmessage = receiveEvent

    window.setInterval(function(){
        ping()
    }, 10000);
})
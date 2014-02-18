@()(implicit r: RequestHeader)

$(document).ready(function () {
  var stream = {
    title: 'Uberadio stream',
    mp3: 'http://uberadio.tk:8000/uberadio.mp3'
  }

  var ready = false
  $('#player').jPlayer( {
    ready: function (event) {
      ready = true;
      $(this).jPlayer('setMedia', stream)
    },

    pause: function() {
      $(this).jPlayer('clearMedia')
    },

    ended: function () {
      $(this).jPlayer('clearMedia')
      $(this).jPlayer('setMedia', stream)
      $(this).jPlayer('play')
    },

    error: function(event) {
      if(ready && event.jPlayer.error.type === $.jPlayer.error.URL_NOT_SET) {
        $(this).jPlayer('setMedia', stream).jPlayer('play')
      }
    },

    supplied: 'mp3',
    preload: 'none',
    wmode: 'window',
    swfPath: '/assets/swf/'
  })
}) 
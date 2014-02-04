var playing = false
$(document).ready(function () {
  var player = $('#player')[0]
  $('#player-button').click(function (e) {
    e.preventDefault()
    if(!playing) {
      $('#player-button-glyph').removeClass('glyphicon-play').addClass('glyphicon-stop')
      player.play()
      playing = true
    } else {
      $('#player-button span').removeClass('glyphicon-stop').addClass('glyphicon-play')
      
      player.pause()
      var src = player.src
      player.src = ''
      player.load()
      player.remove()
      $('#player-wrapper').html("<audio id='player' preload='none'><source src='http://uberadio.tk:8000/uberadio.ogg'></audio>")
      player = $('#player')[0]

      playing = false
    }
  })
})

$(document).ready(function () {
    var audioElement = $('#player')[0];
    audioElement.onended = audioElement.onerror = function() {
        audioElement.src = audioElement.currentSrc;
        audioElement.play();
    }
})
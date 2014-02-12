var player

var playing = false
var currentVolume = 60;

$(document).ready(function() {
  player = $('#player')[0]
})

$('#player').ready(function () {
  $('#player-button').click(function (e) {
    e.preventDefault()
    if(!playing) {
      $('#player-button-glyph').removeClass('glyphicon-play').addClass('glyphicon-stop')
      player.volume = currentVolume / 100
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
      player.onended = function() {
        console.log("END")
        // player.src = player.currentSrc;
        // player.play();
      }

      player.onerror = function() {
        console.log("ERROR")
        // player.src = player.currentSrc;
        // player.play();
      }
      playing = false
    }
  })
})

$('#player').ready(function () {
    player.onended = function() {
      console.log("END")
      // player.src = player.currentSrc;
      // player.play();
    }

    player.onerror = function() {
      console.log("ERROR")
      // player.src = player.currentSrc;
      // player.play();
    }

    var slide 
    var changeVol = function() {
      currentVolume = slider.getValue()
      player.volume = (slider.getValue() / 100)
    }

    $('#player-volume-button').popover({
      html: true,
      content: function() {
        return '<input id="player-volume" type="text" data-slider-min="0" data-slider-max="100" data-slider-step="1" data-slider-value="100"/>';
      }
    }).click(function() {
      slider = $('#player-volume').slider({
        tooltip: 'hide',
      }).on('slide', changeVol).data('slider')

      $('#player-volume').slider('setValue', currentVolume)
    })
})
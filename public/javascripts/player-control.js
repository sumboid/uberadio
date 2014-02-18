var STREAM = 'http://uberadio.tk:8000/uberadio.mp3'
var currentVolume = 60

$(document).ready(function() {
  var glyph = $('#player-button-glyph')
  $('#player-button').click(function (e) {
    if(glyph.hasClass('glyphicon-play')) {
      glyph.removeClass('glyphicon-play').addClass('glyphicon-stop')
      $('#player').jPlayer('volume', currentVolume / 100)
      $('#player').jPlayer('play')
    } else if(glyph.hasClass('glyphicon-stop')) {
      glyph.removeClass('glyphicon-stop').addClass('glyphicon-play')

      $('#player').jPlayer('pause')
    }
  })

    var slider
    var changeVol = function() {
      currentVolume = slider.getValue()
      $('#player').jPlayer('volume', currentVolume / 100)
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
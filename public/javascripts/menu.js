$('#playlist a').click(function (e) {
  e.preventDefault()
  $(this).tab('show')
})

$('#add a').click(function (e) {
  e.preventDefault()
  $(this).tab('show')
})

$('#rec a').click(function (e) {
  e.preventDefault()
  $(this).tab('show')
})

$(document).ready(function() {
  $('#chat-settings').popover()
  var chath = $('#chat-heading')
  var chatf = $('#chat-footer')
  var p = chath.height() + chath.padding().bottom + chath.padding().top + chatf.height() + chatf.padding().bottom + chatf.padding().top
  $('#chat').padding({bottom: p + 1})
  $('#chat-footer').margin({top: -p - 1})

  var staffnav = $('#stuff-nav')
  p = staffnav.height()
  $('#stuff').padding({bottom: p})
})
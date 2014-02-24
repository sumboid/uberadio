var loaded = 0
var count = 100

var searchLink = '/vk-search'
var personalLink = '/vk-personal'

var globalScope = false
var query = ''
var search = false
var link = personalLink

var end = false


function makeButtonActive() {
    $('#vk-load-more').removeAttr('disabled')
    $('#vk-load-more').click(function(e) {
        e.preventDefault()
        load()
    })
}

function makeButtonNotActive() {
    $('#vk-load-more').attr('disabled', 'disabled')
}

function check() {
    $.getJSON('/vk-check')
        .done(function(r) {
            if(r.state == 'connected') { createModalContent(); makeButtonActive(); load() }
            else showConnectButton()
        })
}

var createModalContent = function () {
    $('#vk-modal-body').append('<div class=\'row\' id=\'vk-modal-content\'></div>')
    $('#vk-modal-content').append('<div class=\'scrollable\'><table class=\'table table-hover\'><thead><tr><th>Artist</th><th>Title</th><th>Time</th><th></th></tr></thead><tbody id=\'vk-tracks-data\'></tbody></table></div>')
}

var clear = function() {
    $('#vk-tracks-data tr').remove()
    loaded = 0
}

var load = function() {
    var args

    if(search) args = { count: count, offset: loaded, q: query }
    else args = { count: count, offset: loaded }

    $.getJSON(link, args)
        .done(function(data) {
            var playlist
            if(data.tracks.length == 0) {
                makeButtonNotActive()
                end = true
                return
            }
            $.each(data.tracks, function(ind, val) {
                var artist = val.artist
                var title = val.title
                var id = val.owner_id + '_' + val.id
                var minutes = Math.floor(val.duration / 60)
                var seconds = val.duration % 60
                if(seconds < 10) seconds = "0" + seconds
                playlist = playlist + '<tr><td>' + artist + '</td>' + 
                '<td>' + title + '</td>' + 
                '<td>' + minutes + ":" + seconds + '</td>' + 
                '<td><a class=\'btn btn-xs btn-success\' href=\'#\' onclick="javascript:upload(\'' + id + '\')"><span class="glyphicon glyphicon-plus"></span></a></td></tr>'
            })

            $('#vk-tracks-data').append(playlist)
            loaded = loaded + count
        })
}

var searchload = function(wat, item) {
    $.getJSON(wat, { count: count, offset: loaded, q: $('#vk-search-field').val() })
        .done(function(data) {
            var playlist
            if(data.tracks.length == 0) {
                makeButtonNotActive()
                end = true
                return
            }
            $.each(data.tracks, function(ind, val) {
                var artist = val.artist
                var title = val.title
                var id = val.owner_id + '_' + val.id
                var minutes = Math.floor(val.duration / 60)
                var seconds = val.duration % 60
                if(seconds < 10) seconds = "0" + seconds
                playlist = playlist + '<tr><td>' + artist + '</td>' + 
                '<td>' + title + '</td>' + 
                '<td>' + minutes + ":" + seconds + '</td>' + 
                '<td><a class=\'btn btn-xs btn-success\' href=\'#\' onclick="javascript:upload(\'' + id + '\')"><span class="glyphicon glyphicon-plus"></span></a></td></tr>'
            })

            $('#vk-tracks-data').append(playlist)
            loaded = loaded + count
        })
}

var upload = function(url) {
    $.get('/vk-add/' + url)
}

var showConnectButton = function () {
    $('#vk-modal-body').append('<center><a id="vk-connect-button" target="_blank" href="/vk-connect" class="btn btn-lg btn-success">Connect</button></center>')
        $('#vk-connect-button').click(function (e) {
            e.preventDefault()
            var popup = window.open($(this).prop('href'))
            if(window.focus) {
                popup.focus()
            }

            var timer
            var crapListener = function () {
                if(popup && popup.closed) {
                    clearInterval(timer);
                    window.focus()
                    $('#vk-modal-body center').remove()
                    check()
                }
            }

            timer = setInterval(crapListener, 100)
        })
}

$(document).ready (function () {
    check()
})

$(document).ready(function() {
    var beginSearching = function(e) {
        query = encodeURIComponent($('#vk-search-field').val())
        if(query == '') {
            if(!search) {
                return
            }
            search = false
            link = personalLink
        }
        else {
            link = searchLink
            search = true
        }

        clear()
        load()
        makeButtonActive()
    }

    $('#vk-search-button').click(beginSearching)
    $('#vk-own-music-button').click(function (e) {
        link = personalLink
        search = false
        clear()
        load()
        makeButtonActive()
    })

    var handleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            beginSearching()
        }
    }

    $("#vk-search-field").keypress(handleReturnKey)
})
    var scloaded = 0
    var sccount = 100

    var sclink = '/sc-search'

    var scquery = ''

    var scend = false


    function scmakeButtonActive() {
        $('#sc-load-more').removeAttr('disabled')
        $('#sc-load-more').click(function(e) {
            e.preventDefault()
            scload()
        })
    }

    function scmakeButtonNotActive() {
        $('#sc-load-more').attr('disabled', 'disabled')
    }

    var sccreateModalContent = function () {
        $('#sc-modal-body').append('<div class=\'row\' id=\'sc-modal-content\'></div>')
        $('#sc-modal-content').append('<div class=\'scrollable\'><table class=\'table table-hover\'><thead><tr><th>Artist</th><th>Title</th><th>Time</th><th></th></tr></thead><tbody id=\'sc-tracks-data\'></tbody></table></div>')
    }

    var scclear = function() {
        $('#sc-tracks-data tr').remove()
        scloaded = 0
    }

    var scload = function() {
        var args = { count: sccount, offset: scloaded, q: scquery }

        $.getJSON(sclink, args)
            .done(function(data) {
                var playlist
                if(data.tracks.length == 0) {
                    scmakeButtonNotActive()
                    scend = true
                    return
                }
                $.each(data.tracks, function(ind, val) {
                    var artist = val.artist
                    var title = val.title
                    var id = val.id
                    var minutes = Math.floor(val.duration / 60)
                    var seconds = val.duration % 60
                    if(seconds < 10) seconds = "0" + seconds
                    playlist = playlist + '<tr><td>' + artist + '</td>' + 
                    '<td>' + title + '</td>' + 
                    '<td>' + minutes + ":" + seconds + '</td>' + 
                    '<td><a class=\'btn btn-xs btn-success\' href=\'#\' onclick="javascript:scupload(\'' + id + '\')"><span class="glyphicon glyphicon-plus"></span></a></td></tr>'
                })

                $('#sc-tracks-data').append(playlist)
                scloaded = scloaded + sccount
            })
    }
    var scupload = function(url) {
        $.get('/sc-add/' + url)
    }

$(document).ready(function() {

    var scbeginSearching = function(e) {
        scquery = encodeURIComponent($('#sc-search-field').val())
        if(scquery == '') {
            return
        }

        scclear()
        scload()
        scmakeButtonActive()
    }

    $('#sc-search-button').click(scbeginSearching)

    var schandleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            scbeginSearching()
        }
    }

    $("#sc-search-field").keypress(schandleReturnKey)
    sccreateModalContent()
    scload()
})
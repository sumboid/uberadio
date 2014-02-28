/****   Model    ****/

function SoundCloudModel(controller) {
    this.controller = controller
    this.loaded = 0
    this.count = 100

    this.loadLink = '/sc-search'

    this.query = ''
    this.end = false
}

SoundCloudModel.prototype.load = function() {
    console.log("load")
    var args = { count: this.count, offset: this.loaded, q: this.query }
    var controller = this.controller
    var self = this
    $.getJSON(this.loadLink, args)
        .done(function(data) {
            if(data.tracks.length == 0) {
                controller.onend()
            } else {
                controller.onload(data.tracks)
            }
            self.loaded = self.loaded + self.count
        })
}

SoundCloudModel.prototype.clear = function() {
    this.loaded = 0
    this.end = false
}

SoundCloudModel.prototype.add = function(id) {
    $.get('/sc-add/' + id)
}

/****    View    ****/

function SoundCloudView() {
}

SoundCloudView.prototype.init = function() {
    $('<div>', { class: 'row', id: 'sc-modal-content' })
        .appendTo('#sc-modal-body')
}

SoundCloudView.prototype.createModalContent = function() {
    $('<tr>')
        .appendTo(
    $('<thead>')
        .appendTo(
    $('<table>', { class: 'table table-hover' })
        .appendTo(
    $('<div>', { class: 'scrollable'})
        .appendTo('#sc-modal-content'))))

    $('<tbody>', { id: 'sc-tracks-data' }).appendTo('#sc-modal-content table')

    var header = [ 'Artist', 'Title', 'Time', '' ]
    $.each(header, function(i, v) { $('#sc-modal-content thead tr').append('<th>' + v + '</th>') })
}

SoundCloudView.prototype.clearTracks = function() { $('#sc-tracks-data tr').remove() }

SoundCloudView.prototype.appendTracks = function(tracks) {
    $.each(tracks, function(i, v) {
        var track = '<tr>' + 
                    '<td>' + v.artist + '</td>' +
                    '<td>' + v.title + '</td>' +
                    '<td>' + v.time + '</td>' +
                    '<td>' + '<a href="' + v.id + '" target="_blank" class="btn btn-xs btn-success sc-upload"><span class="glyphicon glyphicon-plus"></span></a>' + '</td>' +
                    '</tr>'
        $('#sc-tracks-data').append(track)
    })
}

SoundCloudView.prototype.moreButton = function(active) {
    if(active) {
        $('#sc-load-more').removeAttr('disabled')
    } else {
        $('#sc-load-more').attr('disabled', 'disabled')
    }
}

SoundCloudView.prototype.getMoreButton = function() {
    return $('#sc-load-more')
}

SoundCloudView.prototype.getSearchField = function() {
    return $('#sc-search-field')
}

SoundCloudView.prototype.getSearchFieldValue = function() {
    return $('#sc-search-field').val()
}

SoundCloudView.prototype.getSearchButton = function() {
    return $('#sc-search-button')
}

SoundCloudView.prototype.getAddElements = function() {
    return $('.sc-upload')
}

/**** Controller ****/

function SoundCloud() {
    this.model = new SoundCloudModel(this)
    this.view = new SoundCloudView
}

SoundCloud.prototype.init = function () {
    this.view.init()
    this.view.createModalContent()
    this.view.moreButton(false)
    this.startListen()
    this.model.load()
}

SoundCloud.prototype.onend = function(tracks) {
    this.view.moreButton(false)
}

SoundCloud.prototype.onload = function(tracks) {
    this.view.moreButton(true)
    var viewTracks = []
    $.each(tracks, function(i, v) {
        var track = {}
        track['artist'] = v.artist
        track['title'] = v.title

        var minutes = Math.floor(v.duration / 60)
        var seconds = v.duration % 60
        if(seconds < 10) seconds = '0' + seconds

        track['time'] = minutes + ':' + seconds
        track['id'] = v.id

        viewTracks.push(track)
    })
    this.view.appendTracks(viewTracks)

    this.updateListeners()
}

SoundCloud.prototype.updateListeners = function () {
    var addElements = this.view.getAddElements()
    var model = this.model

    addElements.click(function(e) {
        e.preventDefault()
        var id = $(this).attr('href')
        model.add(id)
    })
}

SoundCloud.prototype.startListen = function() {
    var searchButton = this.view.getSearchButton()
    var searchField = this.view.getSearchField()
    var view = this.view
    var model = this.model

    var search = function() {
        var query = encodeURIComponent(view.getSearchFieldValue().trim())
        if(query == '') return
        model.query = query
        view.clearTracks()
        model.clear()
        model.load()
    }

    searchField.keypress(function (e) {
        if(e.charCode == 13 || e.keyCode == 13)
            search()
    })

    searchButton.click(search)

    var moreButton = this.view.getMoreButton()
    moreButton.click(function(e) {
        e.preventDefault()
        model.load()
    })
}


$(document).ready(function() {
    var sc = new SoundCloud
    sc.init()
})

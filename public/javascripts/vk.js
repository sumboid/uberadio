/****   Model    ****/

function VKModel(controller) {
    this.controller = controller
    this.loaded = 0
    this.count = 100

    this.checkLink = '/vk-check'
    this.searchLink = '/vk-search'
    this.personalLink = '/vk-personal'
    this.loadLink = this.personalLink

    this.globalScope = false
    this.query = ''
    this.search = false

    this.end = false
}

VKModel.prototype.check = function() {
    var controller = this.controller
    $.getJSON(this.checkLink)
        .done(function(r) {
            console.log(controller.connected)
            if(r.state == 'connected') controller.connected()
            else controller.disconnected()
        })
}

VKModel.prototype.load = function() {
    var args

    if(this.search) args = { count: this.count, offset: this.loaded, q: this.query }
    else args = { count: this.count, offset: this.loaded }
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

VKModel.prototype.clear = function() {
    this.loaded = 0
    this.end = false
}

VKModel.prototype.toggleSearch = function() {
    this.clear()
    if(this.search) {
        this.search = false
        this.loadLink = this.personalLink
    } else {
        this.search = true
        this.loadLink = this.searchLink
    }
}

VKModel.prototype.add = function(id) {
    $.get('/vk-add/' + id)
}

/****    View    ****/

function VKView() {
}

VKView.prototype.init = function() {
    $('<div>', { class: 'row', id: 'vk-modal-content' })
        .appendTo('#vk-modal-body')
}

VKView.prototype.createModalContent = function() {
    $('<tr>')
        .appendTo(
    $('<thead>')
        .appendTo(
    $('<table>', { class: 'table table-hover' })
        .appendTo(
    $('<div>', { class: 'scrollable'})
        .appendTo('#vk-modal-content'))))

    $('<tbody>', { id: 'vk-tracks-data' }).appendTo('#vk-modal-content table')

    var header = [ 'Artist', 'Title', 'Time', '' ]
    $.each(header, function(i, v) { $('#vk-modal-content thead tr').append('<th>' + v + '</th>') })
}

VKView.prototype.clearTracks = function() { $('#vk-tracks-data tr').remove() }

VKView.prototype.appendTracks = function(tracks) {
    var tracks
    $.each(tracks, function(i, v) {
        var track = '<tr>' + 
                    '<td>' + v.artist + '</td>' +
                    '<td>' + v.title + '</td>' +
                    '<td>' + v.time + '</td>' +
                    '<td>' + '<a href="' + v.id + '" target="_blank" class="btn btn-xs btn-success vk-upload"><span class="glyphicon glyphicon-plus"></span></a>' + '</td>' +
                    '</tr>'
        $('#vk-tracks-data').append(track)
    })
}

VKView.prototype.connectButton = function(show) {
    if(show) {
        $('#vk-modal-content').append('<center><a id="vk-connect-button" target="_blank" href="/vk-connect" class="btn btn-lg btn-success">Connect</a></center>')
    } else {
        $('#vk-modal-content center').remove()
    }
}

VKView.prototype.moreButton = function(active) {
    if(active) {
        $('#vk-load-more').removeAttr('disabled')
    } else {
        $('#vk-load-more').attr('disabled', 'disabled')
    }
}

VKView.prototype.search = function(active) {
    if(active) {
        $('#vk-search-field').removeAttr('disabled')
        $('#vk-own-music-button').removeAttr('disabled')
        $('#vk-search-button').removeAttr('disabled')
    } else {
        $('#vk-search-field').attr('disabled', 'disabled')
        $('#vk-own-music-button').attr('disabled', 'disabled')
        $('#vk-search-button').attr('disabled', 'disabled')
    }
}

VKView.prototype.getMoreButton = function() {
    return $('#vk-load-more')
}

VKView.prototype.getConnectButton = function() {
    return $('#vk-connect-button')
}

VKView.prototype.getSearchField = function() {
    return $('#vk-search-field')
}

VKView.prototype.getSearchFieldValue = function() {
    return $('#vk-search-field').val()
}

VKView.prototype.getSearchButton = function() {
    return $('#vk-search-button')
}

VKView.prototype.getOwnMusicButton = function() {
    return $('#vk-own-music-button')
}

VKView.prototype.getAddElements = function() {
    return $('.vk-upload')
}

/**** Controller ****/

function VK() {
    this.model = new VKModel(this)
    this.view = new VKView

    this.isconnected = true
    this.issearch = false
}

VK.prototype.init = function () {
    this.view.init()
    this.view.moreButton(false)
    this.model.check(this)
}

VK.prototype.connected = function() {
    this.isconnected = true
    this.view.connectButton(false)
    this.view.search(true)
    this.view.createModalContent()
    this.startListen()
    this.model.load(this)
}

VK.prototype.disconnected = function() {
    if(this.isconnected) {
        this.view.connectButton(true)
        this.view.search(false)
        this.isconnected = false
    }

    var button = this.view.getConnectButton()
    var model = this.model
    var controller = this

    button.click(function (e) {
        e.preventDefault()
        var popup = window.open($(this).prop('href'))
        if(window.focus) {
            popup.focus()
        }

        var timer
        var crapListener = function () {
            if(popup && popup.closed) {
                clearInterval(timer)
                window.focus()
                model.check()
            }
        }

        timer = setInterval(crapListener, 100)
    })
}

VK.prototype.onend = function(tracks) {
    this.view.moreButton(false)
}

VK.prototype.onload = function(tracks) {
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
        track['id'] = v.owner_id  + '_' + v.id

        viewTracks.push(track)
    })
    this.view.appendTracks(viewTracks)

    this.updateListeners()
}

VK.prototype.updateListeners = function () {
    var addElements = this.view.getAddElements()
    var model = this.model

    addElements.click(function(e) {
        e.preventDefault()
        var id = $(this).attr('href')
        model.add(id)
    })
}

VK.prototype.startListen = function() {
    var ownMusicButton = this.view.getOwnMusicButton()
    var searchButton = this.view.getSearchButton()
    var searchField = this.view.getSearchField()
    var view = this.view
    var model = this.model
    var controller = this

    ownMusicButton.click(function(e) {
        if(model.search) {
            view.clearTracks()
            model.clear()
            model.toggleSearch()
            model.load()
        }
    })

    var search = function() {
        var query = encodeURIComponent(view.getSearchFieldValue().trim())
        console.log(query)
        if(model.search) {
            view.clearTracks()
            model.clear()
            if(query == '') {
                model.toggleSearch()
            } else {
                model.query = query
            }
            model.load()
        } else {
            if(query == '') return
            view.clearTracks()
            model.clear()
            model.toggleSearch()
            model.query = query
            model.load()
        }
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
    var vk = new VK
    vk.init()
})

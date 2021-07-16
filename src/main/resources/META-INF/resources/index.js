const params = new URLSearchParams(location.search)

function connect() {
    params.set('user', $('#user').val())

    location.search = "?" + params.toString();
}

function showConnectBtn() {
    $('#connect').show()
}
function connectTo(project) {
    var baseUrl = location.protocol.replace('http', 'ws') + location.host
    if (params.get('prod') == 'true') {
        baseUrl = 'wss://scene-api.dapp-craft.com'
    }
    const url = baseUrl + '/dc_panel/' + project
    console.log('connecting to ', url)
    const ws = new WebSocket(url);
    return ws;
}

var wSocket = null

function sendCommand(cmd, step) {
    if (wSocket != null) {
        const c = {
            cmd: cmd
        }
        if (cmd == 'broadcast') {
            if (step == 'startEvent') {
                c.data = JSON.stringify({cmd: "startEvent"})
            } else {
                c.data = JSON.stringify({cmd: step})
            }
        } else if (cmd == 'updateSceneParams') {
//            c.data = JSON.stringify({test: "datatest"})
           str = $('#sceneParams').val()
            console.log(str)
            try {
                JSON.parse(str);
            } catch (e) {
                console.log(e)
                alert(JSON.stringify(e));
                return false;
            }
            c.data = str
        } else if (cmd == 'updateParams') {
//            c.step = $('#paramName').val()
//            c.data = $('#paramValue').val()
            var pName = $('#paramName').val()
            var pVal = $('#paramValue').val()
            var pp = {}
            pp[pName] = pVal

            bcmd = {
                cmd: "updateSceneParams",
                data: JSON.stringify(pp)
            }

            c.cmd = 'broadcast'
            c.data = JSON.stringify(bcmd)
        }

        console.log(c)
        wSocket.send(JSON.stringify(c));
    } else {
        console.log('socket closed')
    }
}

$(document).ready(function () {
    const params = new URLSearchParams(location.search)
    var user = params.get("user")
    var project = params.get("project")

    console.log($('#user').value, user)
    if (user != null) {
        $('#user').val(user)
        wSocket = connectTo(project);
        wSocket.onopen = function (event) {
            $('#connect').hide()
            $('#user').hide()
        };
    } else {
        $('#btnStart').hide()
    }
})
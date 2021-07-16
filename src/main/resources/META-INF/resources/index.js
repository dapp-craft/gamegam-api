function connect() {
    const params = new URLSearchParams(location.search)

    params.set('user', $('#user').val())

    location.search = "?" + params.toString();
}

function showConnectBtn() {
    $('#connect').show()
}
function connectTo(project) {
    const ws = new WebSocket(location.protocol.replace('http', 'ws') + location.host + '/dc_panel/' + project);
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
            c.step = $('#paramName').val()
            c.data = $('#paramValue').val()
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
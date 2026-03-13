(function () {
    'use strict';

    const RECONNECT_DELAY = 5000;
    const connectCallbacks = [];
    let connected = false;
    let activeClient = null;

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    const client = new StompJs.Client({
        brokerURL: `${protocol}://${location.host}/ws`,
        reconnectDelay: RECONNECT_DELAY,
    });

    client.onConnect = function () {
        connected = true;
        activeClient = client;
        connectCallbacks.forEach(function (fn) { fn(client); });
    };

    client.onDisconnect = function () {
        connected = false;
    };

    client.onWebSocketError = function (error) {
        console.error('WebSocket error:', error);
    };

    client.onStompError = function (frame) {
        console.error('STOMP error:', frame.headers['message']);
    };

    window.stompClient = {
        onConnect: function (fn) {
            connectCallbacks.push(fn);
            if (connected && activeClient) {
                fn(activeClient);
            }
        }
    };

    client.activate();
})();

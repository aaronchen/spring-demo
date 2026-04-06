// WebSocket STOMP client — ES module wrapper.
// Exports onConnect(fn) for Stimulus controllers to register callbacks.
// Also assigns window.stompClient for backward compat with inline template scripts.

const RECONNECT_DELAY = 5000;
const connectCallbacks = [];
let connected = false;
let activeClient = null;

/**
 * Register a callback to run when the STOMP connection is established.
 * If already connected, the callback fires immediately.
 *
 * @param {function(StompJs.Client): void} fn
 */
export function onConnect(fn) {
    connectCallbacks.push(fn);
    if (connected && activeClient) {
        fn(activeClient);
    }
}

// Guard: StompJs is only loaded for authenticated users (UMD script in base.html).
// For unauthenticated pages, this module is still imported but does nothing.
if (typeof StompJs !== 'undefined') {
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

    client.activate();
}

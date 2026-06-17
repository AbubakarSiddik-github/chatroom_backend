const WebSocket = require('ws');
const { Client } = require('@stomp/stompjs');
const http = require('http');

const PRIVATE_ROOM_ID = "6a327752d2fe74f042ae5d1f";
const SENDER_USER_ID  = "6a327427f67726791ce3df02"; // jwtuser1
const SENDER_NAME     = "jwtuser1";
const PASSWORD        = "123456";
const EMAIL           = "jwtuser1@gmail.com";

function login(callback) {
    const postData = JSON.stringify({ identifier: EMAIL, password: PASSWORD });
    const options = {
        hostname: 'localhost', port: 8080, path: '/api/auth/login', method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(postData) }
    };
    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => { callback(JSON.parse(data).token); });
    });
    req.write(postData); req.end();
}

function runTest(token) {
    let receivedBySubscriber = false;

    // Subscriber (simulates User B receiving)
    const subscriber = new Client({
        webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
        connectHeaders: { Authorization: 'Bearer ' + token },
        reconnectDelay: 0,
        onConnect: () => {
            console.log('[Subscriber] Connected to PRIVATE room:', PRIVATE_ROOM_ID);
            subscriber.subscribe('/topic/room/' + PRIVATE_ROOM_ID, (frame) => {
                const msg = JSON.parse(frame.body);
                console.log('[Subscriber] RECEIVED:', JSON.stringify(msg, null, 2));
                receivedBySubscriber = true;
                subscriber.deactivate();
                sender.deactivate();
                console.log('\n=== H. WebSocket with PRIVATE room: TEST PASSED ===');
                process.exit(0);
            });
            setTimeout(() => startSender(token), 300);
        }
    });
    subscriber.activate();

    let sender;
    function startSender(token) {
        sender = new Client({
            webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
            connectHeaders: { Authorization: 'Bearer ' + token },
            reconnectDelay: 0,
            onConnect: () => {
                const payload = JSON.stringify({
                    roomId: PRIVATE_ROOM_ID,
                    senderId: SENDER_USER_ID,
                    senderName: SENDER_NAME,
                    content: 'Private message via WebSocket!',
                    type: 'TEXT'
                });
                sender.publish({ destination: '/app/chat/' + PRIVATE_ROOM_ID, body: payload });
                console.log('[Sender] Sent private message to /app/chat/' + PRIVATE_ROOM_ID);
            }
        });
        sender.activate();
    }

    setTimeout(() => {
        if (!receivedBySubscriber) {
            console.error('\n=== H. TEST FAILED: Message not received within 8s ===');
            process.exit(1);
        }
    }, 8000);
}

login(runTest);

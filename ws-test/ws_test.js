/**
 * Connectify WebSocket Integration Test
 * Simulates: User A subscribes to room, User B sends a message, User A receives it.
 */
const WebSocket = require('ws');
const { Client } = require('@stomp/stompjs');

// Get fresh JWT by logging in first via HTTP
const http = require('http');

const ROOM_ID = "6a327451f67726791ce3df03";
const USER_A_EMAIL = "jwtuser1@gmail.com";
const USER_B_EMAIL = "jwtuser1@gmail.com"; // same user for simplicity
const PASSWORD = "123456";

function login(callback) {
    const postData = JSON.stringify({ identifier: USER_A_EMAIL, password: PASSWORD });
    const options = {
        hostname: 'localhost',
        port: 8080,
        path: '/api/auth/login',
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(postData) }
    };
    const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => data += chunk);
        res.on('end', () => {
            const parsed = JSON.parse(data);
            console.log('[LOGIN] message:', parsed.message, '| token:', parsed.token ? parsed.token.substring(0, 30) + '...' : 'null');
            callback(parsed.token);
        });
    });
    req.write(postData);
    req.end();
}

function runTest(token) {
    let receivedByA = false;

    // ── User A: Subscriber ──────────────────────────────────────────────────
    const clientA = new Client({
        webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
        connectHeaders: { Authorization: 'Bearer ' + token },
        reconnectDelay: 0,
        onConnect: () => {
            console.log('[User A] Connected via WebSocket');
            clientA.subscribe('/topic/room/' + ROOM_ID, (frame) => {
                const msg = JSON.parse(frame.body);
                console.log('[User A] RECEIVED message:', JSON.stringify(msg));
                receivedByA = true;
                clientA.deactivate();
                clientB.deactivate();
                console.log('\n=== TEST PASSED: User A received message sent by User B in real-time ===');
                process.exit(0);
            });
            console.log('[User A] Subscribed to /topic/room/' + ROOM_ID);

            // Give User A 300ms to subscribe before User B sends
            setTimeout(() => startUserB(token), 300);
        },
        onStompError: (frame) => {
            console.error('[User A] STOMP error:', frame.headers['message']);
        }
    });
    clientA.activate();

    // ── User B: Sender ──────────────────────────────────────────────────────
    let clientB;
    function startUserB(token) {
        clientB = new Client({
            webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
            connectHeaders: { Authorization: 'Bearer ' + token },
            reconnectDelay: 0,
            onConnect: () => {
                console.log('[User B] Connected via WebSocket');
                const payload = JSON.stringify({
                    roomId: ROOM_ID,
                    senderId: '6a327427f67726791ce3df02',
                    senderName: 'jwtuser1',
                    content: 'Hello from User B via WebSocket!',
                    type: 'TEXT'
                });
                clientB.publish({ destination: '/app/chat/' + ROOM_ID, body: payload });
                console.log('[User B] Sent message to /app/chat/' + ROOM_ID);
            },
            onStompError: (frame) => {
                console.error('[User B] STOMP error:', frame.headers['message']);
            }
        });
        clientB.activate();
    }

    // Timeout guard
    setTimeout(() => {
        if (!receivedByA) {
            console.error('\n=== TEST FAILED: User A did not receive message within 8 seconds ===');
            process.exit(1);
        }
    }, 8000);
}

login(runTest);

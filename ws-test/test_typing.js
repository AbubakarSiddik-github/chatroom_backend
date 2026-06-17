/**
 * PHASE 2 TEST — Typing Indicator
 * User A subscribes to /topic/typing/{roomId}
 * User B sends typing-start then typing-stop
 * User A receives both events
 */
const WebSocket = require('ws');
const { Client } = require('@stomp/stompjs');
const http = require('http');

const ROOM_ID  = "6a327451f67726791ce3df03";
const EMAIL    = "jwtuser1@gmail.com";
const PASSWORD = "123456";
const SENDER_ID   = "6a327427f67726791ce3df02";
const SENDER_NAME = "jwtuser1";

function login(cb) {
    const body = JSON.stringify({ identifier: EMAIL, password: PASSWORD });
    const req = http.request({ hostname:'localhost', port:8080, path:'/api/auth/login',
        method:'POST', headers:{'Content-Type':'application/json','Content-Length':Buffer.byteLength(body)} },
        res => { let d=''; res.on('data',c=>d+=c); res.on('end',()=>cb(JSON.parse(d).token)); });
    req.write(body); req.end();
}

login(token => {
    let receivedStart = false, receivedStop = false;

    const subscriber = new Client({
        webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
        connectHeaders: { Authorization: 'Bearer ' + token },
        reconnectDelay: 0,
        onConnect: () => {
            subscriber.subscribe('/topic/typing/' + ROOM_ID, frame => {
                const ev = JSON.parse(frame.body);
                if (ev.typing === true)  { console.log('[A] RECEIVED typing=true  :', JSON.stringify(ev)); receivedStart = true; }
                if (ev.typing === false) { console.log('[A] RECEIVED typing=false :', JSON.stringify(ev)); receivedStop = true; }
                if (receivedStart && receivedStop) {
                    console.log('\n=== PHASE 2 TYPING INDICATOR: TEST PASSED ===');
                    process.exit(0);
                }
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
                const payload = JSON.stringify({ userId: SENDER_ID, username: SENDER_NAME });
                // typing start
                sender.publish({ destination: '/app/typing/' + ROOM_ID, body: payload });
                console.log('[B] Sent typing START');
                setTimeout(() => {
                    // typing stop
                    sender.publish({ destination: '/app/typing-stop/' + ROOM_ID, body: payload });
                    console.log('[B] Sent typing STOP');
                }, 500);
            }
        });
        sender.activate();
    }

    setTimeout(() => { console.error('=== PHASE 2 FAILED: timeout ==='); process.exit(1); }, 8000);
});

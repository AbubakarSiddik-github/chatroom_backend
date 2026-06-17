/**
 * PHASE 3 TEST — Read Receipts
 * A) Create a new message via REST
 * B) Mark it read via REST → check readBy in response
 * C) Subscribe to /topic/read/{roomId} and mark read via WS
 * D) Verify receipt received, MongoDB updated
 */
const WebSocket = require('ws');
const { Client } = require('@stomp/stompjs');
const http = require('http');

const ROOM_ID   = "6a327451f67726791ce3df03";
const SENDER_ID = "6a327427f67726791ce3df02";
const EMAIL     = "jwtuser1@gmail.com";
const PASSWORD  = "123456";

function login(cb) {
    const body = JSON.stringify({ identifier: EMAIL, password: PASSWORD });
    const req = http.request({ hostname:'localhost', port:8080, path:'/api/auth/login',
        method:'POST', headers:{'Content-Type':'application/json','Content-Length':Buffer.byteLength(body)} },
        res => { let d=''; res.on('data',c=>d+=c); res.on('end',()=>cb(JSON.parse(d).token)); });
    req.write(body); req.end();
}

function apiPost(path, body, token, cb) {
    const data = JSON.stringify(body);
    const req = http.request({ hostname:'localhost', port:8080, path, method:'POST',
        headers:{'Content-Type':'application/json','Content-Length':Buffer.byteLength(data),'Authorization':'Bearer '+token} },
        res => { let d=''; res.on('data',c=>d+=c); res.on('end',()=>cb(JSON.parse(d))); });
    req.write(data); req.end();
}

login(token => {
    // Step A — create a message
    apiPost('/api/messages', { roomId: ROOM_ID, senderId: SENDER_ID, content: 'Read receipt test msg', type: 'TEXT' }, token, msg => {
        console.log('[A] Message created. id:', msg.id);

        // Subscribe to /topic/read/{roomId} to catch WS broadcast
        const client = new Client({
            webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
            connectHeaders: { Authorization: 'Bearer ' + token },
            reconnectDelay: 0,
            onConnect: () => {
                client.subscribe('/topic/read/' + ROOM_ID, frame => {
                    const ev = JSON.parse(frame.body);
                    console.log('[WS] Read receipt broadcast:', JSON.stringify(ev));
                    console.log('\n=== PHASE 3 READ RECEIPT WS BROADCAST: TEST PASSED ===');
                    client.deactivate();
                    process.exit(0);
                });

                // Step B — mark read via REST
                setTimeout(() => {
                    apiPost('/api/messages/' + msg.id + '/read', {}, token, saved => {
                        console.log('[B] REST mark-read response:');
                        console.log('    readBy:', saved.readBy);
                        console.log('    lastReadAt:', saved.lastReadAt);
                        if (saved.readBy && saved.readBy.includes(SENDER_ID)) {
                            console.log('[C] MongoDB readBy persisted correctly');
                        }
                        // Step C — mark via WS
                        client.publish({ destination: '/app/read/' + msg.id, body: '{}' });
                        console.log('[D] Sent read event via WS /app/read/' + msg.id);
                    });
                }, 300);
            }
        });
        client.activate();
    });

    setTimeout(() => { console.error('=== PHASE 3 FAILED: timeout ==='); process.exit(1); }, 10000);
});

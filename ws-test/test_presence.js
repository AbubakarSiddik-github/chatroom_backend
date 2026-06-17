/**
 * PHASE 4 TEST — Online/Offline Presence
 * A) Subscribe to /topic/presence
 * B) User connects → ONLINE broadcast received
 * C) User disconnects → OFFLINE broadcast received
 */
const WebSocket = require('ws');
const { Client } = require('@stomp/stompjs');
const http = require('http');

const EMAIL    = "jwtuser1@gmail.com";
const PASSWORD = "123456";

function login(cb) {
    const body = JSON.stringify({ identifier: EMAIL, password: PASSWORD });
    const req = http.request({ hostname:'localhost', port:8080, path:'/api/auth/login',
        method:'POST', headers:{'Content-Type':'application/json','Content-Length':Buffer.byteLength(body)} },
        res => { let d=''; res.on('data',c=>d+=c); res.on('end',()=>cb(JSON.parse(d).token)); });
    req.write(body); req.end();
}

login(token => {
    let gotOnline = false, gotOffline = false;

    // Observer: watches /topic/presence
    const observer = new Client({
        webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
        connectHeaders: { Authorization: 'Bearer ' + token },
        reconnectDelay: 0,
        onConnect: () => {
            observer.subscribe('/topic/presence', frame => {
                const ev = JSON.parse(frame.body);
                console.log('[Observer] Presence event:', JSON.stringify(ev));
                if (ev.status === 'ONLINE')  { gotOnline  = true; console.log('[A/B] ONLINE  received — userId:', ev.userId, 'username:', ev.username); }
                if (ev.status === 'OFFLINE') { gotOffline = true; console.log('[C/D] OFFLINE received — lastSeen:', ev.lastSeen); }
                if (gotOnline && gotOffline) {
                    console.log('\n=== PHASE 4 PRESENCE: TEST PASSED ===');
                    observer.deactivate();
                    process.exit(0);
                }
            });

            // Subject: connects then disconnects after 1s
            setTimeout(() => {
                const subject = new Client({
                    webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
                    connectHeaders: { Authorization: 'Bearer ' + token },
                    reconnectDelay: 0,
                    onConnect: () => {
                        console.log('[Subject] Connected — ONLINE should broadcast');
                        // Disconnect after 800ms — triggers OFFLINE broadcast
                        setTimeout(() => {
                            console.log('[Subject] Disconnecting — OFFLINE should broadcast');
                            subject.deactivate();
                        }, 800);
                    }
                });
                subject.activate();
            }, 400);
        }
    });
    observer.activate();

    setTimeout(() => { console.error('=== PHASE 4 FAILED: timeout ==='); process.exit(1); }, 10000);
});

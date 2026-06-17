/**
 * Real-Time Message Test
 * User A subscribes to /topic/room/{roomId}
 * User B sends via /app/chat/{roomId}
 * User A receives the message instantly without REST
 */
const WebSocket = require('ws');
const { Client } = require('@stomp/stompjs');
const http = require('http');

const ROOM_ID = "6a327451f67726791ce3df03";

function login(email, password, cb) {
  const body = JSON.stringify({ identifier: email, password });
  const req = http.request({ hostname:'localhost', port:8080, path:'/api/auth/login',
    method:'POST', headers:{'Content-Type':'application/json','Content-Length':Buffer.byteLength(body)} },
    res => { let d=''; res.on('data',c=>d+=c); res.on('end',()=>cb(JSON.parse(d).token)); });
  req.write(body); req.end();
}

// Login user A
login("jwtuser1@gmail.com", "123456", (tokenA) => {
  // Login user B
  login("frontenduser", "test123", (tokenB) => {
    let received = false;

    // User A subscribes
    const userA = new Client({
      webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
      connectHeaders: { Authorization: 'Bearer ' + tokenA },
      reconnectDelay: 0,
      onConnect: () => {
        console.log('[A] Connected + subscribed to /topic/room/' + ROOM_ID);
        userA.subscribe('/topic/room/' + ROOM_ID, frame => {
          const msg = JSON.parse(frame.body);
          console.log('[A] RECEIVED message:', JSON.stringify({
            id: msg.id,
            senderName: msg.senderName,
            content: msg.content,
            type: msg.type
          }));
          received = true;
          console.log('\n=== REAL-TIME MESSAGE TEST: PASSED ===');
          process.exit(0);
        });

        // After A is subscribed, User B sends a message
        setTimeout(() => {
          const userB = new Client({
            webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-raw'),
            connectHeaders: { Authorization: 'Bearer ' + tokenB },
            reconnectDelay: 0,
            onConnect: () => {
              const payload = JSON.stringify({
                senderId: "6a32b38d1739f74e64a0d057",
                senderName: "frontenduser",
                content: "Real-time test message from User B!",
                type: "TEXT"
              });
              userB.publish({ destination: '/app/chat/' + ROOM_ID, body: payload });
              console.log('[B] Sent message via WebSocket');
            }
          });
          userB.activate();
        }, 500);
      }
    });
    userA.activate();

    setTimeout(() => { 
      if (!received) { console.error('=== REAL-TIME MESSAGE TEST: FAILED (timeout) ==='); process.exit(1); }
    }, 8000);
  });
});

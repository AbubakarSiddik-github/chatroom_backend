# Connectify — Real-Time Chat Application

> A full-stack, production-grade real-time chat application built with **Java Spring Boot**, **React**, **MongoDB**, **WebSocket (STOMP/SockJS)**, **JWT authentication**, and **Cloudinary** media management.

---

## 🌐 Live Demo

| Service | URL |
|---|---|
| **Frontend (Vercel)** | https://chatroom-fortend.vercel.app |
| **Backend API (Render)** | https://chatroom-backend-1-zbsi.onrender.com |

---

## ✨ Features

- 🔐 **JWT Authentication** — Secure register, login, and protected API routes
- 💬 **Private Chat** — One-on-one messaging between users
- 👥 **Group Chat** — Multi-user rooms with shared history
- ⚡ **Real-Time WebSocket** — STOMP over SockJS for live message delivery
- ⌨️ **Typing Indicator** — Live "user is typing…" indicator
- ✅ **Read Receipts** — Sent / Read status per message
- 🟢 **Online/Offline Presence** — Live user availability status
- 📷 **Image Sharing** — Upload and share images in chat
- 📄 **File Sharing** — Upload and share documents (PDF, DOCX, TXT, ZIP)
- 🗑️ **Permanent Media Deletion** — Deleted messages permanently remove files from Cloudinary
- 👤 **Profile Photo** — Upload, replace, or remove avatar (synced with Cloudinary)
- 📝 **Bio Update** — Add a personal bio to your profile
- ↩️ **Reply to Message** — Quote-reply any text, image, or file message
- 😊 **Emoji Picker** — Built-in lightweight emoji panel (no heavy libraries)
- 📱 **Responsive UI** — Works on mobile and desktop
- 🔒 **Secure Configuration** — All secrets via environment variables, nothing hardcoded

---

## 🛠️ Tech Stack

### Frontend
| Technology | Purpose |
|---|---|
| React 18 | UI framework |
| Vite | Build tool & dev server |
| Vanilla CSS | Custom styling (dark theme) |
| Axios | HTTP client with JWT interceptors |
| @stomp/stompjs + sockjs-client | WebSocket client |
| React Router v6 | Client-side routing |

### Backend
| Technology | Purpose |
|---|---|
| Java 17 | Language |
| Spring Boot 3 | Framework |
| Spring Security | JWT authentication & authorization |
| Spring WebSocket (STOMP) | Real-time messaging broker |
| MongoDB (Spring Data) | Database |
| Cloudinary Java SDK | Media upload & deletion |
| Lombok | Boilerplate reduction |

### Infrastructure
| Service | Purpose |
|---|---|
| MongoDB Atlas | Cloud database |
| Cloudinary | Image/file cloud storage |
| Render | Backend Docker deployment |
| Vercel | Frontend static deployment |

---

## 📁 Project Structure

```
chatroom_backend/
├── src/
│   └── main/
│       ├── java/com/connectify/backend/
│       │   ├── config/          # Security, CORS, WebSocket, Cloudinary config
│       │   ├── controller/      # REST + WebSocket controllers
│       │   ├── dto/             # Request/response DTOs
│       │   ├── model/           # MongoDB document models
│       │   ├── repository/      # Spring Data MongoDB repos
│       │   ├── security/        # JwtFilter, JwtUtil
│       │   └── service/         # Business logic services
│       └── resources/
│           └── application.properties   # Uses ${ENV_VAR} placeholders
├── Dockerfile
├── .dockerignore
├── pom.xml
└── .env.example

chatroom_fortend/
├── src/
│   ├── api/            # Axios instance (VITE_API_URL + JWT interceptor)
│   ├── components/     # ChatWindow, MessageInput, ProfileModal, etc.
│   ├── pages/          # LoginPage, RegisterPage, ChatPage
│   ├── services/       # ChatService, uploadService, websocketService
│   ├── styles/         # CSS per component
│   └── utils/          # helpers, roomHelpers
├── vercel.json         # SPA rewrite rules
├── vite.config.js
└── .env.example
```

---

## 🔑 Environment Variables

### Backend (Render / `.env` locally)
```env
MONGO_URI=<your_mongodb_atlas_connection_string>
JWT_SECRET=<your_strong_jwt_secret_key>
CLOUDINARY_CLOUD_NAME=<your_cloudinary_cloud_name>
CLOUDINARY_API_KEY=<your_cloudinary_api_key>
CLOUDINARY_API_SECRET=<your_cloudinary_api_secret>
```

### Frontend (Vercel / `.env.local` locally)
```env
VITE_API_URL=https://your-backend.onrender.com
VITE_WS_URL=https://your-backend.onrender.com/ws
```

> ⚠️ Never commit `.env` files. Always use environment variable settings in Render and Vercel dashboards for production.

---

## 🚀 Local Setup

### Prerequisites
- Java 17+
- Node.js 18+
- MongoDB Atlas account (or local MongoDB)
- Cloudinary account

### Backend
```bash
cd chatroom_backend

# Create .env file (copy from example)
cp .env.example .env
# Fill in your real values

# Run with Maven wrapper
./mvnw spring-boot:run

# Backend starts at http://localhost:8080
```

### Frontend
```bash
cd chatroom_fortend

# Install dependencies
npm install

# Create .env.local
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws

# Start dev server
npm run dev

# Frontend starts at http://localhost:5173
```

---

## ☁️ Deployment

### Backend → Render
1. Connect your GitHub backend repository to Render
2. Set **Runtime** to `Docker`
3. Set **Dockerfile Path** to `./Dockerfile`
4. Add all 5 environment variables in Render's Environment tab
5. Deploy — Render auto-deploys on every `git push`

### Frontend → Vercel
1. Connect your GitHub frontend repository to Vercel
2. Add environment variables `VITE_API_URL` and `VITE_WS_URL` in Vercel's Settings → Environment Variables
3. Deploy — Vercel auto-deploys on every `git push`

---

## 📸 Screenshots

> Screenshots to be added manually. Suggested captures:
> - `screenshots/login.png` — Login page
> - `screenshots/register.png` — Register page
> - `screenshots/chat-dashboard.png` — Main chat dashboard
> - `screenshots/private-chat.png` — Private chat conversation
> - `screenshots/group-chat.png` — Group chat room
> - `screenshots/image-sharing.png` — Image message in chat
> - `screenshots/profile-modal.png` — Profile editing modal
> - `screenshots/mobile.png` — Mobile responsive view

---

## 🔒 Security

- All secrets are stored as environment variables — never hardcoded
- JWT tokens expire after 24 hours
- Message delete/edit is protected — only the message owner can delete their own messages
- Profile updates are protected — only the current user can edit their own profile
- Cloudinary API Secret is never exposed to the frontend
- CORS is configured to allow only known frontend origins
- CSRF is disabled (stateless JWT-based API)

---

## 📊 API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | ❌ | Register new user |
| POST | `/api/auth/login` | ❌ | Login and get JWT |
| GET | `/api/auth/test` | ❌ | Health check |
| GET | `/api/health` | ❌ | Backend health |
| GET | `/api/users` | ✅ | Get all users |
| GET | `/api/users/me` | ✅ | Get current user |
| PUT | `/api/users/me/profile` | ✅ | Update bio |
| POST | `/api/users/me/avatar` | ✅ | Upload profile photo |
| DELETE | `/api/users/me/avatar` | ✅ | Delete profile photo |
| GET | `/api/rooms` | ✅ | Get all rooms |
| POST | `/api/rooms/private/{userId}` | ✅ | Start private chat |
| GET | `/api/messages/room/{roomId}` | ✅ | Get messages |
| DELETE | `/api/messages/{id}` | ✅ | Delete message + Cloudinary media |
| POST | `/api/uploads/image` | ✅ | Upload image |
| POST | `/api/uploads/file` | ✅ | Upload file |

---

## 💼 Resume Highlights

**Connectify – Real-Time Chat Application**
*Tech Stack: Java, Spring Boot, React.js, MongoDB, WebSocket/STOMP, JWT, Cloudinary, Render, Vercel*

- Built and deployed a full-stack real-time chat application with JWT authentication, private chat, group chat, typing indicators, read receipts, and online/offline presence
- Implemented WebSocket-based real-time messaging using Spring Boot STOMP broker and React STOMP client
- Integrated Cloudinary for image/file sharing with permanent media deletion on message delete to optimize free-tier storage usage
- Added user profile features including avatar upload/replace/delete, bio update, message reply threads, and emoji picker
- Deployed backend as a Docker container on Render, frontend on Vercel, and database on MongoDB Atlas using secure environment-based configuration with no hardcoded secrets

---

## 📅 Project Status

| Item | Status |
|---|---|
| Backend | ✅ Complete & Deployed |
| Frontend | ✅ Complete & Deployed |
| Authentication | ✅ Working |
| Real-time Messaging | ✅ Working |
| Media Upload/Delete | ✅ Working |
| Profile Photo/Bio | ✅ Working |
| Reply + Emoji | ✅ Working |
| Security | ✅ No secrets exposed |

**Last Updated:** June 18, 2026

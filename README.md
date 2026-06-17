# Connectify Backend

Connectify is a real-time chat application backend built with Spring Boot and WebSockets.

## Features
- Real-time messaging via STOMP/WebSockets.
- JWT-based authentication.
- Private (1-on-1) and Group chat rooms.
- Real-time read receipts, presence, and typing indicators.
- Cloudinary-based secure image and file attachments.
- MongoDB data persistence.

## Tech Stack
- **Framework**: Spring Boot 3
- **Database**: MongoDB (Spring Data MongoDB)
- **Security**: Spring Security + JWT
- **Real-Time**: Spring WebSocket (STOMP)
- **Media Uploads**: Cloudinary

## Environment Variables
Create a `.env` file at the root of the project with the following variables:
```env
MONGO_URI=your_mongodb_atlas_uri_here
JWT_SECRET=your_jwt_secret_here
CLOUDINARY_CLOUD_NAME=your_cloud_name_here
CLOUDINARY_API_KEY=your_api_key_here
CLOUDINARY_API_SECRET=your_api_secret_here
```
> **Security Note**: Never commit your `.env` file or hardcode secrets into the repository!

## How to Run
1. Ensure Java 17+ is installed.
2. Ensure you have configured your `.env` file.
3. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```
4. The server will run on `http://localhost:8080`.

## WebSocket Destinations
- **Connect Endpoint**: `/ws`
- **App Send Prefix**: `/app`
- **Topic Subscribe Prefix**: `/topic`
- **Topics**: `/topic/room/{roomId}`, `/topic/presence`, `/topic/typing/{roomId}`, `/topic/read/{roomId}`

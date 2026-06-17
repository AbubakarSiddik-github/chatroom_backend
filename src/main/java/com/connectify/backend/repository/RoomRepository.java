package com.connectify.backend.repository;

import com.connectify.backend.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RoomRepository extends MongoRepository<Room, String> {
    // Fetch all rooms of a given type (used to find PRIVATE rooms for duplicate check)
    List<Room> findByType(String type);
}

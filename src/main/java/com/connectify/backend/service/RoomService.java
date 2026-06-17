package com.connectify.backend.service;

import com.connectify.backend.model.Room;
import com.connectify.backend.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(String id) {
        return roomRepository.findById(id);
    }

    public Room updateRoom(Room room) {
        return roomRepository.save(room);
    }

    public void deleteRoom(String id) {
        roomRepository.deleteById(id);
    }

    /**
     * Finds an existing PRIVATE room shared between exactly two specific users.
     * Fetches all PRIVATE rooms then filters in Java to avoid complex Mongo queries.
     */
    public Optional<Room> findPrivateRoom(String userIdA, String userIdB) {
        return roomRepository.findByType("PRIVATE")
                .stream()
                .filter(room -> room.getMemberIds() != null
                        && room.getMemberIds().size() == 2
                        && room.getMemberIds().contains(userIdA)
                        && room.getMemberIds().contains(userIdB))
                .findFirst();
    }
}

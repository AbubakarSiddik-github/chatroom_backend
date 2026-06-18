package com.connectify.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connectify.backend.dto.UploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/zip"
    );

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;  // 5 MB
    private static final long MAX_FILE_SIZE  = 10 * 1024 * 1024; // 10 MB

    // ── Upload image ──────────────────────────────────────────────────────────
    public UploadResponse uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        if (file.getSize() > MAX_IMAGE_SIZE) throw new IllegalArgumentException("Image size exceeds 5 MB limit");
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Invalid image type. Allowed: jpg, png, gif, webp");

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "image",
                "folder", "connectify/images"
        ));
        return buildResponse(file, result);
    }

    // ── Upload raw file ───────────────────────────────────────────────────────
    public UploadResponse uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File size exceeds 10 MB limit");
        if (!ALLOWED_FILE_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Invalid file type. Allowed: pdf, doc, docx, txt, zip");

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "raw",
                "folder", "connectify/files"
        ));
        return buildResponse(file, result);
    }

    // ── Upload avatar ─────────────────────────────────────────────────────────
    public UploadResponse uploadAvatar(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        if (file.getSize() > MAX_IMAGE_SIZE) throw new IllegalArgumentException("Avatar size exceeds 5 MB limit");
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Invalid image type. Allowed: jpg, png, gif, webp");

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "image",
                "folder", "connectify/avatars"
        ));
        return buildResponse(file, result);
    }

    // ── Delete by publicId ────────────────────────────────────────────────────
    // resourceType: "image" or "raw"
    public void deleteByPublicId(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType != null ? resourceType : "image"
            ));
        } catch (Exception e) {
            // Log but don't block — media is still removed from our DB
            System.err.println("[Cloudinary] Failed to delete asset: " + publicId + " — " + e.getMessage());
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────
    private UploadResponse buildResponse(MultipartFile file, Map<?, ?> result) {
        return UploadResponse.builder()
                .url(result.get("secure_url").toString())
                .publicId(result.get("public_id").toString())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
    }
}

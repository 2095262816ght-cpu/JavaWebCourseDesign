package com.shop.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage Service Seam (Codebase Design: Local-substitutable dependency)
 * Hides concrete file I/O, disk directories, and URL resolution behind a small interface.
 */
public interface StorageService {
    /**
     * Store incoming multipart file and return its public URL
     */
    String store(MultipartFile file);

    /**
     * Load stored file as Spring Resource
     */
    Resource loadAsResource(String filename);

    /**
     * Delete stored file by filename
     */
    void delete(String filename);
}

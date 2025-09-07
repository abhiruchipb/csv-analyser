package com.abhiruchi.csvanalyzer.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists() && !dir.mkdirs()) {
                return ResponseEntity.status(500).body("Failed to create upload directory: " + uploadDir);
            }

            String filePath = uploadDir + File.separator + file.getOriginalFilename();
            File dest = new File(filePath);

            // Overwrite if exists
            if (dest.exists() && !dest.delete()) {
                return ResponseEntity.status(500).body("Failed to overwrite existing file: " + filePath);
            }

            file.transferTo(dest);

            return ResponseEntity.ok("File uploaded successfully: " + filePath);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("File upload failed: " + e.getMessage());
        }
    }
}
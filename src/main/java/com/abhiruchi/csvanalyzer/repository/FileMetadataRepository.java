package com.abhiruchi.csvanalyzer.repository;

import com.abhiruchi.csvanalyzer.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {}
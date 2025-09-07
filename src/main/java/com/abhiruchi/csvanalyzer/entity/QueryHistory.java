package com.abhiruchi.csvanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    @Column(length = 5000)
    private String answer;

    private LocalDateTime askedAt;

    @ManyToOne
    private FileMetadata file; // link query to file
}
package com.abhiruchi.csvanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ColumnStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String columnName;
    private String columnType;

    private String countVal;
    private String meanVal;
    private String medianVal;
    private String modeVal;
    private String minVal;
    private String maxVal;
    private String uniqueValues;
    private String sampleValues;

    @ManyToOne
    private FileMetadata file; // link stats to file
}
package com.abhiruchi.csvanalyzer.repository;

import com.abhiruchi.csvanalyzer.entity.ColumnStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColumnStatsRepository extends JpaRepository<ColumnStats, Long> {}
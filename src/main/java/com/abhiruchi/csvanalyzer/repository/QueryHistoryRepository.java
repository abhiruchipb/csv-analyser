package com.abhiruchi.csvanalyzer.repository;

import com.abhiruchi.csvanalyzer.entity.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {}
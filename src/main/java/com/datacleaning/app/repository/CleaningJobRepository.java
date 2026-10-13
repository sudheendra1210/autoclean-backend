package com.datacleaning.app.repository;
import com.datacleaning.app.entity.CleaningJob;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CleaningJobRepository extends JpaRepository<CleaningJob, String> {}

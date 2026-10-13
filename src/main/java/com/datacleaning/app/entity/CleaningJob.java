package com.datacleaning.app.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="cleaning_jobs")
public class CleaningJob {
    @Id @Column(length=36) private String sessionId;
    @Column(nullable=false) private String filename;
    @Column(nullable=false) private int originalRows;
    @Column(nullable=false) private int columnsCount;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    protected CleaningJob() {}
    public CleaningJob(String sessionId, String filename, int originalRows, int columnsCount) { this.sessionId=sessionId; this.filename=filename; this.originalRows=originalRows; this.columnsCount=columnsCount; }
    public String getSessionId(){return sessionId;} public String getFilename(){return filename;}
    public int getOriginalRows(){return originalRows;} public int getColumnsCount(){return columnsCount;} public Instant getCreatedAt(){return createdAt;}
}

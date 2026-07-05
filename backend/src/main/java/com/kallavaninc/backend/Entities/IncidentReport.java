package com.kallavaninc.backend.Entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    // Optional text-based location
    private String location;

    // From IncidentPin mapping
    private Double latitude;
    private Double longitude;

    // Maps to your existing User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User reportedBy;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;


    @Column(columnDefinition = "TEXT")
    private String blockRemark;

    // Stores the array of photo strings (either Base64 or URLs from cloud storage)
    @ElementCollection
    @CollectionTable(name = "incident_photos", joinColumns = @JoinColumn(name = "incident_id"))
    @Column(name = "photo_data", columnDefinition = "TEXT")
    private List<String> photos = new ArrayList<>();

    // Automatically set the timestamp when the report is first saved
    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    // --- Enums to match Frontend Types ---

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum ReportStatus {
        PENDING, VERIFIED, IN_PROGRESS, RESOLVED, REJECTED, BLOCKED
    }

    // --- NEW AI FIELDS ---
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_urgency")
    private String aiUrgency;

    @Column(name = "duplicate_of_id")
    private Long duplicateOfId;

    // ... (Add these getters and setters at the bottom of the file) ...
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiUrgency() { return aiUrgency; }
    public void setAiUrgency(String aiUrgency) { this.aiUrgency = aiUrgency; }

    public Long getDuplicateOfId() { return duplicateOfId; }
    public void setDuplicateOfId(Long duplicateOfId) { this.duplicateOfId = duplicateOfId; }


    @Column(name = "assigned_team_id")
    private Long assignedTeamId;

    // Add these getters/setters at the bottom of the file
    public Long getAssignedTeamId() { return assignedTeamId; }
    public void setAssignedTeamId(Long assignedTeamId) { this.assignedTeamId = assignedTeamId; }

    // --- Getters and Setters ---
    public String getBlockRemark() {
        return blockRemark;
    }

    public void setBlockRemark(String blockRemark) {
        this.blockRemark = blockRemark;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }

    public Double getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(Double aiConfidence) { this.aiConfidence = aiConfidence; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
}
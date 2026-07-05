package com.kallavaninc.backend.Controllers;

import com.kallavaninc.backend.Entities.IncidentReport;
import com.kallavaninc.backend.Services.GoogleTokenVerifier;
import com.kallavaninc.backend.Services.IncidentReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:5173")
public class IncidentReportController {

    @Autowired
    private IncidentReportService incidentReportService;

    @Autowired
    private GoogleTokenVerifier tokenVerifier;

    public static class IncidentReportRequest {
        private String title;
        private String description;
        private String severity;
        private List<String> photos;
        private Double latitude;
        private Double longitude;
        private String location;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public List<String> getPhotos() { return photos; }
        public void setPhotos(List<String> photos) { this.photos = photos; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    // --- 1. THE FIX: ADDED AI FIELDS TO THE RESPONSE OBJECT ---
    public record ReportResponse(
            Long id,
            String title,
            String description,
            String severity,
            String location,
            String reportedBy,
            String submittedAt,
            List<String> photos,
            Long assignedTeamId,
            String status,
            String aiSummary,      // NEW
            String aiUrgency,      // NEW
            Long duplicateOfId     // NEW
    ) {}

    @PostMapping("/incident")
    public ResponseEntity<?> submitReport(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody IncidentReportRequest request
    ) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }
            String idToken = authHeader.substring(7);
            GoogleTokenVerifier.GoogleTokenInfo tokenInfo = tokenVerifier.verify(idToken);
            String userEmail = tokenInfo.email();

            IncidentReport savedReport = incidentReportService.createReport(request, userEmail);
            return ResponseEntity.ok(savedReport);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving report: " + e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingReports(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }
            tokenVerifier.verify(authHeader.substring(7));

            List<IncidentReport> pendingReports = incidentReportService.getPendingReports();

            // --- 2. THE FIX: SEND AI DATA TO THE FRONTEND ---
            List<ReportResponse> responseData = pendingReports.stream()
                    .map(report -> new ReportResponse(
                            report.getId(),
                            report.getTitle() != null ? report.getTitle() : "Untitled Report",
                            report.getDescription() != null ? report.getDescription() : "No Description",
                            report.getSeverity() != null ? report.getSeverity().name() : "LOW",
                            report.getLocation() != null ? report.getLocation() : "Unknown Location",
                            report.getReportedBy() != null ? report.getReportedBy().getEmail() : "Unknown User",
                            report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : LocalDateTime.now().toString(),
                            report.getPhotos() != null ? report.getPhotos() : new ArrayList<>(),
                            report.getAssignedTeamId(),
                            report.getStatus() != null ? report.getStatus().name() : "PENDING",
                            report.getAiSummary(),    // <-- AI Data
                            report.getAiUrgency(),    // <-- AI Data
                            report.getDuplicateOfId() // <-- AI Data
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseData);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body("Invalid token");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching reports: " + e.getMessage());
        }
    }

    public static class StatusUpdateRequest {
        private String status;
        private String blockRemark;
        public String getStatus() { return status; }
        public String getBlockRemark() { return blockRemark; }
        public void setBlockRemark(String blockRemark) { this.blockRemark = blockRemark; }
        public void setStatus(String status) { this.status = status; }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getReportsByStatus(@PathVariable String status, @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing Authorization header");
            }
            tokenVerifier.verify(authHeader.substring(7));

            IncidentReport.ReportStatus reportStatus = IncidentReport.ReportStatus.valueOf(status.toUpperCase());
            List<IncidentReport> reports = incidentReportService.getReportsByStatus(reportStatus);

            List<ReportResponse> responseData = reports.stream()
                    .map(report -> new ReportResponse(
                            report.getId(),
                            report.getTitle() != null ? report.getTitle() : "Untitled Report",
                            report.getDescription() != null ? report.getDescription() : "No Description",
                            report.getSeverity() != null ? report.getSeverity().name() : "LOW",
                            report.getLocation() != null ? report.getLocation() : "Unknown Location",
                            report.getReportedBy() != null ? report.getReportedBy().getEmail() : "Unknown User",
                            report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : LocalDateTime.now().toString(),
                            report.getPhotos() != null ? report.getPhotos() : new ArrayList<>(),
                            report.getAssignedTeamId(),
                            report.getStatus() != null ? report.getStatus().name() : "PENDING",
                            report.getAiSummary(),    // <-- AI Data
                            report.getAiUrgency(),    // <-- AI Data
                            report.getDuplicateOfId() // <-- AI Data
                    )).collect(Collectors.toList());

            return ResponseEntity.ok(responseData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status parameter");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateReportStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            tokenVerifier.verify(authHeader.substring(7));

            IncidentReport.ReportStatus newStatus = IncidentReport.ReportStatus.valueOf(request.getStatus().toUpperCase());
            IncidentReport updated = incidentReportService.updateReportStatus(id, newStatus, request.getBlockRemark());
            return ResponseEntity.ok("Status updated to " + updated.getStatus());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating status: " + e.getMessage());
        }
    }

    public static class AssignRequest {
        private Long teamId;
        public Long getTeamId() { return teamId; }
        public void setTeamId(Long teamId) { this.teamId = teamId; }
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignTask(
            @PathVariable Long id,
            @RequestBody AssignRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            tokenVerifier.verify(authHeader.substring(7));

            IncidentReport assigned = incidentReportService.assignReport(id, request.getTeamId());
            return ResponseEntity.ok("Assigned successfully to team " + request.getTeamId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error assigning task: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeTask(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null) return ResponseEntity.status(401).body("Unauthorized");
            tokenVerifier.verify(authHeader.substring(7));

            IncidentReport updated = incidentReportService.updateReportStatus(id, IncidentReport.ReportStatus.RESOLVED, null);

            return ResponseEntity.ok("Task marked as RESOLVED");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/my-reports")
    public ResponseEntity<?> getMyReports(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }

            String idToken = authHeader.substring(7);
            GoogleTokenVerifier.GoogleTokenInfo tokenInfo = tokenVerifier.verify(idToken);
            String userEmail = tokenInfo.email();

            List<IncidentReport> myReports = incidentReportService.getReportsByUserEmail(userEmail);

            List<ReportResponse> responseData = myReports.stream()
                    .map(report -> new ReportResponse(
                            report.getId(),
                            report.getTitle() != null ? report.getTitle() : "Untitled Report",
                            report.getDescription() != null ? report.getDescription() : "No Description",
                            report.getSeverity() != null ? report.getSeverity().name() : "LOW",
                            report.getLocation() != null ? report.getLocation() : "Unknown Location",
                            report.getReportedBy() != null ? report.getReportedBy().getEmail() : "Unknown User",
                            report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : LocalDateTime.now().toString(),
                            report.getPhotos() != null ? report.getPhotos() : new ArrayList<>(),
                            report.getAssignedTeamId(),
                            report.getStatus() != null ? report.getStatus().name() : "PENDING",
                            report.getAiSummary(),    // <-- AI Data
                            report.getAiUrgency(),    // <-- AI Data
                            report.getDuplicateOfId() // <-- AI Data
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseData);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body("Invalid token");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching my reports: " + e.getMessage());
        }
    }

    @GetMapping("/export/all")
    public ResponseEntity<?> exportAllRecent(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null) return ResponseEntity.status(401).body("Unauthorized");
            tokenVerifier.verify(authHeader.substring(7));

            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            List<IncidentReport> recentReports = incidentReportService.getAllRecentReports(yesterday);

            List<ReportResponse> exportData = recentReports.stream().map(report -> new ReportResponse(
                    report.getId(), report.getTitle(), report.getDescription(),
                    report.getSeverity() != null ? report.getSeverity().name() : "LOW",
                    report.getLocation(), report.getReportedBy() != null ? report.getReportedBy().getEmail() : "Unknown",
                    report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : "",
                    report.getPhotos(), report.getAssignedTeamId(),
                    report.getStatus() != null ? report.getStatus().name() : "PENDING",
                    report.getAiSummary(),
                    report.getAiUrgency(),
                    report.getDuplicateOfId()
            )).toList();

            return ResponseEntity.ok(exportData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating export");
        }
    }

    @GetMapping("/export/team/{teamId}")
    public ResponseEntity<?> exportTeamRecent(@PathVariable Long teamId, @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null) return ResponseEntity.status(401).body("Unauthorized");
            tokenVerifier.verify(authHeader.substring(7));

            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            List<IncidentReport> recentReports = incidentReportService.getTeamRecentReports(teamId, yesterday);

            List<ReportResponse> exportData = recentReports.stream().map(report -> new ReportResponse(
                    report.getId(), report.getTitle(), report.getDescription(),
                    report.getSeverity() != null ? report.getSeverity().name() : "LOW",
                    report.getLocation(), report.getReportedBy() != null ? report.getReportedBy().getEmail() : "Unknown",
                    report.getSubmittedAt() != null ? report.getSubmittedAt().toString() : "",
                    report.getPhotos(), report.getAssignedTeamId(),
                    report.getStatus() != null ? report.getStatus().name() : "PENDING",
                    report.getAiSummary(),
                    report.getAiUrgency(),
                    report.getDuplicateOfId()
            )).toList();

            return ResponseEntity.ok(exportData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating team export");
        }
    }
}
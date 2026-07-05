package com.kallavaninc.backend.Services;

import com.kallavaninc.backend.Controllers.IncidentReportController.IncidentReportRequest;
import com.kallavaninc.backend.Entities.IncidentReport;
import com.kallavaninc.backend.Entities.User;
import com.kallavaninc.backend.Repositories.IncidentReportRepository;
import com.kallavaninc.backend.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class IncidentReportService {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private IncidentReportRepository incidentReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.kallavaninc.backend.Repositories.ResponderTeamRepository responderTeamRepository;


    // Define where the images will be saved
    private final String UPLOAD_DIR = "images/";

    public IncidentReportService() {
        // Create the images directory when the server starts if it doesn't exist
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (Exception ex) {
            System.err.println("Could not create image upload directory: " + ex.getMessage());
        }
    }

    @Transactional
    public IncidentReport createReport(IncidentReportRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        IncidentReport report = new IncidentReport();
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());

        try {
            report.setSeverity(IncidentReport.Severity.valueOf(request.getSeverity().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid severity level provided: " + request.getSeverity());
        }

        // --- EXISTING IMAGE PROCESSING LOGIC ---
        List<String> imageUrls = new ArrayList<>();
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            for (String base64Data : request.getPhotos()) {
                try {
                    String base64Image = base64Data;
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.split(",")[1];
                    }
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                    String fileName = java.util.UUID.randomUUID().toString() + ".jpg";
                    java.nio.file.Path destinationFile = java.nio.file.Paths.get(UPLOAD_DIR + fileName);
                    java.nio.file.Files.write(destinationFile, imageBytes);
                    String imageUrl = "http://localhost:8080/images/" + fileName;
                    imageUrls.add(imageUrl);
                } catch (Exception e) {
                    System.err.println("Failed to process image: " + e.getMessage());
                }
            }
        }
        report.setPhotos(imageUrls);

        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setLocation(request.getLocation());
        report.setReportedBy(user);
        report.setStatus(IncidentReport.ReportStatus.PENDING);

        // --- NEW AI PROCESSING LOGIC ---
        try {
            // 1. Fetch reports from the last 24 hours
            java.time.LocalDateTime yesterday = java.time.LocalDateTime.now().minusHours(24);
            java.util.List<IncidentReport> recentReports = incidentReportRepository.findBySubmittedAtAfter(yesterday);

            // 2. Call Gemini (Assuming geminiService is injected in this class's constructor)
            GeminiService.AiResult ai = geminiService.analyze(report, recentReports);

            // 3. Attach AI insights
            report.setAiSummary(ai.summary());
            report.setAiUrgency(ai.urgency());
            report.setDuplicateOfId(ai.duplicateOfId());
            report.setAiConfidence(88.5); // Hardcoded UI confidence score
        } catch (Exception e) {
            System.err.println("AI Processing skipped or failed: " + e.getMessage());
            // Fallback so the report still saves even if you run out of API quota
            report.setAiSummary("AI analysis temporarily unavailable.");
            report.setAiUrgency("MEDIUM");
        }

        // Save everything to the database
        return incidentReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<IncidentReport> getPendingReports() {
        return incidentReportRepository.findByStatusOrderBySubmittedAtDesc(IncidentReport.ReportStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<IncidentReport> getReportsByStatus(IncidentReport.ReportStatus status) {
        return incidentReportRepository.findByStatusOrderBySubmittedAtDesc(status);
    }

    @Transactional
    public IncidentReport updateReportStatus(Long id, IncidentReport.ReportStatus newStatus, String blockRemark) {
        IncidentReport report = incidentReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(newStatus);

        // If a remark is provided (like when blocking a task), save it!
        if (blockRemark != null) {
            report.setBlockRemark(blockRemark);
        }

        return incidentReportRepository.save(report);
    }


    @Transactional
    public IncidentReport assignReport(Long reportId, Long teamId) {
        IncidentReport report = incidentReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(IncidentReport.ReportStatus.VERIFIED);
        report.setAssignedTeamId(teamId);

        // ONLY attempt to update the team's active tasks if a teamId was actually provided!
        // This prevents the server from crashing when we pass 'null' to unassign a task.
        if (teamId != null) {
            com.kallavaninc.backend.Entities.ResponderTeam team = responderTeamRepository.findById(teamId)
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            team.setActiveTasks(team.getActiveTasks() + 1);
            responderTeamRepository.save(team);
        }

        return incidentReportRepository.save(report);
    }

    // Expose the repository for the export queries in the controller
    public com.kallavaninc.backend.Repositories.IncidentReportRepository getIncidentReportRepository() {
        return incidentReportRepository;
    }

    @Transactional(readOnly = true)
    public List<IncidentReport> getReportsByUserEmail(String email) {
        return incidentReportRepository.findByReportedBy_EmailOrderBySubmittedAtDesc(email);
    }

    // --- NEW EXPORT SERVICE METHODS ---

    @Transactional(readOnly = true)
    public List<IncidentReport> getAllRecentReports(LocalDateTime since) {
        return incidentReportRepository.findBySubmittedAtAfter(since);
    }

    @Transactional(readOnly = true)
    public List<IncidentReport> getTeamRecentReports(Long teamId, LocalDateTime since) {
        return incidentReportRepository.findByAssignedTeamIdAndSubmittedAtAfter(teamId, since);
    }
}
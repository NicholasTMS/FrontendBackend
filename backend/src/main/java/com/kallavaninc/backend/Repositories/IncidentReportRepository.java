package com.kallavaninc.backend.Repositories;

import com.kallavaninc.backend.Entities.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    // Optional: Custom query to find reports by a specific user
    List<IncidentReport> findByReportedByIdOrderBySubmittedAtDesc(Long userId);
    List<IncidentReport> findByStatusOrderBySubmittedAtDesc(IncidentReport.ReportStatus status);
    List<IncidentReport> findBySubmittedAtAfter(LocalDateTime date);
    List<IncidentReport> findByAssignedTeamIdAndSubmittedAtAfter(Long teamId, LocalDateTime date);
    // Add this query to find all reports by a specific user's email, sorted newest first
    List<IncidentReport> findByReportedBy_EmailOrderBySubmittedAtDesc(String email);
}
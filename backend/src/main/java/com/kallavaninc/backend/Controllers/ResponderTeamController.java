package com.kallavaninc.backend.Controllers;

import com.kallavaninc.backend.Entities.ResponderTeam;
import com.kallavaninc.backend.Repositories.ResponderTeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "http://localhost:5173")
public class ResponderTeamController {

    @Autowired
    private ResponderTeamRepository teamRepository;

    @GetMapping
    public ResponseEntity<List<ResponderTeam>> getAllTeams() {
        return ResponseEntity.ok(teamRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<ResponderTeam> createTeam(@RequestBody ResponderTeam team) {
        if (team.getStatus() == null) team.setStatus("Available");
        if (team.getMembers() == null) team.setMembers(3); // Default placeholder
        if (team.getActiveTasks() == null) team.setActiveTasks(0);
        return ResponseEntity.ok(teamRepository.save(team));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        teamRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
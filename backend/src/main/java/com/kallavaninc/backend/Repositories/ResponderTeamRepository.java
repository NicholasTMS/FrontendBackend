package com.kallavaninc.backend.Repositories;

import com.kallavaninc.backend.Entities.ResponderTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponderTeamRepository extends JpaRepository<ResponderTeam, Long> {
}
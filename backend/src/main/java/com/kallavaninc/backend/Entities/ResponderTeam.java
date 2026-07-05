package com.kallavaninc.backend.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "responder_teams")
public class ResponderTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String area;
    private String status = "Available";
    private Integer members = 0;
    private Integer activeTasks = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getMembers() { return members; }
    public void setMembers(Integer members) { this.members = members; }
    public Integer getActiveTasks() { return activeTasks; }
    public void setActiveTasks(Integer activeTasks) { this.activeTasks = activeTasks; }
}
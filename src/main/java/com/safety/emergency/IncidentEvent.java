package com.safety.emergency;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="incident_status_history")
public class IncidentEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public Emergency emergency;
    @ManyToOne(optional=false) public User changedBy;
    public String previousStatus;
    @Column(nullable=false) public String newStatus;
    @Column(length=2000) public String note;
    public Instant createdAt=Instant.now();
}

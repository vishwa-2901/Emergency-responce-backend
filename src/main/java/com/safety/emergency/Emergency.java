package com.safety.emergency;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="emergencies", indexes={@Index(columnList="status,severity"),@Index(columnList="createdAt")})
public class Emergency {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public User reporter;
    @ManyToOne public User responder;
    @Column(nullable=false) public String type;
    @Column(nullable=false) public String severity;
    @Column(nullable=false,length=2000) public String description;
    public Double latitude;
    public Double longitude;
    public String locationText;
    @Column(nullable=false) public String status="REPORTED";
    @Column(length=2000) public String guidance;
    public Instant createdAt=Instant.now();
    public Instant updatedAt=Instant.now();
    public Instant resolvedAt;
}

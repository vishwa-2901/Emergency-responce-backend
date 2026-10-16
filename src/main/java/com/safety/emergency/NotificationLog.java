package com.safety.emergency;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="notification_logs")
public class NotificationLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public Emergency emergency;
    @Column(nullable=false) public String channel="SIMULATED";
    @Column(nullable=false) public String outcome="NOT_SENT";
    @Column(nullable=false) public String detail="No delivery provider configured";
    public Instant createdAt=Instant.now();
}

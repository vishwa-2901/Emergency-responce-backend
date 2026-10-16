package com.safety.emergency;

import jakarta.persistence.*;

@Entity @Table(name="emergency_contacts")
public class EmergencyContact {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public User owner;
    @Column(nullable=false) public String name;
    @Column(nullable=false) public String phone;
    public String relationship;
}

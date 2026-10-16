package com.safety.emergency;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="users", uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public String name;
    @Column(nullable=false, unique=true) public String email;
    @Column(nullable=false) public String passwordHash;
    @Column(nullable=false) public String role="USER";
    public String phone;
    public Instant createdAt=Instant.now();
}

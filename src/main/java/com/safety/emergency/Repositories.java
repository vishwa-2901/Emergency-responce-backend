package com.safety.emergency;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

interface UserRepository extends JpaRepository<User,Long> { Optional<User> findByEmail(String email); }
interface EmergencyRepository extends JpaRepository<Emergency,Long> { List<Emergency> findByReporterIdOrderByCreatedAtDesc(Long id); List<Emergency> findByResponderIdOrderByCreatedAtDesc(Long id); List<Emergency> findAllByOrderByCreatedAtDesc(); }
interface ContactRepository extends JpaRepository<EmergencyContact,Long> { List<EmergencyContact> findByOwnerId(Long id); }
interface EventRepository extends JpaRepository<IncidentEvent,Long> { List<IncidentEvent> findByEmergencyIdOrderByCreatedAtAsc(Long id); }
interface NotificationRepository extends JpaRepository<NotificationLog,Long> { List<NotificationLog> findByEmergencyId(Long id); }

package com.safety.emergency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api")
class EmergencyController {
    private final UserRepository users;private final EmergencyRepository emergencies;private final EventRepository events;private final NotificationRepository notifications;
    EmergencyController(UserRepository u,EmergencyRepository e,EventRepository h,NotificationRepository n){users=u;emergencies=e;events=h;notifications=n;}
    record Report(@NotBlank String type,@NotBlank @Size(max=2000) String description,@DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,@DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,String locationText,String severity){}
    record StatusUpdate(@NotBlank String status,String note){}
    @PostMapping("/emergencies") ResponseEntity<?> create(@Valid @RequestBody Report r,Authentication a){
        User u=user(a); Emergency e=new Emergency();e.reporter=u;e.type=r.type();e.description=r.description();e.latitude=r.latitude();e.longitude=r.longitude();e.locationText=r.locationText();
        e.severity=severity(r.type(),r.description(),r.severity());e.guidance=guidance(e.type,e.severity);e.status="REPORTED"; emergencies.save(e);event(e,u,null,"REPORTED","Report created");NotificationLog n=new NotificationLog();n.emergency=e;notifications.save(n);
        return ResponseEntity.status(201).body(dto(e));
    }
    @GetMapping("/emergencies/my") List<?> mine(Authentication a){return emergencies.findByReporterIdOrderByCreatedAtDesc(user(a).id).stream().map(this::dto).toList();}
    @GetMapping("/emergencies/{id}") Object get(@PathVariable Long id,Authentication a){Emergency e=load(id);checkOwner(e,a);return dto(e);}
    @GetMapping("/emergencies/{id}/history") List<?> history(@PathVariable Long id,Authentication a){Emergency e=load(id);checkOwner(e,a);return events.findByEmergencyIdOrderByCreatedAtAsc(id).stream().map(x->Map.of("from",Objects.toString(x.previousStatus,""),"to",x.newStatus,"note",Objects.toString(x.note,""),"at",x.createdAt,"by",x.changedBy.name)).toList();}
    @PostMapping("/guidance/analyze") Object analyze(@RequestBody Report r){String s=severity(r.type(),r.description(),r.severity());return Map.of("suggestedSeverity",s,"guidance",guidance(r.type(),s),"method","Rule-based decision support");}
    @GetMapping("/emergencies/{id}/guidance") Object guidance(@PathVariable Long id,Authentication a){Emergency e=load(id);checkOwner(e,a);return Map.of("guidance",e.guidance,"severity",e.severity,"method","Rule-based decision support");}
    @PatchMapping("/admin/emergencies/{id}/assign") Object assign(@PathVariable Long id,@RequestBody Map<String,Long> body,Authentication a){Emergency e=load(id);User r=users.findById(body.get("responderId")).orElseThrow();if(!r.role.equals("RESPONDER"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Target account is not a responder");e.responder=r;transition(e,"ASSIGNED",user(a),"Assigned to "+r.name);return dto(emergencies.save(e));}
    @PatchMapping({"/admin/emergencies/{id}/status","/responder/incidents/{id}/status"}) Object update(@PathVariable Long id,@Valid @RequestBody StatusUpdate u,Authentication a){Emergency e=load(id);if(a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("ROLE_ADMIN"))&&!Objects.equals(e.responder==null?null:e.responder.id,user(a).id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);transition(e,u.status().toUpperCase(),user(a),u.note());return dto(emergencies.save(e));}
    @GetMapping("/responder/incidents") List<?> assigned(Authentication a){return emergencies.findByResponderIdOrderByCreatedAtDesc(user(a).id).stream().map(this::dto).toList();}
    @PostMapping("/responder/incidents/{id}/notes") Object note(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication a){Emergency e=load(id);User actor=user(a);if(!Objects.equals(e.responder==null?null:e.responder.id,actor.id)&&!actor.role.equals("ADMIN"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);String note=body.getOrDefault("note","").trim();if(note.isEmpty()||note.length()>2000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Note is required and must be under 2000 characters");event(e,actor,e.status,e.status,note);return Map.of("saved",true);}
    @GetMapping("/admin/emergencies") List<?> all(){return emergencies.findAllByOrderByCreatedAtDesc().stream().map(this::dto).toList();}
    @GetMapping("/admin/dashboard") Object dashboard(){List<Emergency> all=emergencies.findAll();return Map.of("total",all.size(),"reported",all.stream().filter(e->e.status.equals("REPORTED")).count(),"active",all.stream().filter(e->List.of("ACKNOWLEDGED","ASSIGNED","IN_PROGRESS").contains(e.status)).count(),"resolved",all.stream().filter(e->e.status.equals("RESOLVED")).count());}
    private User user(Authentication a){return users.findByEmail(a.getName()).orElseThrow();}
    private Emergency load(Long id){return emergencies.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found"));}
    private void checkOwner(Emergency e,Authentication a){if(!e.reporter.email.equals(a.getName())&&a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("ROLE_ADMIN"))&&!Objects.equals(e.responder==null?null:e.responder.email,a.getName()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
    private void event(Emergency e,User by,String from,String to,String note){IncidentEvent h=new IncidentEvent();h.emergency=e;h.changedBy=by;h.previousStatus=from;h.newStatus=to;h.note=note;events.save(h);}
    private void transition(Emergency e,String to,User by,String note){Set<String> ok=switch(e.status){case "REPORTED"->Set.of("ACKNOWLEDGED","ASSIGNED","CANCELLED");case "ACKNOWLEDGED"->Set.of("ASSIGNED","IN_PROGRESS","CANCELLED");case "ASSIGNED"->Set.of("ACKNOWLEDGED","IN_PROGRESS","CANCELLED");case "IN_PROGRESS"->Set.of("RESOLVED");default->Set.of();};if(!ok.contains(to))throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid status transition");String old=e.status;e.status=to;e.updatedAt=Instant.now();if(to.equals("RESOLVED"))e.resolvedAt=Instant.now();event(e,by,old,to,note);}
    private Map<String,Object> dto(Emergency e){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.id);m.put("type",e.type);m.put("description",e.description);m.put("severity",e.severity);m.put("status",e.status);m.put("latitude",e.latitude);m.put("longitude",e.longitude);m.put("locationText",e.locationText);m.put("guidance",e.guidance);m.put("reporter",e.reporter.name);m.put("responder",e.responder==null?null:e.responder.name);m.put("createdAt",e.createdAt);return m;}
    static String severity(String type,String d,String supplied){String s=(type+" "+d).toLowerCase();if(s.contains("unconscious")||s.contains("trapped")||s.contains("severe")||s.contains("not breathing")||s.contains("fire spreading"))return "CRITICAL";if(supplied!=null&&List.of("LOW","MEDIUM","HIGH","CRITICAL").contains(supplied.toUpperCase()))return supplied.toUpperCase();if(type.equalsIgnoreCase("Fire")||type.equalsIgnoreCase("Medical"))return "HIGH";return "MEDIUM";}
    static String guidance(String type,String severity){return "Rule-based guidance: move to a safe location if possible, share your exact location, and contact local emergency services for urgent danger. Do not put yourself at risk while helping. Incident category: "+type+"; suggested severity: "+severity+".";}
}

package com.safety.emergency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/contacts")
class ContactController {
    private final ContactRepository contacts;private final UserRepository users;
    ContactController(ContactRepository c,UserRepository u){contacts=c;users=u;}
    record ContactInput(@NotBlank String name,@NotBlank String phone,String relationship){}
    @GetMapping List<?> list(Authentication a){User u=me(a);return contacts.findByOwnerId(u.id).stream().map(this::dto).toList();}
    @PostMapping ResponseEntity<?> add(@Valid @RequestBody ContactInput i,Authentication a){EmergencyContact c=new EmergencyContact();c.owner=me(a);fill(c,i);contacts.save(c);return ResponseEntity.status(201).body(dto(c));}
    @PutMapping("/{id}") Object update(@PathVariable Long id,@Valid @RequestBody ContactInput i,Authentication a){EmergencyContact c=own(id,a);fill(c,i);return dto(contacts.save(c));}
    @DeleteMapping("/{id}") ResponseEntity<?> delete(@PathVariable Long id,Authentication a){contacts.delete(own(id,a));return ResponseEntity.noContent().build();}
    private User me(Authentication a){return users.findByEmail(a.getName()).orElseThrow();}
    private EmergencyContact own(Long id,Authentication a){EmergencyContact c=contacts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(!c.owner.email.equals(a.getName()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return c;}
    private void fill(EmergencyContact c,ContactInput i){c.name=i.name();c.phone=i.phone();c.relationship=i.relationship();}
    private Map<String,Object> dto(EmergencyContact c){return Map.of("id",c.id,"name",c.name,"phone",c.phone,"relationship",Objects.toString(c.relationship,""));}
}

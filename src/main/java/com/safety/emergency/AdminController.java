package com.safety.emergency;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin/users")
class AdminController {
    private final UserRepository users;AdminController(UserRepository u){users=u;}
    @GetMapping List<?> list(){return users.findAll().stream().map(AuthController::view).toList();}
    @PatchMapping("/{id}/role") Object role(@PathVariable Long id,@RequestBody Map<String,String> body){String role=body.get("role");if(!List.of("USER","RESPONDER","ADMIN").contains(role))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid role");User u=users.findById(id).orElseThrow();u.role=role;return AuthController.view(users.save(u));}
}

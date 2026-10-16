package com.safety.emergency;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.*;

@RestController @RequestMapping("/api/auth")
class AuthController {
    private final UserRepository users; private final PasswordEncoder encoder;
    AuthController(UserRepository users, PasswordEncoder encoder){this.users=users;this.encoder=encoder;}
    record Register(@NotBlank String name,@Email @NotBlank String email,@Size(min=8) String password,String phone){}
    record Login(@Email String email,@NotBlank String password){}
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
    @PostMapping("/register") ResponseEntity<?> register(@Valid @RequestBody Register r,HttpServletRequest req,HttpServletResponse res){
        if(users.findByEmail(r.email().toLowerCase()).isPresent()) return ResponseEntity.status(409).body(Map.of("error","Email already registered"));
        User u=new User();u.name=r.name();u.email=r.email().toLowerCase();u.passwordHash=encoder.encode(r.password());u.phone=r.phone();users.save(u);
        Authentication auth=authentication(u);SecurityContext context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,req,res);
        return ResponseEntity.status(201).body(view(u));
    }
    @PostMapping("/login") ResponseEntity<?> login(@Valid @RequestBody Login l,HttpServletRequest req,HttpServletResponse res){
        User u=users.findByEmail(l.email().toLowerCase()).orElse(null);
        if(u==null||!encoder.matches(l.password(),u.passwordHash))return ResponseEntity.status(401).body(Map.of("error","Invalid email or password"));
        Authentication auth=authentication(u);
        SecurityContext context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context,req,res);return ResponseEntity.ok(view(u));
    }
    @PostMapping("/logout") ResponseEntity<?> logout(HttpServletRequest req){SecurityContextHolder.clearContext();HttpSession s=req.getSession(false);if(s!=null)s.invalidate();return ResponseEntity.ok(Map.of("message","Logged out"));}
    @GetMapping("/me") Object me(Authentication a){return users.findByEmail(a.getName()).map(AuthController::view).orElseThrow();}
    static Map<String,Object> view(User u){return Map.of("id",u.id,"name",u.name,"email",u.email,"role",u.role,"phone",u.phone==null?"":u.phone);}
    private Authentication authentication(User u){return new UsernamePasswordAuthenticationToken(new org.springframework.security.core.userdetails.User(u.email,u.passwordHash,List.of(()->"ROLE_"+u.role)),null,List.of(()->"ROLE_"+u.role));}
}

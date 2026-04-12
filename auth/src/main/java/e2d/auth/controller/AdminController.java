package e2d.auth.controller;

import e2d.auth.dto.AdminRegisterRequest;
import e2d.auth.dto.AuthResponse;
import e2d.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {


    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/health")
    public String adminOnly(){
        return "admin access granted";
    }


    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PostMapping("/register")
   public AuthResponse register(@RequestBody @Valid AdminRegisterRequest registerRequest) {
        return authService.adminRegister(registerRequest);
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT', 'USER')")
    @GetMapping("/support")
    public String supportAccess() {
        return "Support access";
    }

}

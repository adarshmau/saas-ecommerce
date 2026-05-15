package com.saas.ecommerce.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    //User Registeration
    public String register(String name, String email, String password,String tenantId,String role ){
        if(userRepository.existsByEmail(email)){
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setTenantId(tenantId);
        user.setRole(role != null ? Role.valueOf(role) : Role.CUSTOMER); // dynamic role
        userRepository.save(user);

        return jwtService.generateToken(email, user.getRole().name(), tenantId);

    }

    public String login( String email,String password){

        User user= userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password,user.getPassword())){
            throw new RuntimeException("Invalid email or password");
        }
        return jwtService.generateToken(user.getEmail(),user.getRole().name(),user.getTenantId());

    }


}

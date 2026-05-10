package com.kingmonkey.quizmaker.service;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.dto.auth.LoginRequest;
import com.kingmonkey.quizmaker.dto.auth.LoginResponse;
import com.kingmonkey.quizmaker.dto.auth.RegisterRequest;
import com.kingmonkey.quizmaker.entity.Role;
import com.kingmonkey.quizmaker.entity.User;
import com.kingmonkey.quizmaker.repository.RoleRepository;
import com.kingmonkey.quizmaker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "MANAGER", "USER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("USER 권한이 존재하지 않습니다."));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.getRoles().add(userRole);
        userRepository.save(user);

        String role = resolveRole(user.getRoles());
        String token = jwtUtil.generateToken(user.getUsername(), role);
        return new LoginResponse(token, user.getUsername(), role);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 틀렸습니다.");
        }

        String role = resolveRole(user.getRoles());
        String token = jwtUtil.generateToken(user.getUsername(), role);
        return new LoginResponse(token, user.getUsername(), role);
    }

    private String resolveRole(Set<Role> roles) {
        return ROLE_PRIORITY.stream()
                .filter(p -> roles.stream().anyMatch(r -> r.getName().equals(p)))
                .findFirst()
                .orElse("USER");
    }
}

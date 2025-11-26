package com.scholar.platform.service;

import com.scholar.platform.dto.LoginRequest;
import com.scholar.platform.dto.LoginResponse;
import com.scholar.platform.dto.RegisterRequest;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.UserRepository;
import com.scholar.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;

  @Transactional
  public User register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("邮箱已被注册");
    }
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new RuntimeException("用户名已存在");
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole(User.UserRole.USER);
    user.setCertificationStatus(User.CertificationStatus.NOT_CERTIFIED);

    return userRepository.save(user);
  }

  public LoginResponse login(LoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    String token = jwtTokenProvider.generateToken(authentication);

    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("用户不存在"));

    return new LoginResponse(
        token,
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getRole().name());
  }
}

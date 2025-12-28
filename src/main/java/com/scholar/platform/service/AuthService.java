package com.scholar.platform.service;

import com.scholar.platform.dto.LoginRequest;
import com.scholar.platform.dto.LoginResponse;
import com.scholar.platform.dto.RegisterRequest;
import com.scholar.platform.dto.ForgotPasswordRequest;
import com.scholar.platform.dto.ResetPasswordRequest;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.UserRepository;
import com.scholar.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;
  private final JavaMailSender mailSender;
  private final StringRedisTemplate stringRedisTemplate;

  @Value("${mail.from:no-reply@localhost}")
  private String mailFrom;

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
        new UsernamePasswordAuthenticationToken(request.getAccount(), request.getPassword()));

    String token = jwtTokenProvider.generateToken(authentication);

    User user = findByAccount(request.getAccount())
        .orElseThrow(() -> new RuntimeException("用户不存在"));

    return new LoginResponse(
        token,
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getRole().name());
  }

  public void forgotPassword(ForgotPasswordRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("该邮箱未注册"));
    String code = generateCode();
    String redisKey = buildResetCodeKey(user.getEmail());
    stringRedisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(10));
    sendResetCode(user.getEmail(), code);
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    String redisKey = buildResetCodeKey(request.getEmail());
    String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
    if (cachedCode == null) {
      throw new RuntimeException("验证码已失效，请重新获取");
    }
    if (!cachedCode.equals(request.getVerificationCode())) {
      throw new RuntimeException("验证码不正确");
    }

    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("用户不存在"));
    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
    stringRedisTemplate.delete(redisKey);
  }

  private Optional<User> findByAccount(String account) {
    return userRepository.findByEmail(account).or(() -> userRepository.findByUsername(account));
  }

  private String generateCode() {
    SecureRandom random = new SecureRandom();
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }

  private void sendResetCode(String email, String code) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(mailFrom);
      message.setTo(email);
      message.setSubject("密码重置验证码");
      message.setText("您的验证码是 " + code + "，10 分钟内有效。如非本人操作，请忽略本邮件。");
      mailSender.send(message);
    } catch (Exception ex) {
      log.error("发送验证码邮件失败，email={}", email, ex);
      throw new RuntimeException("验证码发送失败，请稍后重试");
    }
  }

  private String buildResetCodeKey(String email) {
    return "auth:reset:" + email;
  }
}

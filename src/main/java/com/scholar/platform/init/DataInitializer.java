package com.scholar.platform.init;

import com.scholar.platform.entity.User;
import com.scholar.platform.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminUsername = "admin";

            // 1. 检查管理员是否已存在
            Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);

            if (existingAdmin.isEmpty()) {
                // 2. 构造初始管理员对象
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setEmail("admin@scholar.com");

                // passwordEncoder.encode("admin123")
                admin.setPasswordHash(passwordEncoder.encode("123456"));

                admin.setRole(User.UserRole.ADMIN);
                admin.setCertificationStatus(User.CertificationStatus.CERTIFIED);
                admin.setPreferences("{}");

                // 3. 保存到数据库
                userRepository.save(admin);

                System.out.println(">>> 初始管理员用户 [admin] 创建成功！");
            } else {
                System.out.println(">>> 管理员用户已存在，跳过初始化。");
            }
        };
    }
}

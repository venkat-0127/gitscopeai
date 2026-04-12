package com.lms.Digital_library.config;

import com.lms.Digital_library.entity.Admin;
import com.lms.Digital_library.repository.AdminRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SuperAdminSeeder {

    @Bean
    public ApplicationRunner superAdminInitializer(AdminRepository adminRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            String superAdminEmail = "superadmin@limat.edu.in";

            // check if exists
            if (adminRepo.existsById(superAdminEmail)) {
                System.out.println("✅ Super Admin already exists, skipping seeding.");
                return;
            }

            // create super admin if not found
            Admin admin = new Admin();
            admin.setUsername(superAdminEmail);
            admin.setPassword(passwordEncoder.encode("Admin@123")); // bcrypt
            admin.setRole("SUPER_ADMIN");
            admin.setActive(true); // ✅ use boolean instead of int
            admin.setMustChangePassword(false); // ✅ use boolean instead of int

            adminRepo.save(admin);
            System.out.println("🎯 Super Admin created successfully with email: " + superAdminEmail);
        };
    }
}

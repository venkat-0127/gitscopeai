package com.lms.Digital_library.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String raw = "Admin@123"; // 👈 your desired password
        String hash = encoder.encode(raw);
        System.out.println("Generated bcrypt hash:");
        System.out.println(hash);
    }
}

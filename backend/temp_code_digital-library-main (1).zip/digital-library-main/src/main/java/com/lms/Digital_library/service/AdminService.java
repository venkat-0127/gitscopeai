package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.Admin;
import com.lms.Digital_library.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepo;

    // ✅ Check login credentials
    public boolean login(String username, String password) {
        Optional<Admin> admin = adminRepo.findByUsernameAndPassword(username, password);
        return admin.isPresent();
    }

    // Optional: get admin by username
    public Optional<Admin> getAdmin(String username) {
        return adminRepo.findById(username);
    }
}

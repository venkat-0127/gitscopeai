package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.LoginRecord;
import com.lms.Digital_library.repository.LoginRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginRecordService {

    @Autowired
    private LoginRecordRepository loginRecordRepository;

    public void saveLogin(String email, String role) {
        LoginRecord record = new LoginRecord();
        record.setEmail(email);
        record.setRole(role);
        record.setLoginTime(LocalDateTime.now());
        loginRecordRepository.save(record);
    }
}

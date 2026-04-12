package com.lms.Digital_library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index"; // Thymeleaf auto-resolves to index.html in templates/
    }

    @GetMapping("/admin_login")
public String adminLoginPage() {
    return "admin_login";
}

@GetMapping("/teacher_login")
public String teacherLoginPage() {
    return "teacher_login";
}

@GetMapping("/student_login")
public String studentLoginPage() {
    return "student_login";
}

    @GetMapping("/main")
    public String mainDashboard() {
        return "main";
    }
    @GetMapping("/tiles")
    public String tiles() {
        return "tiles";
    }
    @GetMapping("/btech")
    public String btech() {
        return "btech";
    }
    @GetMapping("/mtechmba")
    public String mtechmba() {
        return "mtechmba";
    }
    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
      @GetMapping("/career")
    public String career() {
        return "career";
    }

     @GetMapping("/research")
    public String research() {
        return "research";
    }
    
     @GetMapping("/projects")
    public String projects() {
        return "projects";
    }
     @GetMapping("/digital_resources")
    public String digital_resources() {
        return "digital_resources";
    }

     @GetMapping("/other_archives")
    public String other_archives() {
        return "other_archives";
    }
    @GetMapping("/newspaper_archives")
    public String newspaper_archives() {
        return "newspaper_archives";
    }
    @GetMapping("/trendingai")
    public String trendingai() {
        return "trendingai";
    }
     @GetMapping("/resume")
    public String resume() {
        return "resume";
    }
}

package com.lms.Digital_library.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="github_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // link to whichever user entity you use for logged-in users; if you use Admin or OtpSession adapt accordingly
    private String username;   // store principal name (could be email or githubId)
    private String provider;   // "github"
    @Lob
    private String accessToken;
    private Long createdAt;
}

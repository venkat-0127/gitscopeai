package com.lms.Digital_library.repository;
import com.lms.Digital_library.entity.GithubToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GithubTokenRepository extends JpaRepository<GithubToken, Long> {
    Optional<GithubToken> findByUsernameAndProvider(String username, String provider);
}

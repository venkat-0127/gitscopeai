package com.lms.Digital_library.repository;

import com.lms.Digital_library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Integer> {
}

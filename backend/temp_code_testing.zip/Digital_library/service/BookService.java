// BookService.java
package com.lms.Digital_library.service;

import com.lms.Digital_library.entity.Book;
import com.lms.Digital_library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepo;

    public Book addBook(Book book) {
        return bookRepo.save(book);
    }

    public List<Book> getAllBooks() {
        return bookRepo.findAll();
    }
}

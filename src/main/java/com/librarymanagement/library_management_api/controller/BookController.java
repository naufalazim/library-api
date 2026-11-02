package com.librarymanagement.library_management_api.controller;

import com.librarymanagement.library_management_api.dto.request.CreateBookRequest;
import com.librarymanagement.library_management_api.dto.response.BookResponse;
import com.librarymanagement.library_management_api.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    //register a new book ( a new copy )
    @PostMapping
    public ResponseEntity<BookResponse> registerBook(
            @Valid @RequestBody CreateBookRequest request
            ) {
        BookResponse response =bookService.registerBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //Get all books (all copies)
    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        List<BookResponse> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }
}

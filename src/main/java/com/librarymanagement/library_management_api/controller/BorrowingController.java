package com.librarymanagement.library_management_api.controller;

import com.librarymanagement.library_management_api.dto.request.BorrowReturnRequest;
import com.librarymanagement.library_management_api.dto.response.BorrowingResponse;
import com.librarymanagement.library_management_api.service.BorrowingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
public class BorrowingController {

    private final BorrowingService borrowingService;

    public BorrowingController(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    //Borrow a book: POST /api/v1/books/{bookId}/borrow
    @PostMapping("/{bookId}/borrow")
    public ResponseEntity<BorrowingResponse> borrowBook(
            @PathVariable Long bookId,
            @Valid @RequestBody BorrowReturnRequest request) {

        BorrowingResponse response = borrowingService.borrowBook(bookId, request.borrowerId());
        return ResponseEntity.ok(response);
    }

    //Return a book: POST /api/v1/books/{bookId}/return
    @PostMapping("/{bookId}/return")
    public ResponseEntity<BorrowingResponse> returnBook(
            @PathVariable Long bookId,
            @Valid @RequestBody BorrowReturnRequest request) {
        BorrowingResponse response = borrowingService.returnBook(bookId, request.borrowerId());
        return ResponseEntity.ok(response);
    }

}

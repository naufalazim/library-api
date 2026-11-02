package com.librarymanagement.library_management_api.controller;

import com.librarymanagement.library_management_api.dto.request.CreateBorrowerRequest;
import com.librarymanagement.library_management_api.dto.response.BorrowerResponse;
import com.librarymanagement.library_management_api.service.BorrowerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping
    public ResponseEntity<BorrowerResponse> registerBorrower(
            @Valid @RequestBody CreateBorrowerRequest request)
    {
        BorrowerResponse response = borrowerService.registerBorrower(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
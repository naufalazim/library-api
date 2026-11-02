package com.librarymanagement.library_management_api.dto.request;

import jakarta.validation.constraints.NotNull;

public record BorrowReturnRequest
        (
                @NotNull(message = "Borrower id must not be null")
                Long borrowerId
        ) { }

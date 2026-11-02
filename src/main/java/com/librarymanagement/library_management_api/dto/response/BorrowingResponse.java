package com.librarymanagement.library_management_api.dto.response;

import java.time.Instant;

public record BorrowingResponse
        (
                Long borrowingId,
                Long bookId,
                Long borrowerId,
                Instant borrowedAt,
                Instant returnedAt,
                String status //Borrowed or Returned
        ) { }

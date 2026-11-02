package com.librarymanagement.library_management_api.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors  // validation errors; null otherwise
) {
    public record FieldError(String field, String message){}
}

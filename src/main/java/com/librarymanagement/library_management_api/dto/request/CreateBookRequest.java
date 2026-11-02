package com.librarymanagement.library_management_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBookRequest(
        @Pattern(regexp = "^(?:\\d{9}[\\dXx]|97[89]\\d{10})$", message = "ISBN must be a valid ISBN-10 or ISBN-13")
        String isbn,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author
) {}
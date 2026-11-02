package com.librarymanagement.library_management_api.dto.response;

public record BookResponse
        (
                Long id,
                String isbn,
                String title,
                String author,
                boolean available
        ) { }

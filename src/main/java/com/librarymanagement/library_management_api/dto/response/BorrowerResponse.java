package com.librarymanagement.library_management_api.dto.response;

public record BorrowerResponse
        (
                Long id,
                String name,
                String email
        ) { }

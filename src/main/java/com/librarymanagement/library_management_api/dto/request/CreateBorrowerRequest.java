package com.librarymanagement.library_management_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateBorrowerRequest
        (
                //client sends only name + email. No id
                @NotBlank(message = "Name cannot be blank")
                String name,

                @NotBlank(message = "Email cannot be blank")
                @Email(message = "Email must be valid email")
                String email

        ) { }

package com.librarymanagement.library_management_api.service;

import com.librarymanagement.library_management_api.dto.request.CreateBookRequest;
import com.librarymanagement.library_management_api.dto.request.CreateBorrowerRequest;
import com.librarymanagement.library_management_api.dto.response.BorrowerResponse;
import com.librarymanagement.library_management_api.entity.Borrower;
import com.librarymanagement.library_management_api.exception.ConflictException;
import com.librarymanagement.library_management_api.repository.BorrowerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

// Basically like helpdesk to check if the email is exists or not. If no, it will create new acc something.
@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;

    public BorrowerService(BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
    }

    public BorrowerResponse registerBorrower(CreateBorrowerRequest request) {
        if (borrowerRepository.existsByEmail(request.email())) {
            throw new ConflictException(
                    "A borrower with Email " + request.email() + " already exists"
            );
        }

        Borrower borrower = Borrower.builder()
                .name(request.name())
                .email(request.email())
                .build();

        Borrower saved = borrowerRepository.save(borrower);

        return new BorrowerResponse(saved.getId(), saved.getName(), saved.getEmail());
    }
}

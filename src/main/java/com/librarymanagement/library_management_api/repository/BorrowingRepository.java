package com.librarymanagement.library_management_api.repository;

import com.librarymanagement.library_management_api.entity.Borrowing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    Optional<Borrowing> findByBookCopyIdAndReturnedAtIsNull(Long bookCopyId);

    boolean existsByBookCopyIdAndReturnedAtIsNull(Long bookCopyId);
}

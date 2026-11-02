package com.librarymanagement.library_management_api.repository;
import com.librarymanagement.library_management_api.dto.request.CreateBookRequest;
import com.librarymanagement.library_management_api.entity.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BorrowerRepository extends JpaRepository<Borrower, Long>{
    //check this email existed
    boolean existsByEmail(String email);

    Optional<Borrower> findByEmail(String email);

    boolean existsByEmail(CreateBookRequest request);
}

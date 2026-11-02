package com.librarymanagement.library_management_api.repository;

import com.librarymanagement.library_management_api.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    //Find all physical copies given by ISBN
    List<BookCopy> findByBookTitleIsbn(String isbn);
}

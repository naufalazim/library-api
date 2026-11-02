package com.librarymanagement.library_management_api.repository;

import com.librarymanagement.library_management_api.entity.BookTitle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookTitleRepository extends JpaRepository<BookTitle, String> {

}

package com.librarymanagement.library_management_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "book_title")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookTitle {

    @Id
    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;
}


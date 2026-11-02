CREATE TABLE book_copy (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL,
    version BIGINT   NOT NULL DEFAULT 0,
    CONSTRAINT fk_book_copy_title
        FOREIGN KEY (isbn) REFERENCES book_title (isbn)
);

CREATE INDEX idx_book_copy_isbn ON book_copy (isbn);
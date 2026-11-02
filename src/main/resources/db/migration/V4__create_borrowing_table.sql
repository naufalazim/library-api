CREATE TABLE borrowing (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_copy_id  BIGINT  NOT NULL,
    borrower_id   BIGINT  NOT NULL,
    borrowed_at   TIMESTAMP  NOT NULL,
    returned_at   TIMESTAMP,
    CONSTRAINT fk_borrowing_book_copy
        FOREIGN KEY (book_copy_id) REFERENCES book_copy(id),
    CONSTRAINT fk_borrowing_borrower
        FOREIGN KEY (borrower_id) REFERENCES borrower(id)
);

CREATE UNIQUE INDEX uq_active_borrowing
    ON borrowing (book_copy_id)
    WHERE returned_at IS NULL;
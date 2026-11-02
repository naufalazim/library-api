# Assumptions Document

This document lists assumptions made for requirements not explicitly stated.

1. **Borrower Email Unique**: Email is used as unique identifier for borrower. Not stated but required to prevent duplicate borrowers. Implemented with unique constraint.

2. **No Borrow Limit**: Requirement does not state max books per borrower. Assumption: unlimited. Could be extended to max 3 books.

3. **No Due Date**: No mention of borrow duration. Assumption: no due date, book stays borrowed until returned. `Borrowing` entity has `borrowedAt` and `returnedAt`.

4. **Return Authorization**: Only the borrower who borrowed a specific copy can return it. If another borrower tries to return, it fails with 400/404.

5. **ISBN Format**: Only digits allowed, no dashes. Validation regex `^(?:\d{9}[\dXx]|97[89]\d{10})$` supports ISBN-10 and ISBN-13. Assumption: ISBN without dashes is sufficient for assignment.

6. **Multiple Copies Logic**: Data model uses `BookTitle` (ISBN, title, author) and `BookCopy` (unique id, refers to title, available flag). This satisfies requirement #7: same ISBN = different ids.

7. **Concurrency**: Requirement #8 "no more than one member borrowing same book_id" implemented by checking active borrowing where `returnedAt IS NULL` inside @Transactional.

8. **No Delete/Update**: No endpoints for deleting books or borrowers mentioned, so not implemented.

9. **No Authentication**: API is open, no JWT or login required, as not mentioned.

10. **No Pagination/Search**: GET /books returns all books. For production, would add pagination and search by title/author.

11. **Package Structure**: Clean layered architecture: controller -> service -> repository -> entity.

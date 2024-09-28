package com.zookeeper.useCases;

import com.zookeeper.model.Book;
import com.zookeeper.repository.BookRepository;

import org.springframework.stereotype.Service;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;

    public void addBook(Book book) {
        bookRepository.getBooks().add(book);
    }

    public void addBooks(List<Book> books) {
        bookRepository.getBooks().addAll(books);
    }

    public Book getBook(String id) {
        return bookRepository.getBooks()
                .stream()
                .filter(book -> book.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Book> getAllBooks(){
        return bookRepository.getBooks();
    }


}

package com.zookeeper.repository;

import com.zookeeper.model.Book;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.Data;

import javax.annotation.PostConstruct;

@Repository
@Data
public class BookRepository {

    private final List<Book> books = new ArrayList<>();

    public void save(Book book) {
        books.add(book);
    }

    public void saveAll(List<Book> booksToAdd) {
        books.addAll(booksToAdd);
    }

    public Optional<Book> findById(String id) {
        return books.stream()
                .filter(book -> book.getId().equals(id))
                .findFirst();
    }

    public List<Book> findAll() {
        return new ArrayList<>(books);
    }

    public void deleteById(String id) {
        books.removeIf(book -> book.getId().equals(id));
    }

    public void deleteAll() {
        books.clear();
    }
}

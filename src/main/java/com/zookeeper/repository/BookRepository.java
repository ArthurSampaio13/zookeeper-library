package com.zookeeper.repository;

import com.zookeeper.model.Book;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;


@Repository
@Data
public class BookRepository {

    private final List<Book> books = new ArrayList<>();

    public void save(Book book) {
        books.add(book);
    }

    public void deleteById(String id) {
        books.removeIf(book -> book.getId().equals(id));
    }

}

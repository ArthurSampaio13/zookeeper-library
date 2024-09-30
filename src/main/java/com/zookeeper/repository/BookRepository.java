package com.zookeeper.repository;

import com.zookeeper.model.Book;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import javax.annotation.PostConstruct;

@Repository
@Data
public class BookRepository {

    private final List<Book> books = new ArrayList<>();

    @PostConstruct
    public void init() {
        books.add(new Book("1", "The Great Gatsby", "Fiction", "A story of the fabulously wealthy Jay Gatsby and his love for the beautiful Daisy Buchanan.", "F. Scott Fitzgerald", 1925));
        books.add(new Book("2", "1984", "Fiction", "The story of Winston Smith, an outwardly obedient party member, but inwardly desperate for truth and rebellion.", "George Orwell", 1949));
        books.add(new Book("3", "Pride and Prejudice", "Fiction", "The story of the Bennet family and their five unmarried daughters.", "Jane Austen", 1813));
        books.add(new Book("4", "The Catcher in the Rye", "Fiction", "The story of Holden Caulfield, a teenager who wanders the streets of New York after being expelled from an elite prep school.", "J.D. Salinger", 1951));
    }

}

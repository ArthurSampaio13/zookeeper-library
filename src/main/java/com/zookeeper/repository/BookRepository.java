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

}

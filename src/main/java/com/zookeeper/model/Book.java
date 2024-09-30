package com.zookeeper.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Book {
    private String id;
    private String title;
    private String genre;
    private String description;
    private String author;
    private Integer releaseYear;
}

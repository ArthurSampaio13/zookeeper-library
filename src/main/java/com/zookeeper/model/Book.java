package com.zookeeper.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Book {
    private String id;
    private String author;
    private int year;
}

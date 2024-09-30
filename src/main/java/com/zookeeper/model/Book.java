package com.zookeeper.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Book {

    private String id;
    private String title;
    private String genre;
    private String description;
    private String author;
    private Integer releaseYear;

}

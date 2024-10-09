package com.zookeeper.dto.request;

import lombok.Data;

@Data
public class BookRequestDTO {

    private String title;
    private String genre;
    private String description;
    private String author;
    private Integer releaseYear;
}

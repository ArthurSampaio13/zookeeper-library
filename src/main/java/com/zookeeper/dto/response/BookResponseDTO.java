package com.zookeeper.dto.response;

import lombok.Data;

@Data
public class BookResponseDTO {

    private Long id;
    private String title;
    private String genre;
    private String description;
    private String author;
    private Integer releaseYear;
}

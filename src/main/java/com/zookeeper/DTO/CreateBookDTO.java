package com.zookeeper.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateBookDTO {
    private String id;
    private String title;
    private String genre;
    private String description;
    private String author;
    private Integer releaseYear;

}

package com.zookeeper.dto;

import com.zookeeper.model.Book;
import com.zookeeper.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class CreateLoanDTO {

    private User user;
    private Book book;
    private LocalDate localDate;
    private LocalDate returnDate;
}

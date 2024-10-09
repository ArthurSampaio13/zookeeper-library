package com.zookeeper.model;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Data
public class Loan {

    private String id;
    private User user;
    private Book book;
    private LocalDate loanDate;
    private LocalDate returnDate;

}

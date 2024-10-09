package com.zookeeper.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanResponseDTO {

    private Long id;
    private String userName;
    private String bookTitle;
    private LocalDate loanDate;
    private LocalDate returnDate;
}

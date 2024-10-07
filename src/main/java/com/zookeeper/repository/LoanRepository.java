package com.zookeeper.repository;

import com.zookeeper.model.Loan;
import lombok.Data;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Data
public class LoanRepository {

    private final List<Loan> loanList = new ArrayList<>();

    public void save(Loan loan){
        loanList.add(loan);
    }

    public void deleteById(UUID id){
        loanList.removeIf(loan -> loan.getId().equals(id));
    }

    public List<Loan> findAllByUserId(UUID userId){
        return loanList.stream()
                .filter(user -> user.getId().equals(userId))
                .collect(Collectors.toList());
    }
}

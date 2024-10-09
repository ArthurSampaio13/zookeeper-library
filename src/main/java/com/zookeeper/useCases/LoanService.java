package com.zookeeper.useCases;

import com.zookeeper.model.Book;
import com.zookeeper.model.Loan;
import com.zookeeper.model.User;
import com.zookeeper.repository.BookRepository;
import com.zookeeper.repository.LoanRepository;
import com.zookeeper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final ZooKeeper zooKeeper;

    public void addLoan(String idUser, String idBook) throws InterruptedException, KeeperException {
        Optional<User> userOpt = userRepository.findById(String.valueOf(idUser));
        Optional<Book> bookOpt = bookRepository.findById(idBook);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        if (bookOpt.isEmpty()) {
            throw new RuntimeException("Book not found");
        }

        User user = userOpt.get();
        Book book = bookOpt.get();

        Loan loan = new Loan(null, user, book, LocalDate.now(), null);

        String parentPath = "/loans";

        if (zooKeeper.exists(parentPath, false) == null) {
            zooKeeper.create(parentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        String path = parentPath + "/loan-";
        String createdPath = zooKeeper.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);

        String id = createdPath.substring(path.length());

        loan.setId(id);

        loanRepository.getLoanList().add(loan);

        zooKeeper.delete(createdPath, -1);
    }

    public Loan getLoan(String id) {
        return loanRepository.getLoanList()
                .stream()
                .filter(loan -> loan.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    public List<Loan> getAllLoans() {
        return loanRepository.getLoanList();
    }

    public void deleteLoan(String id) throws KeeperException, InterruptedException {
        String path = "/loans/loan-" + id;

        if (zooKeeper.exists(path, false) != null) {
            zooKeeper.delete(path, -1);
            System.out.println("Loan deleted successfully" + path);
        } else {
            System.out.println("Loan not found");
        }
        loanRepository.deleteById(UUID.fromString(id));
    }
}

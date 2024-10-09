package com.zookeeper.controller;

import com.zookeeper.model.Loan;
import com.zookeeper.useCases.LoanService;
import com.zookeeper.config.Config;
import com.zookeeper.useCases.ClusterInformationService;
import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.KeeperException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;

@RestController
@RequestMapping("v1/loans/")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final Config config;
    private final ClusterInformationService clusterInformationService;
    private final RestTemplate restTemplate;

    @GetMapping("/")
    public ResponseEntity<List<Loan>> getAllLoans() {
        List<Loan> loans = loanService.getAllLoans();
        return ResponseEntity.ok().body(loans);
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<Loan> getLoanById(@PathVariable String loanId) {
        Loan loan = loanService.getLoan(loanId);
        return ResponseEntity.ok().body(loan);
    }

    @DeleteMapping("/{loanId}")
    public ResponseEntity<String> deleteLoan(@PathVariable String loanId) {
        try {
            loanService.deleteLoan(loanId);
            return ResponseEntity.ok("Loan deleted successfully.");
        } catch (KeeperException e) {
            return ResponseEntity.status(500).body("Error accessing Zookeeper: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Operation was interrupted: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addLoan(HttpServletRequest request, @RequestParam String userId, @RequestParam String bookId) throws InterruptedException, KeeperException {
        String requestFrom = request.getHeader("request_from");
        String masterNode = clusterInformationService.getMasterNode();

        if (!isEmpty(requestFrom) && requestFrom.equalsIgnoreCase(masterNode)) {
            loanService.addLoan(userId, bookId);
            return ResponseEntity.ok("Loan created successfully.");
        }

        if (isMaster()) {
            List<String> liveNodes = clusterInformationService.getLiveClusterNodes();
            int successCount = 0;
            for (String node : liveNodes) {
                if (config.getHostPort().equals(node)) {
                    loanService.addLoan(userId, bookId);
                    successCount++;
                } else {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("request_from", config.getHostPort());
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    String requestUrl = "http://" + node + "/v1/loans/add";
                    restTemplate.postForObject(requestUrl, null, String.class);
                    successCount++;
                }
            }
            return ResponseEntity.ok("Successfully updated " + successCount + " nodes.");
        } else {
            String requestUrl = "http://" + masterNode + "/v1/loans/add";
            return restTemplate.postForEntity(requestUrl, null, String.class);
        }
    }

    private boolean isMaster() {
        return config.getHostPort().equals(clusterInformationService.getMasterNode());
    }
}

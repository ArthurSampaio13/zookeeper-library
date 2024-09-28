package com.zookeeper.controller;

import com.zookeeper.config.Config;
import com.zookeeper.model.Book;
import com.zookeeper.useCases.BookService;
import com.zookeeper.useCases.ClusterInformationService;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.util.StringUtils.isEmpty;

@RestController
@RequestMapping("v1/books/")
@RequiredArgsConstructor
@Slf4j
public class BooksControler {

    private final BookService bookService;
    private final Config config;
    private final ClusterInformationService clusterInformationService;
    private final RestTemplate restTemplate;

    @GetMapping("/")
    public List<Book> getAllBooks() {
        log.info("get all books from the database...");
        return bookService.getAllBooks();
    }

    @PostMapping("/add")
    public ResponseEntity<String> addBook(HttpServletRequest request, @RequestBody Book book) {
        log.info("received addBook request...");
        String requestFrom = request.getHeader("request_from");
        String masterNode = clusterInformationService.getMasterNode();

        if (!isEmpty(requestFrom) && requestFrom.equalsIgnoreCase(masterNode)) {
            log.info("addBook request come from master node, save the book...");
            bookService.addBook(book);
            return ResponseEntity.ok("SUCCESS");
        }

        if (isMaster()) {
            List<String> liveNodes = clusterInformationService.getLiveClusterNodes();
            int successCount = 0;
            log.info("distributing addBook request to all slave nodes...");
            for (String node : liveNodes) {
                if (config.getHostPort().equals(node)) {
                    log.info("saving new book in master node...");
                    bookService.addBook(book);
                    successCount++;
                } else {
                    HttpHeaders headers = new HttpHeaders();

                    headers.add("request_from", config.getHostPort());
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<Book> entity = new HttpEntity<>(book, headers);

                    String requestUrl = "http://" + node + "/v1/books/add" + "/";
                    restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class).getBody();

                    successCount++;
                }
            }

            log.info(successCount + " nodes were updated");
            return ResponseEntity.ok()
                    .body("Successfully update " + successCount + " nodes");
        } else {
            log.info("slave node, redirecting request to the master node");
            String requestUrl = "http://" + masterNode + "/v1/books/add" + "/";
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Book> entity = new HttpEntity<>(book, headers);
            return restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class);
        }
    }

    private boolean isMaster() {
        return config.getHostPort()
                .equals(clusterInformationService.getMasterNode());
    }

}

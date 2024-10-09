package com.zookeeper.controller;

import com.zookeeper.DTO.CreateBookDTO;
import com.zookeeper.config.Config;
import com.zookeeper.model.Book;
import com.zookeeper.service.BookService;
import com.zookeeper.service.ClusterInformationService;

import org.apache.zookeeper.KeeperException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import static org.springframework.util.StringUtils.isEmpty;

@RestController
@RequestMapping("v1/books/")
@RequiredArgsConstructor
public class BooksController {

    private final BookService bookService;
    private final Config config;
    private final ClusterInformationService clusterInformationService;
    private final RestTemplate restTemplate;

    @GetMapping("/")
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = bookService.getAllBooks();
        return ResponseEntity.ok().body(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        Book book = bookService.getBook(id);
        return ResponseEntity.ok().body(book);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable String id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok("Livro deletado com sucesso.");
        } catch (KeeperException e) {
            return ResponseEntity.status(500).body("Erro ao acessar o Zookeeper: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("A operação foi interrompida: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro inesperado: " + e.getMessage());
        }
    }
    @PostMapping("/add")
    public ResponseEntity<String> addBook(HttpServletRequest request, @RequestBody CreateBookDTO book) throws InterruptedException, KeeperException {
        String requestFrom = request.getHeader("request_from");
        String masterNode = clusterInformationService.getMasterNode();

        if (!isEmpty(requestFrom) && requestFrom.equalsIgnoreCase(masterNode)) {
            bookService.addBook(book);
            return ResponseEntity.ok("SUCCESS: Book added in ZooKeeper.");
        }

        if (isMaster()) {
            List<String> liveNodes = clusterInformationService.getLiveClusterNodes();
            int successCount = 0;
            for (String node : liveNodes) {
                if (config.getHostPort().equals(node)) {
                    bookService.addBook(book);
                    successCount++;
                } else {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("request_from", config.getHostPort());
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<CreateBookDTO> entity = new HttpEntity<>(book, headers);

                    String requestUrl = "http://" + node + "/v1/books/add" + "/";
                    restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class).getBody();

                    successCount++;
                }
            }

            return ResponseEntity.ok("Successfully updated " + successCount + " nodes");
        } else {
            String requestUrl = "http://" + masterNode + "/v1/books/add" + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateBookDTO> entity = new HttpEntity<>(book, headers);
            return restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class);
        }
    }


    private boolean isMaster() {
        return config.getHostPort()
                .equals(clusterInformationService.getMasterNode());
    }

    @GetMapping("/zookeeper/{id}")
    public ResponseEntity<Book> getBookFromZooKeeper(@PathVariable String id) {
        try {
            Book book = bookService.getBookFromZooKeeper(id);
            if (book != null) {
                return ResponseEntity.ok(book);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (KeeperException | InterruptedException e) {
            return ResponseEntity.status(500).body(null);
        }
    }


}

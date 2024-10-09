package com.zookeeper.useCases;

import com.zookeeper.DTO.CreateBookDTO;
import com.zookeeper.model.Book;
import com.zookeeper.repository.BookRepository;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;

//    private final ZooKeeper zooKeeper;

    public void addBook(CreateBookDTO createBookDTO) throws InterruptedException, KeeperException {

        Book book = new Book(createBookDTO.getId(), createBookDTO.getTitle(), createBookDTO.getGenre(),
                createBookDTO.getDescription(), createBookDTO.getAuthor(),
                createBookDTO.getReleaseYear());

//        String parentPath = "/books";
//
//        if (zooKeeper.exists(parentPath, false) == null) {
//            zooKeeper.create(parentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
//        }
//
//        String path = parentPath + "/book-";
//        String createdPath = zooKeeper.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
//
//        String id = createdPath.substring(path.length());
//
//        book.setId(id);

        bookRepository.getBooks().add(book);

//        zooKeeper.delete(createdPath, -1);
    }

    public void addBooks(List<Book> books) {
        bookRepository.getBooks().addAll(books);
    }

    public Book getBook(String id) {
        return bookRepository.getBooks()
                .stream()
                .filter(book -> book.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Book> getAllBooks(){
        return bookRepository.getBooks();
    }

//    public void deleteBook(String id) throws KeeperException, InterruptedException {
//        String path = "/books/book-" + id;
//        if (zooKeeper.exists(path, false) != null) {
//            zooKeeper.delete(path, -1);
//            System.out.println("Nó deletado: " + path);
//        } else {
//            System.out.println("Nó não existe");
//        }
//
//        bookRepository.deleteById(id);
//    }
}

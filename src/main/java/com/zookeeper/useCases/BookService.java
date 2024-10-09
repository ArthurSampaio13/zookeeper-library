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

    private final ZooKeeper zooKeeper;

    public void addBook(CreateBookDTO createBookDTO) throws InterruptedException, KeeperException {
        // Cria o objeto Book a partir do DTO
        Book book = new Book(null, createBookDTO.getTitle(), createBookDTO.getGenre(),
                createBookDTO.getDescription(), createBookDTO.getAuthor(),
                createBookDTO.getReleaseYear());

        String parentPath = "/books";

        // Verifica se o nó pai existe e o cria se não existir
        if (zooKeeper.exists(parentPath, false) == null) {
            zooKeeper.create(parentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        // Cria um caminho único para o livro
        String path = parentPath + "/book-";
        String createdPath = zooKeeper.create(path, bookToBytes(book), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);

        // Define o ID do livro a partir do caminho criado
        String id = createdPath.substring(path.length());
        book.setId(id);

        // Salva o livro no repositório em memória (opcional)
        bookRepository.save(book);
    }

    private byte[] bookToBytes(Book book) {
        // Implemente a lógica de conversão do objeto Book para bytes (por exemplo, usando JSON)
        // Aqui um exemplo simples, substitua pela lógica real conforme necessário
        return String.format("%s,%s,%s,%s,%s,%d",
                        book.getId(), book.getTitle(), book.getGenre(),
                        book.getDescription(), book.getAuthor(), book.getReleaseYear())
                .getBytes();
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

    public void deleteBook(String id) throws KeeperException, InterruptedException {
        String path = "/books/book-" + id;
        if (zooKeeper.exists(path, false) != null) {
            zooKeeper.delete(path, -1);
            System.out.println("Nó deletado: " + path);
        } else {
            System.out.println("Nó não existe");
        }

        // Remove o livro do repositório em memória
        bookRepository.deleteById(id);
    }

    public Book getBookFromZooKeeper(String id) throws KeeperException, InterruptedException {
        String path = "/books/book-" + id;
        byte[] data = zooKeeper.getData(path, false, null);
        return bytesToBook(data);
    }

    private Book bytesToBook(byte[] data) {
        // Implemente a lógica para converter os bytes de volta para um objeto Book
        String dataString = new String(data);
        String[] fields = dataString.split(",");

        if (fields.length != 6) {
            return null; // ou lance uma exceção se os dados não estiverem no formato esperado
        }

        return new Book(fields[0], fields[1], fields[2], fields[3], fields[4], Integer.parseInt(fields[5]));
    }


}

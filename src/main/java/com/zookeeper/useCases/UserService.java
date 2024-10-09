package com.zookeeper.useCases;

import com.zookeeper.model.User;
import com.zookeeper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private final ZooKeeper zooKeeper;

    public void createUser(String name, String email) throws InterruptedException, KeeperException {

        User user = new
                User(null, name, email);

        String parentPath = "/users";
        if (zooKeeper.exists(parentPath, false) == null) {
            zooKeeper.create(parentPath, null, org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE, org.apache.zookeeper.CreateMode.PERSISTENT);

        }
        String path = parentPath + "/user-";
        String createdPath = zooKeeper.create(path, null, org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE, org.apache.zookeeper.CreateMode.PERSISTENT_SEQUENTIAL);
        String id = createdPath.substring(path.length());
        user.setId(id);
        userRepository.findAll().add(user);
        zooKeeper.delete(createdPath, -1);

    }

    public User getUser(String id) throws InterruptedException, KeeperException {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String id) throws InterruptedException, KeeperException {
        String path = "/users/user-" + id;

        if (zooKeeper.exists(path, false) != null) {
            zooKeeper.delete(path, -1);
            System.out.println("User deleted successfully" + path);
        } else {
            System.out.println("User not found");
        }
        userRepository.deleteById(UUID.fromString(id));


    }
}

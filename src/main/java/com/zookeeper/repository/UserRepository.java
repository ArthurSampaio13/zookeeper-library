package com.zookeeper.repository;

import com.zookeeper.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserRepository {

    private final List<User> users = new ArrayList<>();

    public void save (User user){
        users.add(user);
    }

    public Optional<User> findById(UUID id){
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }

    public void deleteById(UUID id){
        users.removeIf(user -> user.getId().equals(id));
    }

    public List<User> findAll(){
        return  new ArrayList<>(users);
    }
}

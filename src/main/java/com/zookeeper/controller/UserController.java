package com.zookeeper.controller;

import com.zookeeper.model.User;
import com.zookeeper.useCases.UserService;
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
@RequestMapping("v1/users/")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final Config config;
    private final ClusterInformationService clusterInformationService;
    private final RestTemplate restTemplate;

    // Read all users
    @GetMapping("/")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok().body(users);
    }

    // Read a specific user by ID
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        try {
            User user = userService.getUser(userId);
            return ResponseEntity.ok().body(user);
        } catch (KeeperException | InterruptedException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Create a new user
    @PostMapping("/add")
    public ResponseEntity<String> createUser(HttpServletRequest request, @RequestParam String name, @RequestParam String email) throws InterruptedException, KeeperException {
        String requestFrom = request.getHeader("request_from");
        String masterNode = clusterInformationService.getMasterNode();

        if (!isEmpty(requestFrom) && requestFrom.equalsIgnoreCase(masterNode)) {
            userService.createUser(name, email);
            return ResponseEntity.ok("User created successfully.");
        }

        if (isMaster()) {
            List<String> liveNodes = clusterInformationService.getLiveClusterNodes();
            int successCount = 0;
            for (String node : liveNodes) {
                if (config.getHostPort().equals(node)) {
                    userService.createUser(name, email);
                    successCount++;
                } else {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("request_from", config.getHostPort());
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    String requestUrl = "http://" + node + "/v1/users/add";
                    restTemplate.postForObject(requestUrl, null, String.class);
                    successCount++;
                }
            }
            return ResponseEntity.ok("Successfully updated " + successCount + " nodes.");
        } else {
            String requestUrl = "http://" + masterNode + "/v1/users/add";
            return restTemplate.postForEntity(requestUrl, null, String.class);
        }
    }

    // Delete a user by ID
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully.");
        } catch (KeeperException e) {
            return ResponseEntity.status(500).body("Error accessing Zookeeper: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Operation was interrupted: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }

    // Helper method to check if the node is the master
    private boolean isMaster() {
        return config.getHostPort().equals(clusterInformationService.getMasterNode());
    }
}

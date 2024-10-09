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

    @GetMapping("/")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok().body(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        try {
            User user = userService.getUser(userId);
            return ResponseEntity.ok().body(user);
        } catch (KeeperException | InterruptedException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<User> createUser(HttpServletRequest request, @RequestParam String name, @RequestParam String email) throws InterruptedException, KeeperException {
        String requestFrom = request.getHeader("request_from");
        String masterNode = clusterInformationService.getMasterNode();
        User createdUser = new User();

        if (!isEmpty(requestFrom) && requestFrom.equalsIgnoreCase(masterNode)) {
            createdUser = userService.createUser(name, email);
            return ResponseEntity.ok(createdUser);
        }

        if (isMaster()) {
            List<String> liveNodes = clusterInformationService.getLiveClusterNodes();
            int successCount = 0;
            for (String node : liveNodes) {
                if (config.getHostPort().equals(node)) {
                    createdUser = userService.createUser(name, email);
                    successCount++;
                } else {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("request_from", config.getHostPort());
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    String requestUrl = "http://" + node + "/v1/users/add";
                    createdUser = restTemplate.postForObject(requestUrl, null, User.class);
                    successCount++;
                }
            }
            return ResponseEntity.ok(createdUser);
        } else {
            String requestUrl = "http://" + masterNode + "/v1/users/add";
            createdUser = restTemplate.postForObject(requestUrl, null, User.class);
            return ResponseEntity.ok(createdUser);
        }
    }

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

    private boolean isMaster() {
        return config.getHostPort().equals(clusterInformationService.getMasterNode());
    }
}

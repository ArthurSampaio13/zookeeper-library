package com.zookeeper.zookeeper.config;

import org.apache.zookeeper.ZooKeeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZookeeperConfig {
    @Bean
    public ZooKeeper zooKeeper() throws Exception {
        return new ZooKeeper("10.241.11.129:2181", 3000, null);
    }
}

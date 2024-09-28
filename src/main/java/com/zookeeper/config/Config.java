package com.zookeeper.config;

import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Config {
    public static final String ELECTION_NODE = "/election";
    public static final String LIVE_NODES = "/live_nodes";
    public static final String ALL_NODES = "/all_nodes";

    @Value("${server.host}")
    private String host;

    @Value("${server.port}")
    private String port;

    @Value("${zookeeper.host}")
    private String zookeeperHost;

    @Value("${zookeeper.port}")
    private String zookeeperPort;

    @Bean
    public ZkClient zkClient() {
        return new ZkClient(zookeeperHost + ":" + zookeeperPort, 12000, 3000);
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    public String getHostPort() {
        return host + ":" + port;
    }
}

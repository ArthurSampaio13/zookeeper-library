package com.zookeeper;

import com.zookeeper.config.Config;
import com.zookeeper.model.Book;
import com.zookeeper.service.BookService;
import com.zookeeper.service.ClusterInformationService;
import com.zookeeper.zookeeper.service.ZookeeperService;
import com.zookeeper.zookeeper.watcher.AllClusterNodesChangeListener;
import com.zookeeper.zookeeper.watcher.ConnectStateChangeListener;
import com.zookeeper.zookeeper.watcher.LiveClusterNodesChangeListener;
import com.zookeeper.zookeeper.watcher.MasterChangeListener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import javax.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.zookeeper.config.Config.ALL_NODES;
import static com.zookeeper.config.Config.ELECTION_NODE;
import static com.zookeeper.config.Config.LIVE_NODES;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class ZookeeperIntegrationApplication implements ApplicationListener<ContextRefreshedEvent> {

    private final Config config;
    private final ZookeeperService zookeeperService;
    private final ClusterInformationService clusterInformationService;
    private final AllClusterNodesChangeListener allClusterNodesChangeListener;
    private final LiveClusterNodesChangeListener liveClusterNodesChangeListener;
    private final MasterChangeListener masterChangeListener;
    private final ConnectStateChangeListener connectStateChangeListener;
    private final RestTemplate restTemplate;
    private final BookService bookService;

    public static void main(String[] args) {
        SpringApplication.run(ZookeeperIntegrationApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        zookeeperService.createAllParentPersistentNodes();

        addCurrentNodeToAllNodesList();
        addCurrentNodeToElectionNodesList();
        addCurrentNodeToLiveNodesList();

        syncDataFromMaster();

        registerZookeeperWatchers();
    }

    @PreDestroy
    private void destroy(){
        zookeeperService.closeConnection();
    }

    private void addCurrentNodeToAllNodesList() {
        zookeeperService.createAndAddToAllNodes(config.getHostPort(), "Cluster node");

        List<String> allNodes = zookeeperService.getAllNodesInZookeeperCluster();
        log.info("Os znodes em allNodes parent znode: {}", allNodes);

        clusterInformationService.rebuildAllNodesList(allNodes);
    }

    private void addCurrentNodeToElectionNodesList() {
        zookeeperService.createNodeInElectionZnode(config.getHostPort());
        clusterInformationService.setMasterNode(zookeeperService.getLeaderNodeData());
    }

    private void addCurrentNodeToLiveNodesList() {
        zookeeperService.createAndAddToLiveNodes(config.getHostPort(), "Cluster node");

        List<String> liveNodes = zookeeperService.getLiveNodesInZookeeperCluster();
        log.info("Os znodes em liveNodes parent znode: {}", liveNodes);
        clusterInformationService.rebuildLiveNodesList(liveNodes);
    }

    private void registerZookeeperWatchers (){
        zookeeperService.registerChildrenChangeWatcher(ELECTION_NODE, masterChangeListener);
        zookeeperService.registerChildrenChangeWatcher(LIVE_NODES, liveClusterNodesChangeListener);
        zookeeperService.registerChildrenChangeWatcher(ALL_NODES, allClusterNodesChangeListener);
        zookeeperService.registerZkSessionStateListener(connectStateChangeListener);
    }

    private void syncDataFromMaster() {
        if (config.getHostPort().equals(clusterInformationService.getMasterNode())) {
            return;
        }

        String requestUrl = "http://".concat(clusterInformationService.getMasterNode() + "/v1/books/");
        List<Book> books = restTemplate.getForObject(requestUrl, List.class);

        bookService.getAllBooks().clear();
        bookService.addBooks(books);
    }
}

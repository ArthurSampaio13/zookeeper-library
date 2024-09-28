package com.zookeeper;

import com.zookeeper.config.Config;
import com.zookeeper.model.Book;
import com.zookeeper.useCases.BookService;
import com.zookeeper.useCases.ClusterInformationService;
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

    /**
     * Add application to zookeeper cluster by creating persistent znode under /all_nodes path.
     * Name of the znode will be host:port
     */
    private void addCurrentNodeToAllNodesList() {
        zookeeperService.createAndAddToAllNodes(config.getHostPort(), "cluster node");

        List<String> allNodes = zookeeperService.getAllNodesInZookeeperCluster();
        log.info("current znodes in allNodes parent znode: {}", allNodes);

        /**
         * Update local cluster information for havin up to date /all_nodes representation
         */
        clusterInformationService.rebuildAllNodesList(allNodes);
    }

    /**
     * Add application to /election list and define a cluster master
     */
    private void addCurrentNodeToElectionNodesList() {
        zookeeperService.createNodeInElectionZnode(config.getHostPort());

        /**
         * Update local cluster information for having up to date master node representation
         */
        clusterInformationService.setMasterNode(zookeeperService.getLeaderNodeData());
    }

    /**
     * Add application to zookeeper /live_nodes list
     */
    private void addCurrentNodeToLiveNodesList() {
        zookeeperService.createAndAddToLiveNodes(config.getHostPort(), "cluster node");

        List<String> liveNodes = zookeeperService.getLiveNodesInZookeeperCluster();
        log.info("current znodes in liveNodes parent znode: {}", liveNodes);

        /**
         * Update local cluster information for having up to date /live_nodes representation
         */
        clusterInformationService.rebuildLiveNodesList(liveNodes);
    }

    /**
     * Register all needful zookeeper watchers
     */
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

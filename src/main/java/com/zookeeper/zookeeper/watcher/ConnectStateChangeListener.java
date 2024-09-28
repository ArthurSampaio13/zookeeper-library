package com.zookeeper.zookeeper.watcher;

import com.zookeeper.config.Config;
import com.zookeeper.model.Book;
import com.zookeeper.useCases.BookService;
import com.zookeeper.useCases.ClusterInformationService;
import com.zookeeper.zookeeper.service.ZookeeperService;

import org.I0Itec.zkclient.IZkStateListener;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectStateChangeListener implements IZkStateListener {
    private final ZookeeperService zookeeperService;
    private final ClusterInformationService clusterInformationService;
    private final BookService bookService;
    private final RestTemplate restTemplate;
    private final Config config;

    @Override
    public void handleStateChanged(KeeperState keeperState) {
        log.info("current state: {}", keeperState.name());
    }

    @Override
    public void handleNewSession() throws Exception {
        log.info("connected to zookeeper");

        syncDataFromMaster();

        /**
         * Add new znode to /live_nodes and update local cluster information
         */
        zookeeperService.createAndAddToLiveNodes(config.getHostPort(), "cluster node");

        List<String> liveNodes = zookeeperService.getLiveNodesInZookeeperCluster();
        clusterInformationService.rebuildLiveNodesList(liveNodes);

        /**
         * Retry creating znode under /election
         */
        zookeeperService.createNodeInElectionZnode(config.getHostPort());
        clusterInformationService.setMasterNode(zookeeperService.getLeaderNodeData());
    }

    @Override
    public void handleSessionEstablishmentError(Throwable throwable) {
        log.error("could not establish zookeeper session");
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

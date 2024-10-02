package com.zookeeper.zookeeper.service;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.zookeeper.config.Config.ALL_NODES;
import static com.zookeeper.config.Config.ELECTION_NODE;
import static com.zookeeper.config.Config.LIVE_NODES;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZookeeperService {

    private final ZkClient zkClient;

    public void createAllParentPersistentNodes() {
        if (!zkClient.exists(ALL_NODES)) {
            zkClient.create(ALL_NODES, "Todos os nodes:", CreateMode.PERSISTENT);
        }
        if (!zkClient.exists(LIVE_NODES)) {
            zkClient.create(LIVE_NODES, "Todos os nodes:", CreateMode.PERSISTENT);
        }
        if (!zkClient.exists(ELECTION_NODE)) {
            zkClient.create(ELECTION_NODE, "Todos os nodes election:", CreateMode.PERSISTENT);
        }
    }

    public void createAndAddToAllNodes(String nodeName, String data) {
        if (!zkClient.exists(ALL_NODES)) {
            zkClient.create(ALL_NODES, "Todos os nodes:", CreateMode.PERSISTENT);
        }

        String childNode = ALL_NODES.concat("/").concat(nodeName);

        if (zkClient.exists(childNode)) {
            return;
        }

        zkClient.create(childNode, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    public void createAndAddToLiveNodes(String nodeName, String data) {
        if (!zkClient.exists(LIVE_NODES)) {
            zkClient.create(LIVE_NODES, "Todos os nodes:", CreateMode.PERSISTENT);
        }

        String childNode = LIVE_NODES.concat("/").concat(nodeName);

        if (zkClient.exists(childNode)) {
            return;
        }

        zkClient.create(childNode, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
    }

    public void createNodeInElectionZnode(String data) {
        if (!zkClient.exists(ELECTION_NODE)) {
            zkClient.create(ELECTION_NODE, "Election node", CreateMode.PERSISTENT);
        }

        zkClient.create(ELECTION_NODE.concat("/node"), data, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    public List<String> getAllNodesInZookeeperCluster() {
        if (!zkClient.exists(ALL_NODES)) {
            throw new RuntimeException("O node /allNodes não existe");
        }

        return zkClient.getChildren(ALL_NODES);
    }

    public List<String> getLiveNodesInZookeeperCluster() {
        if (!zkClient.exists(LIVE_NODES)) {
            throw new RuntimeException("O node /liveNodes não existe");
        }

        return zkClient.getChildren(LIVE_NODES);
    }

    public String getLeaderNodeData() {
        if (!zkClient.exists(ELECTION_NODE)) {
            throw new RuntimeException("O node /election não existe");
        }
        List<String> nodesInElection = zkClient.getChildren(ELECTION_NODE);
        Collections.sort(nodesInElection);

        String masterZNode = nodesInElection.get(0);

        return getZNodeData(ELECTION_NODE.concat("/").concat(masterZNode));
    }

    public String getZNodeData(String path) {
        return zkClient.readData(path, null);
    }

    public void registerChildrenChangeWatcher(String path, IZkChildListener iZkChildListener) {
        zkClient.subscribeChildChanges(path, iZkChildListener);
    }

    public void registerZkSessionStateListener(IZkStateListener iZkStateListener) {
        zkClient.subscribeStateChanges(iZkStateListener);
    }

    public void closeConnection() {
        log.info("Fechando conexão com o zookeeper...");
        zkClient.close();
        log.info("Conexão com o zookeeper fechada");
    }
}

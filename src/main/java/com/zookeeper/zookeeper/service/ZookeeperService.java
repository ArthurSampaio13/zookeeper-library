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

    /**
     * Create three parent PERSISTENT znodes in zookeeper cluster
     *  - /all_nodes - here all znodes in zookeeper cluster will be saved (including dead ones)
     *  - /live_nodes - here current live znodes in zookeper cluster will saved
     *  - /election - parent znode which is using for master election
     */
    public void createAllParentPersistentNodes() {
        if (!zkClient.exists(ALL_NODES)) {
            zkClient.create(ALL_NODES, "all nodes are displayed here", CreateMode.PERSISTENT);
        }
        if (!zkClient.exists(LIVE_NODES)) {
            zkClient.create(LIVE_NODES, "all live nodes are displayed here", CreateMode.PERSISTENT);
        }
        if (!zkClient.exists(ELECTION_NODE)) {
            zkClient.create(ELECTION_NODE, "all election nodes are displayed here", CreateMode.PERSISTENT);
        }
    }

    /**
     * Add node to /all_nodes children in zookeper cluster
     * <p>
     * Persistent mode is using since we need to have znode in the list when it will be dead
     *
     * @param nodeName - name of the new znode
     * @param data     - znode data
     */
    public void createAndAddToAllNodes(String nodeName, String data) {
        if (!zkClient.exists(ALL_NODES)) {
            zkClient.create(ALL_NODES, "all nodes are displayed here", CreateMode.PERSISTENT);
        }

        /**
         * "/all_nodes/host:port"
         */
        String childNode = ALL_NODES.concat("/").concat(nodeName);

        if (zkClient.exists(childNode)) {
            return;
        }

        zkClient.create(childNode, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    /**
     * Add node to /live_nodes children in zookeper cluster
     * <p>
     * EPHEMERAL mode is using since we need to have znode in the list only when it live
     *
     * @param nodeName - name of the new znode
     * @param data     - znode data
     */
    public void createAndAddToLiveNodes(String nodeName, String data) {
        if (!zkClient.exists(LIVE_NODES)) {
            zkClient.create(LIVE_NODES, "all live nodes are displayed here", CreateMode.PERSISTENT);
        }

        /**
         * "/live_nodes/host:port"
         */
        String childNode = LIVE_NODES.concat("/").concat(nodeName);

        if (zkClient.exists(childNode)) {
            return;
        }

        zkClient.create(childNode, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
    }

    /**
     * Add node to /election children in zookeper cluster
     * <p>
     * EPHEMERAL_SEQUENTIAL mode is using since we need to have possibility to sort nodes in /election list
     *
     * @param data     - znode data
     */
    public void createNodeInElectionZnode(String data) {
        if (!zkClient.exists(ELECTION_NODE)) {
            zkClient.create(ELECTION_NODE, "election node", CreateMode.PERSISTENT);
        }

        zkClient.create(ELECTION_NODE.concat("/node"), data, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    /**
     * Get all znodes in zookeeper cluster (children /all_nodes znode)
     *
     * @return List of all znodes in zookeeper cluster
     */
    public List<String> getAllNodesInZookeeperCluster() {
        if (!zkClient.exists(ALL_NODES)) {
            throw new RuntimeException("No node /allNodes exists");
        }

        return zkClient.getChildren(ALL_NODES);
    }

    /**
     * Get live znodes in zookeeper cluster (children /all_nodes znode)
     *
     * @return List of live znodes in zookeeper cluster
     */
    public List<String> getLiveNodesInZookeeperCluster() {
        if (!zkClient.exists(LIVE_NODES)) {
            throw new RuntimeException("No node /liveNodes exists");
        }

        return zkClient.getChildren(LIVE_NODES);
    }

    /**
     * Get leade node in zookeeper cluset
     *
     * @return Master znode data
     */
    public String getLeaderNodeData() {
        if (!zkClient.exists(ELECTION_NODE)) {
            throw new RuntimeException("No node /election exists");
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
        log.info("closing connection to zookeeper...");
        zkClient.close();
        log.info("connection to zookeeper closed");
    }
}

package com.zookeeper.zookeeper.watcher;

import com.zookeeper.useCases.ClusterInformationService;
import com.zookeeper.zookeeper.service.ZookeeperService;

import org.I0Itec.zkclient.IZkChildListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.zookeeper.config.Config.ELECTION_NODE;

@Component
@RequiredArgsConstructor
@Slf4j
public class MasterChangeListener implements IZkChildListener {

    private final ZookeeperService zookeeperService;
    private final ClusterInformationService clusterInformationService;

    /**
     * listens for deletion of sequential znode under /election znode and updates the clusterinfo
     */
    @Override
    public void handleChildChange(String parentPath, List<String> currentChildren) {
        if (currentChildren.isEmpty()) {
            throw new RuntimeException("No node exists to select master!!");
        } else {
            //get least sequenced znode
            Collections.sort(currentChildren);
            String masterZNode = currentChildren.get(0);

            // once znode is fetched, fetch the znode data to get the hostname of new leader
            String masterNode = zookeeperService.getZNodeData(ELECTION_NODE.concat("/").concat(masterZNode));
            log.info("new master is: {}", masterNode);

            //update the cluster info with new leader
            clusterInformationService.setMasterNode(masterNode);
        }
    }
}
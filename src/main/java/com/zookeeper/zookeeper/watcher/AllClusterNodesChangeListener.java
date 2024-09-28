package com.zookeeper.zookeeper.watcher;

import com.zookeeper.useCases.ClusterInformationService;

import org.I0Itec.zkclient.IZkChildListener;
import org.springframework.stereotype.Component;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AllClusterNodesChangeListener implements IZkChildListener {

    private final ClusterInformationService clusterInformationService;

    /**
     * This method will be invoked for any change in /all_nodes children
     *
     * @param parent "/all_nodes"
     * @param nodes  current list of children for all_nodes. All persistent znodes in the cluster
     */
    @Override
    public void handleChildChange(String parent, List<String> nodes) {
        log.info("current list of all persistent znodes in the cluster: {}", nodes);

        clusterInformationService.rebuildAllNodesList(nodes);
    }
}

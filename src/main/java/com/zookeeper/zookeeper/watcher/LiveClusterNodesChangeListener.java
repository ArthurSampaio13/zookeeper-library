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
public class LiveClusterNodesChangeListener implements IZkChildListener {

    private final ClusterInformationService clusterInformationService;

    /**
     * This method will be invoked for any change in /live_nodes children
     *
     * @param parentPath "/all_nodes"
     * @param currentChildren  current list of children for /live_nodes. All live znodes in the cluster
     */
    @Override
    public void handleChildChange(String parentPath, List<String> currentChildren) {
        log.info("current live size: {}", currentChildren.size());
        clusterInformationService.rebuildLiveNodesList(currentChildren);
    }
}
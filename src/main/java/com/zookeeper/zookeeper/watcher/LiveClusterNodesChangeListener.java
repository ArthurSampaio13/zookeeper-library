package com.zookeeper.zookeeper.watcher;

import com.zookeeper.service.ClusterInformationService;

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

    @Override
    public void handleChildChange(String parentPath, List<String> currentChildren) {
        log.info("Tamanho on: {}", currentChildren.size());
        clusterInformationService.rebuildLiveNodesList(currentChildren);
    }
}
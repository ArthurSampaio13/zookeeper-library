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
public class AllClusterNodesChangeListener implements IZkChildListener {

    private final ClusterInformationService clusterInformationService;

    @Override
    public void handleChildChange(String parent, List<String> nodes) {
        log.info("Lista de persistent znodes no cluster: {}", nodes);

        clusterInformationService.rebuildAllNodesList(nodes);
    }
}

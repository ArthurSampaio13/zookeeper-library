package com.zookeeper.zookeeper.watcher;

        import com.zookeeper.service.ClusterInformationService;
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

    @Override
    public void handleChildChange(String parentPath, List<String> currentChildren) {
        if (currentChildren.isEmpty()) {
            throw new RuntimeException("Sem node mestre");
        } else {
            Collections.sort(currentChildren);
            String masterZNode = currentChildren.get(0);

            String masterNode = zookeeperService.getZNodeData(ELECTION_NODE.concat("/").concat(masterZNode));
            log.info("O novo mestre é: {}", masterNode);

            clusterInformationService.setMasterNode(masterNode);
        }
    }
}
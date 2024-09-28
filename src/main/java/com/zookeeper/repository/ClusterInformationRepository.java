package com.zookeeper.repository;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Repository
@Data
public class ClusterInformationRepository {

    /**
     * Name of the master node in the cluster
     */
    private String masterNode;

    /**
     * List with all persistent Apache Zookeeper znodes in the cluster (both live and terminated)
     */
    private final List<String> allClusterNodes = new ArrayList<>();

    /**
     * List with  live ephemeral Apache Zookeeper znodes in the cluster
     */
    private final List<String> liveClusterNodes = new ArrayList<>();
}

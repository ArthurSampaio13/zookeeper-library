package com.zookeeper.repository;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Repository
@Data
public class ClusterInformationRepository {

    private String masterNode;

    private final List<String> allClusterNodes = new ArrayList<>();

    private final List<String> liveClusterNodes = new ArrayList<>();
}

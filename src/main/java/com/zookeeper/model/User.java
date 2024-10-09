package com.zookeeper.model;

import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Data
public class User {

    private String id;
    private String name;
    private String email;

}

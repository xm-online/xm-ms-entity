package com.icthh.xm.ms.entity.service.swagger.model;

import lombok.Data;

@Data
public class ServerObject {
    private String url;
    private String description;

    public ServerObject(String url) {
        this();
        this.url = url;
    }

    public ServerObject() {
    }
}

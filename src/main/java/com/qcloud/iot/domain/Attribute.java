package com.qcloud.iot.domain;

import lombok.Data;

import java.util.Map;

/**
 * @author matas
 * @date 2019/11/20 11:07
 * @email mataszhang@163.com
 */
@Data
public class Attribute {
    private String access;
    private Map<String, String> contentInfo;
    private String contentType;
    private String index;
    private String name;
    private String value;

    public Attribute() {

    }

    public Attribute(String index, String value) {
        this.index = index;
        this.value = value;
    }
}

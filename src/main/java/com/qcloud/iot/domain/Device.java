package com.qcloud.iot.domain;

import lombok.Data;

import java.util.List;

/**
 * @author matas
 * @date 2019/11/20 10:59
 * @email mataszhang@163.com
 */
@Data
public class Device {
    private String deviceId;
    private String hostId;
    private String classify;
    private String name;
    private Integer aliasId;
    private String type;
    private String manufacturerName;
    private String vaddr;
    private List<Attribute> attributes;

}

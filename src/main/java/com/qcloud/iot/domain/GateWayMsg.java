package com.qcloud.iot.domain;

import lombok.Data;

import java.util.List;

/**
 * @author matas
 * @date 2019/11/20 10:58
 * @email mataszhang@163.com
 */
@Data
public class GateWayMsg {
    private Integer msgType;
    private Integer ack;
    private String msgId;
    private String msg;
    private Integer code;
    private String timeStamp;
    private List<Device> data;

}

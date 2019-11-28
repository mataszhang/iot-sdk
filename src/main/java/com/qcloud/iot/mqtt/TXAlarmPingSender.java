package com.qcloud.iot.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.TimerPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;


@Slf4j
public class TXAlarmPingSender extends TimerPingSender {
    private String clientId;

    @Override
    public void init(ClientComms comms) {
        super.init(comms);
        this.clientId = comms.getClient().getClientId();
    }

    @Override
    public void start() {
        super.start();
        log.debug("MQTT心跳启动,clientId=>{} ", clientId);
    }

    @Override
    public void stop() {
        super.stop();
        log.debug("MQTT心跳停止, clientId=>{} ", clientId);

    }

    @Override
    public void schedule(long delayInMilliseconds) {
        super.schedule(delayInMilliseconds);
        log.debug("MQTT心跳包发送完成，clientId=>{} ,下次心跳包发送时间为=>{}ms后", clientId, delayInMilliseconds);
    }
}

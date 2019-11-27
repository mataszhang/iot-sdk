package com.qcloud.iot.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;


@Slf4j
public class TXAlarmPingSender implements MqttPingSender {

    public static final String TAG = "iot.TXAlarmPingSender";

    private ClientComms mComms;

    private TXAlarmPingSender that;

    private volatile boolean hasStarted = false;

    public TXAlarmPingSender() {
        that = this;
    }

    @Override
    public void init(ClientComms comms) {
        this.mComms = comms;
    }

    @Override
    public void start() {
        String action = TXMqttConstants.PING_SENDER + mComms.getClient().getClientId();
        log.debug("Register alarmreceiver to Context " + action);
        schedule(mComms.getKeepAlive());
        hasStarted = true;
    }

    @Override
    public void stop() {
        log.debug("Unregister alarmreceiver to Context " + mComms.getClient().getClientId());
        if (hasStarted) {
            hasStarted = false;
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        long nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
        log.debug("Schedule next alarm at " + nextAlarmInMilliseconds);
        log.debug("Alarm scheule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
    }
}

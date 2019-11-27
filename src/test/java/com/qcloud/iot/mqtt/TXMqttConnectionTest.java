package com.qcloud.iot.mqtt;

import com.qcloud.iot.common.Status;
import com.qcloud.iot.util.AsymcSslUtils;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.concurrent.TimeUnit;

@Slf4j
public class TXMqttConnectionTest extends TestCase {

    public void testConnect() throws InterruptedException {
        String serverURI = "ssl://iotcloud-mqtt.gz.tencentdevices.com:8883";
        String productId = "1M8L1A6TIF";
        String deviceName = "dev-4zpWoUJx";
        String psk = "SLeVt77zcA0zvrAMMji9uQ==";

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);

        TXMqttConnection connection = new TXMqttConnection(serverURI, productId, deviceName, psk, bufferOptions, null, new TestCallBack());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(5);
        options.setAutomaticReconnect(true);
        options.setSocketFactory(AsymcSslUtils.getSocketFactory());
        connection.connect(options, null);

        new Thread(() -> {
            while (true) {
                TXMqttConstants.ConnectStatus connectStatus = connection.getConnectStatus();
                log.info("====检测连接状态=>{}", connectStatus);

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        Thread.currentThread().join();
    }


    private static class TestCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            log.info("mqtt链接成功, status=>{}, 是否重连=>{}, msg=>{}", status, reconnect, msg);
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            log.info("mqtt链接断开");
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
        }
    }

}
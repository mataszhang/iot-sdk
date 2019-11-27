package com.qcloud.iot.mqtt;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.qcloud.iot.common.Status;
import com.qcloud.iot.domain.Attribute;
import com.qcloud.iot.domain.Device;
import com.qcloud.iot.domain.GateWayMsg;
import com.qcloud.iot.util.AsymcSslUtils;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TXMqttConnectionTest extends TestCase {

    public void testConnect() throws InterruptedException {
        String serverURI = "ssl://iotcloud-mqtt.gz.tencentdevices.com:8883";
        String productId = "1M8L1A6TIF";
        String deviceName = "dev-4zpWoUJx";
        String psk = "SLeVt77zcA0zvrAMMji9uQ==";
        String topic = String.format("%s/%s/%s", productId, deviceName, "event");

        //断线后消息缓存
        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);

        //连接对象
        TXMqttConnection connection = new TXMqttConnection(serverURI, productId, deviceName, psk, bufferOptions, null, new TestCallBack());

        //连接参数选项
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(5);
        options.setAutomaticReconnect(true);
        options.setSocketFactory(AsymcSslUtils.getSocketFactory());
        connection.connect(options, null);


        //检测mqtt链接状态
        new Thread(() -> {
            while (true) {
                MqttAsyncClient mMqttClient = connection.mMqttClient;
                log.info("====检测MQTT是否连接=>{}", mMqttClient.isConnected());

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        //发送网关在线消息
        new Thread(() -> {
            while (true) {
                GateWayMsg pushMsg = new GateWayMsg();
                pushMsg.setMsgType(1);
                pushMsg.setAck(0);
                pushMsg.setMsgId(UUID.randomUUID().toString());
                pushMsg.setMsg("report");
                pushMsg.setCode(200);
                pushMsg.setTimeStamp(System.currentTimeMillis() + "");

                List<Device> deviceList = Lists.newArrayList();
                Device device = new Device();
                device.setDeviceId(deviceName);
                device.setHostId(deviceName);
                device.setClassify("direct");
                device.setName("test-gw-3");
                device.setType("1");
                device.setManufacturerName("JOBO");

                List<Attribute> attributes = Lists.newArrayList(new Attribute("1001", "1"));

                device.setAttributes(attributes);
                deviceList.add(device);
                pushMsg.setData(deviceList);

                MqttMessage mqttMessage = new MqttMessage(new Gson().toJson(pushMsg).getBytes());
                mqttMessage.setQos(TXMqttConstants.QOS1);

                if (connection.getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnected)) {
                    connection.publish(topic, mqttMessage, null);
                }

                try {
                    TimeUnit.SECONDS.sleep(10);
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
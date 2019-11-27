package com.qcloud.iot.gateway;


import com.google.gson.Gson;
import com.qcloud.iot.common.Status;
import com.qcloud.iot.domain.Attribute;
import com.qcloud.iot.domain.Device;
import com.qcloud.iot.domain.GateWayMsg;
import com.qcloud.iot.log.TXMqttLogCallBack;
import com.qcloud.iot.mqtt.TXAlarmPingSender;
import com.qcloud.iot.mqtt.TXMqttActionCallBack;
import com.qcloud.iot.mqtt.TXMqttConnection;
import com.qcloud.iot.mqtt.TXMqttConstants;
import com.qcloud.iot.util.Base64;
import com.qcloud.iot.util.HmacSha256;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.qcloud.iot.mqtt.TXMqttConstants.DEFAULT_SERVER_URI;
import static com.qcloud.iot.mqtt.TXMqttConstants.MQTT_SDK_VER;

/**
 * Created by willssong on 2018/12/25.
 */
@Slf4j
public class TXGatewayConnection extends TXMqttConnection {
    public static final String TAG = "TXMQTT" + MQTT_SDK_VER;

    private HashMap<String, TXGatewaySubdev> mSubdevs = new HashMap<String, TXGatewaySubdev>();
    private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";
    private static final String GW_OPERATION_PREFIX = "$gateway/operation/";


    public TXGatewayConnection(String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                               MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack) {
        super(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, mqttLogFlag, logCallBack, callBack);
    }

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI         服务器URI，腾讯云默认唯一地址 TXMqttConstants.DEFAULT_SERVER_URI="ssl://connect.iot.qcloud.com:8883"
     * @param productID         产品名
     * @param deviceName        设备名，唯一
     * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXGatewayConnection(String serverURI, String productID, String deviceName, String secretKey,
                               DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack) {
        this(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, true, logCallBack, callBack);
    }

    /**
     * @param context
     * @param productID
     * @param deviceName
     * @param secretKey
     * @param bufferOpts
     * @param clientPersistence
     * @param callBack
     */
    public TXGatewayConnection(String productID, String deviceName, String secretKey,
                               DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
                               TXMqttActionCallBack callBack) {
        this(DEFAULT_SERVER_URI, productID, deviceName, secretKey, bufferOpts, clientPersistence, false, null, callBack);
    }

    /**
     * @param context
     * @param productID
     * @param deviceName
     * @param secretKey
     * @param bufferOpts
     * @param callBack
     */
    public TXGatewayConnection(String productID, String deviceName, String secretKey,
                               DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
        this(productID, deviceName, secretKey, bufferOpts, null, callBack);
    }

    public TXGatewayConnection(String srvURL, String productID, String deviceName,
                               String secretKey, TXMqttActionCallBack callBack) {
        this(srvURL, productID, deviceName, secretKey, null, null, false, null, callBack);
    }

    /**
     * @param context
     * @param productID
     * @param deviceName
     * @param secretKey
     * @param callBack
     */
    public TXGatewayConnection(String productID, String deviceName,
                               String secretKey, TXMqttActionCallBack callBack) {
        this(productID, deviceName, secretKey, null, null, callBack);
    }


    /**
     * @param productId
     * @param devName
     * @return null if not existed otherwise the subdev
     */
    private TXGatewaySubdev findSubdev(String productId, String devName) {
        log.debug("The hashed information is " + mSubdevs);
        return mSubdevs.get(productId + devName);
    }

    /**
     * remove the subdev if it is offline
     *
     * @param subdev
     * @return the operation results
     */
    private synchronized TXGatewaySubdev removeSubdev(TXGatewaySubdev subdev) {
        return mSubdevs.remove(subdev.mProductId + subdev.mDevName);
    }

    /**
     * remove the subdev if it is offline
     *
     * @param productId
     * @param devName
     * @return
     */
    private synchronized TXGatewaySubdev removeSubdev(String productId, String devName) {
        return mSubdevs.remove(productId + devName);
    }

    /**
     * add a new subdev entry
     *
     * @param dev
     */
    private synchronized void addSubdev(TXGatewaySubdev dev) {
        mSubdevs.put(dev.mProductId + dev.mDevName, dev);
    }

    /**
     * Get the subdev status
     *
     * @param productId
     * @param devName
     * @return the status of subdev
     */
    public Status getSubdevStatus(String productId, String devName) {
        TXGatewaySubdev subdev = findSubdev(productId, devName);
        if (subdev == null) {
            return Status.SUBDEV_STAT_NOT_EXIST;
        }
        return subdev.getSubdevStatus();
    }

    /**
     * set the status of the subdev
     *
     * @param productId
     * @param devName
     * @param stat
     * @return the status of operation
     */
    public Status setSubdevStatus(String productId, String devName, Status stat) {
        TXGatewaySubdev subdev = findSubdev(productId, devName);
        if (subdev == null) {
            return Status.SUBDEV_STAT_NOT_EXIST;
        }
        subdev.setSubdevStatus(stat);
        return Status.OK;
    }

    /**
     * publish the offline message for the subdev
     *
     * @param subProductID
     * @param subDeviceName
     * @return the result of operation
     */

    private int status = 1;

    public Status gatewaySubdevOffline(String subProductID, String subDeviceName) {
        /*log.debug( "Try to find " + subProductID + " & " + subDeviceName);
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if (subdev == null) {
            log.debug( "Cant find the subdev");
            return Status.SUBDEV_STAT_OFFLINE;
        }*/
        String topic = "1M8L1A6TIF/dev-w7ZsXcPQ/event";

       /* log.debug( "set " + subProductID + " & " + subDeviceName + " to offline");

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "offline");
            JSONObject plObj = new JSONObject();
            String strDev = "[{'product_id':'" + subProductID +"','device_name':'" + subDeviceName + "'}]";
            JSONArray devs = new JSONArray(strDev);
            plObj.put("devices", devs);
            obj.put("payload", plObj);
        } catch (JSONException e) {
            return Status.ERROR;
        }*/

        GateWayMsg msg = new GateWayMsg();
        msg.setMsgType(3);
        msg.setAck(0);
        msg.setMsgId(UUID.randomUUID().toString());
        msg.setMsg("report device");
        msg.setCode(0);
        msg.setTimeStamp(System.currentTimeMillis() + "");

        List<Device> devices = new ArrayList<>();
        Device device = new Device();
        device.setDeviceId("dev-w7ZsXcPQ-2");
        device.setHostId("dev-w7ZsXcPQ");
        device.setClassify("sub");
        device.setName("灯");
        device.setAliasId(6002);
        device.setType("100");
        device.setManufacturerName("JOBO");
        device.setVaddr("LIGHT-11");

        List<Attribute> attributes = new ArrayList<>();
        Attribute attr = new Attribute();
        attr.setIndex("1001");
        attr.setValue("1");
        attributes.add(attr);

        attr = new Attribute();
        attr.setIndex("1002");

        if (status == 1) {
            attr.setValue("1");
            status = 0;
        } else {
            attr.setValue("0");
            status = 1;
        }

        attributes.add(attr);

        device.setAttributes(attributes);
        devices.add(device);

        msg.setData(devices);

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(new Gson().toJson(msg).getBytes());
        log.debug("topic=>" + topic + " , publish message =>" + message);
        return super.publish(topic, message, null);
    }

    public Status gatewaySubdevOnline(String subProductID, String subDeviceName) {
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if (subdev == null) {
            log.debug("Cant find the subdev");
            subdev = new TXGatewaySubdev(subProductID, subDeviceName);
        }
        //String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;
        //log.debug( "set " + subProductID + " & " + subDeviceName + " to Online");
        String topic = "1M8L1A6TIF/dev-w7ZsXcPQ/event";

        // format the payload
        /*JSONObject obj = new JSONObject();
        try {
            obj.put("type", "online");
            JSONObject plObj = new JSONObject();
            String strDev = "[{'product_id':'" + subProductID +"','device_name':'" + subDeviceName + "'}]";
            JSONArray devs = new JSONArray(strDev);
            plObj.put("devices", devs);
            obj.put("payload", plObj);
        } catch (JSONException e) {
            return Status.ERROR;
        }
        addSubdev(subdev);*/


        GateWayMsg msg = new GateWayMsg();
        msg.setMsgType(1);
        msg.setAck(1);
        msg.setMsgId(UUID.randomUUID().toString());
        msg.setMsg("report device");
        msg.setCode(200);
        msg.setTimeStamp(System.currentTimeMillis() + "");

        List<Device> devices = new ArrayList<>();
        Device device = new Device();
        device.setDeviceId("dev-w7ZsXcPQ#lighting#SC#111212121");
        device.setHostId("dev-w7ZsXcPQ");
        device.setClassify("sub");
        device.setName("灯");
        device.setAliasId(6002);
        device.setType("100");
        device.setManufacturerName("JOBO");
        device.setVaddr("LIGHT-11");

        List<Attribute> attributes = new ArrayList<>();
        Attribute attr = new Attribute();
        attr.setIndex("1001");
        attr.setValue("1");
        attributes.add(attr);

        attr = new Attribute();
        attr.setIndex("1002");
        attr.setValue("1");
        attributes.add(attr);

        device.setAttributes(attributes);
        devices.add(device);
        //-----gw device
        device = new Device();
        device.setDeviceId("dev-w7ZsXcPQ");
        device.setHostId("dev-w7ZsXcPQ");
        device.setClassify("direct");
        device.setName("test-gw-2");
        //device.setAliasId(6002);
        device.setType("1");
        device.setManufacturerName("JOBO");
        //device.setVaddr("LIGHT-11");
        devices.add(device);


        msg.setData(devices);

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(new Gson().toJson(msg).getBytes());
        log.debug("topic=>" + topic + " , publish message =>" + message);

        return super.publish(topic, message, null);
    }

    private boolean consumeGwOperationMsg(String topic, MqttMessage message) {
        if (!topic.startsWith(GW_OPERATION_RES_PREFIX)) {
            return false;
        }
        log.debug("got gate operation messga " + topic + message);
        String productInfo = topic.substring(GW_OPERATION_RES_PREFIX.length());
        int splitIdx = productInfo.indexOf('/');
        String productId = productInfo.substring(0, splitIdx);
        String devName = productInfo.substring(splitIdx + 1);

        TXGatewaySubdev subdev = findSubdev(productId, devName);

        // this subdev is not managed by me
        if (subdev == null) {
            return false;
        }

        try {
            byte[] payload = message.getPayload();
            JSONObject jsonObject = new JSONObject(new String(payload));

            String type = jsonObject.getString("type");
            if (type.equalsIgnoreCase("online")) {
                String res = jsonObject.getString("result");

                if (res.equals("0")) {
                    subdev.setSubdevStatus(Status.SUBDEV_STAT_ONLINE);
                }

            } else if (type.equalsIgnoreCase("offline")) {
                String res = jsonObject.getString("result");

                if (res.equals("0")) {
                    removeSubdev(subdev);
                }
            }

        } catch (JSONException e) {

        }

        return true;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.debug("message received " + topic);
        if (!consumeGwOperationMsg(topic, message)) {
            super.messageArrived(topic, message);
        }
    }

    @Override
    public synchronized Status connect(MqttConnectOptions options, Object userContext) {
        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            log.info("The client is connecting. Connect return directly.");
            return Status.MQTT_CONNECT_IN_PROGRESS;
        }

        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnected)) {
            log.info("The client is already connected. Connect return directly.");
            return Status.OK;
        }

        this.mConnOptions = options;
        if (mConnOptions == null) {
            log.error("Connect options == null, will not connect.");
            return Status.PARAMETER_INVALID;
        }

        Long timestamp = System.currentTimeMillis() / 1000 + 600;
        String userNameStr = mUserName + ";" + getConnectId() + ";" + timestamp;

        mConnOptions.setUserName(userNameStr);

        if (mSecretKey != null && mSecretKey.length() != 0) {
            try {
                log.debug("secret is " + mSecretKey);
                String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(), Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
                mConnOptions.setPassword(passWordStr.toCharArray());
            } catch (IllegalArgumentException e) {
                log.debug("Failed to set password");
            }
        }

        mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        IMqttActionListener mActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                log.info("onSuccess!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
                mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(), "connected to " + mServerURI);
                // If the connection is established, subscribe the gateway operation topic
                String gwTopic = GW_OPERATION_RES_PREFIX + mProductId + "/" + mDeviceName;
                int qos = TXMqttConstants.QOS1;

                subscribe(gwTopic, qos, "Subscribe GATEWAY result topic");
                log.debug("Connected, then subscribe the gateway result topic");

                if (mMqttLogFlag) {
                    initMqttLog(TAG);
                }
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                log.error(exception.getMessage(), exception);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString());
            }
        };

        if (mMqttClient == null) {
            try {
                mPingSender = new TXAlarmPingSender();
                mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist, mPingSender);
                mMqttClient.setCallback(this);
                mMqttClient.setBufferOpts(super.bufferOpts);
                mMqttClient.setManualAcks(false);
            } catch (Exception e) {
                log.error("new MqttClient failed", e);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        }

        try {
            log.info("Start connecting to %s", mServerURI);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
            mMqttClient.connect(mConnOptions, userContext, mActionListener);
        } catch (Exception e) {
            log.error("MqttClient connect failed", e);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
            return Status.ERROR;
        }

        return Status.OK;
    }
}

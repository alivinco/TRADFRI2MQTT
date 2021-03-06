package com.alivinco.tradfri;

import com.alivinco.fimp.FimpAddress;
import com.alivinco.fimp.FimpMessage;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import static com.alivinco.tradfri.TradfriConstants.*;
import static com.alivinco.tradfri.TradfriConstants.DIMMER;
import static com.alivinco.tradfri.TradfriConstants.INSTANCE_ID;

/**
 * Created by alivinco on 02/07/2017.
 */
public class MsgRouter {
    private DeviceDb deviceDb;
    private AdapterApi adApi;
    private FimpApi fimpApi;
    private TradfriApi tradfriApi;

    public MsgRouter(AdapterApi adApi,FimpApi fimpApi,TradfriApi tradfriApi,DeviceDb deviceDb) {
        this.adApi = adApi;
        this.fimpApi = fimpApi;
        this.tradfriApi = tradfriApi;
        this.deviceDb = deviceDb;
    }

    public void onMqttMessage(String topic, MqttMessage message) throws Exception {
        // TODO Auto-generated method stub
        System.out.println(topic + " " + message.toString());
        FimpMessage fimp = FimpMessage.stringToMsg(message.toString());
        FimpAddress fimpAddr = FimpAddress.parseStringAddress(topic);

        if (adApi.onMessage(topic,fimp)){
            return;
        }
        JSONObject reqJson = new JSONObject(message.toString());
        boolean isDevice = true;
        int id = Integer.parseInt(fimpAddr.serviceAddress);
        Device dev = deviceDb.getDeviceById(id);
        if (dev.type.equals("group"))
            isDevice = false;
        System.out.println(id);
        try{
            if (fimp.mtype.equals("cmd.binary.set")) {
                if (isDevice)
                    this.tradfriApi.lightSwitchCtrl(id,fimp.getBoolValue());
                else
                    this.tradfriApi.lightSwitchGroupCtrl(id,fimp.getBoolValue());
            }else if (fimp.mtype.equals("cmd.lvl.set")) {
                int lvlValue = fimp.getIntValue();
                int duration = 3;
                if (fimp.props.get("duration")!=null)
                    duration = Integer.parseInt(fimp.props.get("duration"));
                if (isDevice)
                    this.tradfriApi.lightDimmerCtrl(id,lvlValue,duration);
                else
                    this.tradfriApi.lightDimmerGroupCtrl(id,lvlValue,duration);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onCoapMessage(CoapResponse response) {
        System.out.println(response.getResponseText());
        System.out.println(response.getOptions().toString());
        try {
            JSONObject json = new JSONObject(response.getResponseText());
            //TODO change this test to something based on 5750 values
            // 2 = light?
            // 0 = remote/dimmer?
            if (json.has(LIGHT) && (json.has(TYPE) && json.getInt(TYPE) == 2)) { // single bulb

                JSONObject light = json.getJSONArray(LIGHT).getJSONObject(0);
                JSONObject device = json.getJSONObject(DEVICE);
                deviceDb.upsertDevice(json.getInt(INSTANCE_ID),json.getString(NAME),device.getString(PRODUCT_NAME),device.getString(DEVICE_MANUFACTURER),"light_bulb","device",device.getString(DEVICE_VERSION));

                if (light.has(ONOFF)) {
                    boolean stateBool = (light.getInt(ONOFF) != 0);
                    fimpApi.reportSwitchStateChange(json.getInt(INSTANCE_ID),stateBool);
                }else {
                    System.err.println("Bulb '" + json.getString(NAME) + "' has no On/Off value (probably no power on lightbulb socket)");
                    return; // skip this lamp for now
                }

                if (light.has(DIMMER)) {
                    int level = light.getInt(DIMMER);
                    fimpApi.reportDimLvlChange(json.getInt(INSTANCE_ID),level);
                } else {
                    System.err.println("Bulb '" + json.getString(NAME) + "' has no dimming value (maybe just no power on lightbulb socket)");
                }

                MqttMessage message3 = null;
                if (light.has(COLOR)) {
                    message3 = new MqttMessage();
                    String temperature = light.getString(COLOR);
                    message3.setPayload(temperature.getBytes());
                } else { // just fyi for the user. maybe add further handling later
                    System.out.println("Bulb '" + json.getString(NAME) + "' doesn't support color temperature");
                }

            } else if (json.has(HS_ACCESSORY_LINK)) { // groups have this entry
                //room?
                System.out.println("room");
                JSONObject group = json.getJSONObject(HS_ACCESSORY_LINK);
                deviceDb.upsertDevice(json.getInt(INSTANCE_ID),json.getString(NAME),"","","light_bulb","group","");

                if (json.has(ONOFF)) {
                    boolean stateBool = (json.getInt(ONOFF) != 0);
                    fimpApi.reportSwitchStateChange(json.getInt(INSTANCE_ID),stateBool);
                }else {
                    System.out.println("No switch in the group ");
                }
                if (json.has(DIMMER)) {
                    int level = json.getInt(DIMMER);
                    fimpApi.reportDimLvlChange(json.getInt(INSTANCE_ID),level);
                }else {
                    System.out.println("No dim level in the group ");
                }
            } else {
                System.out.println("not bulb");
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

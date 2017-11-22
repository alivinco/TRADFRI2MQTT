package com.alivinco.tradfri;

import com.alivinco.fimp.FimpMessage;
import com.alivinco.tradfri.DeviceDb;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Logger;

/**
 * Created by alivinco on 02/05/2017.
 */
public class FimpApi {
    Logger logger = Logger.getLogger("ikea");
    MqttClient mqttClient;
    DeviceDb deviceDb;
    String binSwitchTopic = "pt:j1/mt:evt/rt:dev/rn:ikea/ad:1/sv:out_bin_switch/ad:";
    String lvlSwitchTopic = "pt:j1/mt:evt/rt:dev/rn:ikea/ad:1/sv:out_lvl_switch/ad:";
    String colorCtrlTopic = "pt:j1/mt:evt/rt:dev/rn:ikea/ad:1/sv:color_ctrl/ad:";
    //
    public FimpApi(MqttClient mqttClient, DeviceDb deviceDb){
        this.deviceDb = deviceDb;
        this.mqttClient = mqttClient;
    }

    public void reportSwitchStateChange(int id , boolean state ){

        if (!this.deviceDb.updateLightState(id,state)){
            logger.fine("Switch value is not changed , skipp.");
            return ;
        }

        FimpMessage binSwitchFimp = new FimpMessage("out_bin_switch","evt.binary.report",state,null,null,null);
        MqttMessage binSwitchMsgMqtt = new MqttMessage();
        try {
            binSwitchMsgMqtt.setPayload(binSwitchFimp.msgToString().getBytes());
            try {
                this.mqttClient.publish(binSwitchTopic+Integer.toString(id) , binSwitchMsgMqtt);
            } catch (MqttException e) {
                logger.warning(e.getMessage());
            }
        } catch (JSONException e) {
            logger.warning(e.getMessage());
        }

    }

    public void reportDimLvlChange(int id , int level ){
        if (!this.deviceDb.updateDimLevel(id,level)){
            logger.fine("Dim value is not changed , skipp.");
            return ;
        }
        level = (int)Math.round(level/2.55);
        FimpMessage lvlSwitchFimp = new FimpMessage("out_lvl_switch","evt.lvl.report",level,null,null,null);
        MqttMessage lvlSwitchMsgMqtt = new MqttMessage();
        try {
            lvlSwitchMsgMqtt.setPayload(lvlSwitchFimp.msgToString().getBytes());
            try {
                this.mqttClient.publish(lvlSwitchTopic+Integer.toString(id) , lvlSwitchMsgMqtt);
            } catch (MqttException e) {
                logger.warning(e.getMessage());
            }
        } catch (JSONException e) {
            logger.warning(e.getMessage());
        }

    }

    public void reportColorChange(int id , int red,int green,int blue ) {
        JSONObject value = new JSONObject();
        if (!this.deviceDb.updateColorState(id,red,green,blue)){
            logger.fine("Color value is not changed , skipp.");
            return ;
        }
        try {
            value.put("red", red);
            value.put("green", green);
            value.put("blue", blue);
            FimpMessage lvlSwitchFimp = new FimpMessage("cmd.color.get_report", "evt.color.report", "int_map", value, null, null, null);
            MqttMessage lvlSwitchMsgMqtt = new MqttMessage();
            try {
                lvlSwitchMsgMqtt.setPayload(lvlSwitchFimp.msgToString().getBytes());
                try {
                    this.mqttClient.publish(colorCtrlTopic + Integer.toString(id), lvlSwitchMsgMqtt);
                } catch (MqttException e) {
                    logger.warning(e.getMessage());
                }
            } catch (JSONException e) {
                logger.warning(e.getMessage());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}

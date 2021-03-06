package com.alivinco.tradfri;

import com.alivinco.fimp.FimpMessage;
import com.alivinco.tradfri.DeviceDb;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;

/**
 * Created by alivinco on 02/05/2017.
 */
public class FimpApi {
    MqttClient mqttClient;
    DeviceDb deviceDb;
    String binSwitchTopic = "pt:j1/mt:evt/rt:dev/rn:ikea/ad:1/sv:out_bin_switch/ad:";
    String lvlSwitchTopic = "pt:j1/mt:evt/rt:dev/rn:ikea/ad:1/sv:out_lvl_switch/ad:";
    //
    public FimpApi(MqttClient mqttClient, DeviceDb deviceDb){
        this.deviceDb = deviceDb;
        this.mqttClient = mqttClient;
    }

    public void reportSwitchStateChange(int id , boolean state ){

        if (!this.deviceDb.updateLightState(id,state)){
            System.out.println("Switch value is not changed , skipp.");
            return ;
        }

        FimpMessage binSwitchFimp = new FimpMessage("out_bin_switch","evt.binary.report",state,null,null,null);
        MqttMessage binSwitchMsgMqtt = new MqttMessage();
        try {
            binSwitchMsgMqtt.setPayload(binSwitchFimp.msgToString().getBytes());
            try {
                this.mqttClient.publish(binSwitchTopic+Integer.toString(id) , binSwitchMsgMqtt);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void reportDimLvlChange(int id , int level ){
        if (!this.deviceDb.updateDimLevel(id,level)){
            System.out.println("Dim value is not changed , skipp.");
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
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}

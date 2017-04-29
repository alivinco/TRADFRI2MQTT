package uk.me.hardill.TRADFRI2MQTT;

import com.alivinco.fimp.FimpMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by alivinco on 28/04/2017.
 */
public class AdapterApi {
    DeviceDb deviceDb;
    MqttClient mqttClient;

    public AdapterApi(DeviceDb deviceDb,MqttClient mqttClient){
        this.deviceDb = deviceDb;
        this.mqttClient = mqttClient;

    }
    public void onMessage(String topic , FimpMessage fimp){
        if (fimp.service == "ikead-ad"){
            if (fimp.mtype == "cmd.network.get_all_nodes") {


                JSONArray jsonArray ;

                Map <Integer,Device> deviceDb = this.deviceDb.getAllDevices();
                deviceDb.forEach((id,device)->{
                   JSONObject dev = new JSONObject();
//                   jsonArray.put()
                });

                FimpMessage fimpResp = new FimpMessage("ikea-ad","evt.network.all_nodes_report","object",report,null,null,fimp.uid);
                try {
                    mqttClient.publish("pt:j1/mt:evt/rt:ad/rn:ikea/ad:1",fimpResp.msgToString().getBytes(),1,false);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }else if (fimp.mtype == "evt.thing.get_inclusion_report"){

            }
        }
    }
}

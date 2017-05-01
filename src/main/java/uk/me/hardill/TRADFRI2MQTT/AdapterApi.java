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
    public boolean onMessage(String topic , FimpMessage fimp){
        System.out.println("New message of type ="+fimp.mtype+ " service = "+fimp.service);
        if (fimp.service.equals("ikea-ad")){
            System.out.println("Service is ok");
            if (fimp.mtype.equals("cmd.network.get_all_nodes")) {

                System.out.println("cmd.network.get_all_nodes");
                JSONArray jsonArray = new JSONArray();

                Map <Integer,Device> deviceDb = this.deviceDb.getAllDevices();
                deviceDb.forEach((id,device)->{
                   JSONObject dev = new JSONObject();
                    try {
                        dev.put("id",id);
                        dev.put("type",device.type);
                        dev.put("service_type",device.serviceType);
                        jsonArray.put(dev);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                });

                FimpMessage fimpResp = new FimpMessage("ikea-ad","evt.network.all_nodes_report",jsonArray,null,null,fimp.uid);

                try {
                    System.out.println("Sending response :"+fimpResp.msgToString());
                    mqttClient.publish("pt:j1/mt:evt/rt:ad/rn:ikea/ad:1",fimpResp.msgToString().getBytes(),1,false);
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;

            }else if (fimp.mtype == "evt.thing.get_inclusion_report"){
                return false;
            }
        }
        return false;
    }
}

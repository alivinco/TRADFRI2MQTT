package uk.me.hardill.TRADFRI2MQTT;

import com.alivinco.fimp.FimpMessage;
import com.oracle.javafx.jmx.json.JSONFactory;
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
                        dev.put("alias",device.alias);
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

            }else if (fimp.mtype.equals("cmd.thing.get_inclusion_report")){
                String id = fimp.getStringValue();
                Device dev = deviceDb.getDeviceById(Integer.parseInt(id));
                JSONObject jdev = new JSONObject();
                try {

                    JSONArray services = new JSONArray();
                    services.put(getBinSwitchServiceDescriptor(id));
                    services.put(getLvlSwitchSeviceDescriptor(id));
                    jdev.put("address",id);
                    jdev.put("comm_tech","");
                    jdev.put("device_id","");
                    jdev.put("alias",dev.alias);
                    jdev.put("hw_ver",dev.hwVersion);
                    jdev.put("sw_ver",dev.swVersion);
                    jdev.put("manufacturer_id",dev.manufacturer);
                    jdev.put("power_source","ac");
                    jdev.put("product_hash",dev.productName);
                    jdev.put("product_id",dev.productName);
                    jdev.put("services",services);
                    FimpMessage fimpResp = new FimpMessage("ikea-ad","evt.thing.inclusion_report",jdev,null,null,fimp.uid);
                    try {
                        System.out.println("Sending response :"+fimpResp.msgToString());
                        mqttClient.publish("pt:j1/mt:evt/rt:ad/rn:ikea/ad:1",fimpResp.msgToString().getBytes(),1,false);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    private JSONObject getInterfaceJobj(String interfaceType,String msgType ,String valueType) throws JSONException {
        JSONObject intf = new JSONObject();
        intf.put("intf_t",interfaceType);
        intf.put("msg_t",msgType);
        intf.put("val_t",valueType);
        intf.put("ver","1");
        return intf;
    }

    private JSONObject getBinSwitchServiceDescriptor(String id) throws JSONException {
        JSONObject switchService = new JSONObject();
        switchService.put("address","/rt:dev/rn:ikea/ad:1/sv:out_bin_switch/ad:"+id);
        switchService.put("group",new JSONArray().put("ch_1"));
        switchService.put("name","out_bin_switch");
        switchService.put("location","");
        switchService.put("props",new JSONObject());
        JSONArray interfaces = new JSONArray();
        interfaces.put(getInterfaceJobj("out","evt.binary.report","bool"));
        interfaces.put(getInterfaceJobj("in","cmd.binary.set","bool"));
        interfaces.put(getInterfaceJobj("in","cmd.binary.get_report","null"));
        switchService.put("interfaces",interfaces);
        return switchService;
    }

    private JSONObject getLvlSwitchSeviceDescriptor(String id) throws JSONException {
        JSONObject switchService = new JSONObject();
        switchService.put("address","/rt:dev/rn:ikea/ad:1/sv:out_lvl_switch/ad:"+id);
        switchService.put("group",new JSONArray().put("ch_1"));
        switchService.put("name","out_lvl_switch");
        switchService.put("location","");
        switchService.put("props",new JSONObject());
        JSONArray interfaces = new JSONArray();
        interfaces.put(getInterfaceJobj("out","evt.lvl.report","int"));
        interfaces.put(getInterfaceJobj("in","cmd.lvl.set","int"));
        interfaces.put(getInterfaceJobj("in","cmd.lvl.start","string"));
        interfaces.put(getInterfaceJobj("in","cmd.lvl.stop","null"));
        interfaces.put(getInterfaceJobj("in","cmd.lvl.get_report","null"));
        interfaces.put(getInterfaceJobj("in","cmd.binary.set","bool"));
        switchService.put("interfaces",interfaces);
        return switchService;

    }
}

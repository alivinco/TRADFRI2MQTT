package com.alivinco.tradfri;

import com.alivinco.fimp.FimpMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.alivinco.tradfri.TradfriConstants.*;

/**
 * Created by alivinco on 28/04/2017.
 */
public class AdapterApi {
    private DeviceDb deviceDb;
    private MqttClient mqttClient;
    private TradfriApi tradfriApi;
    private String requestId;
    Logger logger = Logger.getLogger("ikea");
    public AdapterApi(DeviceDb deviceDb,MqttClient mqttClient , TradfriApi tradfriApi){
        this.deviceDb = deviceDb;
        this.mqttClient = mqttClient;
        this.tradfriApi = tradfriApi;

    }
    public boolean onMessage(String topic , FimpMessage fimp){
        logger.fine("New message of type ="+fimp.mtype+ " service = "+fimp.service);
        if (fimp.service.equals("ikea")){
            logger.fine("Service is ok");
            if (fimp.mtype.equals("cmd.network.get_all_nodes")) {
                logger.info("cmd.network.get_all_nodes");
                JSONArray jsonArray = new JSONArray();

                Map <Integer,Device> deviceDb = this.deviceDb.getAllDevices();
                for (Map.Entry<Integer,Device>  entry : deviceDb.entrySet()) {
                    int id = entry.getKey();
                    Device device = entry.getValue();
                    JSONObject dev = new JSONObject();
                    try {
                        dev.put("address",Integer.toString(id));
                        dev.put("alias",device.alias);
                        dev.put("type",device.type);
                        dev.put("service_type",device.serviceType);
                        jsonArray.put(dev);
                    } catch (JSONException e) {
//                        e.printStackTrace();
                        logger.warning(e.getMessage());
                    }
                }
                FimpMessage fimpResp = new FimpMessage("ikea","evt.network.all_nodes_report",jsonArray,null,null,fimp.uid);

                try {
                    logger.fine("Sending response :"+fimpResp.msgToString());
                    mqttClient.publish("pt:j1/mt:evt/rt:ad/rn:ikea/ad:1",fimpResp.msgToString().getBytes(),1,false);
                } catch (MqttException e) {
//                    e.printStackTrace();
                    logger.warning(e.getMessage());
                } catch (JSONException e) {
//                    e.printStackTrace();
                    logger.warning(e.getMessage());
                }
                return true;

            }else if (fimp.mtype.equals("cmd.thing.get_inclusion_report")){
                String id = fimp.getStringValue();
                sendInclusionReport(Integer.parseInt(id),fimp.uid);
                return true;
            }else if (fimp.mtype.equals("cmd.system.disconnect")){
                Map <Integer,Device> deviceDb = this.deviceDb.getAllDevices();
                try{
                    this.tradfriApi.disconnect();
                    for (Map.Entry<Integer,Device>  entry : deviceDb.entrySet()) {
                        JSONObject value = new JSONObject();
                        value.put("address",entry.getKey().toString());
                        FimpMessage fimpResp = new FimpMessage("ikea","evt.thing.exclusion_report",value,null,null,fimp.uid);
                        logger.fine("Sending response :"+fimpResp.msgToString());
                        mqttClient.publish("pt:j1/mt:evt/rt:ad/rn:ikea/ad:1",fimpResp.msgToString().getBytes(),1,false);
                    }
                    this.deviceDb.clear();
                } catch (MqttException e) {
                    logger.warning(e.getMessage());
//                    e.printStackTrace();
                } catch (JSONException e) {
//                    e.printStackTrace();
                    logger.warning(e.getMessage());
                }
                return true;
            }else if (fimp.mtype.equals("cmd.system.connect")) {
                JSONObject jconfig = fimp.getJsonObjectValue();

                try {
                    this.tradfriApi.connect(jconfig.getString("id"),jconfig.getString("address"),jconfig.getString("username"),jconfig.getString("security_key"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;

            }else if (fimp.mtype.equals("cmd.system.get_status")) {

            }else if (fimp.mtype.equals("cmd.system.get_connect_params")) {
                this.tradfriApi.getConnectionInfoAsync();
                return true ;
            }
        }
        return false;
    }

    public synchronized void sendInclusionReport(int deviceId,String requestUid) {
        Device dev = deviceDb.getDeviceById(deviceId);
        JSONObject jdev = new JSONObject();
        try {

            JSONArray services = new JSONArray();
            services.put(getBinSwitchServiceDescriptor(Integer.toString(deviceId)));
            services.put(getLvlSwitchSeviceDescriptor(Integer.toString(deviceId)));
            services.put(getColorCtrlServiceDescriptor(Integer.toString(deviceId)));
            jdev.put("address",Integer.toString(deviceId));
            jdev.put("comm_tech","ikea");
            jdev.put("device_id","");
            jdev.put("alias",dev.alias);
            jdev.put("hw_ver",dev.hwVersion);
            jdev.put("sw_ver",dev.swVersion);
            jdev.put("manufacturer_id",dev.manufacturer);
            jdev.put("power_source","ac");
            jdev.put("product_hash",dev.productName);
            jdev.put("product_id",dev.productName);
            jdev.put("product_name",dev.productName);
            jdev.put("services",services);
            FimpMessage fimpResp = new FimpMessage("ikea","evt.thing.inclusion_report",jdev,null,null,requestUid);
            try {
                logger.fine("Sending response :"+fimpResp.msgToString());
                mqttClient.publish("pt:j1/mt:evt/rt:ad/rn:ikea/ad:1",fimpResp.msgToString().getBytes(),1,false);
            } catch (MqttException e) {
//                e.printStackTrace();
                logger.warning(e.getMessage());
            } catch (JSONException e) {
//                e.printStackTrace();
                logger.warning(e.getMessage());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendConnectParmsReport(String gwId, String gwIpAddress) {
        Map<String,String> params = new HashMap<String,String>();
        params.put("address",gwIpAddress);
        params.put("id",gwId);
        FimpMessage fimpResp = new FimpMessage("ikea","evt.system.connect_params_report",params,null,null,requestId);
        try {
            mqttClient.publish("pt:j1/mt:evt/rt:ad/rn:ikea/ad:1",fimpResp.msgToString().getBytes(),1,false);
        }catch (JSONException e) {
//            e.printStackTrace();
            logger.warning(e.getMessage());
        }catch (MqttException e) {
//            e.printStackTrace();
            logger.warning(e.getMessage());
        }
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
        switchService.put("groups",new JSONArray().put("ch_1"));
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

    private JSONObject getColorCtrlServiceDescriptor(String id) throws JSONException {
        JSONObject switchService = new JSONObject();
        switchService.put("address","/rt:dev/rn:ikea/ad:1/sv:color_ctrl/ad:"+id);
        switchService.put("groups",new JSONArray().put("ch_1"));
        switchService.put("name","color_ctrl");
        switchService.put("location","");
        JSONArray supComponents = new JSONArray();
        supComponents.put("red");
        supComponents.put("green");
        supComponents.put("blue");
        JSONObject props = new JSONObject();
        props.put("sup_components",supComponents);
        switchService.put("props",props);
        JSONArray interfaces = new JSONArray();
        interfaces.put(getInterfaceJobj("out","evt.color.report","int_map"));
        interfaces.put(getInterfaceJobj("in","cmd.color.set","int_map"));
        interfaces.put(getInterfaceJobj("in","cmd.color.get_report","null"));
        switchService.put("interfaces",interfaces);
        return switchService;
    }

    private JSONObject getLvlSwitchSeviceDescriptor(String id) throws JSONException {
        JSONObject switchService = new JSONObject();
        switchService.put("address","/rt:dev/rn:ikea/ad:1/sv:out_lvl_switch/ad:"+id);
        switchService.put("groups",new JSONArray().put("ch_1"));
        switchService.put("name","out_lvl_switch");
        switchService.put("location","");
        JSONObject props = new JSONObject();
        props.put("min_lvl",DIMMER_MIN);
        props.put("max_lvl",DIMMER_MAX);
        switchService.put("props",props);
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

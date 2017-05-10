package uk.me.hardill.TRADFRI2MQTT;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import static uk.me.hardill.TRADFRI2MQTT.TradfriConstants.*;
import static uk.me.hardill.TRADFRI2MQTT.TradfriConstants.DIMMER_MIN;
import static uk.me.hardill.TRADFRI2MQTT.TradfriConstants.TRANSITION_TIME;

/**
 * Created by alivinco on 09/05/2017.
 */
public class TradfriApi {
    private CoapEndpoint endPoint;
    private String gatewayIp;

    public TradfriApi(CoapEndpoint endPoint,String gatewayIp) {
        this.endPoint = endPoint;
        this.gatewayIp = gatewayIp;
    }

    public void lightSwitchCtrl(int id , boolean state) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject settings = new JSONObject();
        JSONArray array = new JSONArray();
        array.put(settings);
        json.put(LIGHT, array);
        if (state) {
            settings.put(ONOFF, 1);
        } else {
            settings.put(ONOFF, 0);
        }
        this.sendMsg("coaps://" + this.gatewayIp + "//" + DEVICES + "/" + id, json.toString());

    }
    public void lightDimmerCtrl(int id , int level,int duration) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject settings = new JSONObject();
        JSONArray array = new JSONArray();
        array.put(settings);
        json.put(LIGHT, array);
        int lvlValue = (int)Math.round(level*2.55);
        settings.put(DIMMER, Math.min(DIMMER_MAX, Math.max(DIMMER_MIN, lvlValue)));
        settings.put(TRANSITION_TIME, duration);	// transition in seconds
        this.sendMsg("coaps://" + this.gatewayIp + "//" + DEVICES + "/" + id, json.toString());
    }

    public void lightSwitchGroupCtrl(int id , boolean state) throws JSONException {
        JSONObject json = new JSONObject();
        if (state) {
                json.put(ONOFF, 1);
        } else {
                json.put(ONOFF, 0);
        }
        this.sendMsg("coaps://" + this.gatewayIp + "//" + GROUPS + "/" + id, json.toString());
    }
    public void lightDimmerGroupCtrl(int id , int level,int duration) throws JSONException {
        JSONObject json = new JSONObject();
        int lvlValue = (int)Math.round(level*2.55);
        json.put(DIMMER, Math.min(DIMMER_MAX, Math.max(DIMMER_MIN, lvlValue)));
        json.put(TRANSITION_TIME, duration);	// transition in seconds
        this.sendMsg("coaps://" + this.gatewayIp + "//" + GROUPS + "/" + id, json.toString());
    }

    public void lightColorCtrl(int id , String color) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject settings = new JSONObject();
        JSONArray array = new JSONArray();
        array.put(settings);
        json.put(LIGHT, array);
        switch (color) {
			case "cold":
				settings.put(COLOR, COLOR_COLD);
				break;
			case "normal":
				settings.put(COLOR, COLOR_NORMAL);
				break;
			case "warm":
				settings.put(COLOR, COLOR_WARM);
				break;
			default:
			    System.err.println("Invalid temperature supplied: " + color);
		}
        this.sendMsg("coaps://" + this.gatewayIp + "//" + DEVICES + "/" + id, json.toString());

    }

    private void sendMsg(String uriString, String payload) {
        System.out.println("Sending message \n" + payload);
        try {
            URI uri = new URI(uriString);
            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);
            CoapResponse response = client.put(payload, MediaTypeRegistry.TEXT_PLAIN);
            if (response.isSuccess()) {
                System.out.println("Ok");
            } else {
                System.out.println("Fail");
            }

            client.shutdown();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


}

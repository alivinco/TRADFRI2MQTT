package com.alivinco.tradfri;

import com.alivinco.tradfri.types.ColorMap;
import com.alivinco.tradfri.types.HSBType;
import com.alivinco.tradfri.types.XY;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.logging.Logger;

import static com.alivinco.tradfri.TradfriConstants.*;

/**
 * Created by alivinco on 19/11/2017.
 */
public class TradfriApi {
    Logger logger = Logger.getLogger("ikea");
    private TradfriClient client;

    public TradfriApi(TradfriClient client) {
        this.client = client;
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
        this.client.sendToDevice(id, json);

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
        this.client.sendToDevice(id, json);
    }

    public void lightSwitchGroupCtrl(int id , boolean state) throws JSONException {
        JSONObject json = new JSONObject();
        if (state) {
            json.put(ONOFF, 1);
        } else {
            json.put(ONOFF, 0);
        }
        this.client.sendToGroup(id, json);
    }
    public void lightDimmerGroupCtrl(int id , int level,int duration) throws JSONException {
        JSONObject json = new JSONObject();
        int lvlValue = (int)Math.round(level*2.55);
        json.put(DIMMER, Math.min(DIMMER_MAX, Math.max(DIMMER_MIN, lvlValue)));
        json.put(TRANSITION_TIME, duration);	// transition in seconds
        this.client.sendToGroup(id, json);
    }

    public void lightColorControl(int id,int red ,int green,int blue) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject settings = new JSONObject();
        JSONArray array = new JSONArray();
        array.put(settings);
        json.put(LIGHT, array);

//        HSBType hsb = HSBType.fromRGB(red,green,blue);
//        ColorConverter color = ColorConverter.fromHSBType(hsb);
        ColorMap cmap = new ColorMap();
        XY xy = cmap.RGBtoXY(red,green,blue);
        logger.info("Setting color x = " + xy.x+" y = "+xy.y+" \n");
        settings.put(COLOR_X,xy.x);
        settings.put(COLOR_Y,xy.y);
        this.client.sendToDevice(id, json);
    }
}

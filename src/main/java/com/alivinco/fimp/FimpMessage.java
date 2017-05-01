package com.alivinco.fimp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by alivinco on 23/04/2017.
 */



public class FimpMessage {
    public String service;
    public String mtype ;
    public String valueType;
    public Object value ;
    public List<String> tags;
    public Map<String,String> props;
    public String ver;
    public String uid;
    public String corid;
    public String ctime;
    public FimpMessage(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        this.ctime = dateFormat.format(new Date());
        this.ver = "1.0";
    }

    public FimpMessage(String service , String mtype , String valueType , Object value , Map<String,String> props, List<String> tags, String reqUid){
        this.service = service;
        this.mtype = mtype;
        this.valueType = valueType;
        this.value = value;
        this.props = props;
        this.tags = tags ;
        this.ver = "1.0";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        this.ctime = dateFormat.format(new Date());
        this.corid = reqUid;
    }
    public FimpMessage(String service , String mtype , int value , Map<String,String> props, List<String> tags, String reqUid){
        this(service , mtype , "int" , value , props, tags, reqUid);
    }
    public FimpMessage(String service , String mtype , String value , Map<String,String> props, List<String> tags, String reqUid){
        this(service , mtype , "string" , value , props, tags, reqUid);
    }
    public FimpMessage(String service , String mtype , double value , Map<String,String> props, List<String> tags, String reqUid){
        this(service , mtype , "float" , value , props, tags, reqUid);
    }
    public FimpMessage(String service , String mtype , boolean value , Map<String,String> props, List<String> tags, String reqUid){
        this(service , mtype , "bool" , value , props, tags, reqUid);
    }
    public FimpMessage(String service , String mtype , Object value , Map<String,String> props, List<String> tags, String reqUid){
        this(service , mtype , "object" , value , props, tags, reqUid);
    }
    public void setIntValue(int value){
        this.value = value;
    }

    public int getIntValue() {
        return (int)this.value;
    }

    public double getDoubleValue() {
        return (double)this.value;
    }

    public void seStringValue(String value){
        this.value = value;
    }

    public String getStringValue() {
        return (String)this.value;
    }

    public JSONObject getJsonObjectValue () {
        return (JSONObject)this.value;
    }

    public void setJsonObjectValue (JSONObject value) {
        this.value = value;
    }

    public String msgToString() throws JSONException {
        JSONObject jobj = new JSONObject();
        jobj.put("serv",this.service);
        jobj.put("type",this.mtype);
        jobj.put("val_t",this.valueType);
        jobj.put("val",this.value);
        jobj.put("props",this.props);
        jobj.put("tags",this.tags);
        jobj.put("ctime",this.ctime);
        jobj.put("uid",this.uid);
        return jobj.toString();
    }

    public static FimpMessage stringToMsg(String strMsg) throws JSONException {
        JSONObject reqJson = new JSONObject(strMsg);

        FimpMessage fimp = new FimpMessage();
        fimp.service = reqJson.getString("serv");
        fimp.mtype =  reqJson.getString("type");
        fimp.valueType = reqJson.getString("val_t");
        fimp.value = reqJson.get("val");
        if (reqJson.has("props"))
            try{
                fimp.props = toMap(reqJson.getJSONObject("props"));
            }catch (Exception e){

            }

        if (reqJson.has("tags"))
            try{
                fimp.tags = toList(reqJson.getJSONArray("tags"));
            }catch (Exception e){

            }
        if (reqJson.has("uid"))
            fimp.corid = reqJson.getString("uid");
        return fimp;
    };

    static Map<String, String> toMap(JSONObject object) throws JSONException {
        Map<String, String> map = new HashMap<String,String>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);
            if(value instanceof String) {
                map.put(key, (String)value);
            }
        }
        return map;
    }

    static List<String> toList(JSONArray array) throws JSONException {
        List<String> list = new ArrayList<String>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof String) {
                list.add((String)value);
            }
        }
        return list;
    }

}

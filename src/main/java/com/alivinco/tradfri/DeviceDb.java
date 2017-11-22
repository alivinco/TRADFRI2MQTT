package com.alivinco.tradfri;

import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Created by alivinco on 25/04/2017.
 */
public class DeviceDb {
    Hashtable<Integer,Device> deviceDb;
    Logger logger = Logger.getLogger("ikea");
    public DeviceDb(){
        this.deviceDb = new Hashtable<Integer,Device>();
    }
    // returns true if device was added
    public boolean upsertDevice(int instanceId,String alias,String productName,String manufacturer ,String serviceName,String type,String swVersion){
        Device dev = deviceDb.get(instanceId);
        if (dev == null) {
            Device newDev = new Device();
            newDev.id = instanceId;
            newDev.alias = alias;
            newDev.productName = productName;
            newDev.manufacturer = manufacturer;
            newDev.serviceType = serviceName;
            newDev.type = type;
            newDev.swVersion = swVersion;
            newDev.hwVersion = "1";
            deviceDb.put(instanceId,newDev);
            logger.fine("Device DB size = "+deviceDb.size());
            return true;
        }
        return false;
    };
    public void clear() {
        this.deviceDb.clear();
    }
    // returns true if value was updated
    public boolean updateDimLevel(int instanceId,int level){
        Device dev = deviceDb.get(instanceId);
        if (dev != null) {
            if (dev.dimLevel != level){
                dev.dimLevel = level;
                return true;

            }
        }else {
            // TODO : rise exception
            logger.fine("Device doesn't exist . Use upsert device first");
        }
        return false;
    }
    // returns true if value was updated
    public boolean updateLightState(int instanceId,boolean state){
        Device dev = deviceDb.get(instanceId);
        if (dev != null) {
            if (dev.lightState != state){
                dev.lightState = state;
                return true;
            }
        }else {
            logger.fine("Device doesn't exist . Use upsert device first");
        }
        return false;
    }

    // returns true if value was updated
    public boolean updateColorState(int instanceId,int red,int green, int blue){
        Device dev = deviceDb.get(instanceId);
        if (dev != null) {
            if (dev.red != red && dev.green != green && dev.blue != blue){
                dev.red = red;
                dev.green = green;
                dev.blue = blue;
                return true;
            }
        }else {
            logger.fine("Device doesn't exist . Use upsert device first");
        }
        return false;
    }

    public Device getDeviceById(int instanceId) {
        return deviceDb.get(instanceId);
    }

    public Hashtable<Integer,Device> getAllDevices() {
        return deviceDb;
    }


}


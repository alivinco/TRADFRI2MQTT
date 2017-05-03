package uk.me.hardill.TRADFRI2MQTT;

import java.util.Hashtable;

/**
 * Created by alivinco on 25/04/2017.
 */
public class DeviceDb {
    Hashtable<Integer,Device> deviceDb;

    DeviceDb(){
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
            newDev.alias = productName;
            newDev.manufacturer = manufacturer;
            newDev.serviceType = serviceName;
            newDev.type = type;
            newDev.swVersion = swVersion;
            newDev.hwVersion = "1";
            deviceDb.put(instanceId,newDev);
            System.out.println("Device DB size = "+deviceDb.size());
            return true;
        }
        return false;
    };
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
            System.out.println("Device doesn't exist . Use upsert device first");
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
            // TODO : rise exception
            System.out.println("Device doesn't exist . Use upsert device first");
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


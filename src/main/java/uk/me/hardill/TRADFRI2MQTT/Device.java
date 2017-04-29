package uk.me.hardill.TRADFRI2MQTT;

/**
 * Created by alivinco on 28/04/2017.
 */
public class Device {
    int id;
    String productName;
    String serviceType; // dimmer,light_bulb,remote
    String type;  // device , group
    String manufacturer;
    String swVersion;
    String hwVersion;
    int dimLevel;
    boolean lightState;

}
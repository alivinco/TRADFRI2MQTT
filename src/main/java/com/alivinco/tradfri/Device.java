package com.alivinco.tradfri;

/**
 * Created by alivinco on 28/04/2017.
 */
public class Device {
    public int id;
    public String productName;
    public String alias;
    public String serviceType; // dimmer,light_bulb,remote
    public String type;  // device , group
    public String manufacturer;
    public String swVersion;
    public String hwVersion;
    public int dimLevel;
    public boolean lightState;

}
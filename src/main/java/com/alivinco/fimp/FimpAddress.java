package com.alivinco.fimp;

/**
 * Created by alivinco on 04/05/2017.
 */
public class FimpAddress {
    public String payloadType;
    public String msgType;
    public String resourceType;
    public String resourceName;
    public String resourceAddress;
    public String serviceName;
    public String serviceAddress;

    public static FimpAddress parseStringAddress(String address){
        FimpAddress fimpAddr = new FimpAddress();
        String [] addrTokens = address.split("/");
        for(String item:addrTokens){
            String[] tokenPair = item.split(":");
            if(tokenPair.length==2){
                if(tokenPair[0].equals("sv"))
                    fimpAddr.serviceName = tokenPair[1];
                else if (tokenPair[0].equals("ad") && fimpAddr.serviceName!=null)
                    fimpAddr.serviceAddress = tokenPair[1];

            }
        }
        return fimpAddr;
    }
}

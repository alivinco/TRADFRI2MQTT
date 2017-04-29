package com.alivinco.fimp;

import org.junit.Test;

/**
 * Created by alivinco on 24/04/2017.
 */
public class FimpMessageTest {
    @Test
    public void stringToMsg() throws Exception {
        String strMsg = "{\"ctime\":\"2017-04-11T13:48:35+0200\",\"props\":{\"unit\":\"C\"},\"serv\":\"sensor_temp\",\"tags\":[],\"type\":\"evt.sensor.report\",\"val\":25.1,\"val_t\":\"float\"}";
        FimpMessage fimp = FimpMessage.stringToMsg(strMsg);
        System.out.println(fimp.service);
        System.out.println(fimp.mtype);
        System.out.println(fimp.getDoubleValue());
        System.out.println(fimp.msgToString());

    }

}
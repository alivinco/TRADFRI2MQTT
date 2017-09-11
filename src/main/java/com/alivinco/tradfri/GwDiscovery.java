package com.alivinco.tradfri;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by alivinco on 28/06/2017.
 */


//interface GwUpdate

public class GwDiscovery {
    protected String gwId;
    protected String gwIpAddress;
    private class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
//            System.out.println("Service added: " + event.getInfo());
            ServiceInfo info = event.getDNS().getServiceInfo(event.getType(), event.getName());
//            System.out.println("Service address: " + info.getInet4Addresses()[0].getHostAddress());
            System.out.println("Service name: " +info.getName());
            for (InetAddress addr:info.getInet4Addresses()) {
                System.out.println("Service address: " +addr.getHostAddress());
                gwId = info.getName();
                gwIpAddress = addr.getHostAddress();
                GwDiscovery.this.onGwDiscovered(info.getName(),addr.getHostAddress());

            }
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("Service resolved: " + event.getInfo());
        }
    }

    void onGwDiscovered(String gwId , String gwIpAddress)  {
        System.out.println("<GwDiscovery> New gateway is discovered. GW id = " +gwId+" IP = "+gwIpAddress);
    }

    GwDiscovery() {
        try {
            System.out.println("Starting gateway discovery");
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            // Add a service listener
            jmdns.addServiceListener("_coap._udp.local.", new SampleListener());
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


}

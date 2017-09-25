package com.alivinco.tradfri;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

/**
 * Created by alivinco on 28/06/2017.
 */


//interface GwUpdate

public class GwDiscovery {
    Logger logger = Logger.getLogger("ikea");
    protected String gwId;
    protected String gwIpAddress;
    JmDNS jmdns;
    private class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
//            System.out.println("Service added: " + event.getInfo());
            ServiceInfo info = event.getDNS().getServiceInfo(event.getType(), event.getName());
//            System.out.println("Service address: " + info.getInet4Addresses()[0].getHostAddress());
            logger.info("Service name: " +info.getName());
            for (InetAddress addr:info.getInet4Addresses()) {
                logger.info("Service address: " +addr.getHostAddress());
                gwId = info.getName();
                gwIpAddress = addr.getHostAddress();
                GwDiscovery.this.onGwDiscovered(info.getName(),addr.getHostAddress());

            }
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            logger.info("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            logger.info("Service resolved: " + event.getInfo());
        }
    }

    void onGwDiscovered(String gwId , String gwIpAddress)  {
        logger.info("<GwDiscovery> New gateway is discovered. GW id = " +gwId+" IP = "+gwIpAddress);
    }

    void requestServiceInfoAsync() {
        ServiceInfo []infos = jmdns.list("_coap._udp.local.");
        for(ServiceInfo info:infos) {
            for (InetAddress addr:info.getInet4Addresses()) {
                logger.info("Service address: " +addr.getHostAddress());
                gwId = info.getName();
                gwIpAddress = addr.getHostAddress();
                GwDiscovery.this.onGwDiscovered(info.getName(),addr.getHostAddress());

            }
        }

    }

    GwDiscovery() {
        try {
            logger.info("Starting gateway discovery");
            // Create a JmDNS instance
            jmdns = JmDNS.create(InetAddress.getLocalHost());
            // Add a service listener
            jmdns.addServiceListener("_coap._udp.local.", new SampleListener());

        } catch (UnknownHostException e) {
            logger.warning(e.getMessage());
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }


}

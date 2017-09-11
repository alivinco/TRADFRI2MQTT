package com.alivinco.tradfri;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.file.Paths;

import static com.alivinco.tradfri.TradfriConstants.*;
import static com.alivinco.tradfri.TradfriConstants.DIMMER_MIN;
import static com.alivinco.tradfri.TradfriConstants.TRANSITION_TIME;



interface TradfriApiEvents {
    void onCoapMessage(CoapResponse response) ;
    void onGwDiscovered(String gwId, String gwIpAddress);
    void onGwConnected(String gwId);
    void onGwLostConnection(String gwId);
}

public class TradfriApi extends GwDiscovery {
    private DTLSConnector dtlsConnector;
    private Vector<CoapObserveRelation> watching = new Vector<>();
    private CoapEndpoint endPoint;
    private ScheduledExecutorService executor;
    private TradfriApiEvents eventsHandler;
    private IkeaGwConnectionInfo connInfo;
    private String configFilePath;


    public TradfriApi(String configFilePath) {
        connInfo = new IkeaGwConnectionInfo();
        connInfo.isConnected = false;
        this.configFilePath = configFilePath;
        loadConectionInfoFromFile();
        if (connInfo.isConfigured){
            System.out.println("Connecting to gateway");
            connect();
        }else {
            System.out.println("Adapter is not configured.Configure first.");
        }


    }

    public void loadConectionInfoFromFile() {
        String confStr = null;
        try {
            confStr = new String(Files.readAllBytes(Paths.get(this.configFilePath)));
            JSONObject jconfig = new JSONObject(confStr );
            connInfo.gwId = jconfig.getString("gw_id");
            connInfo.gwIpAddress = jconfig.getString("gw_ip_address");
            connInfo.gwPskKey = jconfig.getString("gw_psk");
            if (connInfo.gwId != "" && connInfo.gwIpAddress != "" && connInfo.gwPskKey != "") {
                connInfo.isConfigured = true;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            this.connInfo.isConfigured = false;
        } catch (IOException e) {
            e.printStackTrace();
            saveConectionInfoToFile();
            this.connInfo.isConfigured = false;
        }

    }

    public void saveConectionInfoToFile() {
        JSONObject jconf = new JSONObject();
        try {
            jconf.put("gw_id",connInfo.gwId);
            jconf.put("gw_ip_address",connInfo.gwIpAddress);
            jconf.put("gw_psk",connInfo.gwPskKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            Files.write(Paths.get(this.configFilePath), jconf.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getConnectionInfoAsync() {
        if (eventsHandler != null) {
            eventsHandler.onGwDiscovered(connInfo.gwId,connInfo.gwIpAddress);
        }
    }

    @Override
    void onGwDiscovered(String gwId, String gwIpAddress) {

      if (!connInfo.isConfigured) {
          connInfo.gwId = gwId;
          connInfo.gwIpAddress = gwIpAddress;
          saveConectionInfoToFile();
      }else if (connInfo.gwId == gwId) {
          // in case if gateway changed ip address
          connInfo.gwIpAddress = gwIpAddress;
          saveConectionInfoToFile();
      }
       if (eventsHandler != null) {
          eventsHandler.onGwDiscovered(gwId,gwIpAddress);
       }
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
        this.sendMsg("coaps://" + connInfo.gwIpAddress + "//" + DEVICES + "/" + id, json.toString());

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
        this.sendMsg("coaps://" + connInfo.gwIpAddress + "//" + DEVICES + "/" + id, json.toString());
    }

    public void lightSwitchGroupCtrl(int id , boolean state) throws JSONException {
        JSONObject json = new JSONObject();
        if (state) {
                json.put(ONOFF, 1);
        } else {
                json.put(ONOFF, 0);
        }
        this.sendMsg("coaps://" + connInfo.gwIpAddress + "//" + GROUPS + "/" + id, json.toString());
    }
    public void lightDimmerGroupCtrl(int id , int level,int duration) throws JSONException {
        JSONObject json = new JSONObject();
        int lvlValue = (int)Math.round(level*2.55);
        json.put(DIMMER, Math.min(DIMMER_MAX, Math.max(DIMMER_MIN, lvlValue)));
        json.put(TRANSITION_TIME, duration);	// transition in seconds
        this.sendMsg("coaps://" + connInfo.gwIpAddress + "//" + GROUPS + "/" + id, json.toString());
    }

    public void lightColorCtrl(int id , String color) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject settings = new JSONObject();
        JSONArray array = new JSONArray();
        array.put(settings);
        json.put(LIGHT, array);
        switch (color) {
			case "cold":
				settings.put(COLOR, COLOR_COLD);
				break;
			case "normal":
				settings.put(COLOR, COLOR_NORMAL);
				break;
			case "warm":
				settings.put(COLOR, COLOR_WARM);
				break;
			default:
			    System.err.println("Invalid temperature supplied: " + color);
		}
        this.sendMsg("coaps://" + connInfo.gwIpAddress + "//" + DEVICES + "/" + id, json.toString());

    }

    private void sendMsg(String uriString, String payload) {
        System.out.println("Sending message \n" + payload);
        try {
            URI uri = new URI(uriString);
            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);
            CoapResponse response = client.put(payload, MediaTypeRegistry.TEXT_PLAIN);
            if (response.isSuccess()) {
                System.out.println("Ok");
            } else {
                System.out.println("Fail");
            }

            client.shutdown();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    public void configure(String gwId,String ipAddress,String psk) {
        connInfo.gwId = gwId;
        connInfo.gwIpAddress = ipAddress;
        connInfo.gwPskKey = psk;
        connInfo.isConfigured = true;
        saveConectionInfoToFile();
    }
    public String connect() {
        return  connect(null,null,null);
    }

    public String connect(String gwId,String ipAddress,String psk) {
        if (gwId == null || ipAddress == null || psk == null)  {
            if (!connInfo.isConfigured)
                return "CONFIG_ERR_EMPTY_PARAM";
        }else {
            configure(gwId,ipAddress,psk);
        }

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
        builder.setPskStore(new StaticPskStore("", connInfo.gwPskKey.getBytes()));
        dtlsConnector = new DTLSConnector(builder.build());
        endPoint = new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard());

        if (executor== null){
            executor = Executors.newScheduledThreadPool(1);
        }
//        executor.shutdown();

        Runnable command = new Runnable() {

            @Override
            public void run() {
                System.out.println("re-reg");
                for(CoapObserveRelation rel: watching) {
                    rel.reregister();
                }
            }
        };
        executor.scheduleAtFixedRate(command, 120, 120, TimeUnit.SECONDS);

        discoverDevicesAndGroups();
        return  "CONFIGURED";
    }

    public void setEventsHandler(TradfriApiEvents  handler) {
        this.eventsHandler = handler;
    }


    private void discoverDevicesAndGroups() {
        //bulbs
        try {
            URI uri = new URI("coaps://" + connInfo.gwIpAddress + "//" + DEVICES);
            System.out.println("Connecting to ip = "+connInfo.gwIpAddress);
            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);

            CoapResponse response = client.get();
            if (response == null) {
                System.out.println("1 Connection to Gateway timed out, please check ip address or increase the ACK_TIMEOUT in the Californium.properties file");
//                System.exit(-1);
                connInfo.isConnected = false;
                executor.shutdown();
                return;
            }
            connInfo.isConnected = true;
            System.out.println("Devices: "+response.getResponseText());

            JSONArray array = new JSONArray(response.getResponseText());
            for (int i=0; i<array.length(); i++) {
                String devUri = "coaps://" + connInfo.gwIpAddress + "//" + DEVICES + "/" + array.getInt(i);
                this.subscribeForCoapMessages(devUri);
            }
            client.shutdown();
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            URI uri = new URI("coaps://" + connInfo.gwIpAddress + "//" + GROUPS);
            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);
            CoapResponse response = client.get();
            if (response == null) {
                System.out.println("2 Connection to Gateway timed out, please check ip address or increase the ACK_TIMEOUT in the Californium.properties file");
//                System.exit(-1);
                connInfo.isConnected = false;
            }
            JSONArray array = new JSONArray(response.getResponseText());
            for (int i=0; i<array.length(); i++) {
                String devUri = "coaps://" + connInfo.gwIpAddress + "//" + GROUPS + "/" + array.getInt(i);
                this.subscribeForCoapMessages(devUri);
            }
            client.shutdown();
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void subscribeForCoapMessages(String uriString) {

        try {
            URI uri = new URI(uriString);

            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);
            CoapHandler handler = new CoapHandler() {

                @Override
                public void onLoad(CoapResponse response) {
                   //Main.this.msgRouter.onCoapMessage(response);
                    TradfriApi.this.eventsHandler.onCoapMessage(response);
                }

                @Override
                public void onError() {
                    // TODO Auto-generated method stub
                    System.out.println("problem with observe");
                }
            };
            CoapObserveRelation relation = client.observe(handler);
            watching.add(relation);


        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }



}

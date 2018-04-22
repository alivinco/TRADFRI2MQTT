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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.alivinco.tradfri.TradfriConstants.*;
import static com.alivinco.tradfri.TradfriConstants.DIMMER_MIN;
import static com.alivinco.tradfri.TradfriConstants.TRANSITION_TIME;
import static com.alivinco.tradfri.TradfriConstantsAll.CLIENT_IDENTITY_PROPOSED;
import static com.alivinco.tradfri.TradfriConstantsAll.NEW_PSK_BY_GW;


interface TradfriApiEvents {
    void onCoapMessage(CoapResponse response) ;
    void onGwDiscovered(String gwId, String gwIpAddress);
    void onGwConnected(String gwId);
    void onGwLostConnection(String gwId);
}

public class TradfriClient extends GwDiscovery {
    Logger logger = Logger.getLogger("ikea");
    private DTLSConnector dtlsConnector;
    private Vector<CoapObserveRelation> watching = new Vector<>();
    private CoapEndpoint endPoint;
    private ScheduledExecutorService executor;
    private TradfriApiEvents eventsHandler;
    private IkeaGwConnectionInfo connInfo;
    private String configFilePath;
    private Set<Integer> devices = new HashSet<>();
    private Set<Integer> groups  = new HashSet<>();



    public TradfriClient(String configFilePath) {
        connInfo = new IkeaGwConnectionInfo();
        connInfo.identity = "";
        connInfo.isConnected = false;
        this.configFilePath = configFilePath;
        loadConectionInfoFromFile();
    }

    public void init() {
        if (connInfo.isConfigured){
            logger.info("Connecting to gateway");
            connect();
        }else {
            logger.info("Adapter is not configured.Configure first.");
        }
    }

    public void loadConectionInfoFromFile() {
        String confStr = null;
        try {
            confStr = new String(Files.readAllBytes(Paths.get(this.configFilePath)));
            JSONObject jconfig = new JSONObject(confStr);
            connInfo.gwId = jconfig.getString("gw_id");
            connInfo.gwIpAddress = jconfig.getString("gw_ip_address");
            connInfo.gwPskKey = jconfig.getString("gw_psk");
            connInfo.identity = jconfig.getString("identity");

            if ( !connInfo.gwId.isEmpty() && !connInfo.gwIpAddress.isEmpty() && !connInfo.gwPskKey.isEmpty()) {
                logger.info("Connection is configured");
                connInfo.isConfigured = true;
            }

        } catch (JSONException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
            logger.warning(e.getMessage());
            this.connInfo.isConfigured = false;
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
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
            jconf.put("identity",connInfo.identity);
        } catch (JSONException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        }
        try {
            Files.write(Paths.get(this.configFilePath), jconf.toString().getBytes());
        } catch (IOException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        }
    }

    public void getConnectionInfoAsync() {
        requestServiceInfoAsync();
//        if (eventsHandler != null) {
//            eventsHandler.onGwDiscovered(connInfo.gwId,connInfo.gwIpAddress);
//        }
    }

    @Override
    void onGwDiscovered(String gwId, String gwIpAddress) {
      logger.info("IKEA gateway is discovered");
      if (!connInfo.isConfigured) {
          connInfo.gwId = gwId;
          connInfo.gwIpAddress = gwIpAddress;
          saveConectionInfoToFile();
      }else if (connInfo.gwId == gwId) {
          logger.info("IKEA gateway has changed it's IP address . Updating.");
          // in case if gateway changed ip address
          connInfo.gwIpAddress = gwIpAddress;
          saveConectionInfoToFile();
      }
       if (eventsHandler != null) {
          eventsHandler.onGwDiscovered(gwId,gwIpAddress);
       }
    }

    public void sendToDevice(int id,JSONObject payload) {
        this.sendMsg("coaps://" + connInfo.gwIpAddress + "//" + DEVICES + "/" + id, payload.toString());
    }

    public void sendToGroup(int id,JSONObject payload) {
        this.sendMsg("coaps://" + connInfo.gwIpAddress + "//" + GROUPS + "/" + id, payload.toString());
    }

    public void sendMsg(String uriString, String payload) {
        logger.info("Sending message \n" + payload);
        try {
            URI uri = new URI(uriString);
            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);
            CoapResponse response = client.put(payload, MediaTypeRegistry.TEXT_PLAIN);
            if (response.isSuccess()) {
                logger.fine("ok");
            } else {
                logger.fine("failed");
            }

            client.shutdown();

        } catch (URISyntaxException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Configures connection to IKEA gateway .
     *
     * @param gwId gateway Id
     * @param ipAddress gatewayy IP address
     * @param code security code printed on box
     */
    public String configure(String gwId,String ipAddress,String code) {
        String identity = UUID.randomUUID().toString().replace("-", "");
        String pskKey = obtainIdentityAndPreSharedKey(identity,code);
        if (pskKey==null) {
            connInfo.isConfigured = false;
            return "ERROR_OBTAINING_PSK";
        }else {
            connInfo.isConfigured = true;
        }
        connInfo.gwId = gwId;
        connInfo.gwIpAddress = ipAddress;
        connInfo.gwPskKey = pskKey;
        connInfo.identity = identity;
        saveConectionInfoToFile();
        return null;
    }

    /**
     *  Creates endpoint connection .
     * @return
     */

    public String connect() {
        if (!connInfo.isConfigured)
           return "CONFIG_ERR_EMPTY_PARAM";

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
        builder.setPskStore(new StaticPskStore(connInfo.identity, connInfo.gwPskKey.getBytes()));
        dtlsConnector = new DTLSConnector(builder.build());
        endPoint = new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard());

        if (executor== null){
            executor = Executors.newScheduledThreadPool(1);
        }

        Runnable command = new Runnable() {

            @Override
            public void run() {
                logger.info("re-reg");
                for(CoapObserveRelation rel: watching) {
                    rel.reregister();
                }
            }
        };
        executor.scheduleAtFixedRate(command, 10, 30, TimeUnit.MINUTES);

        discoverDevicesAndGroups();
        observeNetworkUpdates();
        return  "OK";
    }

    /**
     * Authenticates against the gateway with the security code in order to receive a pre-shared key for a newly
     * generated identity.
     * As this requires a remote request, this method might be long-running.
     *
     * @return true, if credentials were successfully obtained, false otherwise
     */
    protected String obtainIdentityAndPreSharedKey(String identity,String code) {

        String preSharedKey = null;

        CoapResponse gatewayResponse;
        String authUrl = null;
        String responseText = null;
        try {
            DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
            builder.setPskStore(new StaticPskStore("Client_identity", code.getBytes()));

            DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
            CoapEndpoint authEndpoint = new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard());
            authUrl = "coaps://" + connInfo.gwIpAddress + "/15011/9063";

            CoapClient deviceClient = new CoapClient(new URI(authUrl));
            deviceClient.setTimeout(TimeUnit.SECONDS.toMillis(10));
            deviceClient.setEndpoint(authEndpoint);

            JSONObject json = new JSONObject();
            json.put(CLIENT_IDENTITY_PROPOSED, identity);

            gatewayResponse = deviceClient.post(json.toString(), 0);

            authEndpoint.destroy();
            deviceClient.shutdown();

            if (gatewayResponse == null) {
                // seems we ran in a timeout, which potentially also happens
                logger.warning("Wrong response can't authenticate ");
                return null;
            }

            if (gatewayResponse.isSuccess()) {
                responseText = gatewayResponse.getResponseText();
                json = new JSONObject(responseText);
                preSharedKey = json.getString(NEW_PSK_BY_GW);
                if (preSharedKey=="") {
                    logger.warning("Empty pre-shared key");
                    return null;
                } else {
                    logger.info("Received pre-shared key for gateway ");
                    return preSharedKey;
                }
            } else {
                logger.warning(String.format("Failed obtaining pre-shared key with status code '%s'", gatewayResponse.getCode()));
            }
        } catch (URISyntaxException e) {
            logger.warning("Illegal gateway URI , URI = "+authUrl);
        } catch (JSONException e) {
            logger.warning("Invalid response recieved from gateway '{}'"+ e.getMessage());
        }
        return null;
    }


    public String disconnect() {
        // shutting down observable registration loop
        executor.shutdown();
        executor = null;
        connInfo.gwId = "";
        connInfo.gwIpAddress = "";
        connInfo.gwPskKey = "";
        connInfo.identity = "";
        connInfo.isConfigured = false;
        connInfo.isConnected = false;
        saveConectionInfoToFile();
        return "OK";
    }

    public void setEventsHandler(TradfriApiEvents  handler) {
        this.eventsHandler = handler;
    }


    private void discoverDevicesAndGroups() {
        //bulbs
        try {
            URI uri = new URI("coaps://" + connInfo.gwIpAddress + "//" + DEVICES);
            logger.info("Connecting to ip = "+connInfo.gwIpAddress);
            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);
            CoapResponse response = client.get();
            for(int i=0;i<4;i++) {
                if (response == null) {
                    logger.warning("First connection attempt failed , trying one more time after 5 seconds");
                    Thread.sleep(5000);
                    uri = new URI("coaps://" + connInfo.gwIpAddress + "//" + DEVICES);
                    logger.info("Connecting to ip = "+connInfo.gwIpAddress);
                    client = new CoapClient(uri);
                    client.setEndpoint(endPoint);
                    response = client.get();
                } else {
                    break;
                }

            }
            if (response == null) {
                logger.warning("1 Connection to Gateway timed out, please check ip address or increase the ACK_TIMEOUT in the Californium.properties file");
                connInfo.isConnected = false;
                executor.shutdown();
                return;
            }
            connInfo.isConnected = true;
            logger.info("Devices: "+response.getResponseText());

            JSONArray array = new JSONArray(response.getResponseText());
            for (int i=0; i<array.length(); i++) {
                String devUri = "coaps://" + connInfo.gwIpAddress + "//" + DEVICES + "/" + array.getInt(i);
                this.devices.add(array.getInt(i));
                client.setURI(devUri);
                CoapResponse devResponse = client.get();
                this.eventsHandler.onCoapMessage(devResponse);
                this.subscribeForCoapMessages(devUri);
            }
            client.shutdown();
        } catch (URISyntaxException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            URI uri = new URI("coaps://" + connInfo.gwIpAddress + "//" + GROUPS);
            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);
            CoapResponse response = client.get();
            if (response == null) {
                logger.warning("2 Connection to Gateway timed out, please check ip address or increase the ACK_TIMEOUT in the Californium.properties file");
                connInfo.isConnected = false;
            }else {
                logger.info("Groups: "+response.getResponseText());
            }
            JSONArray array = new JSONArray(response.getResponseText());
            for (int i=0; i<array.length(); i++) {
                String devUri = "coaps://" + connInfo.gwIpAddress + "//" + GROUPS + "/" + array.getInt(i);
                client.setURI(devUri);
                CoapResponse devResponse = client.get();
                this.eventsHandler.onCoapMessage(devResponse);
                this.subscribeForCoapMessages(devUri);
            }
            client.shutdown();
        } catch (URISyntaxException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        }
    }

    private void observeNetworkUpdates() {
        try {
            URI uri = new URI("coaps://" + connInfo.gwIpAddress + "//" + DEVICES);

            CoapClient client = new CoapClient(uri);
            client.setEndpoint(endPoint);

            CoapHandler handler = new CoapHandler() {

                @Override
                public void onLoad(CoapResponse response) {
                    //Main.this.msgRouter.onCoapMessage(response);
                    logger.info("Network changes : "+response.getResponseText());
                    try {
                        JSONArray array = new JSONArray(response.getResponseText());
                        for (int i=0; i<array.length(); i++) {
//                            TradfriClient.this.eventsHandler.onNetworkUpdate(array,false);
                            if (!devices.contains(array.getInt(i))) {
                                logger.info("New device discovered = "+array.getInt(i));
                                String devUri = "coaps://" + connInfo.gwIpAddress + "//" + DEVICES + "/" + array.getInt(i);
                                TradfriClient.this.devices.add(array.getInt(i));
                                client.setURI(devUri);
                                CoapResponse devResponse = client.get();
                                TradfriClient.this.eventsHandler.onCoapMessage(devResponse);
                                TradfriClient.this.subscribeForCoapMessages(devUri);
                                devices.add(array.getInt(i));

                            }
                        }

                    }catch (JSONException e) {
                        logger.warning(e.getMessage());
                        e.printStackTrace();
                    }

                }

                @Override
                public void onError() {
                    logger.warning("problem with observe");
                }
            };
            CoapObserveRelation relation = client.observe(handler);

            watching.add(relation);


        } catch (URISyntaxException e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
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
                    if(TradfriClient.this.connInfo.isConnected)
                        TradfriClient.this.eventsHandler.onCoapMessage(response);
                }

                @Override
                public void onError() {
                    logger.warning("problem with observe");
                }
            };
            CoapObserveRelation relation = client.observe(handler);

            watching.add(relation);


        } catch (URISyntaxException e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }

    }



}

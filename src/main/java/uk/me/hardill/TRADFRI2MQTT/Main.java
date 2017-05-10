/**
 * 
 */
package uk.me.hardill.TRADFRI2MQTT;

import static uk.me.hardill.TRADFRI2MQTT.TradfriConstants.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.alivinco.fimp.FimpAddress;
import com.alivinco.fimp.FimpMessage;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author hardillb
 *
 */
public class Main {
	
	static {
		CaliforniumLogger.disableLogging();
//		ScandiumLogger.disable();
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.FINE);
	}
	
	private DTLSConnector dtlsConnector;
	private MqttClient mqttClient;
	private CoapEndpoint endPoint;
	
	private String ip;
	private DeviceDb deviceDb;
	private AdapterApi adApi;
	private FimpApi fimpApi;
	private TradfriApi tradfriApi;

	private HashMap<String, Integer> name2id = new HashMap<>();
	private Vector<CoapObserveRelation> watching = new Vector<>();
	
	Main(String psk, String ip, String broker) {
		this.ip = ip;
		this.deviceDb = new DeviceDb();

		DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
		builder.setPskStore(new StaticPskStore("", psk.getBytes()));
		dtlsConnector = new DTLSConnector(builder.build());
		endPoint = new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard());
		
		MemoryPersistence persistence = new MemoryPersistence();
		try {
			mqttClient = new MqttClient(broker, MqttClient.generateClientId(), persistence);
			mqttClient.connect();
			this.adApi = new AdapterApi(this.deviceDb,this.mqttClient);
			this.fimpApi = new FimpApi(mqttClient,this.deviceDb);
			this.tradfriApi = new TradfriApi(this.endPoint,this.ip);
			mqttClient.setCallback(new MqttCallback() {
				
				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					// TODO Auto-generated method stub
					System.out.println(topic + " " + message.toString());
					FimpMessage fimp = FimpMessage.stringToMsg(message.toString());
					FimpAddress fimpAddr = FimpAddress.parseStringAddress(topic);

					if (adApi.onMessage(topic,fimp)){
						return;
					}
					JSONObject reqJson = new JSONObject(message.toString());
					boolean isDevice = true;
					int id = Integer.parseInt(fimpAddr.serviceAddress);
					Device dev = deviceDb.getDeviceById(id);
					if (dev.type.equals("group"))
						isDevice = false;
					System.out.println(id);
					try{
						if (fimp.mtype.equals("cmd.binary.set")) {
							if (isDevice)
								Main.this.tradfriApi.lightSwitchCtrl(id,fimp.getBoolValue());
							else
								Main.this.tradfriApi.lightSwitchGroupCtrl(id,fimp.getBoolValue());
						}else if (fimp.mtype.equals("cmd.lvl.set")) {
							int lvlValue = fimp.getIntValue();
							int duration = 3;
							if (fimp.props.get("duration")!=null)
								 duration = Integer.parseInt(fimp.props.get("duration"));
							if (isDevice)
								Main.this.tradfriApi.lightDimmerCtrl(id,lvlValue,duration);
							else
								Main.this.tradfriApi.lightDimmerGroupCtrl(id,lvlValue,duration);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void connectionLost(Throwable cause) {
					// TODO Auto-generated method stub
					System.out.println("Connection lost:");
					cause.printStackTrace();
				}
			});
			mqttClient.subscribe("pt:j1/mt:cmd/rt:ad/rn:ikea/ad:1");
			mqttClient.subscribe("pt:j1/mt:cmd/rt:dev/rn:ikea/ad:1/sv:out_bin_switch/+");
			mqttClient.subscribe("pt:j1/mt:cmd/rt:dev/rn:ikea/ad:1/sv:out_lvl_switch/+");
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
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
	}
	
	private void discover() {
		//bulbs
		try {
			URI uri = new URI("coaps://" + ip + "//" + DEVICES);
			System.out.println("Connecting to ip="+ip);
			CoapClient client = new CoapClient(uri);
			client.setEndpoint(endPoint);

			CoapResponse response = client.get();
			if (response == null) {
				System.out.println("1 Connection to Gateway timed out, please check ip address or increase the ACK_TIMEOUT in the Californium.properties file");
				System.exit(-1);
			}
			System.out.println("Devices: "+response.getResponseText());

			JSONArray array = new JSONArray(response.getResponseText());
			for (int i=0; i<array.length(); i++) {
				String devUri = "coaps://" + ip + "//" + DEVICES + "/" + array.getInt(i);
				this.watch(devUri);
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
			URI uri = new URI("coaps://" + ip + "//" + GROUPS);
			CoapClient client = new CoapClient(uri);
			client.setEndpoint(endPoint);
			CoapResponse response = client.get();
			if (response == null) {
				System.out.println("2 Connection to Gateway timed out, please check ip address or increase the ACK_TIMEOUT in the Californium.properties file");
				System.exit(-1);
			}
			JSONArray array = new JSONArray(response.getResponseText());
			for (int i=0; i<array.length(); i++) {
				String devUri = "coaps://" + ip + "//" + GROUPS + "/" + array.getInt(i);
				this.watch(devUri);
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
	
//	private void set(String uriString, String payload) {
//		System.out.println("payload\n" + payload);
//		try {
//			URI uri = new URI(uriString);
//			CoapClient client = new CoapClient(uri);
//			client.setEndpoint(endPoint);
//			CoapResponse response = client.put(payload, MediaTypeRegistry.TEXT_PLAIN);
//			if (response.isSuccess()) {
//				System.out.println("Yay");
//			} else {
//				System.out.println("Boo");
//			}
//
//			client.shutdown();
//
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	private void watch(String uriString) {
		
		try {
			URI uri = new URI(uriString);
			
			CoapClient client = new CoapClient(uri);
			client.setEndpoint(endPoint);
			CoapHandler handler = new CoapHandler() {
				
				@Override
				public void onLoad(CoapResponse response) {
					System.out.println(response.getResponseText());
					System.out.println(response.getOptions().toString());
					try {
						JSONObject json = new JSONObject(response.getResponseText());
						//TODO change this test to something based on 5750 values
						// 2 = light?
						// 0 = remote/dimmer?
						if (json.has(LIGHT) && (json.has(TYPE) && json.getInt(TYPE) == 2)) { // single bulb

							JSONObject light = json.getJSONArray(LIGHT).getJSONObject(0);
							JSONObject device = json.getJSONObject(DEVICE);
							deviceDb.upsertDevice(json.getInt(INSTANCE_ID),json.getString(NAME),device.getString(PRODUCT_NAME),device.getString(DEVICE_MANUFACTURER),"light_bulb","device",device.getString(DEVICE_VERSION));

							if (light.has(ONOFF)) {
								boolean stateBool = (light.getInt(ONOFF) != 0);
								fimpApi.reportSwitchStateChange(json.getInt(INSTANCE_ID),stateBool);
							}else {
								System.err.println("Bulb '" + json.getString(NAME) + "' has no On/Off value (probably no power on lightbulb socket)");
								return; // skip this lamp for now
							}

							name2id.put(json.getString(NAME), json.getInt(INSTANCE_ID));

							if (light.has(DIMMER)) {
								int level = light.getInt(DIMMER);
								fimpApi.reportDimLvlChange(json.getInt(INSTANCE_ID),level);
							} else {
								System.err.println("Bulb '" + json.getString(NAME) + "' has no dimming value (maybe just no power on lightbulb socket)");
							}

							MqttMessage message3 = null;
							if (light.has(COLOR)) {
								message3 = new MqttMessage();
								String temperature = light.getString(COLOR);
								message3.setPayload(temperature.getBytes());
							} else { // just fyi for the user. maybe add further handling later
								System.out.println("Bulb '" + json.getString(NAME) + "' doesn't support color temperature");
							}

						} else if (json.has(HS_ACCESSORY_LINK)) { // groups have this entry
							//room?
							System.out.println("room");
							JSONObject group = json.getJSONObject(HS_ACCESSORY_LINK);
							deviceDb.upsertDevice(json.getInt(INSTANCE_ID),json.getString(NAME),"","","light_bulb","group","");

							if (json.has(ONOFF)) {
								boolean stateBool = (json.getInt(ONOFF) != 0);
								fimpApi.reportSwitchStateChange(json.getInt(INSTANCE_ID),stateBool);
							}else {
								System.out.println("No switch in the group ");
							}
							if (json.has(DIMMER)) {
								int level = json.getInt(DIMMER);
								fimpApi.reportDimLvlChange(json.getInt(INSTANCE_ID),level);
							}else {
								System.out.println("No dim level in the group ");
							}

							name2id.put(json.getString(NAME), json.getInt(INSTANCE_ID));

						} else {
							System.out.println("not bulb");
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("psk", true, "The Secret on the base of the gateway");
		options.addOption("ip", true, "The IP address of the gateway");
		options.addOption("broker", true, "MQTT URL");
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String psk = cmd.getOptionValue("psk");
		String ip = cmd.getOptionValue("ip");
		String broker = cmd.getOptionValue("broker");
		
		if (psk == null || ip == null || broker == null) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "TRADFRI2MQTT", options );
			System.exit(1);
		}

		Main m = new Main(psk, ip, broker);
		m.discover();
	}

}

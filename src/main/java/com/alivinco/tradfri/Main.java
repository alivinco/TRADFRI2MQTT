/**
 * 
 */
package com.alivinco.tradfri;

import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author alivinco
 *
 */
public class Main {
	
	static {
		CaliforniumLogger.disableLogging();
//		ScandiumLogger.disable();
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.FINE);
	}
	static Logger logger = Logger.getLogger("ikea");
	private MqttClient mqttClient;
	private DeviceDb deviceDb;
	private AdapterApi adApi;
	private FimpApi fimpApi;
	private TradfriApi tradfriApi;
	private MsgRouter msgRouter;

	
	Main(String broker,String configFilePath, String username, String password) {
		this.deviceDb = new DeviceDb();
		MemoryPersistence persistence = new MemoryPersistence();
		try {
			MqttConnectOptions options = new MqttConnectOptions();
                        if (username != null && password != null) {
                                options.setUserName(username);
                                options.setPassword(password.toCharArray());
                        }
			options.setAutomaticReconnect(true);
			options.setMaxInflight(200);
			mqttClient = new MqttClient(broker, MqttClient.generateClientId(), persistence);
			mqttClient.connect(options);
			this.fimpApi = new FimpApi(mqttClient,this.deviceDb);
			this.tradfriApi = new TradfriApi(configFilePath);
			this.adApi = new AdapterApi(this.deviceDb,this.mqttClient,this.tradfriApi );
			this.msgRouter = new MsgRouter(this.adApi,this.fimpApi,this.tradfriApi,this.deviceDb);
			mqttClient.setCallback(new MqttCallback() {
				
				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					Main.this.msgRouter.onMqttMessage(topic,message);
				}
				
				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void connectionLost(Throwable cause) {
					// TODO Auto-generated method stub
					logger.warning("Connection lost:"+cause.getMessage());
//					cause.printStackTrace();
				}
			});
			tradfriApi.setEventsHandler(this.msgRouter);
			mqttClient.subscribe("pt:j1/mt:cmd/rt:ad/rn:ikea/ad:1");
			mqttClient.subscribe("pt:j1/mt:cmd/rt:dev/rn:ikea/ad:1/sv:out_bin_switch/+");
			mqttClient.subscribe("pt:j1/mt:cmd/rt:dev/rn:ikea/ad:1/sv:out_lvl_switch/+");
			mqttClient.subscribe("pt:j1/mt:cmd/rt:dev/rn:ikea/ad:1/sv:color_ctrl/+");
		} catch (MqttException e) {
//			e.printStackTrace();
			logger.warning(e.getMessage());
		}

	}

	public static void setupLogger() {
		FileHandler fh;

		try {
			fh = new FileHandler("ikea.log", true);
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.setLevel(Level.ALL);
		logger.log(Level.INFO, "First log entry");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//-psk LUMozPGBWkaFSCgU
		Main.setupLogger();
		logger.info("Loading configurations");
		Options options = new Options();
		options.addOption("conf", true, "Configuration storage file path ");
		options.addOption("broker", true, "MQTT URL");
                options.addOption("username", true, "MQTT username");
                options.addOption("password", true, "MQTT password");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		String confFile = cmd.getOptionValue("conf");
		String broker = cmd.getOptionValue("broker");
                String username = cmd.getOptionValue("username");
                String password = cmd.getOptionValue("password");
		
		if (confFile == null || broker == null) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "TRADFRI2MQTT", options );
			System.exit(1);
		}

		Main m = new Main(broker,confFile, username, password);

	}

}

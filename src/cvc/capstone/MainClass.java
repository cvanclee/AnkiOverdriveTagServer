package cvc.capstone;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.Properties;

public class MainClass {

	public static int NODE_JS_SERVER_PORT = -1;
	public static String NODE_JS_SERVER_NAME = null;
	public static String NODE_JS_SERVER_PATH = null;
	public static int MY_SERVER_PORT = 4999;
	public static int COMMUNICATION_TIMEOUT = -1; // ms
	public static int MAX_QUEUE_SIZE = 20; //max size of the client-server and server-car command queues

	public static void main(String[] args) throws ServerException {
		String OS = System.getProperty("os.name").toLowerCase();
		if (!(OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 )) {
			System.out.println("Linux only. Exiting");
		}
		String propPath;
		try {
			propPath = new File(MainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent()
					+ "/res/serverProperties.properties";
			NODE_JS_SERVER_PATH = new File(MainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent()
					+ "/nodejs/server.js";
		} catch (URISyntaxException e) {
			throw new ServerException(e);
		}
		if (!readProperties(propPath)) {
			throw new ServerException("Failed to read properties file");
		}

		while (true) {
			System.out.println("-----------------");
			NodeServerController nsc = new NodeServerController();
			nsc.start();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			System.out.println("Server started. Beginning game setup");
			GameManager gameManager = new GameManager();
			try {
				if (gameManager.setUp()) {
					gameManager.play();
				} else {
					;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Game has ended. Preparing new game.");
			gameManager.killClientManagers();
			gameManager.ankiCleanup();
			nsc.interrupt();
			nsc.close();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	public static boolean readProperties(String path) {
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream(path));
			NODE_JS_SERVER_PORT = Integer.parseInt(prop.getProperty("NODE_JS_SERVER_PORT"));
			NODE_JS_SERVER_NAME = prop.getProperty("NODE_JS_SERVER_NAME");
			MY_SERVER_PORT = Integer.parseInt(prop.getProperty("MY_SERVER_PORT"));
			COMMUNICATION_TIMEOUT = Integer.parseInt(prop.getProperty("COMMUNICATION_TIMEOUT"));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
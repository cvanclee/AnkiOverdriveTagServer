package cvc.capstone;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.Properties;

public class MainClass {

	public static int NODE_JS_SERVER_PORT = -1;
	public static String NODE_JS_SERVER_NAME = null;
	public static int MY_SERVER_PORT = -1;
	public static int COMMUNICATION_TIMEOUT = -1; // ms

	public static void main(String[] args) throws ServerException {
		String propPath;
		try {
			propPath = new File(MainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent()
					+ "/res/serverProperties.properties";
		} catch (URISyntaxException e) {
			throw new ServerException(e);
		}
		if (!readProperties(propPath)) {
			throw new ServerException("Failed to read properties file");
		}

		GameManager gameManager = new GameManager();
		if (!gameManager.setUp()) {
			return;
		}
		gameManager.play();
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
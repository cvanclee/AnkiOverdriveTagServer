package cvc.capstone;

import java.util.List;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.PingRequestMessage;
import de.adesso.anki.messages.SetSpeedMessage;
import de.adesso.anki.AnkiConnector;

public class MainClass {

	public static final int NODE_JS_SERVER_PORT = 5000;
	public static final String NODE_JS_SERVER_NAME = "localhost";

	public static void main(String[] args) {
		AnkiConnector anki = null;
		List<Vehicle> vehicles = null;

		System.out.println("Launching connectors...");
		try {
			anki = new AnkiConnector(NODE_JS_SERVER_NAME, NODE_JS_SERVER_PORT);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to create AnkiConnector. Exiting");
			return;
		}
		
		System.out.println("Looking for cars...");
		try {
			vehicles = anki.findVehicles();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to find vehicles. Exiting");
			return;
		}
		
		System.out.println("Found " + vehicles.size() + " cars:");
		for (Vehicle v : vehicles) {
			String vModelId = String.valueOf(v.getAdvertisement().getModelId());
			System.out.println("Attempting to connect to model " + vModelId + " address " + v.getAddress());
			v.connect();
			System.out.println("Connected. Testing communication with ping waiting for response");
			v.sendMessage(new PingRequestMessage());
		}
	}
}
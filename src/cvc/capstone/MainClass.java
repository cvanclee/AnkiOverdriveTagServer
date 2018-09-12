package cvc.capstone;

import java.util.List;
import de.adesso.anki.Vehicle;
import de.adesso.anki.AnkiConnector;

public class MainClass {

	public static final int PORT = 5000;
	public static final String NODE_JS_SERVER_IP = "localhost";

	public static void main(String[] args) {
		AnkiConnector anki = null;
		List<Vehicle> vehicles = null;

		System.out.println("Launching connectors...");
		try {
			anki = new AnkiConnector(NODE_JS_SERVER_IP, PORT);
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
			System.out.println(v.getAdvertisement().getModelId());
		}
	}
}
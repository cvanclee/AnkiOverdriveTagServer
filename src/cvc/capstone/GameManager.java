package cvc.capstone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.PingRequestMessage;

public class GameManager {

	private AnkiConnector anki;
	private List<Vehicle> vehicles;
	private volatile ConcurrentHashMap<Integer, ClientManager> connectedClients;
	private volatile AtomicInteger readyCount;
	private CarManager carManager;

	public GameManager() {
		carManager = new CarManager();
		connectedClients = new ConcurrentHashMap<Integer, ClientManager>();
		readyCount = new AtomicInteger(0);
	}

	public void play() {
		waitForPlayers();
	}

	public boolean setUp() {
		System.out.println("Launching Anki connector...");
		try {
			anki = new AnkiConnector(MainClass.NODE_JS_SERVER_NAME, MainClass.NODE_JS_SERVER_PORT);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to create AnkiConnector. Exiting");
			return false;
		}

		System.out.println("Looking for cars...");
		try {
			vehicles = anki.findVehicles();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to find vehicles. Exiting");
			return false;
		}
		System.out.println("Found " + vehicles.size() + " Anki cars");
		if (vehicles.size() != 2) {
			System.out.println("Need exactly 2 cars to be located. Exiting.");
			return false;
		}
		for (Vehicle v : vehicles) {
			String vModelId = String.valueOf(v.getAdvertisement().getModelId());
			System.out.println("Attempting to connect to model " + vModelId + " address " + v.getAddress());
			v.connect();
			System.out.println("Connected. Testing communication with a ping.");
			v.sendMessage(new PingRequestMessage());
		}
		return true;
	}

	private void waitForPlayers() {
		System.out.println("Waiting for clients");
		try {
			int n = 0;
			ServerSocket serverSocket = new ServerSocket(MainClass.MY_SERVER_PORT);
			while (readyCount.get() < 2) {
				while (connectedClients.size() < 2) {
					Socket cs = serverSocket.accept();
					ClientManager cm = new ClientManager(cs, this, n);
					cm.start();
					connectedClients.put(n, cm);
					n++;
					Thread.sleep(10);
				}
				Thread.sleep(10);
			}
			System.out.println("Have enough clients to start");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void stop() {
		for (ClientManager cm : connectedClients.values()) {
			cm.interrupt();
		}
	}

	protected ConcurrentHashMap<Integer, ClientManager> getConnectedClients() {
		return connectedClients;
	}
	
	protected AtomicInteger getReadyCount() {
		return readyCount;
	}
}

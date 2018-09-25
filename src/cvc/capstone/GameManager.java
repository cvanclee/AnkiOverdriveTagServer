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
import de.adesso.anki.RoadmapScanner;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.PingRequestMessage;
import de.adesso.anki.roadmap.Roadmap;

public class GameManager {

	private AnkiConnector anki;
	private volatile List<VehicleWrapper> vehicles;
	private volatile ConcurrentHashMap<Integer, ClientManager> connectedClients;
	private volatile AtomicInteger readyCount;
	private Roadmap roadMap;

	public GameManager() {
		vehicles = new ArrayList<VehicleWrapper>();
		connectedClients = new ConcurrentHashMap<Integer, ClientManager>();
		readyCount = new AtomicInteger(0);
	}

	public void play() {
		waitForPlayers();
		scanTrack();
		mainGameLoop();
	}

	public void mainGameLoop() {
		for (ClientManager m : connectedClients.values()) {
			m.setGameReady(true);
		}

		// Keep reading the command queue that is populated by the client managers, and
		// the car queue
		while (true) {

		}
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
			List<Vehicle> regVehicles = anki.findVehicles();
			for (Vehicle v : regVehicles) {
				VehicleWrapper vw = new VehicleWrapper(v);
				vehicles.add(vw);
			}
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
		for (VehicleWrapper v : vehicles) {
			String vModelId = String.valueOf(v.getVehicle().getAdvertisement().getModelId());
			System.out
					.println("Attempting to connect to model " + vModelId + " address " + v.getVehicle().getAddress());
			v.getVehicle().connect();
			System.out.println("Connected. Testing communication with a ping.");
			v.getVehicle().sendMessage(new PingRequestMessage());
		}
		return true;
	}

	private void waitForPlayers() {
		Collections.shuffle(vehicles); // Random vehicle per client
		System.out.println("Waiting for clients");
		try {
			int n = 0;
			ServerSocket serverSocket = new ServerSocket(MainClass.MY_SERVER_PORT);
			while (readyCount.get() < 2) {
				while (connectedClients.size() < 2) {
					Socket cs = serverSocket.accept();
					System.out.println("New client has had socket connection accepted");
					ClientManager clientManager = new ClientManager(cs, this, n, vehicles.remove(0));
					clientManager.start();
					connectedClients.put(n, clientManager);
					n++;
					Thread.sleep(10);
				}
				Thread.sleep(10);
			}
			System.out.println("Have enough clients to start");
			for (int i = 0; i < connectedClients.size(); i++) {
				vehicles.add(connectedClients.get(i).getVehicle());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ServerException e) {
			e.printStackTrace();
		}
	}

	public void scanTrack() {
		System.out.println("Scanning track...");
		RoadmapScanner roadMapScannerOne = new RoadmapScanner(vehicles.get(0).getVehicle());
		RoadmapScanner roadMapScannerTwo = new RoadmapScanner(vehicles.get(1).getVehicle());
		roadMapScannerOne.startScanning();
		roadMapScannerTwo.startScanning();
		roadMap = roadMapScannerOne.getRoadmap();
		System.out.println("Done scanning...");
	}

	private void stop() {
		for (ClientManager cm : connectedClients.values()) {
			cm.interrupt();
		}
	}

	protected ConcurrentHashMap<Integer, ClientManager> getConnectedClients() {
		return connectedClients;
	}

	protected synchronized void addVehicle(VehicleWrapper v) {
		vehicles.add(v);
	}

	protected AtomicInteger getReadyCount() {
		return readyCount;
	}
}

package cvc.capstone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.RoadmapScanner;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.LocalizationIntersectionUpdateMessage;
import de.adesso.anki.messages.LocalizationPositionUpdateMessage;
import de.adesso.anki.messages.LocalizationTransitionUpdateMessage;
import de.adesso.anki.messages.Message;
import de.adesso.anki.messages.SdkModeMessage;
import de.adesso.anki.messages.SetSpeedMessage;
import de.adesso.anki.messages.VehicleDelocalizedMessage;
import de.adesso.anki.roadmap.Roadmap;

public class GameManager {

	public static final int SPEED_INCREMENT = 5;
	public static final int ACCEL_INCREMENT = 5;
	public static final int MAX_SPEED = 1500;
	public static final int MAX_ACCEL = 13000;
	public static final int MIN_SPEED = 500;
	public static final int MIN_ACCEL = 12000;
	private static final float LEFTMOST_OFFSET = -68.0f;
	private static final float LEFTINNER_OFFSET = -23.0f;
	private static final float RIGHTINNER_OFFERSET = 23.0f;
	private static final float RIGHTMOST_OFFSET = 68.0f;
	private AnkiConnector anki;
	private volatile List<VehicleWrapper> vehicles;
	private volatile ConcurrentHashMap<Integer, ClientManager> connectedClients;
	private volatile AtomicInteger readyCount;
	private volatile AtomicBoolean clientOverflowStatus;
	private Roadmap roadMap;
	private volatile ArrayBlockingQueue<SocketMessageWithVehicle> serverClientQueue; //Client populates this
	private volatile ArrayBlockingQueue<Message> serverCarQueue; //Anki cars/SDK populates this

	public GameManager() {
		vehicles = new ArrayList<VehicleWrapper>();
		connectedClients = new ConcurrentHashMap<Integer, ClientManager>();
		readyCount = new AtomicInteger(0);
		serverClientQueue = new ArrayBlockingQueue<SocketMessageWithVehicle>(MainClass.MAX_QUEUE_SIZE, false);
		serverCarQueue = new ArrayBlockingQueue<Message>(MainClass.MAX_QUEUE_SIZE, false);
		clientOverflowStatus = new AtomicBoolean(false);
	}

	public void play() {
		waitForPlayers();
		if (!scanTrack()) {
			System.out.println("Failed to properly scan track. Exiting.");
			return;
		}
		mainGameLoop();
	}

	/**
	 * Notify clients their roles and that the game is starting, handle game logic
	 * until end condition is detected
	 */
	public void mainGameLoop() {
		for (ClientManager m : connectedClients.values()) {
			m.setGameReady(true);
		}

		// Keep reading the command queue that is populated by the client managers, and
		// the car queue that is populated from the physical Anki cars until end
		// condition is reached.
		while (true) {
			if (clientOverflowStatus.get()) { // end condition
				System.out.println("Overflow in client-server queue. Ending game.");
				return;
			}
			while (!serverCarQueue.isEmpty()) {
				Message m = serverCarQueue.poll();
			}
			while (!serverClientQueue.isEmpty()) {
				SocketMessageWithVehicle msgv = serverClientQueue.poll();
				switch (msgv.msg.cmd) {
				case 1005:
					increaseCarSpeed(msgv);
					break;
				case 1003:
					changeLaneLeft(msgv);
					break;
				case 1004:
					changeLaneRight(msgv);
					break;
				default:
					;
				}
			}
		}
	}

	/**
	 * Connects to the nodejs server and scans and connects to vehicles
	 * 
	 * @return true if all procedures finished expectedly
	 */
	public boolean setUp() {
		System.out.println("Launching Anki connector...");
		if(!launchAnki()) {
			System.out.println("Unable to create AnkiConnector. Exiting");
			return false;
		}
		System.out.println("Looking for vehicles...");
		if(!findVehicles()) {
			return false;
		}
		return true;
	}

	/**
	 * Wait for two clients to connect and ready up, randomly assigning a car to
	 * each client to connect. If a client leaves, the clientManager is stopped, then the
	 * vehicle is added back to the unassigned pool, and it will wait for another
	 * client to connect.
	 */
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
					vehicles.get(0).setClientManagerId(n);
					ClientManager clientManager = new ClientManager(cs, this, n, vehicles.remove(0));
					clientManager.start();
					connectedClients.put(n, clientManager);
					n++;
				}
				Thread.sleep(10);
			}
			serverSocket.close();
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

	/**
	 * Scan the track with both cars, and get them centered on different lanes.
	 * 
	 * @return true if a valid track is scanned
	 */
	public boolean scanTrack() {
		System.out.println("Scanning track...");
		RoadmapScanner roadMapScannerOne = new RoadmapScanner(vehicles.get(0).getVehicle());
		RoadmapScanner roadMapScannerTwo = new RoadmapScanner(vehicles.get(1).getVehicle());
		roadMapScannerOne.startScanning();
		try {
			Thread.sleep(2000); //To try to get them not lined up, so I can change lanes
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		roadMapScannerTwo.startScanning();
		while (!roadMapScannerOne.isComplete()) {
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}
		vehicles.get(0).getVehicle().sendMessage(new SetSpeedMessage(0, 12500));
		while (!roadMapScannerTwo.isComplete()) {
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}
		vehicles.get(0).getVehicle().sendMessage(new SetSpeedMessage(0, 12500));
		vehicles.get(1).getVehicle().sendMessage(new SetSpeedMessage(0, 12500));
		roadMap = roadMapScannerOne.getRoadmap();
		vehicles.get(0).getVehicle().removeAllListeners();
		vehicles.get(1).getVehicle().removeAllListeners(); //For the listeners the roadmap scanner added
		attachHandles(vehicles);
		System.out.println("Done scanning. Game beginning.");
		return true;
	}
	
	/**
	 * Connect to nodejs server
	 * 
	 * @return true if nodejs server is connected
	 */
	private boolean launchAnki() {
		try {
			anki = new AnkiConnector(MainClass.NODE_JS_SERVER_NAME, MainClass.NODE_JS_SERVER_PORT);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Scan for vehicles and establish a connection to them
	 * 
	 * @return true if 2 vehicles are found and connected to.
	 */
	public boolean findVehicles() {
		try {
			List<Vehicle> regVehicles = anki.findVehicles();
			for (Vehicle v : regVehicles) {
				VehicleWrapper vw = new VehicleWrapper(v, -1);
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
			v.getVehicle().sendMessage(new SdkModeMessage());
			System.out.println("Connected");
		}
		attachHandles(vehicles);
		return true;
	}
	
	/**
	 * Attach listeners for car messages
	 * 
	 * @param vehicles vehicles to attach the listeners to
	 */
	private void attachHandles(List<VehicleWrapper> vehicles) {
		for (VehicleWrapper vw : vehicles) {
			CarDelocalizedHandler cdh = new CarDelocalizedHandler(vw);
			CarLocalizationHandler cdl = new CarLocalizationHandler(vw);
			CarTransitionHandler cth = new CarTransitionHandler(vw);
			CarIntersectionHandler cih = new CarIntersectionHandler(vw);
			vw.getVehicle().addMessageListener(VehicleDelocalizedMessage.class, cdh);
			vw.getVehicle().addMessageListener(LocalizationPositionUpdateMessage.class, cdl);
			vw.getVehicle().addMessageListener(LocalizationTransitionUpdateMessage.class, cth);
			vw.getVehicle().addMessageListener(LocalizationIntersectionUpdateMessage.class, cih);
		}
	}

	/**
	 * Kill the client manager threads
	 */
	private void stopClientManagers() {
		for (ClientManager cm : connectedClients.values()) {
			cm.interrupt();
		}
	}
	
	private void increaseCarSpeed(SocketMessageWithVehicle msgv) {
		VehicleWrapper vw = msgv.myVehicle;
		if (vw.getSpeed().get() > MAX_SPEED || vw.getAcceleration().get() > MAX_ACCEL) {
			return;
		}
		vw.changeSpeedAndAccel(SPEED_INCREMENT, ACCEL_INCREMENT);
		vw.getVehicle().sendMessage(new SetSpeedMessage(vw.getSpeed().get(), vw.getAcceleration().get()));
	}
	
	private void changeLaneLeft(SocketMessageWithVehicle msgv) {
		
	}
	
	private void changeLaneRight(SocketMessageWithVehicle msgv) {
		
	}
	
	public ArrayBlockingQueue<SocketMessageWithVehicle> getServerClientQueue() {
		return serverClientQueue;
	}

	public ConcurrentHashMap<Integer, ClientManager> getConnectedClients() {
		return connectedClients;
	}

	public synchronized void addVehicle(VehicleWrapper v) {
		vehicles.add(v);
	}

	public AtomicInteger getReadyCount() {
		return readyCount;
	}
	
	public AtomicBoolean getClientOverflowStatus() { 
		return clientOverflowStatus;
	}
	
	private class CarDelocalizedHandler implements MessageListener<VehicleDelocalizedMessage> {
		private VehicleWrapper myVehicle;
		
		public CarDelocalizedHandler(VehicleWrapper myVehicle) {
			this.myVehicle = myVehicle;
		}
		
		@Override
		public void messageReceived(VehicleDelocalizedMessage m) {
			System.out.println("DELOCALIZED");
		}
	}
	
	private class CarLocalizationHandler implements MessageListener<LocalizationPositionUpdateMessage> {
		private VehicleWrapper myVehicle;
		
		public CarLocalizationHandler(VehicleWrapper myVehicle) {
			this.myVehicle = myVehicle;
		}
		
		@Override
		public void messageReceived(LocalizationPositionUpdateMessage m) {
			//myVehicle.setLaneOffset(m.getOffsetFromRoadCenter());
		}
	}
	
	private class CarTransitionHandler implements MessageListener<LocalizationTransitionUpdateMessage> {
		private VehicleWrapper myVehicle;
		
		public CarTransitionHandler(VehicleWrapper myVehicle) {
			this.myVehicle = myVehicle;
		}
		
		@Override
		public void messageReceived(LocalizationTransitionUpdateMessage m) {
			myVehicle.setLaneOffset(m.getOffsetFromRoadCenter());
		}
	}
	
	private class CarIntersectionHandler implements MessageListener<LocalizationIntersectionUpdateMessage> {
		private VehicleWrapper myVehicle;
		
		public CarIntersectionHandler(VehicleWrapper myVehicle) {
			this.myVehicle = myVehicle;
		}
		
		@Override
		public void messageReceived(LocalizationIntersectionUpdateMessage m) {

		}
	}
}

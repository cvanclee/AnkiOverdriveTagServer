package cvc.capstone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.adesso.anki.*;
import de.adesso.anki.messages.*;
import de.adesso.anki.messages.LightsPatternMessage.*;
import de.adesso.anki.roadmap.*;
import de.adesso.anki.roadmap.roadpieces.Roadpiece;

public class GameManager {

	public static final int SPEED_INCREMENT = 10;
	public static final int ACCEL_INCREMENT = 10;
	public static final int MAX_SPEED = 1400;
	public static final int MAX_ACCEL = 13000;
	public static final int MIN_SPEED = 400; //default 400
	public static final int MIN_ACCEL = 12000;
	public static final float LEFTMOST_OFFSET = -68.0f;
	public static final float LEFTINNER_OFFSET = -23.0f;
	public static final float RIGHTINNER_OFFSET = 23.0f;
	public static final float RIGHTMOST_OFFSET = 68.0f;
	private static final int TURN_SPEED = 500;
	private static final int TURN_ACCEL = 1000;
	private static final int SLOWDOWN_OCCURENCE = 500; //how often the cars get a slowdown command (ms)
	private AnkiConnector anki;
	private volatile List<VehicleWrapper> vehicles;
	private volatile ConcurrentHashMap<Integer, ClientManager> connectedClients;
	private volatile AtomicInteger readyCount; //number of clients that are ready
	private volatile AtomicBoolean clientOverflowStatus; //true if command queue overflows (really bad)
	private Roadmap roadMap; //the physical roadmap, created by scanning the track
	private volatile ArrayBlockingQueue<SocketMessageWithVehicle> serverClientQueue; //Client populates this with cmds
	private volatile ArrayBlockingQueue<Message> serverCarQueue; //Anki cars/SDK populates this with responses
	private Timer slowTimer; //schedules car slowsdowns
	private Timer scoreTimer; //schedules score increments for 'it'
	private Timer blockCooldown; //keeps track of when the last successful block was applied
	private Timer blockDuration; //keeps track of how much longer 'it' will be blocking
	private VehicleWrapper it; //The vehicle that is 'it'
	private VehicleWrapper tagger; //The vehicle that is 'tagger'
	private volatile AtomicBoolean blocking; //Keeps track of whether 'it' is blocking

	public GameManager() {
		vehicles = new ArrayList<VehicleWrapper>();
		connectedClients = new ConcurrentHashMap<Integer, ClientManager>();
		readyCount = new AtomicInteger(0);
		serverClientQueue = new ArrayBlockingQueue<SocketMessageWithVehicle>(MainClass.MAX_QUEUE_SIZE, false);
		serverCarQueue = new ArrayBlockingQueue<Message>(MainClass.MAX_QUEUE_SIZE, false);
		clientOverflowStatus = new AtomicBoolean(false);
		blocking = new AtomicBoolean(false);
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
		try {
			//Set lights for start of game
			startingLights();
			
			//Notify clients of roles and that the game has begun
			it = vehicles.get(0);
			tagger = vehicles.get(1);
			connectedClients.get(vehicles.get(0).getClientManagerId()).sendCmd(1010, ""); //it
			connectedClients.get(vehicles.get(1).getClientManagerId()).sendCmd(1011, ""); //tagger
			for (ClientManager m : connectedClients.values()) {
				m.setGameReady(true);
			}
			
			//Start the timers for slowing the cars down and incrementing it's score
			slowTimer = new Timer();
			slowTimer.scheduleAtFixedRate(new SlowCarsTask(), SLOWDOWN_OCCURENCE, SLOWDOWN_OCCURENCE);
			scoreTimer = new Timer();
			scoreTimer.scheduleAtFixedRate(new IncrementScoreTask(), 30000, 30000); //Give a 5 second delay for fairness

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
					case 1006:
						turnAround(msgv);
					case 1007:
						tagAttempt(msgv);
					case 1008:
						blockAttempt(msgv);
					default:
						;
					}
				}
			}
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
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
	public void waitForPlayers() {
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
			}
			serverSocket.close();
			System.out.println("Have enough clients to start");
			for (ClientManager cm : connectedClients.values()) {
				vehicles.add(cm.getVehicle());
			}
		} catch (IOException e) {
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
		try {
			RoadmapScanner roadMapScannerOne = new RoadmapScanner(vehicles.get(0).getVehicle());
			RoadmapScanner roadMapScannerTwo = new RoadmapScanner(vehicles.get(1).getVehicle());
			roadMapScannerOne.startScanning();
			vehicles.get(0).getVehicle().sendMessage(new SetOffsetFromRoadCenterMessage(0));
			vehicles.get(0).getVehicle().sendMessage(new ChangeLaneMessage(LEFTMOST_OFFSET, 50, 1000));
			try {
				Thread.sleep(1000); // To try to get them not lined up, so I can change lanes
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
			roadMapScannerTwo.startScanning();
			vehicles.get(1).getVehicle().sendMessage(new SetOffsetFromRoadCenterMessage(0));
			vehicles.get(1).getVehicle().sendMessage(new ChangeLaneMessage(RIGHTMOST_OFFSET, 50, 1000));
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
			vehicles.get(1).getVehicle().sendMessage(new SetSpeedMessage(0, 12500));
			roadMap = roadMapScannerOne.getRoadmap();
			Roadmap tempMap = roadMapScannerTwo.getRoadmap();
			vehicles.get(0).getVehicle().removeAllListeners();
			vehicles.get(1).getVehicle().removeAllListeners(); // For the listeners the roadmap scanner added
			attachHandles(vehicles);
			System.out.println("Done scanning. Game beginning.");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
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
	 * Set lights for start of game
	 * TODO: Engine light is strobing instead of steady...
	 */
	private void startingLights() {
		LightConfig itLightEngine = new LightConfig(LightChannel.ENGINE_RED, LightEffect.STEADY, 100, 0, 0);
		LightConfig itLightFront = new LightConfig(LightChannel.FRONT_RED, LightEffect.STEADY, 100, 0, 0);
		LightConfig tagLightEngine = new LightConfig(LightChannel.ENGINE_GREEN, LightEffect.STEADY, 100, 0, 0);
		LightConfig tagLightFront = new LightConfig(LightChannel.FRONT_GREEN, LightEffect.STEADY, 100, 0, 0);
		LightsPatternMessage lpm = new LightsPatternMessage();
		lpm.add(itLightEngine);
		lpm.add(itLightFront);
		vehicles.get(0).getVehicle().sendMessage(lpm);
		lpm = new LightsPatternMessage();
		lpm.add(tagLightEngine);
		lpm.add(tagLightFront);
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		vehicles.get(1).getVehicle().sendMessage(lpm);
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
		switch ((int) msgv.myVehicle.getLaneOffset()) { // has to be int to switch
		case (int) LEFTMOST_OFFSET:
			return;
		case (int) LEFTINNER_OFFSET:
			msgv.myVehicle.getVehicle().sendMessage(new ChangeLaneMessage(LEFTMOST_OFFSET, TURN_SPEED, TURN_ACCEL));
			msgv.myVehicle.setLaneOffset(LEFTMOST_OFFSET);
			break;
		case (int) RIGHTINNER_OFFSET:
			msgv.myVehicle.getVehicle().sendMessage(new ChangeLaneMessage(LEFTINNER_OFFSET, TURN_SPEED, TURN_ACCEL));
			msgv.myVehicle.setLaneOffset(LEFTINNER_OFFSET);
			break;
		case (int) RIGHTMOST_OFFSET:
			msgv.myVehicle.getVehicle().sendMessage(new ChangeLaneMessage(RIGHTINNER_OFFSET, TURN_SPEED, TURN_ACCEL));
			msgv.myVehicle.setLaneOffset(RIGHTINNER_OFFSET);
			break;
		default:
			return;
		}
	}
	
	private void changeLaneRight(SocketMessageWithVehicle msgv) {
		switch ((int) msgv.myVehicle.getLaneOffset()) { // has to be int to switch
		case (int) RIGHTMOST_OFFSET:
			return;
		case (int) RIGHTINNER_OFFSET:
			msgv.myVehicle.getVehicle().sendMessage(new ChangeLaneMessage(RIGHTMOST_OFFSET, TURN_SPEED, TURN_ACCEL));
			msgv.myVehicle.setLaneOffset(RIGHTMOST_OFFSET);
			break;
		case (int) LEFTINNER_OFFSET:
			msgv.myVehicle.getVehicle().sendMessage(new ChangeLaneMessage(RIGHTINNER_OFFSET, TURN_SPEED, TURN_ACCEL));
			msgv.myVehicle.setLaneOffset(RIGHTINNER_OFFSET);
			break;
		case (int) LEFTMOST_OFFSET:
			msgv.myVehicle.getVehicle().sendMessage(new ChangeLaneMessage(LEFTINNER_OFFSET, TURN_SPEED, TURN_ACCEL));
			msgv.myVehicle.setLaneOffset(LEFTINNER_OFFSET);
			break;
		default:
			return;
		}
	}
	
	private void turnAround(SocketMessageWithVehicle msgv) {
		msgv.myVehicle.getVehicle().sendMessage(new TurnMessage(3, 0));
	}

	/**
	 * If the command issuer is 'tagger', 'tagger' is in the same lane as 'it', and
	 * there are no curved road pieces between 'it' and 'tagger' (in the direction
	 * 'tagger' is facing), a successful tag occurs.
	 * 
	 * @param msgv
	 */
	private void tagAttempt(SocketMessageWithVehicle msgv) {
		if (msgv.myVehicle == it) {
			return; //it can't tag itself
		}
		if (it.getLaneOffset() != tagger.getLaneOffset()) { // not same lane
			return;
		}
		
	}
	
	private void blockAttempt(SocketMessageWithVehicle msgv) {
		if (msgv.myVehicle != it) {
			return; //taggers can't block
		}
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
			System.out.println("roadpiece:" + m.getRoadPieceId());
			myVehicle.setLaneOffset(m.getOffsetFromRoadCenter());
		}
	}
	
	private class CarTransitionHandler implements MessageListener<LocalizationTransitionUpdateMessage> {
		private VehicleWrapper myVehicle;
		
		public CarTransitionHandler(VehicleWrapper myVehicle) {
			this.myVehicle = myVehicle;
		}
		
		@Override
		public void messageReceived(LocalizationTransitionUpdateMessage m) { //transition bar
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
	
	private class IncrementScoreTask extends TimerTask {

		/**
		 * Increments 'its' score by 2 every 30 seconds, this should get restarted if a
		 * new player becomes 'it'
		 */
		@Override
		public void run() {
			it.incScore(2);
		}
	}
	
	private class SlowCarsTask extends TimerTask {
		private static final int SPEED_SLOWDOWN = -20;
		private static final int ACCEL_SLOWDOWN = -20;

		/**
		 * Slow down the cars every s seconds by some -speed, -acceleration
		 */
		@Override
		public void run() {
			for (VehicleWrapper vw : vehicles) {
				if (vw.getSpeed().get() < MIN_SPEED || vw.getAcceleration().get() < MIN_ACCEL) {
					continue;
				}
				vw.changeSpeedAndAccel(SPEED_SLOWDOWN, ACCEL_SLOWDOWN);
				vw.getVehicle().sendMessage(new SetSpeedMessage(vw.getSpeed().get(), vw.getAcceleration().get()));
			}
		}
	}
}

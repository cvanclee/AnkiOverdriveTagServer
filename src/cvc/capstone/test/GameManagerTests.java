package cvc.capstone.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cvc.capstone.ClientManager;
import cvc.capstone.GameManager;
import cvc.capstone.ServerException;
import cvc.capstone.SocketMessage;
import cvc.capstone.SocketMessageWithVehicle;
import cvc.capstone.VehicleWrapper;
import de.adesso.anki.AdvertisementData;
import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.RoadmapScanner;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.ChangeLaneMessage;
import de.adesso.anki.messages.LocalizationTransitionUpdateMessage;
import de.adesso.anki.messages.Message;
import de.adesso.anki.messages.SetOffsetFromRoadCenterMessage;
import de.adesso.anki.messages.SetSpeedMessage;
import de.adesso.anki.roadmap.Roadmap;
import de.adesso.anki.roadmap.roadpieces.Roadpiece;

public class GameManagerTests {
	
	private final String host = "localhost";
	private final int port = 4999;
	private Socket fakeClientSocketOne = null;
	private Socket fakeClientSocketTwo = null;
	private GameManager manager;
	Thread t1 = new Thread(new Runnable() {public void run() {}}); //To stop compile error/warning
	Thread t2 = new Thread(new Runnable() {public void run() {}});
	
	@Before
	public void setup() throws IOException {
		manager = new GameManager();
		fakeClientSocketOne = new Socket();
		fakeClientSocketTwo = new Socket();
	}
	
	@After
	public void cleanup() {
		try {
			//fakeClientSocketOne.close();
		} catch (Exception e) {}
		try {
			//fakeClientSocketTwo.close();
		} catch (Exception e) {}
	}
	
	/**
	 * Equivalence classes:
	 * Two clients connect and ready up (return)
	 * No clients connect (infinite loop)
	 */
	@Test
	public void waitForPlayersTestOne(){
		t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10);
					fakeClientSocketOne.setSoTimeout(2000);
					fakeClientSocketOne.connect(new InetSocketAddress(host, port));
					ObjectOutputStream os = new ObjectOutputStream(fakeClientSocketOne.getOutputStream());
					os.writeObject(new SocketMessage("a", 1000, ""));
					Thread.sleep(10);
					os.writeObject(new SocketMessage("a", 1001, ""));
				} catch (Exception e) {
					e.printStackTrace();
					assertEquals(0, 1);
				}
			}
		});
		t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10);
					fakeClientSocketTwo.setSoTimeout(2000);
					fakeClientSocketTwo.connect(new InetSocketAddress(host, port));
					ObjectOutputStream os = new ObjectOutputStream(fakeClientSocketTwo.getOutputStream());
					os.writeObject(new SocketMessage("a", 1000, ""));
					Thread.sleep(10);
					os.writeObject(new SocketMessage("a", 1001, ""));
				} catch (Exception e) {
					e.printStackTrace();
					assertEquals(0, 1);
				}
			}
		});
		t1.start();
		t2.start();
		VehicleSpoofer v1 = new VehicleSpoofer();
		VehicleSpoofer v2 = new VehicleSpoofer();
		VehicleWrapperSpoofer vs1 = new VehicleWrapperSpoofer(v1, 1);
		VehicleWrapperSpoofer vs2 = new VehicleWrapperSpoofer(v2, 2);
		manager.addVehicle(vs1);
		manager.addVehicle(vs2);
		manager.waitForPlayers();
	}
	
	/**
	 * Equivalence classes:
	 * The cars finish scanning a valid track (return true)
	 * The cars finish scanning an invalid track (return false)
	 * The cars never finish scanning the track (timeout reached, return false)
	 * 
	 * BUGS: 
	 * forgot to return false if no start piece is found (test two)
	 * timeout check should be >= SCAN_TRACK_TIMEOUT, instead of > SCAN_TRACK_TIMEOUT (test three)
	 */
	@Test
	public void scanTrackTestOne() throws IOException {
		VehicleSpoofer v1 = new VehicleSpoofer();
		VehicleSpoofer v2 = new VehicleSpoofer();
		VehicleWrapperSpoofer vw1 = new VehicleWrapperSpoofer(v1, 1);
		VehicleWrapperSpoofer vw2 = new VehicleWrapperSpoofer(v2, 2);
		manager.addVehicle(vw1);
		manager.addVehicle(vw2);
		RoadmapScannerSpoofer s1 = new RoadmapScannerSpoofer(v1);
		RoadmapScannerSpoofer s2 = new RoadmapScannerSpoofer(v2);
		manager.setRoadmapScannerTESTONLY(s1, s2);
		boolean r = manager.scanTrack();
		assertEquals(r, true);
	}
	
	@Test
	public void scanTrackTestTwo() {
		VehicleSpoofer v1 = new VehicleSpoofer();
		VehicleSpoofer v2 = new VehicleSpoofer();
		VehicleWrapperSpoofer vw1 = new VehicleWrapperSpoofer(v1, 1);
		VehicleWrapperSpoofer vw2 = new VehicleWrapperSpoofer(v2, 2);
		manager.addVehicle(vw1);
		manager.addVehicle(vw2);
		RoadmapScannerSpooferTwo s1 = new RoadmapScannerSpooferTwo(v1);
		RoadmapScannerSpooferTwo s2 = new RoadmapScannerSpooferTwo(v2);
		manager.setRoadmapScannerTESTONLY(s1, s2);
		boolean r = manager.scanTrack();
		assertEquals(r, false);
	}
	
	@Test
	public void scanTrackTestThree() {
		VehicleSpoofer v1 = new VehicleSpoofer();
		VehicleSpoofer v2 = new VehicleSpoofer();
		VehicleWrapperSpoofer vw1 = new VehicleWrapperSpoofer(v1, 1);
		VehicleWrapperSpoofer vw2 = new VehicleWrapperSpoofer(v2, 2);
		manager.addVehicle(vw1);
		manager.addVehicle(vw2);
		RoadmapScannerSpooferThree s1 = new RoadmapScannerSpooferThree(v1);
		RoadmapScannerSpooferThree s2 = new RoadmapScannerSpooferThree(v2);
		manager.setRoadmapScannerTESTONLY(s1, s2);
		boolean r = manager.scanTrack();
		assertEquals(r, false);
	}
	
	/**
	 * Equivalence classes:
	 * The connection to the nodejs server is made (return true)
	 * The connection to the nodejs server is not made (return false)
	 * @throws IOException 
	 */
	@Test
	public void launchAnkiTestOne() throws IOException {
		AnkiConnectorSpoofer anki = new AnkiConnectorSpoofer();
	}
	
	@Test
	public void launchAnkiTestTwo() {
		try {
			AnkiConnectorSpoofer anki = new AnkiConnectorSpoofer("", 1);
		} catch (IOException e) {
			return;
		}
		assertEquals(0, 1);
	} 
	
	/**
	 * Equivalence classes:
	 * Less than 2 vehicles are found (return false)
	 * More than 2 vehicles are found (return false)
	 * 2 vehicles are found (return true)
	 */
	@Test
	public void findVehiclesTestOne() {
		manager.setAnkiTESTONLY(new AnkiConnectorSpoofer());
		boolean r =  manager.findVehicles();
		assertEquals(r, false);
	}
	
	@Test
	public void findVehiclesTestTwo() {
		manager.setAnkiTESTONLY(new AnkiConnectorSpooferTwo());
		boolean r = manager.findVehicles();
		assertEquals(r, true);
	}
	
	@Test
	public void findVehiclesTestThree() {
		manager.setAnkiTESTONLY(new AnkiConnectorSpooferThree());
		boolean r = manager.findVehicles();
		assertEquals(r, false);
	}
	
	/**
	 * Equivalence classes:
	 * The road map is null (nullptr exception)
	 * The road map contains ÅenÅf unique pieces (uniquePieceIdMap.size() == ÅenÅf)
	 * The road map is empty (uniquePieceIdMap.size() == 0)
	 */
	@Test
	public void generateUniqueMapTestOne() {
		ArrayList<Roadpiece> map = null;
		manager.setRoadMapTESTONLY(map);
		try {
			manager.generateUniqueMap();
		} catch (NullPointerException e) {
			assertEquals(1, 1);
			return;
		}
		assertEquals(0, 1);
	}
	
	@Test
	public void generateUniqueMapTestTwo() {
		ArrayList<Roadpiece> map = new ArrayList<Roadpiece>();
		manager.setRoadMapTESTONLY(map);
		Roadpiece p1 = Roadpiece.createFromId(17);
		Roadpiece p2 = Roadpiece.createFromId(18);
		Roadpiece p3 = Roadpiece.createFromId(20);
		Roadpiece p4 = Roadpiece.createFromId(23);
		Roadpiece p5 = Roadpiece.createFromId(23);
		p1.setPieceId(17);
		p2.setPieceId(18);
		p3.setPieceId(20);
		p4.setPieceId(23);
		p5.setPieceId(23);
		manager.getRoadMap().add(p1);
		manager.getRoadMap().add(p2);
		manager.getRoadMap().add(p3);
		manager.getRoadMap().add(p4);
		manager.getRoadMap().add(p5);
		manager.generateUniqueMap();
		assertEquals(manager.getUniquePieceIdMap().size(), 3);
	}
	
	@Test
	public void generateUniqueMapTestThree() {
		ArrayList<Roadpiece> map = new ArrayList<Roadpiece>();
		manager.setRoadMapTESTONLY(map);
		manager.generateUniqueMap();
	}
	
	/**
	 * Equivalence classes:
	 * The message passed is null (NullPointerException thrown)
	 * VehicleÅfs speed is greater than max speed (return)
	 * VehicleÅfs speed is less than max speed (return, increase vehicle speed)
	 */
	@Test
	public void increaseCarSpeedTestOne() {
		SocketMessageWithVehicle msgv = null;
		try {
			manager.increaseCarSpeed(msgv);
		} catch (NullPointerException e) {
			assertEquals(1, 1);
			return;
		}
		assertEquals(0, 1);
	}
	
	@Test
	public void increaseCarSpeedTestTwo() {
		SocketMessage msg = new SocketMessage("", 0, "");
		VehicleSpoofer v = new VehicleSpoofer();
		VehicleWrapperSpoofer vws = new VehicleWrapperSpoofer(v, 0);
		SocketMessageWithVehicle msgv = new SocketMessageWithVehicle(msg, vws);
		vws.getSpeed().set(GameManager.MAX_SPEED + 1);
		manager.increaseCarSpeed(msgv);
		manager.setAnkiTESTONLY(new AnkiConnectorSpoofer());
		assertEquals(vws.getSpeed().get(), GameManager.MAX_SPEED + 1);
	}
	
	@Test
	public void increaseCarSpeedTestThree() {
		SocketMessage msg = new SocketMessage("", 0, "");
		VehicleSpoofer v = new VehicleSpoofer();
		VehicleWrapperSpoofer vws = new VehicleWrapperSpoofer(v, 0);
		SocketMessageWithVehicle msgv = new SocketMessageWithVehicle(msg, vws);
		manager.increaseCarSpeed(msgv);
		manager.setAnkiTESTONLY(new AnkiConnectorSpoofer());
		assertEquals(msgv.myVehicle.getSpeed().get(), GameManager.SPEED_INCREMENT + GameManager.MIN_SPEED);
	}
	
	/**
	 * No equivalence classes, too much going on. Use CFG for code coverage
	 */
	@Test
	public void tagAttemptTestOne() throws Exception {
		VehicleSpoofer itv = new VehicleSpoofer();
		VehicleWrapperSpoofer it = new VehicleWrapperSpoofer(itv, 0);
		SocketMessageWithVehicle ms1 = new SocketMessageWithVehicle(null, it);
		manager.setItTESTONLY(it);
		try {
			manager.tagAttempt(ms1); //it can't tag self
		} catch (ServerException e) {
			throw e;
		}
	}
	
	@Test
	public void tagAttemptTestTwo() throws Exception {
		VehicleSpoofer itv = new VehicleSpoofer();
		VehicleWrapperSpoofer it = new VehicleWrapperSpoofer(itv, 0);
		VehicleSpoofer taggerv = new VehicleSpoofer();
		VehicleWrapperSpoofer tagger = new VehicleWrapperSpoofer(taggerv, 1);
		SocketMessageWithVehicle ms1 = new SocketMessageWithVehicle(null, tagger);
		manager.setItTESTONLY(it);
		manager.setTaggerTESTONLY(tagger);
		manager.forceBlocking();
		try {
			manager.tagAttempt(ms1);
		} catch (ServerException e) {
			throw e;
		}
	}
	
	/**
	 * No equivalence classes, too much going on. Use CFG for code coverage
	 */
	@Test
	public void endGameTestOne() throws Exception {
		VehicleSpoofer itv = new VehicleSpoofer();
		VehicleWrapperSpoofer it = new VehicleWrapperSpoofer(itv, 0);
		VehicleSpoofer taggerv = new VehicleSpoofer();
		VehicleWrapperSpoofer tagger = new VehicleWrapperSpoofer(taggerv, 1);
		manager.setItTESTONLY(it);
		manager.setTaggerTESTONLY(tagger);
		ClientManagerSpoofer itcm = new ClientManagerSpoofer(0);
		ClientManagerSpoofer taggercm = new ClientManagerSpoofer(1);
		ConcurrentHashMap<Integer, ClientManager> connectedClients = new ConcurrentHashMap<Integer, ClientManager>();
		connectedClients.put(0, itcm);
		connectedClients.put(1, taggercm);
		manager.setConnectedClientsTESTONLY(connectedClients);
		AtomicBoolean tr = new AtomicBoolean(true);
		itcm.setLeft(tr);	
		manager.endGame(true);
		assertEquals(taggercm.expectedVal, 1013);
	}
	
	@Test
	public void endGameTestTwo() throws Exception {
		VehicleSpoofer itv = new VehicleSpoofer();
		VehicleWrapperSpoofer it = new VehicleWrapperSpoofer(itv, 0);
		VehicleSpoofer taggerv = new VehicleSpoofer();
		VehicleWrapperSpoofer tagger = new VehicleWrapperSpoofer(taggerv, 1);
		manager.setItTESTONLY(it);
		manager.setTaggerTESTONLY(tagger);
		ClientManagerSpoofer itcm = new ClientManagerSpoofer(0);
		ClientManagerSpoofer taggercm = new ClientManagerSpoofer(1);
		ConcurrentHashMap<Integer, ClientManager> connectedClients = new ConcurrentHashMap<Integer, ClientManager>();
		connectedClients.put(0, itcm);
		connectedClients.put(1, taggercm);
		manager.setConnectedClientsTESTONLY(connectedClients);
		AtomicBoolean fle = new AtomicBoolean(false);
		itcm.setLeft(fle);	
		taggercm.setLeft(fle);
		it.incScore(15);
		tagger.incScore(2);
		manager.endGame(false);
		assertEquals(taggercm.expectedVal, 1014);
		assertEquals(itcm.expectedVal, 1013);
	}
	
	@Test
	public void endGameTestThree() throws Exception {
		VehicleSpoofer itv = new VehicleSpoofer();
		VehicleWrapperSpoofer it = new VehicleWrapperSpoofer(itv, 0);
		VehicleSpoofer taggerv = new VehicleSpoofer();
		VehicleWrapperSpoofer tagger = new VehicleWrapperSpoofer(taggerv, 1);
		manager.setItTESTONLY(it);
		manager.setTaggerTESTONLY(tagger);
		ClientManagerSpoofer itcm = new ClientManagerSpoofer(0);
		ClientManagerSpoofer taggercm = new ClientManagerSpoofer(1);
		ConcurrentHashMap<Integer, ClientManager> connectedClients = new ConcurrentHashMap<Integer, ClientManager>();
		connectedClients.put(0, itcm);
		connectedClients.put(1, taggercm);
		manager.setConnectedClientsTESTONLY(connectedClients);
		AtomicBoolean fle = new AtomicBoolean(false);
		itcm.setLeft(fle);	
		taggercm.setLeft(fle);
		it.incScore(2);
		tagger.incScore(17);
		manager.endGame(false);
		assertEquals(taggercm.expectedVal, 1013);
		assertEquals(itcm.expectedVal, 1014);
	}
	
	@Test
	public void endGameTestFour() throws Exception {
		VehicleSpoofer itv = new VehicleSpoofer();
		VehicleWrapperSpoofer it = new VehicleWrapperSpoofer(itv, 0);
		VehicleSpoofer taggerv = new VehicleSpoofer();
		VehicleWrapperSpoofer tagger = new VehicleWrapperSpoofer(taggerv, 1);
		manager.setItTESTONLY(it);
		manager.setTaggerTESTONLY(tagger);
		ClientManagerSpoofer itcm = new ClientManagerSpoofer(0);
		ClientManagerSpoofer taggercm = new ClientManagerSpoofer(1);
		ConcurrentHashMap<Integer, ClientManager> connectedClients = new ConcurrentHashMap<Integer, ClientManager>();
		connectedClients.put(0, itcm);
		connectedClients.put(1, taggercm);
		manager.setConnectedClientsTESTONLY(connectedClients);
		AtomicBoolean fle = new AtomicBoolean(false);
		itcm.setLeft(fle);	
		taggercm.setLeft(fle);
		it.incScore(22);
		tagger.incScore(22);
		manager.endGame(false);
		assertEquals(taggercm.expectedVal, 1015);
		assertEquals(itcm.expectedVal, 1015);
	}
	
	/**
	 * No equivalence classes, too much going on, use CFG and cover code
	 */
	@Test
	public void mainGameLoopTestOne() {
		
	}
	
	/**
	 * Equivalence classes:
	 * The message passed is null [NullPointerException thrown]
     * The vehicle is on the leftmost offset [LEFTMOST_OFFSET] (return, no change in vehicle offset value)
     * The vehicle is on one of the other lane offsets [LEFTINNER_OFFSET] (return, vehicle is moved to LEFTMOST_OFFSET)
	 */
	@Test
	public void changeLaneLeftTestOne() {
		try {
			manager.changeLaneLeft(null);
		} catch(NullPointerException e) {
			return;
		}
		assertEquals(0, 1);
	}
	
	@Test
	public void changeLaneLeftTestTwo() {
		VehicleSpoofer v1 = new VehicleSpoofer();
		VehicleWrapperSpoofer vws1 = new VehicleWrapperSpoofer(v1, 0);
		vws1.setLaneOffset(GameManager.LEFTINNER_OFFSET);
		SocketMessageWithVehicle sm1 = new SocketMessageWithVehicle(null, vws1);
		manager.changeLaneLeft(sm1);
		assertEquals(vws1.getLaneOffset(), GameManager.LEFTMOST_OFFSET, .01f);
	}
	
	@Test
	public void changeLaneLeftTestThree() {
		VehicleSpoofer v1 = new VehicleSpoofer();
		VehicleWrapperSpoofer vws1 = new VehicleWrapperSpoofer(v1, 0);
		vws1.setLaneOffset(GameManager.LEFTINNER_OFFSET);
		SocketMessageWithVehicle sm1 = new SocketMessageWithVehicle(null, vws1);
		manager.changeLaneLeft(sm1);
		assertEquals(vws1.getLaneOffset(), GameManager.LEFTMOST_OFFSET, .01f);
	}
	
	/**
	 * Equivalence classes:	
	 * The message passed is null [NullPointerException thrown]
	 * The vehicle is on the rightmost offset [RIGHTMOST_OFFSET] (return, no change in vehicle offset value)
	 * The vehicle is on one of the other lane offsets [LEFTINNER_OFFSET] (return, vehicle is moved to RIGHTINNER_OFFSET)
	 */
	@Test
	public void changeLaneRightTestOne() {
		try {
			manager.changeLaneLeft(null);
		} catch(NullPointerException e) {
			return;
		}
		assertEquals(0, 1);
	}
	
	@Test
	public void changeLaneRightTestTwo() {
		VehicleSpoofer v1 = new VehicleSpoofer();
		VehicleWrapperSpoofer vws1 = new VehicleWrapperSpoofer(v1, 0);
		vws1.setLaneOffset(GameManager.RIGHTMOST_OFFSET);
		SocketMessageWithVehicle sm1 = new SocketMessageWithVehicle(null, vws1);
		manager.changeLaneRight(sm1);
		assertEquals(vws1.getLaneOffset(), GameManager.RIGHTMOST_OFFSET, .01f);
	}
	
	@Test
	public void changeLaneRightTestThree() {
		VehicleSpoofer v1 = new VehicleSpoofer();
		VehicleWrapperSpoofer vws1 = new VehicleWrapperSpoofer(v1, 0);
		vws1.setLaneOffset(GameManager.LEFTINNER_OFFSET);
		SocketMessageWithVehicle sm1 = new SocketMessageWithVehicle(null, vws1);
		manager.changeLaneRight(sm1);
		assertEquals(vws1.getLaneOffset(), GameManager.RIGHTINNER_OFFSET, .01f);
	}
	
	/**
	 * Utility classes and class spoofers
	 */
	private class AnkiConnectorSpoofer extends AnkiConnector {
		public AnkiConnectorSpoofer(String s, int p) throws IOException {
			super(s, p);
		}
		
		public AnkiConnectorSpoofer() {
			
		}
		
		public List<Vehicle> findVehicles() {
			List<Vehicle> carList = new ArrayList<Vehicle>();
			VehicleSpoofer vs = new VehicleSpoofer();
			carList.add(vs);
			return carList;
		}
	}
	
	private class AnkiConnectorSpooferTwo extends AnkiConnector {
		public List<Vehicle> findVehicles() {
			List<Vehicle> carList = new ArrayList<Vehicle>();
			VehicleSpoofer vs = new VehicleSpoofer();
			VehicleSpoofer vs2 = new VehicleSpoofer();
			carList.add(vs);
			carList.add(vs2);
			return carList;
		}
	}
	
	private class AnkiConnectorSpooferThree extends AnkiConnector {
		public List<Vehicle> findVehicles() {
			List<Vehicle> carList = new ArrayList<Vehicle>();
			VehicleSpoofer vs = new VehicleSpoofer();
			VehicleSpoofer vs2 = new VehicleSpoofer();
			VehicleSpoofer vs3 = new VehicleSpoofer();
			carList.add(vs);
			carList.add(vs2);
			carList.add(vs3);
			return carList;
		}
	}
	
	private class VehicleWrapperSpoofer extends VehicleWrapper {
		public VehicleWrapperSpoofer(Vehicle vehicle, int clientManagerId) {
			super(vehicle, clientManagerId);
		}
	}
	
	private class VehicleSpoofer extends Vehicle {
		public VehicleSpoofer(AnkiConnector anki, String address, String manufacturerData, String localName) {
			super();
		}
		
		public VehicleSpoofer() {
			AdvertisementDataSpoofer ad = new AdvertisementDataSpoofer("1111111111111111111111111111",
					"111111111111111111111111111111111111111111111111111111111111");
			this.setAdvertisement(ad);
		}
		
		@Override
		public void sendMessage(Message m) {
			return;
		}
		
		@Override
		public void sendMessage(Message m, boolean verbose) {
			return;
		}
		
		@Override
		public void connect() {
			return;
		}
		
		@Override
		public <T extends Message> void addMessageListener(Class<T> klass, MessageListener<T> listener) {
			return;
		}
	}
	
	private class AdvertisementDataSpoofer extends AdvertisementData {

		public AdvertisementDataSpoofer(String manufacturerData, String localName) {
			super(manufacturerData, localName);
		}
		
	}
	
	private class RoadmapScannerSpoofer extends RoadmapScanner {

		public RoadmapScannerSpoofer(Vehicle vehicle) {
			super(vehicle);
		}
		
		@Override
		public void startScanning() {
			
		}
		
		@Override
		public boolean isComplete() {
			return true;
		}
		
		@Override
		public Roadmap getRoadmap() {
			Roadmap map = new Roadmap();
			map.add(33, 0, true);
			return map;
		}
	}
	
	private class RoadmapScannerSpooferTwo extends RoadmapScanner {

		public RoadmapScannerSpooferTwo(Vehicle vehicle) {
			super(vehicle);
		}
		
		@Override
		public void startScanning() {
			
		}
		
		@Override
		public boolean isComplete() {
			return true;
		}
		
		@Override
		public Roadmap getRoadmap() {
			Roadmap map = new Roadmap();
			map.add(17, 0, true);
			return map;
		}
	}
	
	private class RoadmapScannerSpooferThree extends RoadmapScanner {

		public RoadmapScannerSpooferThree(Vehicle vehicle) {
			super(vehicle);
		}
		
		@Override
		public void startScanning() {
			
		}
		
		@Override
		public boolean isComplete() {
			return false;
		}
		
		@Override
		public Roadmap getRoadmap() {
			Roadmap map = new Roadmap();
			map.add(33, 0, true);
			return map;
		}
	}
	
	private class ClientManagerSpoofer extends ClientManager {
		
		public int expectedVal = 0;
		
		public ClientManagerSpoofer(int myId) {
			this.myId = myId;
		}
		
		@Override
		public void sendCmd(int cmd, String extra) {
			expectedVal = cmd + expectedVal;
		}
		
		public void setLeft(AtomicBoolean l) {
			this.leftGame = l;
		}
	}
}

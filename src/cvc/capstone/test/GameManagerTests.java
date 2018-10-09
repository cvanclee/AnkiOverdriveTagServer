package cvc.capstone.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cvc.capstone.GameManager;
import cvc.capstone.SocketMessage;
import cvc.capstone.VehicleWrapper;
import de.adesso.anki.AnkiConnector;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.Message;

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
			fakeClientSocketOne.close();
		} catch (Exception e) {}
		try {
			fakeClientSocketTwo.close();
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
		manager.waitForPlayers();
	}
	
	/**
	 * Equivalence classes:
	 * The cars finish scanning (return true)
	 * The cars are detected off track (ServerException thrown in separate method)
	 * The cars never finish scanning the track (infinite loop) 
	 */
	@Test
	public void scanTrackTestOne() throws IOException {

	}
	
	/**
	 * Equivalence classes:
	 * The connection to the nodejs server is made (return true)
	 * The connection to the nodejs server is not made (return false)
	 */
	@Test
	public void launchAnkiTestOne() {
		
	}
	
	/**
	 * Equivalence classes:
	 * Less than 2 vehicles are found (return false)
	 * More than 2 vehicles are found (return false)
	 * 2 vehicles are found (return true)
	 */
	@Test
	public void findVehiclesTestOne() {
		
	}
	
	private class AnkiConnectorSpoofer extends AnkiConnector {
		public AnkiConnectorSpoofer() throws IOException {
			super();
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
		
		@Override
		public void sendMessage(Message m) {
			
		}
	}
}

package cvc.capstone.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cvc.capstone.ClientManager;
import cvc.capstone.GameManager;
import cvc.capstone.ServerException;
import cvc.capstone.SocketMessage;
import cvc.capstone.VehicleWrapper;
import de.adesso.anki.AnkiConnector;
import de.adesso.anki.Vehicle;

public class ClientManagerTests {
	
	private final String host = "localhost";
	private final int port = 4998;
	private Socket fakeClientSocket = null;
	private ServerSocket fakeServerSocket = null;
	private Socket s;
	private GameManager fakeManager;
	private ObjectOutputStream out;
	Thread t1 = new Thread(new Runnable() {public void run() {}}); //To stop compile error/warning
	
	@Before
	public void setup() throws Exception {
		fakeManager = new GameManager();
		fakeClientSocket = new Socket();
		fakeServerSocket = new ServerSocket(port);
		t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10);
					fakeClientSocket.setSoTimeout(2000);
					fakeClientSocket.connect(new InetSocketAddress(host, port));
					OutputStream os = fakeClientSocket.getOutputStream();
					out = new ObjectOutputStream(os);
					out.writeObject(new SocketMessage("", -1, ""));
					out.flush();
					os.flush();
					while(!t1.isInterrupted()) {
						;
					}
				} catch (Exception e) {
					assertEquals(0, 1); //Can't throw, so I'll do this instead
				}
			}
		});
		t1.start();
		fakeServerSocket.setSoTimeout(2000);
		s = fakeServerSocket.accept();
	}
	
	@After
	public void cleanup() throws Exception {
		try {
			fakeClientSocket.close();
		} catch (Exception e) {}
		try {
			fakeServerSocket.close();
		} catch (Exception e) {}
		try {
			t1.interrupt();
		} catch (Exception e) {}
		try {
			out.flush();
			out.close();
		} catch(Exception e) {}
		out = null;
		Thread.sleep(100);
	}

	/**
	 * Equivalence classes:
	 * Socket with no connection is passed (ServerException thrown)
	 * Socket with a connection is passed (return)
	 * Socket null is passed (ServerException thrown)
	 * null manager is passed (Server
	 * 
	 * BUGS: 
	 * socket timeout needed to be set at start, not end of constructor
	 */
	@Test
	public void constructorTestOne() throws Exception {
		try {
			int fakeClientManagerId = 0;
			fakeClientSocket = new Socket();
			ClientManager cm1 = new ClientManager(fakeClientSocket, fakeManager, fakeClientManagerId, null);
		} catch (ServerException e) {
			return;
		}
		throw new Exception("fail");
	}
	
	@Test
	public void constructorTestTwo() throws Exception {
		try {
			int fakeClientManagerId = 0;
			ClientManager cm1 = new ClientManager(s, fakeManager, fakeClientManagerId, null);
		} catch (Exception e) {
			throw e;
		} finally {
			s.close();
			t1.interrupt();
		}
	}
	
	@Test
	public void constructorTestThree() {
		try {
			ClientManager cm1 = new ClientManager(null, null, 0, null);
		} catch (ServerException e) {
			return;
		}
	}
	
	/**
	 * Equivalence classes:
	 * The socket is connected (return)
	 * The socket is connected and the string passed is null (return)
	 * The socket is not connected (ServerException thrown)
	 */
	@Test
	public void sendCmdTestOne() throws Exception {
		ClientManager cm1 = new ClientManager(s, fakeManager, 0, null);
		cm1.sendCmd(1010, "");
	}
	
	@Test
	public void sendCmdTestTwo() throws Exception {
		ClientManager cm1 = new ClientManager(s, fakeManager, 0, null);
		cm1.sendCmd(1010, null);
	}
	
	@Test
	public void sendCmdTestThree() throws Exception {
		ClientManager cm1 = new ClientManager(s, fakeManager, 0, null);
		s.close();
		s = new Socket();
		try {
			cm1.sendCmd(1010, "");
		} catch (ServerException e) {
			return;
		}
		assertEquals(0, 1);
	}
	
	/**
	 * Equivalence classes:
	 * The thread receives an interrupt (return)
	 * The game is made ready, then not ready (return)
	 * The socket connection is closed (ServerException thrown)
	 * The game is never made ready (infinite loop)
	 * The game is made ready, but never made not ready (infinite loop)
	 * @throws Exception 
	 */
	@Test
	public void gameLoopCommunicationTestOne() throws Exception {
		ClientManagerSpooferTwo cm1 = new ClientManagerSpooferTwo(s, fakeManager, 0, null);
		Thread.sleep(3000);
		cm1.interrupt();
		Thread.sleep(100);
		assertEquals(cm1.isAlive(), false);
	}
	
	@Test
	public void gameLoopCommunicationTestTwo() throws Exception {
		ClientManagerSpooferTwo cm1 = new ClientManagerSpooferTwo(s, fakeManager, 0, null);
		cm1.setGameReady(true);
		Thread.sleep(3000);
		cm1.setGameReady(false);
		Thread.sleep(100);
		assertEquals(cm1.isAlive(), false);
	}
	
	@Test
	public void gameLoopCommunicationTestThree() throws Exception {
		ClientManagerSpooferTwo cm1 = new ClientManagerSpooferTwo(s, fakeManager, 0, null);
		cm1.setGameReady(true);
		Thread.sleep(3000);
		s.close();
		Thread.sleep(100);
		assertEquals(cm1.isAlive(), false);
	}
	
	/**
	 * Equivalence classes:
	 * The thread receives an interrupt (return)
	 * The socket connection is terminated (return)
	 * A client connects and disconnects (return)
	 * A client sends a connect and ready up (ready is set to true)
	 * A client never connects (infinite loop)
	 * A client connects, but never sends a ready up (infinite loop)
	 */
	@Test
	public void waitForNewClientTestOne() throws Exception {
		ClientManagerSpoofer cm1 = new ClientManagerSpoofer(s, fakeManager, 0, null);
		cm1.start();
		cm1.interrupt();
		Thread.sleep(100);
		assertEquals(cm1.isAlive(), false);
	}
	
	@Test
	public void waitForNewClientTestTwo() throws Exception {
		ClientManagerSpoofer cm1 = new ClientManagerSpoofer(s, fakeManager, 0, null);
		cm1.start();
		s.close();
		s = new Socket();
		Thread.sleep(100);
		assertEquals(cm1.isAlive(), false);
	}
	
	@Test
	public void waitForNewClientTestThree() throws Exception {
		ClientManagerSpoofer cm1 = new ClientManagerSpoofer(s, fakeManager, 0, null);
		cm1.start();
		SocketMessage msg = new SocketMessage("", 1000, "");
		out.writeObject(msg);
		msg = new SocketMessage("a", 1000, "");
		out.writeObject(msg);
		msg = new SocketMessage("a", 1002, "");
		out.writeObject(msg);
		Thread.sleep(100);
		assertEquals(cm1.isAlive(), false);
		t1.interrupt();
	}
	
	@Test
	public void waitForNewClientTestFour() throws Exception {
		ClientManagerSpoofer cm1 = new ClientManagerSpoofer(s, fakeManager, 0, null);
		VehicleSpoofer v = new VehicleSpoofer();
		VehicleWrapperSpoofer vw = new VehicleWrapperSpoofer(v, 0);
		cm1.setVehicle(vw);
		cm1.start();
		SocketMessage msg = new SocketMessage("abc", 1000, "");
		out.writeObject(msg);
		SocketMessage msgg = new SocketMessage("abc", 1001, "");
		Thread.sleep(100);
		out.writeObject(msgg);
		Thread.sleep(100);
		assertEquals(cm1.isReady(), true);
		t1.interrupt();
	}
	
	private class ClientManagerSpoofer extends ClientManager {

		public ClientManagerSpoofer(Socket clientSocket, GameManager myManager, int myId, VehicleWrapper myVehicle)
				throws ServerException {
			super(clientSocket, myManager, myId, myVehicle);
		} 
		
		@Override
		public void run() {
			try {
				waitForNewClient();
				if (isInterrupted()) {
					return;
				}
			} catch (ServerException e) {
				e.printStackTrace();
			}
		}	
	}
	
	private class ClientManagerSpooferTwo extends ClientManager {

		public ClientManagerSpooferTwo(Socket clientSocket, GameManager myManager, int myId, VehicleWrapper myVehicle)
				throws ServerException {
			super(clientSocket, myManager, myId, myVehicle);
		} 
		
		@Override
		public void run() {
			gameLoopCommunication();
		}	
	}
	
	private class VehicleWrapperSpoofer extends VehicleWrapper {
		
		public VehicleWrapperSpoofer(Vehicle vehicle, int clientManagerId) {
			super(vehicle, clientManagerId);
		}
	}
	
	private class VehicleSpoofer extends Vehicle {
		@Override
		public String toString() { 
			return "";
		}
	}
}

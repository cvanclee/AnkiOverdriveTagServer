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
	Thread t1 = new Thread(new Runnable() {public void run() {}}); //To stop compile error/warning
	
	@Before
	public void setup() throws Exception {
		fakeClientSocket = new Socket();
		fakeServerSocket = new ServerSocket(port);
	}
	
	@After
	public void cleanup() throws Exception {
		try {
			fakeClientSocket.close();
		} catch (Exception e) {
			;
		}
		try {
			fakeServerSocket.close();
		} catch (Exception e) {
			;
		}
	}

	/**
	 * Equivalence classes:
	 * Socket with no connection is passed (ServerException thrown)
	 * Socket with a connection is passed (return)
	 * 
	 * Bug found and fixed: socket timeout needs to be set at start, not end of constructor
	 */
	@Test
	public void constructorTestOne() throws Exception {
		try {
			GameManager fakeManager = new GameManager();
			int fakeClientManagerId = 0;

			ClientManager cm1 = new ClientManager(fakeClientSocket, fakeManager, fakeClientManagerId, null);
		} catch (ServerException e) {
			return;
		}
		throw new Exception("fail");
	}
	
	@Test
	public void constructorTestTwo() throws Exception {
		t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10);
					fakeClientSocket.setSoTimeout(2000);
					fakeClientSocket.connect(new InetSocketAddress(host, port));
					OutputStream os = fakeClientSocket.getOutputStream();
					ObjectOutputStream out = new ObjectOutputStream(os);
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
		Socket s = fakeServerSocket.accept();
		try {
			if (!s.isConnected()) {
				throw new Exception("Socket connection failed");
			}
			GameManager fakeManager = new GameManager();
			int fakeClientManagerId = 0;

			ClientManager cm1 = new ClientManager(s, fakeManager, fakeClientManagerId, null);
		} catch (Exception e) {
			throw e;
		} finally {
			s.close();
			t1.interrupt();
		}
	}
	
	/**
	 * Equivalence classes:
	 * The socket is connected (return, the client receives the command)
	 * The socket is not connected (ServerException thrown)
	 */
	@Test
	public void sendCmdTestOne() {
		
	}
	
	/**
	 * Equivalence classes:
	 * The thread receives an interrupt (return)
	 * The game is made ready, then not ready (return)
	 * The game is never made ready (infinite loop)
	 * The game is made ready, but never made not ready (infinite loop)
	 * TODO not finished with this method
	 */
	@Test
	public void gameLoopCommunicationTestOne() {
		
	}
	
	/**
	 * Equivalence classes:
	 * The thread receives an interrupt (return)
	 * The socket connection is terminated (return)
	 * A client sends a connect and ready up (return, manager's ready count is incremented, client receives command 1009)
	 * A client never connects (infinite loop)
	 * A client connects, but never sends a ready up (infinite loop)
	 */
	@Test
	public void waitForNewClientTestOne() {
		
	}
}

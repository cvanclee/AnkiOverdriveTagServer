package cvc.capstone;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientManager extends Thread {
	private String UUID;
	private final Socket clientSocket;
	private final GameManager myManager;
	private String myUUID;
	private boolean ready;
	private int myId;

	public ClientManager(Socket clientSocket, GameManager myManager, int myId) {
		this.clientSocket = clientSocket;
		this.myManager = myManager;
		this.myId = myId;
		myUUID = "";
		try {
			clientSocket.setSoTimeout(1000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		waitForNewClient();
		if (isInterrupted()) {
			return;
		}
		gameLoopCommunication();
	}
	
	private void gameLoopCommunication() {
		
	}
	
	private void waitForNewClient() {
		InputStream is = null;
		ObjectInputStream in = null;
		try {
			is = clientSocket.getInputStream();
			in = new ObjectInputStream(is);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (!isInterrupted() && !ready) {
			try {
				SocketMessage msg = (SocketMessage) in.readObject();
				if (msg.cmd == 1000) {
					myUUID = msg.UUID;
				} else if (msg.cmd == 1001 && msg.UUID.equals(myUUID)) {
					ready = true;
					myManager.getReadyCount().incrementAndGet();
				} else if (msg.cmd == 1002 && msg.UUID.equals(myUUID)) {
					myUUID = "";
					ready = false;
					myManager.getReadyCount().decrementAndGet();
					myManager.getConnectedClients().remove(myId);
					clientSocket.close();
					interrupt();
				}
				Thread.sleep(10);
			} catch (InterruptedException e) {
				; // Should be fine
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public String getUUID() {
		return UUID;
	}

	public Socket getClientSocket() {
		return clientSocket;
	}
	
	public int getMyId() {
		return myId;
	}
}

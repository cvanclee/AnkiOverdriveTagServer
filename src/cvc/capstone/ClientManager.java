package cvc.capstone;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ClientManager extends Thread {
	private String UUID;
	private final Socket clientSocket;
	private final GameManager myManager;
	private String myUUID;
	private boolean ready;
	private int myId;
	private OutputStream os;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public ClientManager(Socket clientSocket, GameManager myManager, int myId) {
		this.clientSocket = clientSocket;
		this.myManager = myManager;
		this.myId = myId;
		myUUID = "";
		try {
			os = clientSocket.getOutputStream();
			out = new ObjectOutputStream(os);
			in = new ObjectInputStream(clientSocket.getInputStream());
			clientSocket.setSoTimeout(1000);
		} catch (SocketException e) {
			e.printStackTrace();
		}catch (IOException ex) {
			ex.printStackTrace();
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
		while (!isInterrupted() && !ready) {
			try {
				in.reset();
				SocketMessage msg = (SocketMessage) in.readObject();
				if (msg.cmd == 1000) {
					myUUID = msg.UUID;
					msg = new SocketMessage("", 1009, "");
					out.reset();
					out.writeObject(msg);
					out.flush();
					os.flush();
				} else if (msg.cmd == 1001 && msg.UUID.equals(myUUID) && !ready) {
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
			} catch (SocketTimeoutException e) { // This is expected, the client may not send ready right away
				try {
					Thread.sleep(10);
				} catch (InterruptedException e1) {
					;
				}
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

package cvc.capstone;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientManager extends Thread {
	private Socket clientSocket;
	private final GameManager myManager;
	private String myUUID;
	private volatile AtomicBoolean ready;
	protected int myId;
	private OutputStream os;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private VehicleWrapper myVehicle;
	private volatile AtomicBoolean isGameReady;
	protected volatile AtomicBoolean leftGame;

	public ClientManager(Socket clientSocket, GameManager myManager, int myId, VehicleWrapper myVehicle)
			throws ServerException {
		this.clientSocket = clientSocket;
		this.myManager = myManager;
		this.myId = myId;
		this.myVehicle = myVehicle;
		isGameReady = new AtomicBoolean(false); //game manager is in main game loop
		ready = new AtomicBoolean(false); //is client ready
		leftGame = new AtomicBoolean(false);
		myUUID = "";
		try {
			clientSocket.setSoTimeout(2000);
			os = clientSocket.getOutputStream();
			out = new ObjectOutputStream(os);
			in = new ObjectInputStream(clientSocket.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServerException(e);
		}
	}

	@Override
	public void run() {
		try {
			waitForNewClient();
			if (isInterrupted()) {
				return;
			}
			gameLoopCommunication();
		} catch (ServerException e) {
			e.printStackTrace();
		}
		try {
			out.flush();
			os.flush();
			clientSocket.close();
		} catch (IOException e) {e.printStackTrace();}
	}

	/**
	 * Send a command to the connected client
	 * 
	 * @param cmd the command to send
	 * @param extra stores extra command parameters
	 * @throws ServerException
	 */
	public void sendCmd(int cmd, String extra) throws ServerException {
		try {
			SocketMessage msg = new SocketMessage(myUUID, cmd, extra);
			out.writeObject(msg);
			out.flush();
			os.flush();
		} catch (IOException e) {
			try {
				out.flush();
				os.flush();
				clientSocket.close();
			} catch (IOException io) {
				clientSocket = null;
			}
			leftGame.set(true);
			throw new ServerException(e);
		}
	}

	/**
	 * Loop until game manager notifies it is ready to start accepting movement
	 * commands. Then loop until game manager notifies the game is over, accepting
	 * client commands 
	 */
	protected void gameLoopCommunication() {
		try {
			while (!isInterrupted() && !isGameReady.get()) {
				try {
					SocketMessage msg = (SocketMessage) in.readObject();
					if (msg.cmd == 1002) {
						clientLeftLobby();
					}
				} catch (SocketTimeoutException e) {
					continue;
				} catch (Exception e) {
					try {
						out.flush();
						os.flush();
						clientSocket.close();
					} catch (IOException io) {
						clientSocket = null;
					}
					leftGame.set(true);
					return;
				}
			}
			if (isInterrupted()) {
				return;
			}
			while (!isInterrupted() && isGameReady.get()) { // main game loop, after scanning
				SocketMessage msg = null;
				try {
					msg = (SocketMessage) in.readObject();
				} catch (SocketTimeoutException e) {
					continue;
				} catch (Exception e) {
					try {
						out.flush();
						os.flush();
						clientSocket.close();
					} catch (IOException io) {
						clientSocket = null;
					}
					leftGame.set(true);
					return;
				}
				try {
					SocketMessageWithVehicle msgv = new SocketMessageWithVehicle(msg, myVehicle);
					myManager.getServerClientQueue().add(msgv);
				} catch (IllegalStateException e) { // BAD. Notify server queue is over capacity
					myManager.getClientOverflowStatus().set(true);
					return;
				}
			}
		} catch (Exception e) {
			try {
				out.flush();
				os.flush();
				clientSocket.close();
			} catch (IOException io) {
				clientSocket = null;
			}
			leftGame.set(true);
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Wait for a client to connect, and confirm it has been accepted.
	 * One connected, wait for the client to send a ready signal.
	 * Waiting ends when the client sends a ready signal, sends a 
	 * disconnect signal, the connection is closed, or the thread receives
	 * an interrupt.
	 * 
	 * @throws ServerException
	 */
	protected void waitForNewClient() throws ServerException {
		while (!isInterrupted() && !ready.get()) {
			try {
				SocketMessage msg = (SocketMessage) in.readObject();
				if (msg.cmd == 1000) {
					System.out.println(
							"New client attempting to connect " + clientSocket.getRemoteSocketAddress().toString());
					myUUID = msg.UUID;
					sendCmd(1009, myVehicle.getVehicle().toString());
				} else if (msg.cmd == 1001 && msg.UUID.equals(myUUID) && !ready.get()) {
					ready.set(true);
					myManager.getReadyCount().incrementAndGet();
				} else if (msg.cmd == 1002 && msg.UUID.equals(myUUID)) {
					clientLeftLobby();
					return;
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
			} catch (Exception e) {
				try {
					out.flush();
					os.flush();
					clientSocket.close();
				} catch (IOException ie) {
					clientSocket = null;
				}
				leftGame.set(true);
				e.printStackTrace();
				return;
			}
		}
	}
	
	private void clientLeftLobby() {
		System.out.println("A client disconnected normally");
		myUUID = "";
		if (ready.get()) {
			myManager.getReadyCount().decrementAndGet();
		}
		ready.set(false);;
		myManager.addVehicle(myVehicle);
		myManager.getConnectedClients().remove(myId);
		try {
			out.flush();
			os.flush();
			clientSocket.close();
		} catch (IOException e) {
			clientSocket = null;;
		}
		leftGame.set(true);
	}
	
	public String getUUID() {
		return myUUID;
	}

	public Socket getClientSocket() {
		return clientSocket;
	}

	public int getMyId() {
		return myId;
	}

	public void setVehicle(VehicleWrapper v) {
		this.myVehicle = v;
	}

	public VehicleWrapper getVehicle() {
		return myVehicle;
	}

	public void setGameReady(boolean gr) {
		isGameReady.set(gr);
	}
	
	public boolean isReady() {
		return ready.get();
	}
	
	public boolean isClosed() {
		return clientSocket.isClosed();
	}
	
	public AtomicBoolean getLeftGame() {
		return leftGame;
	}
	
	/**
	 * TEST ONLY
	 */
	public ClientManager() {
		myManager = null;
	}
}

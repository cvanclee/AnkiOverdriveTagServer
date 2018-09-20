package cvc.capstone;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.PingRequestMessage;
import de.adesso.anki.messages.SetSpeedMessage;
import de.adesso.anki.AnkiConnector;

public class MainClass {

	public static final int NODE_JS_SERVER_PORT = 5000;
	public static final String NODE_JS_SERVER_NAME = "localhost";
	public static final int MY_SERVER_PORT = 4999;
	

	public static void main(String[] args) {
		GameManager gameManager = new GameManager();
		if (!gameManager.setUp()) {
			return;
		}
		gameManager.play();
	}
}
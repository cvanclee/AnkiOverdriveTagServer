package cvc.capstone;

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
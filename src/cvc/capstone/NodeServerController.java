package cvc.capstone;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NodeServerController extends Thread {

	Process p = null;
	BufferedReader input;
	InputStream is;

	@Override
	public void run() {
		try {
			p = Runtime.getRuntime().exec("sudo node " + MainClass.NODE_JS_SERVER_PATH);
			String line;
			is = p.getInputStream();
			input = new BufferedReader(new InputStreamReader(is));
			try {
				while (!isInterrupted() && (line = input.readLine()) != null) {
					System.out.println("[NODE]:" + line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				input.close();
				is.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		while (!isInterrupted()) {
			interrupt();
		}
		p.destroy();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (p.isAlive()) {
			p.destroyForcibly();
		}
		System.out.println("Stopped node server.");
	}
}

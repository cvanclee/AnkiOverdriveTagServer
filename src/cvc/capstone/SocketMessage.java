package cvc.capstone;

public class SocketMessage {
	public String UUID;
	public int cmd;
	public String extra;
	
	public SocketMessage(String UUID, int cmd, String extra) {
		this.UUID = UUID;
		this.cmd = cmd;
		this.extra = extra;
	}
}

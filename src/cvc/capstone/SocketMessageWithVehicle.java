package cvc.capstone;

public class SocketMessageWithVehicle {
	public SocketMessage msg;
	public VehicleWrapper myVehicle;
	
	public SocketMessageWithVehicle(SocketMessage msg, VehicleWrapper myVehicle) {
		this.msg = msg;
		this.myVehicle = myVehicle;
	}
}

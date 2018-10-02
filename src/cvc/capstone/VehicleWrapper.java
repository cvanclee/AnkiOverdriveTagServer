package cvc.capstone;

import java.util.concurrent.atomic.AtomicInteger;

import de.adesso.anki.Vehicle;

public class VehicleWrapper {
	private Vehicle vehicle;
	private int clientManagerId; //The client manager linked with this vehicle
	private volatile AtomicInteger speed;
	private volatile AtomicInteger accel;
	private volatile float laneOffset; //no AtomicFloat. synchronize all access instead

	public VehicleWrapper(Vehicle vehicle, int clientManagerId) {
		this.vehicle = vehicle;
		this.clientManagerId = clientManagerId;
		speed = new AtomicInteger(GameManager.MIN_SPEED);
		accel = new AtomicInteger(GameManager.MIN_ACCEL);
	}
	
	public Vehicle getVehicle() {
		return vehicle;
	}
	
	public int getClientManagerId() {
		return clientManagerId;
	}
	
	public void setClientManagerId(int cmi) {
		clientManagerId = cmi;
	}
	
	/**
	 * 
	 * @param speedChange negative to reduce speed, positive to increase speed
	 * @param accelChange negative to reduce acceleration, positive to increase acceleration
	 */
	public synchronized void changeSpeedAndAccel(int speedChange, int accelChange) {
		speed.set(speed.get() + speedChange);
		accel.set(accel.get() + accelChange);
	}
	
	public synchronized void setSpeedAndAccel(int s, int a) {
		speed.set(s);
		accel.set(a);
	}
	
	public AtomicInteger getSpeed() {
		return speed;
	}
	
	public AtomicInteger getAcceleration() {
		return accel;
	}
	
	public synchronized float getLaneOffset() {
		return laneOffset;
	}
	
	public synchronized void setLaneOffset(float offset) {
		laneOffset = offset;
	}

	private enum VehicleNames {
		KOURAI(0x01, "Kourai"), BOSON(0x02, "Boson"), RHO(0x03, "Rho"), KATAL(0x04, "Katal"), HADION(0x05,
				"Hadion"), SPEKTRIX(0x06, "Spektrix"), CORAX(0x07, "Corax"), GROUNDSHOCK(0x08, "Groundshock"), SKULL(
						0x09, "Skull"), THERMO(0x0a, "Thermo"), NUKE(0x0b, "Nuke"), GUARDIAN(0x0d, "Guardian"), BIGBANG(
								0x0e, "BigBang"), FREEWHEEL(0x0f, "Freewheel"), X52ICE(0x11, "X52Ice");

		int id;
		String name;

		private VehicleNames(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}

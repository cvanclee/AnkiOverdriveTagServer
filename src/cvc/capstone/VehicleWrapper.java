package cvc.capstone;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.adesso.anki.Vehicle;

public class VehicleWrapper {
	private Vehicle vehicle;
	private int clientManagerId; //The client manager linked with this vehicle
	private volatile AtomicInteger speed;
	private volatile AtomicInteger accel;
	private volatile float laneOffset; // no AtomicFloat. synchronize all access instead
	private volatile float tagLaneOffset; //Offset that takes bearing into account (lane swapping won't work without this)
	private volatile int score; //Game score
	private float[] offsets = { GameManager.LEFTMOST_OFFSET, GameManager.LEFTINNER_OFFSET,
			GameManager.RIGHTINNER_OFFSET, GameManager.RIGHTMOST_OFFSET };
	private volatile AtomicBoolean bearing; //true for starting direction, false for the other
	private volatile AtomicInteger pieceIndex; //the index on the map this car is thought to be on
	private volatile AtomicBoolean offTrack; //set to true if vehicle is delocalized

	public VehicleWrapper(Vehicle vehicle, int clientManagerId) {
		this.vehicle = vehicle;
		this.clientManagerId = clientManagerId;
		speed = new AtomicInteger(GameManager.MIN_SPEED);
		accel = new AtomicInteger(GameManager.MIN_ACCEL);
		score = 0;
		bearing = new AtomicBoolean(true);
		pieceIndex = new AtomicInteger(-1);
		offTrack = new AtomicBoolean(false);
	}
	
	public AtomicBoolean getOffTrack() {
		return offTrack;
	}
	
	public synchronized void incScore(int inc) {
		score = score + inc;
	}
	
	public int getScore() { 
		return score;
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
	
	public AtomicBoolean getBearing() {
		return bearing;
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
	
	public synchronized float getTagLaneOffset() {
		return tagLaneOffset;
	}
	
	public AtomicInteger getPieceIndex() {
		return pieceIndex;
	}

	/**
	 * Sets the lane offset, rounding to the nearest valid lane offset from the
	 * given offset
	 * 
	 * @param offset offset to set to
	 */
	public synchronized void setLaneOffset(float offset) {
		float distance = Math.abs(offsets[0] - offset);
		int idx = 0;
		for (int c = 1; c < offsets.length; c++) {
			float cdistance = Math.abs(offsets[c] - offset);
			if (cdistance < distance) {
				idx = c;
				distance = cdistance;
			}
		}
		laneOffset = offsets[idx];
		if (bearing.get()) {
			tagLaneOffset = offsets[idx];
		} else {
			tagLaneOffset = -(offsets[idx]);
		}
	}
}

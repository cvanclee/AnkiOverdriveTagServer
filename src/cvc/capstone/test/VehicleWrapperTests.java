package cvc.capstone.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cvc.capstone.GameManager;
import cvc.capstone.VehicleWrapper;

public class VehicleWrapperTests {
	
	private float[] offsets = { GameManager.LEFTMOST_OFFSET, GameManager.LEFTINNER_OFFSET,
			GameManager.RIGHTINNER_OFFSET, GameManager.RIGHTMOST_OFFSET };
	
	/**
	 * Equivalence classes:
	 * The offset is closest to far right lane (return 68.0)
	 * The offset is closest to inner right lane (return 23.0)
	 * The offset is closest to inner left lane (return -23.0)
	 * The offset is closest to far left left (return -68.0)
	 */
	@Test
	public void setLaneOffsetTestOne() {
		VehicleWrapper vw = new VehicleWrapper(null, -1);
		vw.setLaneOffset(-63.0f);
		assertEquals(vw.getLaneOffset(), offsets[0], .000001);
	}
	
	@Test
	public void setLaneOffsetTestTwo() {
		VehicleWrapper vw = new VehicleWrapper(null, -1);
		vw.setLaneOffset(-31.12f);
		assertEquals(vw.getLaneOffset(), offsets[1], .000001);
	}
	
	@Test
	public void setLaneOffsetTestThree() {
		VehicleWrapper vw = new VehicleWrapper(null, -1);
		vw.setLaneOffset(1.05f);
		assertEquals(vw.getLaneOffset(), offsets[2], .000001);
	}
	
	@Test
	public void setLaneOffsetTestFour() {
		VehicleWrapper vw = new VehicleWrapper(null, -1);
		vw.setLaneOffset(61.023121f);
		assertEquals(vw.getLaneOffset(), offsets[3], .000001);
	}
}

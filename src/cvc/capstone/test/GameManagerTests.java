package cvc.capstone.test;

import org.junit.Test;

public class GameManagerTests {
	
	/**
	 * Equivalence classes:
	 * Two clients connect and ready up (return)
	 * No clients connect (infinite loop)
	 */
	@Test
	public void waitForPlayersTestOne(){
		
	}
	
	/**
	 * Equivalence classes:
	 * The cars finish scanning (return)
	 * The cars are detected off track (ServerException thrown in separate method)
	 * The cars never finish scanning the track (infinite loop)
	 */
	@Test
	public void scanTrackTestOne() {
		
	}
	
	/**
	 * Equivalence classes:
	 * The connection to the nodejs server is made (return true)
	 * The connection to the nodejs server is not made (return false)
	 */
	@Test
	public void launchAnkiTestOne() {
		
	}
	
	/**
	 * Equivalence classes:
	 * Less than 2 vehicles are found (return false)
	 * More than 2 vehicles are found (return false)
	 * 2 vehicles are found (return true)
	 */
	@Test
	public void findVehiclesTestOne() {
		
	}
}

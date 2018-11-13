package cvc.capstone.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Test;

import cvc.capstone.MainClass;

public class MainClassTests {

	/**
	 * Equivalence classes:
	 * The path given is nonexistent (return false)
	 * The path is not a valid properties file (return false)
	 * The path given is null (return false)
	 * The path given is to a valid properties file (return true)
	 */
	@Test
	public void readPropertiesTestOne() {
		String propPath = "";
		boolean r = MainClass.readProperties(propPath);
		assertEquals(r, false);
	}
	
	@Test
	public void readPropertiesTestTwo() throws Exception {
		String propPath = new File(MainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent()
				+ "/test/res/serverProperties.properties";
		boolean r = MainClass.readProperties(propPath);
		assertEquals(r, false);
	}
	
	@Test
	public void readPropertiesTestThree() {
		String propPath = null;
		boolean r = MainClass.readProperties(propPath);
		assertEquals(r, false);
	}
	
	@Test
	public void readPropertiesTestFour() throws Exception {
		String propPath = new File(MainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent()
				+ "/dist/res/serverProperties.properties";
		boolean r = MainClass.readProperties(propPath);
		assertEquals(r, true);
		assertEquals(MainClass.COMMUNICATION_TIMEOUT, 2000);
	}
}

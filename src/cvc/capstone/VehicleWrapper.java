package cvc.capstone;

import de.adesso.anki.Vehicle;

public class VehicleWrapper {
	private Vehicle vehicle;

	public VehicleWrapper(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public String getName() {
		int idx = vehicle.getAdvertisement().getIdentifier();
		return VehicleNames.getNameById(idx);
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
		
		public static String getNameById(int id) {
			for (VehicleNames vns: values()) {
				if (vns.id == id) {
					return vns.name;
				}
			}
			return null;
		}
	}
}

package cvc.capstone;

public class ServerException extends Exception {

	public ServerException(String errorStatement) {
		super(errorStatement);
	}
	
	public ServerException(Exception exc) {
		super(exc);
	}
}

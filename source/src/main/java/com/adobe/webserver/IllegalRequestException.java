package com.adobe.webserver;


/**
 * Signals that a wrong/ill-formed request has come from client
 * @author khemka
 *
 */
public class IllegalRequestException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IllegalRequestException(String message){
		super(message);
	}
}


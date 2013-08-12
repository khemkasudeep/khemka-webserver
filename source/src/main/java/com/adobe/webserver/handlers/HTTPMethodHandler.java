package com.adobe.webserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.adobe.webserver.ClientHandler;
import com.adobe.webserver.IllegalRequestException;
import com.adobe.webserver.ServerParams;
import com.adobe.webserver.util.Helper;

/**
 * It Is the base class for all Handlers for specific HTTP Methods
 * 
 * It provides some very basic functionalities used by all handlers.
 * 
 * All Implementors of this class must ensure that they don't close the three
 * input/output Streams (which are protected parameters of this class) They can
 * only use this streams to read and write to them.
 * 
 * they must implement the handle method(which is a abstract method)
 * 
 * @author KHEMKA
 * 
 */
public abstract class HTTPMethodHandler {

	protected BufferedInputStream byteStreamBufferedIn;
	protected BufferedOutputStream byteStreamBufferedOut;
	protected BufferedWriter charStreamBufferedOut;

	private static Logger logger = Logger.getLogger(ClientHandler.class
			.getName());

	/**
	 * creates a HTTPMethodHandler initializing the Streams connected to client
	 * 
	 * @param byteStreamBufferedIn
	 * @param byteStreamBufferedOut
	 * @param charStreamBufferedOut
	 */
	public HTTPMethodHandler(BufferedInputStream byteStreamBufferedIn,
			BufferedOutputStream byteStreamBufferedOut,
			BufferedWriter charStreamBufferedOut) {
		this.byteStreamBufferedIn = byteStreamBufferedIn;
		this.byteStreamBufferedOut = byteStreamBufferedOut;
		this.charStreamBufferedOut = charStreamBufferedOut;
	}

	/**
	 * reads header from client and stores it into a HashMap.
	 * 
	 * @return the HashMap of key value pairs of header
	 * @throws IOException
	 *             when read/write error occurs
	 * @throws IllegalRequestException
	 *             if an invalid header is sent
	 */
	protected HashMap<String, String> getHeader() throws IOException,
			IllegalRequestException {
		String messageHeader = Helper
				.readHeaderFromClient(this.byteStreamBufferedIn);
		return Helper.convertHTTPHeaderToHashMap(messageHeader);
	}

	/**
	 * URI passed in request-line is generally encoded. for processing request
	 * this must be decoded This method decodes such URI.
	 * 
	 * @param requestURI
	 *            the uri to decode
	 * @return decoded uri. null if it can't decode.
	 * @throws IOException when some read write occurs
	 */
	protected String decodeURI(String requestURI) throws IOException {
		String decodedURI = null;
		try {
			decodedURI = URLDecoder
					.decode(requestURI, ServerParams.URLEncoding);
		} catch (UnsupportedEncodingException e2) {
			logger.error("request uri could not decoded . " + e2.getMessage());
			Helper.sendClientMessage("500", "Internal Server Error", null,
					"url could not be decoded .. thats all we know",
					charStreamBufferedOut, byteStreamBufferedOut);
		}

		return decodedURI;

	}
	
	/**
	 * this method reads header/ analyses header and responds accordingly.
	 * don't close the streams . 
	 * 
	 * @param requestURI the request-uri in request-line
	 * @throws IOException if some read/write error occurs
	 * @throws IllegalRequestException when request is not properly formed
	 */
	public abstract void handle(String requestURI) throws IOException,IllegalRequestException;

}

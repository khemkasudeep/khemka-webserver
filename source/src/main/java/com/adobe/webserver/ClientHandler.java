package com.adobe.webserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.adobe.webserver.handlers.GETHandler;
import com.adobe.webserver.handlers.POSTHandler;
import com.adobe.webserver.util.Helper;

/**
 * A ClientHandler takes the responsibility of dealing with HTTP clients. This
 * class Implements Runnable so that client can be handled in a separate thread.
 * So anybody who wants this class to handle a client, should create an instance
 * of this object, wrap it with a thread object and start the thread This class
 * is thread safe.
 * 
 * It reads HTTP request-line from client and evaluates the type of request
 * (GET/POST). It then forwards the request to appropriate Request Handler to
 * read the request Header/Body from client and then send an appropriate
 * response
 * 
 * It assumes that the Method Handler does not close InputStream/OutputStream
 * passed, and that it can throw only two types of checked exception -
 * IOException - in case of error in read/write of input stream and
 * IllegalRequestException - in case of a HTTP 400 Error.
 * 
 * In case of IllegalRequestException - It assumes that the Method handler has
 * not sent a response to client. so it sends client, a 400 Bad Request message
 * 
 * At the end, it closes the connection with client
 * 
 * @author KHEMKA
 * 
 */
public class ClientHandler implements Runnable {

	private Socket client;

	private BufferedInputStream byteStreamBufferedIn = null;
	private BufferedOutputStream byteStreamBufferedOut = null;
	private BufferedWriter charStreamBufferedOut = null;

	private static Logger logger = Logger.getLogger(ClientHandler.class
			.getName());

	/**
	 * Creates a ClientHandler to handle the input client socket
	 * 
	 * @param client
	 *            the socket connected to client
	 * @throws SocketException
	 *             raised while setting SO timeout on socket, if there is an
	 *             error in the underlying protocol, such as a TCP error.
	 */
	public ClientHandler(Socket client) throws SocketException {
		this.client = client;
		client.setSoTimeout(ServerParams.SocketSoTimeout);
	}

	/**
	 * Overriding the run() method of Runnable Interface.
	 */
	public void run() {

		OutputStream byteStreamOut = null;
		try {

			byteStreamBufferedIn = new BufferedInputStream(
					client.getInputStream());
			byteStreamOut = client.getOutputStream();
			// for writing headers
			byteStreamBufferedOut = new BufferedOutputStream(byteStreamOut);
			// for writing body
			charStreamBufferedOut = new BufferedWriter(new OutputStreamWriter(
					byteStreamOut, ServerParams.HTTPHeadersEncoding));

			String requestLine = Helper
					.readRequestLineFromClient(byteStreamBufferedIn);

			logger.trace("a request arrived with the request line - "
					+ requestLine);

			String[] requestLineSplitted = requestLine.split(" ");

			if (requestLineSplitted.length < 3)
				throw new IllegalRequestException("improper request line");

			URI requestURI = null;
			try {
				requestURI = new URI(requestLineSplitted[1]);
			} catch (URISyntaxException e) {
				throw new IllegalRequestException(
						"wrong request-uri in request-line - " + requestURI);
			}
			String requestPath = requestURI.getPath();
			logger.info("path requested is " + requestPath);

			if (requestLineSplitted[0].equals("GET")) {
				new GETHandler(byteStreamBufferedIn, byteStreamBufferedOut,
						charStreamBufferedOut).handle(requestPath);
			} else if (requestLineSplitted[0].equals("POST")) {
				new POSTHandler(byteStreamBufferedIn, byteStreamBufferedOut,
						charStreamBufferedOut).handle(requestPath);

			} else {

				Helper.sendClientMessage(
						"405",
						"Method Not Allowed",
						null,
						"the <i>HTTP</i> method you requested is not supported by our server .. regret any inconvenience!"
								+ "<hr>", charStreamBufferedOut,
						byteStreamBufferedOut);

			}

		} catch (IOException e) {
			logger.error("error in read/write of connection" + e.getMessage());
		} catch (IllegalRequestException e) {
			logger.error("illegal request .. sending 400 bad requst .. ");
			try {
				charStreamBufferedOut.write(Helper
						.createResponseMessageWithoutBody("400", "Bad Request",
								null));
				charStreamBufferedOut.flush();
			} catch (IOException e1) {
				logger.error("error in read/write of connection"
						+ e1.getMessage());
			}

		} finally {
			cleanUp();
		}
	}

	private void cleanUp() {
		try {
			if (charStreamBufferedOut != null)
				charStreamBufferedOut.close();
			charStreamBufferedOut = null;
		} catch (IOException e) {
			logger.error("error in read/write of connection" + e.getMessage());
		}
		try {
			if (byteStreamBufferedOut != null)
				byteStreamBufferedOut.close();
			byteStreamBufferedOut = null;
		} catch (IOException e) {
			logger.error("error in read/write of connection" + e.getMessage());
		}
		try {
			if (byteStreamBufferedIn != null)
				byteStreamBufferedIn.close();
			byteStreamBufferedIn = null;
		} catch (IOException e) {
			logger.error("error in read/write of connection" + e.getMessage());
		}
		try {
			if (client != null)
				client.close();
			client = null;
		} catch (IOException e) {
			logger.error("error in read/write of connection" + e.getMessage());
		}

	}

}

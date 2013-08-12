package com.adobe.webserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.adobe.webserver.IllegalRequestException;
import com.adobe.webserver.ServerParams;
import com.adobe.webserver.util.Helper;
import com.adobe.webserver.util.MimeTypes;

/**
 * The Handler class for HTTP GET Request GETHandler reads Header from client.
 * Analyses header and then sends approppriate response.
 * 
 * It Checks the header - If-Modified-Since
 * 
 * @author KHEMKA
 * 
 */
public class GETHandler extends HTTPMethodHandler {

	private static Logger logger = Logger.getLogger(GETHandler.class.getName());

	/**
	 * creates a GETHandler object.
	 * 
	 * @param byteStreamBufferedIn
	 *            stream connected to client
	 * @param byteStreamBufferedOut
	 *            stream connected to client
	 * @param charStreamBufferedOut
	 *            stream connected to client
	 */
	public GETHandler(BufferedInputStream byteStreamBufferedIn,
			BufferedOutputStream byteStreamBufferedOut,
			BufferedWriter charStreamBufferedOut) {
		super(byteStreamBufferedIn, byteStreamBufferedOut,
				charStreamBufferedOut);

	}

	/**
	 * {@inheritDoc}
	 */
	public void handle(String requestURI) throws IOException,
			IllegalRequestException {
		HashMap<String, String> header = getHeader();

		if ((requestURI = decodeURI(requestURI)) == null) {
			return;
		}

		File file = findRquestedEntity(requestURI);

		// resource not found on server
		if (file == null) {
			Helper.sendClientMessage("404", "Not Found", null,
					"the file you requested - " + requestURI
							+ " does not exist on server" + "<hr>",
					charStreamBufferedOut, byteStreamBufferedOut);
			logger.info("file requested does not exist - " + requestURI);
			return;
		}

		// resource is a directory
		if (file.isDirectory()) {
			sendDirContentList(file, requestURI);
			return;
		}

		if (isModified(header, file) == false) {
			return;
		}

		BufferedInputStream resourceIS = null;
		try {
			resourceIS = getResourceStream(file);

			if (resourceIS == null)
				return;

			int checkByte = getCheckByte(resourceIS, requestURI);

			if (checkByte >= -1) {
				sendClientMessage(file, resourceIS, checkByte);
			}

		} finally {
			resourceIS.close();
		}

	}

	/**
	 * looks request URI and tries to find out the corresponding file requested
	 * in the GET request. Used to find the resource by GET handler. if the
	 * requested resource is not found it returns null If requestURI equals "/"
	 * it return index.html
	 * 
	 * @param requestURI
	 * @return the resource identified by requestURI, if it exist . null
	 *         otherwise
	 */
	private static File findRquestedEntity(String requestURI) {
		File requestedEntity = null;

		if (requestURI.equals(ServerParams.URISeparator)) {
			requestedEntity = new File(ServerParams.WWW_PATH + File.separator
					+ "index.html");
		} else {
			String[] pathList = requestURI.split(ServerParams.URISeparator);
			StringBuffer path = new StringBuffer(50);
			for (int i = 0; i < pathList.length; i++) {
				if (pathList[i] != null && pathList[i].length() > 0)
					path.append(File.separator + pathList[i]);
			}
			requestedEntity = new File(ServerParams.WWW_PATH + path.toString());
		}

		if (requestedEntity != null && requestedEntity.exists()) {
			return requestedEntity;
		}
		return null;

	}

	private void sendDirContentList(File directory, String requestURI)
			throws IOException {
		File[] list = directory.listFiles();
		StringBuffer htmlLinks = new StringBuffer(100);
		for (int i = 0; i < list.length; i++) {
			htmlLinks.append("<a href=\"" + requestURI
					+ ServerParams.URISeparator + list[i].getName() + "\">"
					+ list[i].getName() + "</a></BR>");
		}

		Helper.sendClientMessage(
				"200",
				"OK",
				null,
				"The location you requested is a folder. Please follow links below to browse through the files .. <hr>"
						+ htmlLinks.toString(), charStreamBufferedOut,
				byteStreamBufferedOut);
		return;
	}

	private int getCheckByte(InputStream resourceIS, String requestURI)
			throws IOException {
		int checkByte = -1;
		try {
			checkByte = resourceIS.read();
		} catch (IOException e) {
			Helper.sendClientMessage("404", "Not Found", null,
					"the file you requested - " + requestURI
							+ " does not exist on server" + "<hr>",
					charStreamBufferedOut, byteStreamBufferedOut);
			logger.info("file requested does not exist - " + requestURI);
			return -2;
		}
		return checkByte;

	}

	private void sendClientMessage(File file, InputStream fileIS, int checkByte)
			throws IOException {
		charStreamBufferedOut.write("HTTP/1.1 200 OK");
		charStreamBufferedOut.write(ServerParams.CRLF);

		charStreamBufferedOut.write(Helper.getCommonHeaders());

		charStreamBufferedOut.write("Content-length: " + file.length());
		charStreamBufferedOut.write(ServerParams.CRLF);

		String contentType = null;
		if (file.getName() != null) {
			try {
				contentType = MimeTypes.getInstance()
						.guessMimeTypeFromFileName(file.getName());
			} catch (IOException e) {
				logger.error("mime type could not be determined");
			}
		}

		if (contentType != null) {
			charStreamBufferedOut.write("Content-Type: " + contentType);
			charStreamBufferedOut.write(ServerParams.CRLF);
		}

		charStreamBufferedOut.write(ServerParams.CRLF);
		charStreamBufferedOut.flush();

		if (checkByte > -1) {
			byteStreamBufferedOut.write(checkByte);
			Helper.readAndWriteByteStream(fileIS, byteStreamBufferedOut, -1L);
		}
	}

	private BufferedInputStream getResourceStream(File file) throws IOException {
		BufferedInputStream fileIS = null;
		try {
			fileIS = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e1) {
			Helper.sendClientMessage("404", "Not Found", null,
					"the file you requested - " + file.getName()
							+ " does not exist on server" + "<hr>",
					charStreamBufferedOut, byteStreamBufferedOut);
			logger.info("file requested does not exist - " + file.getName());

			return null;
		}

		return fileIS;

	}

	private boolean isModified(HashMap<String, String> headerMap, File file)
			throws IOException {

		if (headerMap.containsKey("If-Modified-Since"))

		{
			try {
				long clientCopyDate = Helper.parseHTTPdate(
						headerMap.get("If-Modified-Since")).getTime();
				long serverCopyDate = file.lastModified();
				if (clientCopyDate > serverCopyDate) {
					charStreamBufferedOut.write(Helper
							.createResponseMessageWithoutBody("304",
									"Not Modified", null));
					charStreamBufferedOut.flush();
					return false;
				}
			} catch (ParseException e) {
				logger.error("could not verify 'If-Modified-Since' header .. sending the file");
				Helper.sendClientMessage("400", " Bad Request", null,
						"date value in if-Modified-Since can't be parsed - "
								+ headerMap.get("If-Modified-Since") + "<hr>",
						charStreamBufferedOut, byteStreamBufferedOut);
				return false;
			}
		}
		return true;
	}

}

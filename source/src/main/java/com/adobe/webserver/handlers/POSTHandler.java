package com.adobe.webserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.adobe.webserver.IllegalRequestException;
import com.adobe.webserver.ServerParams;
import com.adobe.webserver.util.Helper;

/**
 * POSTHandler handles all POST requests.
 * It handles Multipart request as well
 * 
 * It reads Header and body of client Message, analyses them and then sends appropriate response.
 * @author KHEMKA
 *
 */
public class POSTHandler extends HTTPMethodHandler {

	private static Logger logger = Logger
			.getLogger(POSTHandler.class.getName());

	private static File _defaultUploadDir = new File(ServerParams.WWW_PATH,
			ServerParams.WWW_UPLOAD_PATH);
	private static String _defaultUploadRelativePath = (File.separator + ServerParams.WWW_UPLOAD_PATH)
			.replace(File.separator, ServerParams.URISeparator).replaceAll(
					ServerParams.URISeparator + "+", ServerParams.URISeparator);
	private static String _boundaryParameterName = "boundary";
	private static String _mediaTypeMultipartName = "multipart";
	private static String _contentLengthHeaderKey = "Content-Length";

	/**
	 * creates a POSTHandler
	 * @param byteStreamBufferedIn stream connected to client
	 * @param byteStreamBufferedOut stream connected to client
	 * @param charStreamBufferedOut stream connected to client
	 */
	public POSTHandler(BufferedInputStream byteStreamBufferedIn,
			BufferedOutputStream byteStreamBufferedOut,
			BufferedWriter charStreamBufferedOut) {
		super(byteStreamBufferedIn, byteStreamBufferedOut,
				charStreamBufferedOut);
	}

	/**
	 * assumes that the requestURI is a folder location and tries to validate it
	 * and find the absolute path corresponding to it. Used in case of multipart
	 * request In case request the location is not found/created, default upload
	 * location is used
	 * 
	 * basically checks where file can be uploaded . it can be either the one sugested by uri or the default one
	 * 
	 * @param requestURI
	 *            the uri to be evaluated
	 * @return relative path(url format) denoting upload location.  null if
	 *         the said location and default location both are unaccessible
	 */
	private static String findLocationToUpload(String requestURI) {
		File uploadDir = null;

		if (requestURI.length() > 0
				&& !requestURI.equals(ServerParams.URISeparator)) {

			uploadDir = new File(ServerParams.WWW_PATH, requestURI.replace(
					ServerParams.URISeparator, File.separator));

			try {
				if (uploadDir.isDirectory() || uploadDir.mkdirs()) {
					return requestURI;
				}

			} catch (SecurityException e) {

				logger.error("error in trying to access upload dir provided with request uri - "
						+ e.getMessage());
			}
		}

		try {

			if (_defaultUploadDir.isDirectory() || _defaultUploadDir.mkdirs()) {
				return _defaultUploadRelativePath;
			}
		} catch (SecurityException e) {
			logger.error("error in trying to access default upload dir - "
					+ e.getMessage());
		}

		return null;

	}

	/**
	 * analyses header and extracts required information out of it.
	 * 
	 * 
	 * @param headerMap
	 * @return some properties(extracted from header) to be used by caller - 
	 * 1. Content-Length
	 * 2. multipart
	 * 3. boundary
	 * 
	 * It may or may not have above properties, depending upon the header provided
	 * 
	 * it returns null to indicate request is not proper and appropriate
	 * response has been sent to the client. Also the caller can return after
	 * cleanup and should not write any thing to client
	 * 
	 * @throws IOException
	 */
	private HashMap<String, Object> analyseHeaders(
			HashMap<String, String> headerMap) throws IOException {
		HashMap<String, Object> properties = new HashMap<String, Object>(5);

		boolean isMultipart = false;
		String boundary = null;
		String[] contentTypeSplitted = null;

		long length = -1;
		if (headerMap.containsKey(_contentLengthHeaderKey)) {
			try {
				length = Long.parseLong(headerMap.get(_contentLengthHeaderKey));
			} catch (NumberFormatException e) {

			}
		}

		if (length < 0) {
			logger.error(_contentLengthHeaderKey + " should be properly set ");
			Helper.sendClientMessage("400", " Bad Request", null,
					_contentLengthHeaderKey + " should be properly set"
							+ "<hr>", charStreamBufferedOut,
					byteStreamBufferedOut);
			return null;
		}

		properties.put(_contentLengthHeaderKey, length);

		if (headerMap.containsKey("Content-Type")) {
			String contentType = headerMap.get("Content-Type");
			contentTypeSplitted = contentType.split(";");
			String[] mediaType = contentTypeSplitted[0].split("/");

			if (mediaType[0].trim().toLowerCase()
					.equals(_mediaTypeMultipartName)) {
				isMultipart = true;

				for (int i = 1; i < contentTypeSplitted.length; i++) {
					String parameter = contentTypeSplitted[i].trim();
					if (parameter.startsWith(_boundaryParameterName + "=")) {
						boundary = parameter.split("=", 2)[1];
					}

					if (boundary.charAt(0) == '"'
							&& boundary.charAt(boundary.length() - 1) == '"'
							&& boundary.length() >= 2) {
						boundary = boundary.substring(1, boundary.length() - 1);
					}

				}

				if (boundary == null) {
					logger.error("boundary parameter not available in multipart post request");
					Helper.sendClientMessage("400", " Bad Request", null,
							"boundary parameter not available  in multipart post request - "
									+ "<hr>", charStreamBufferedOut,
							byteStreamBufferedOut);
					return null;
				}
			}// if multipart
		}// if has Content-Type

		properties.put(_mediaTypeMultipartName, isMultipart);
		// if multipart and boundary not available .. error is indicated
		if (isMultipart) {
			properties.put(_boundaryParameterName, boundary);
		}

		return properties;
	}

	private void send201Response(ArrayList<String> listOfFilesUploaded,
			String relativePath) throws IOException {
		StringBuffer linkTags = new StringBuffer(100);
		Iterator<String> itr = listOfFilesUploaded.iterator();
		while (itr.hasNext()) {
			String name = itr.next();
			linkTags.append("<a href=" + "\""
					+ (relativePath + ServerParams.URISeparator + name) + "\""
					+ ">" + name + "</a>" + "</BR>");
		}

		Helper.sendClientMessage(
				"201",
				"Created",
				null,
				"your data has been uploaded to the server. please follow the below links to check uploaded data<hr>"
						+ linkTags.toString(), charStreamBufferedOut,
				byteStreamBufferedOut);
	}

	private void logPOSTDataFromNonMultipartReq(long size) throws IOException {

		byte[] messageBody = new byte[1024];

		while (size > 0) {
			int len = (int) (size > 1024 ? 1024 : size);
			len = byteStreamBufferedIn.read(messageBody, 0, len);
			for (int i = 0; i < len; i++)
				logger.trace((char) messageBody[i]);
			size -= len;

			Helper.sendClientMessage("200", "OK", null,
					"request received and analysed successfully<hr>",
					charStreamBufferedOut, byteStreamBufferedOut);

		}
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

		HashMap<String, Object> properties = analyseHeaders(header);
		boolean isMultipart = (Boolean) properties.get(_mediaTypeMultipartName);

		if (isMultipart) { // if multipart

			String path = findLocationToUpload(requestURI);
			if (path == null) {

				Helper.sendClientMessage(
						"404",
						"Not Found",
						null,
						"neither the location requested nor default location is available for upload of files"
								+ "<hr>", charStreamBufferedOut,
						byteStreamBufferedOut);
				logger.info("upload location could not be created - ");
				return;
			}

			String relativePath = path;
			File uploadDir = new File(ServerParams.WWW_PATH,
					relativePath.replace(ServerParams.URISeparator,
							File.separator));

			String boundary = (String) properties.get(_boundaryParameterName);

			ArrayList<String> listOfFilesUploaded = new MultipartHandler(byteStreamBufferedIn, byteStreamBufferedOut, 
					charStreamBufferedOut).handleMultipartBody("--"+boundary, uploadDir);
					
//					MultipartHandler
//					.handleMultipartBody(byteStreamBufferedIn,
//							charStreamBufferedOut, byteStreamBufferedOut, "--"
//									+ boundary, uploadDir);
			if(listOfFilesUploaded==null){
				return;
			}
			
			send201Response(listOfFilesUploaded, relativePath);
		} else { // if not multipart

			long size = (Long) properties.get(_contentLengthHeaderKey);
			logPOSTDataFromNonMultipartReq(size);

		}
	}

}

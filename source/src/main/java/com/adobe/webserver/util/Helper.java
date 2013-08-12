package com.adobe.webserver.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.adobe.webserver.IllegalRequestException;
import com.adobe.webserver.ServerParams;

public class Helper {

	private static Logger logger = Logger.getLogger(Helper.class.getName());

	private Helper() {

	}

	/**
	 * creates a hash map from key value pairs, each pair being separated by
	 * delimeter delim1 and key value being separated by delim2
	 * 
	 * @param input
	 *            the string to be converted into hash map
	 * @param delim1
	 *            first delimeter to separate different key-value pairs
	 * @param delim2
	 *            second delimeter to separate key from value
	 * @return the hashMap
	 */
	public static HashMap<String, String> stringToHashMap(String input,
			String delim1, String delim2) {
		HashMap<String, String> map = new HashMap<String, String>(20);
		if (!(input == null || delim1 == null || delim2 == null
				|| input.length() <= 0 || delim1.length() <= 0 || delim2
				.length() <= 0)) {
			String[] inputSplitted = input.split(delim1);

			for (int i = 0; i < inputSplitted.length; i++) {
				String[] keyValuePair = inputSplitted[i].trim()
						.split(delim2, 2);
				if (keyValuePair.length == 2 && keyValuePair[0].length() > 0
						&& keyValuePair[1].length() > 0) {
					map.put(keyValuePair[0], keyValuePair[1]);
				}
			}
		}
		return map;

	}

	/**
	 * takes HTTP header as input, parses it, forms key value pairs, and puts it
	 * in a HashMap
	 * 
	 * @param header
	 *            the HTTP header
	 * @return key value pairs of Header
	 */
	public static HashMap<String, String> convertHTTPHeaderToHashMap(
			String header) {
		HashMap<String, String> headerMap = new HashMap<String, String>(50);
		String[] headerList = header.split(ServerParams.CRLF);

		for (int i = 0; i < headerList.length && headerList[i] != null
				&& headerList[i].length() > 0; i++) {
			String[] keyValuePair = parseMessageHeader(headerList[i]);
			if (keyValuePair != null) {
				if (headerMap.containsKey(keyValuePair[0])) {
					String val = headerMap.get(keyValuePair[0]) + ","
							+ keyValuePair[1];
					headerMap.put(keyValuePair[0], val);
				} else {
					headerMap.put(keyValuePair[0], keyValuePair[1]);
				}
			}// if
		}// for

		return headerMap;
	}

	/**
	 * takes a message header line and separates the field-name and field-value
	 * 
	 * @param header
	 *            HTTP message header
	 * @return array containing 2 elements - field-name and field-value. will
	 *         throw NullPointerException if null is passed as header value
	 */
	private static String[] parseMessageHeader(String header) {
		header = header.replaceAll("\\s+", " ");
		String[] strArr = header.split(":", 2);

		if (strArr[0] != null && strArr[1] != null) {
			strArr[0] = strArr[0].trim();
			strArr[1] = strArr[1].trim();
			if (strArr[0].length() != 0 && strArr[1].length() != 0)
				return strArr;
		}
		return null;
	}

	/**
	 * read request line from client. a request lIne is of form Request-Line =
	 * Method SP Request-URI SP HTTP-Version CRLF
	 * 
	 * @param clientInpStream
	 *            client Reader
	 * @return the request line without trailing \r\n
	 * @throws IOException
	 *             when there is error in read
	 * @throws IllegalRequestException
	 *             if an invalid request comes from client..
	 */
	public static String readRequestLineFromClient(InputStream clientInpStream)
			throws IOException, IllegalRequestException {
		StringBuffer requestLine = new StringBuffer(20);
		int inp = 0;
		while ((inp = clientInpStream.read()) > -1) {
			if ((char) inp != '\r') {
				requestLine.append((char) inp);
			} else {
				inp = clientInpStream.read();
				if ((char) inp != '\n') {
					logger.error("the client passed a ill formed header");
					throw new IllegalRequestException(
							"while parsing a request line a \r was found. But the character following it was not \n");
				}
				return requestLine.toString();
			}
		}

		throw new IllegalRequestException(
				"client socket closed connection .. request line could not be read completely");

	}

	/**
	 * read header from client and return it as a string this method assumes
	 * that the request line has been read from stream and is now not available
	 * 
	 * @param clientInpStream
	 *            stream connected to client
	 * @return header
	 * @throws IOException
	 *             if header could not be read completely
	 * @throws IllegalRequestException
	 *             if an invalid request comes from client..
	 */
	public static String readHeaderFromClient(InputStream clientInpStream)
			throws IOException, IllegalRequestException {
		StringBuffer header = new StringBuffer(500);

		int inp = 0;
		int dfaState = 0;
		int FINAL_STATE = 4; // a DFA being used with 4 states
		while (dfaState != FINAL_STATE && (inp = clientInpStream.read()) > -1) {
			switch ((char) inp) {
			case '\r':
				if (dfaState == 0) {
					dfaState = 1;
				} else if (dfaState == 2) {
					dfaState = 3;
				}
				break;
			case '\n':
				if (dfaState == 1) {
					dfaState = 2;
				} else if (dfaState == 3) {
					dfaState = 4;
				}
				break;
			default:
				dfaState = 0;
			}

			header.append((char) inp);
		}

		if (dfaState != FINAL_STATE) {
			throw new IllegalRequestException(
					"client closed connection before server could read HTTP header");
		}

		header.deleteCharAt(header.length() - 1); // deleting \n
		header.deleteCharAt(header.length() - 1); // deleting \r

		logger.debug("header from the client - " + header.toString());

		return header.toString();
	}

	/**
	 * reads bytes from an InputStream and writes the same to an OutputStream
	 * 
	 * @param in
	 *            the input Stream to be read
	 * @param out
	 *            the output Stream to write
	 * @param size
	 *            number of bytes to read and write. if size = -1 then read
	 *            until end of stream is found else write size number of bytes
	 * @throws IOException
	 */
	public static void readAndWriteByteStream(InputStream in, OutputStream out,
			long size) throws IOException {
		byte[] messageBody = new byte[1024];
		if (size <= -1L) { // read until end of stream
			int len = 0;

			while ((len = in.read(messageBody)) > -1) {
				out.write(messageBody, 0, len);
			}
		} else { // read size number of bytes from inputstream

			while (size > 0) {
				int len = (int) (size > 1024 ? 1024 : size);
				len = in.read(messageBody, 0, len);
				out.write(messageBody, 0, len);
				size -= len;
			}
		}
		out.flush();
	}

	/**
	 * suggest a unique file name in the absolute location provided so that the
	 * name don't clash with the existing names
	 * 
	 * If a file with same name exists in server, it creates a UUID and append
	 * the file name to the UUID to generate a unique name else it returns the
	 * name as it is
	 * 
	 * @param location
	 *            the absolute location to be checked
	 * @param suggestion
	 *            suggested name
	 * @return the unique name
	 */
	public static String getUUIDBasedUniqueFileName(String location,
			String suggestion) {
		if (suggestion != null && suggestion.length() != 0) {
			File file = new File(location, suggestion);
			if (file.exists())
				return UUID.randomUUID().toString() + "__" + suggestion;
			else
				return suggestion;
		} else {
			return UUID.randomUUID().toString();
		}

	}

	/**
	 * if location is not given, this method assumes the current location as the
	 * required location if suggestion is not given, this method suggests a name
	 * "NewFile" If no file with the given name exists, it returns the name as
	 * it is. otherwise it creates name of format filename (index).extension the
	 * index goes from 2 to a fixed number indicated by - {@link ServerParams.#NumberOfDuplicateFileNamesAcceptedInPostReq}
	 * 
	 * if all names are already used the method returns null
	 * 
	 * @param location
	 *            the path in which method needs to suggest appropriate name
	 * @param suggestion
	 *            the name suggested
	 * @return the suggested name. returns null if location is given but not
	 *         valid.or file name given is directory or could not find an
	 *         appropriate name
	 */
	public static String getUniqueFileName(String location, String suggestion) {

		File searchDir = (location != null && location.length() > 0) ? new File(
				location) : new File(System.getProperty("user.dir"));
		if (!searchDir.isDirectory())
			return null;

		suggestion = (suggestion != null && suggestion.length() > 0) ? suggestion
				: "NewFile";

		File file = new File(searchDir, suggestion);

		if (file.isDirectory())
			return null;

		if (!file.exists()) {
			return suggestion;
		}

		String baseName = suggestion.split("(\\.\\w+)?$")[0];
		String extension = "";
		if (baseName.length() != suggestion.length())
			extension = suggestion.substring(baseName.length() + 1);

		int i = 1;
		while (i < 1024) {
			i++;
			if (!(new File(searchDir, baseName + " (" + i + ")." + extension)
					.exists()))
				return baseName + " (" + i + ")." + extension;
		}

		return null;

	}

	/**
	 * used for parsing date coming with HTTP headers. it recognizes the
	 * standard in which date is received and parses accordingly. it
	 * differentiates only three date patters - RFC1123,Asctime,RFC850 . this
	 * method assumes that user sends date in either of the three above
	 * mentioned date format. In case user does not , it will fail will throw an
	 * Exception
	 * 
	 * @param dateString
	 *            the date to be parsed
	 * @return Date object
	 * @throws ParseException
	 *             if the method could not recognize the date pattern
	 */
	public static Date parseHTTPdate(String dateString) throws ParseException {
		String inputDateFormat = null;

		if (dateString.charAt(3) == ',') // rfc1123-date
			inputDateFormat = ServerParams.RFC1123DateFormat;
		else if (dateString.charAt(3) == ' ') // asctime-date
			inputDateFormat = ServerParams.AsctimeDateFormat;
		else
			// rfc850-date
			inputDateFormat = ServerParams.RFC850DateFormat;

		return new SimpleDateFormat(inputDateFormat).parse(dateString);

	}

	/**
	 * returns a string of headers common to all responses. It includes
	 * following headers 1. Server 2. Date 3. Connection
	 * 
	 * see <a href=
	 * "http://xml.resource.org/public/rfc/html/rfc2616.html#HTTP.Message">rfc
	 * 2616</a>
	 * 
	 * it sends headers in following format *(message-header CRLF)
	 * message-header = field-name ":" [ field-value ] field-name = token
	 * field-value = *( field-content | LWS ) field-content = <the OCTETs making
	 * up the field-value and consisting of either *TEXT or combinations of
	 * token, separators, and quoted-string>
	 * 
	 * @return string of common headers
	 */
	public static String getCommonHeaders() {
		StringBuffer message = new StringBuffer(50);
		message.append("Server: " + ServerParams.serverHeader);
		message.append(ServerParams.CRLF);
		message.append("Date: "
				+ new SimpleDateFormat(ServerParams.RFC1123DateFormat)
						.format(new Date()));
		message.append(ServerParams.CRLF);

		message.append("Connection: close");
		message.append(ServerParams.CRLF);
		return message.toString();

	}

	/**
	 * creates a response message without message body. can be used when no
	 * message body is required
	 * 
	 * @param statusCode
	 *            the http response status code to be sent
	 * @param statusMessage
	 *            status message corresponding to status code
	 * @param additionalHeaders
	 *            any additional headers if required, other than headers
	 *            received from {@link Helper.#getCommonHeaders()}
	 * @return the message string
	 */
	public static String createResponseMessageWithoutBody(String statusCode,
			String statusMessage, String[] additionalHeaders) {
		StringBuffer message = new StringBuffer(50);

		// HEADERS
		message.append("HTTP/1.1 " + statusCode + " " + statusMessage);
		message.append(ServerParams.CRLF);

		message.append(getCommonHeaders());

		if (additionalHeaders != null) {
			for (int i = 0; i < additionalHeaders.length; i++) {
				message.append(additionalHeaders[i]);
				message.append(ServerParams.CRLF);
			}
		}

		message.append(ServerParams.CRLF);
		return message.toString();
	}

	/**
	 * sends client a response. takes a file to be treated as body of message
	 * 
	 * @param statusCode
	 *            the http response status code to be sent
	 * @param statusMessage
	 *            status message corresponding to status code
	 * @param additionalHeaders
	 *            any additional headers if required, other than headers
	 *            received from {@link Helper.#getCommonHeaders()}
	 * @param body
	 *            to be sent as body of message
	 * @param charStreamBufferedOut
	 *            the stream connected to client
	 * @param byteStreamBufferedOut
	 *            the stream connected to client
	 * @throws IOException
	 *             when some read/write error occurs
	 */
	public static void sendClientMessage(String statusCode,
			String statusMessage, String[] additionalHeaders, File body,
			BufferedWriter charStreamBufferedOut,
			BufferedOutputStream byteStreamBufferedOut) throws IOException {
		StringBuffer message = new StringBuffer(50);

		// HEADERS
		message.append("HTTP/1.1 " + statusCode + " " + statusMessage);
		message.append(ServerParams.CRLF);

		message.append(getCommonHeaders());

		if (additionalHeaders != null) {
			for (int i = 0; i < additionalHeaders.length; i++) {
				message.append(additionalHeaders[i]);
				message.append(ServerParams.CRLF);
			}
		}

		message.append("Content-length: " + body.length());
		message.append(ServerParams.CRLF);

		BufferedInputStream in = null;
		try {
			try {
				in = new BufferedInputStream(new FileInputStream(body));
			} catch (FileNotFoundException e) {
				logger.error("error message file not found");
				charStreamBufferedOut.write(Helper
						.createResponseMessageWithoutBody("500",
								"Internal Server Error", null));
				return;
			}

			charStreamBufferedOut.write(message.toString());
			charStreamBufferedOut.flush();

			Helper.readAndWriteByteStream(in, byteStreamBufferedOut, -1);
		} finally {
			in.close();
		}

	}

	/**
	 * sends a message to the client with some default formatting for body of
	 * message
	 * 
	 * @param statusCode
	 *            http response code
	 * @param statusMessage
	 *            http response message
	 * @param additionalHeaders
	 *            any additional header if required
	 * @param bodyMesg
	 *            short message to be inserted in body. this is placed in tag
	 *            <p/>
	 * @param charStreamBufferedOut
	 *            the stream coonected to client
	 * @param byteStreamBufferedOut
	 *            the stream coonected to client
	 * @throws IOException
	 *             when some read/write error occurs
	 */
	public static void sendClientMessage(String statusCode,
			String statusMessage, String[] additionalHeaders, String bodyMesg,
			BufferedWriter charStreamBufferedOut,
			BufferedOutputStream byteStreamBufferedOut) throws IOException {
		StringBuffer message = new StringBuffer(50);

		// HEADERS
		message.append("HTTP/1.1 " + statusCode + " " + statusMessage);
		message.append(ServerParams.CRLF);

		message.append(getCommonHeaders());

		if (additionalHeaders != null) {
			for (int i = 0; i < additionalHeaders.length; i++) {
				message.append(additionalHeaders[i]);
				message.append(ServerParams.CRLF);
			}
		}

		String body = Helper.generateGenricMessageBody(statusCode,
				statusMessage, bodyMesg);

		message.append("Content-length: " + body.length());
		message.append(ServerParams.CRLF);
		message.append(ServerParams.CRLF);
		message.append(body);

		charStreamBufferedOut.write(message.toString());
		charStreamBufferedOut.flush();

	}

	/**
	 * generates a html with a standard format
	 * 
	 * @param statusCode
	 *            http statuscode
	 * @param statusMesg
	 *            http status mesage
	 * @param bodyMesg
	 *            message to be inserted into body of html
	 * @return the html string
	 */
	private static String generateGenricMessageBody(String statusCode,
			String statusMesg, String bodyMesg) {
		if (bodyMesg == null || bodyMesg.length() == 0) {
			bodyMesg = "thats all we know<hr>";
		}

		String body = "<HTML>" + "<HEAD>" + "<TITLE>" + statusMesg + "</TITLE>"
				+ "</HEAD>" + "<BODY>" + "<img src=\"/images/logo.jpg\">"
				+ "<H3>" + statusCode + ". " + statusMesg + "</H3>" + "</BR>"
				+ "<p>" + bodyMesg + "</p>" + "</BODY>" + "</HTML>";

		return body;
	}

}

package com.adobe.webserver;

import java.io.File;


/**
 * defines and holds all the properties of the server
 * @author KHEMKA
 *
 */
public class ServerParams {
	
	/**
	 *Maximum number of concurrency to serve clients
	 */
	public static int THREAD_POOL_SIZE = 50;
	
	/**
	 * Maximum number of client requests which can be queued before start of process. Beyond this all requests are ignored
	 */
	public static int QUEUE_SIZE = 500;
	
	/**
	 * the port on Which Server Should tun
	 */
	public static int PORT = 80;
	
	/**
	 * path to folder exposed to external world for requests 
	 */
	public static String WWW_PATH = "webapps";
	
	/**
	 * Default folder where files should be uploaded by POST request. This path is relative to WWW_PATH
	 */
	public static String WWW_UPLOAD_PATH = File.separator +"root" +File.separator+ "upload"; 
	
	/**
	 * The hostname (IP address) on which server should listen
	 * If it is null, server will listen on any IP connected with the server
	 */
	public static String HOSTNAME = null;
	
	/**
	 * The header value for the header filed Server
	 */
	public static final String serverHeader = "Java HTTP Server 1.1";
	
	/**
	 * the SO_TIMEOUT value for ServerSocket
	 * (in milliseconds).
	 */
	public static final int ServerSocketSoTimeout = 10000;
	/**
	 * the SO_TIMEOUT value for Client Socket
	 * (in milliseconds).
	 */
	public static final int SocketSoTimeout = 20000;
	
	/**
	 * Number of indexes to be used for uploading files with same name
	 */
	public static final int NumberOfDuplicateFileNamesAcceptedInPostReq = 1024;

	/**
	 * path to properties.xml file
	 */
	public static final String PropertiesPath = "conf"+File.separator + "properties.xml";
	
	/**
	 * the path separator used by URI
	 */
	public static final String URISeparator="/";
	
	/**
	 * a 2 byte string - (carriage-return , new line)
	 */
	public static final String CRLF="\r\n";
	
	/**
	 * RFC1123 date format - 
	 */
	public static final String RFC1123DateFormat="EEE, dd MMM yyyy HH:mm:ss zzz";
	
	/**
	 * ascii time date format
	 */
	public static final String AsctimeDateFormat="EEE MMM d HH:mm:ss yyyy";
	
	/**
	 * RFC 850 date format
	 */
	public static final String RFC850DateFormat="EEEEE, dd-MMM-yy HH:mm:ss zzz"; 
	
	/**
	 * Encoding used in HTTP headers
	 */
	public static final String HTTPHeadersEncoding="ISO8859_1";
	
	/**
	 * encoding used in URL
	 */
	public static final String URLEncoding="UTF-8";
	
	
	
	

}

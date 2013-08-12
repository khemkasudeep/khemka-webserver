package com.adobe.webserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.adobe.webserver.IllegalRequestException;
import com.adobe.webserver.util.Helper;

/**
 * handles multipart body in POST request by client
 * 
 * @author KHEMKA
 * 
 */
class MultipartHandler {

	protected BufferedInputStream byteStreamBufferedIn;
	protected BufferedOutputStream byteStreamBufferedOut;
	protected BufferedWriter charStreamBufferedOut;

	private static Logger logger = Logger.getLogger(MultipartHandler.class
			.getName());

	public MultipartHandler(BufferedInputStream byteStreamBufferedIn,
			BufferedOutputStream byteStreamBufferedOut,
			BufferedWriter charStreamBufferedOut) {
		this.byteStreamBufferedIn = byteStreamBufferedIn;
		this.byteStreamBufferedOut = byteStreamBufferedOut;
		this.charStreamBufferedOut = charStreamBufferedOut;
	}

	/**
	 * it creates a pre kmp DFA to be used in boundary string search copied from
	 * "https://weblogs.java.net/blog/potty/archive/2012/05/10/string-searching-algorithms-part-ii"
	 * generates the KMP pi function(DFA)
	 * 
	 * @param pattern
	 *            this is formed by ("--" + boundary )
	 * @return the DFA
	 */
	private static int[] prekmp(String pattern) {
		int[] next = new int[pattern.length()];
		int i = 0, j = -1;
		next[0] = -1;
		while (i < pattern.length() - 1) {
			while (j >= 0 && pattern.charAt(i) != pattern.charAt(j))
				j = next[j];
			i++;
			j++;
			next[i] = j;
		}
		return next;
	}

	/**
	 * it searches the boundary, in body of get request. As soon as it gets the
	 * boundary, it returns
	 * 
	 * @param byteStreamBufferedIn
	 * @param boundary
	 *            this is formed by ("--" + boundary )
	 * @param next
	 *            the pre kmp DFA
	 * @throws IOException
	 *             if read/write error occurs
	 * @throws IllegalRequestException
	 *             if the request is wrong
	 */
	private static void seakBoundary(InputStream byteStreamBufferedIn,
			String boundary, int[] next) throws IOException,
			IllegalRequestException {

		int state = 0;
		int c = -1;
		while ((c = byteStreamBufferedIn.read()) > -1) {
			while (state >= 0 && (char) c != boundary.charAt(state))
				state = next[state];
			state++;
			if (state == boundary.length()) {
				return;
			}
		}

		throw new IllegalRequestException("wrong Multipart body format");

	}

	/**
	 * it reads data from inputstream and dumps it into outputstream, until
	 * boundary is found
	 * 
	 * @param byteStreamBufferedIn
	 *            input stream to be read. generally client input stream
	 * @param out
	 *            output stream to dump data . (generally an appropriate file)
	 * @param boundary
	 *            "--" + boundary
	 * @param next
	 *            the pre kmp DFA
	 * @throws IOException
	 * @throws IllegalRequestException
	 */
	private static void searchBoundaryAndDumpTextInFile(
			InputStream byteStreamBufferedIn, OutputStream out,
			String boundary, int[] next) throws IOException,
			IllegalRequestException {

		int state = 0;
		int c = -1;
		while ((c = byteStreamBufferedIn.read()) > -1) {
			while (state >= 0 && (char) c != boundary.charAt(state))
				state = next[state];
			state++;
			out.write(c);
			if (state == boundary.length()) {
				out.flush();
				return;
			}
		}
		throw new IllegalRequestException("wrong Multipart body format");

	}

	/**
	 * called only after boundary found. it checks if this is end of message or
	 * some more data is left
	 * 
	 * @param byteStreamBufferedIn
	 * @return true is this is the end of the message
	 * @throws IOException
	 */
	private static boolean isMessageBodyEnd(InputStream byteStreamBufferedIn)
			throws IOException {
		int c1 = byteStreamBufferedIn.read();
		int c2 = byteStreamBufferedIn.read();
		if ((char) c1 == '-' && (char) c2 == '-') {
			return true;
		}
		return false;
	}

	/**
	 * used to delete boundary from file. During process generally boundary gets
	 * written in file.
	 * 
	 * @param f
	 *            the file from which boundary is written
	 * @param boundary
	 * @throws IOException
	 */
	private static void DeleteBoundaryFromFile(File f, String boundary)
			throws IOException {
		FileChannel outChan = null;
		FileOutputStream file = null;
		try {
			file = new FileOutputStream(f, true);
			outChan = file.getChannel();
			long newSize = f.length() - boundary.length();
			outChan.truncate(newSize);
		} finally {
			if (outChan != null)
				outChan.close();
			if (file != null)
				file.close();
		}
	}

	private String analyseHeaders(HashMap<String, String> headerMap)
			throws IOException {
		String fileName = "";
		// in case there is an error
		String errorText = null;

		if (headerMap.containsKey("Content-Disposition")) {
			String contentDisposition = headerMap.get("Content-Disposition");

			if (contentDisposition != null) {
				HashMap<String, String> contentDispositionMap = Helper
						.stringToHashMap(contentDisposition, ";", "=");
				if (contentDispositionMap.containsKey("filename")) {

					fileName = contentDispositionMap.get("filename");

					if (fileName.length() >= 2) {
						fileName = fileName.substring(1, fileName.length() - 1);
					} else if (fileName.length() == 1) {
						fileName = null;
						errorText = "filename should be quoted";
					}

				}
			}
		} else {
			fileName = null;
			errorText = "Content-Disposition not available in multipart body";
		}

		if (fileName == null) {
			logger.error(errorText);
			Helper.sendClientMessage("400", " Bad Request", null, errorText
					+ "<hr>", charStreamBufferedOut, byteStreamBufferedOut);
			return null;
		}

		// remove path info if any
		File temp = new File(fileName);
		fileName = temp.getName();

		logger.info("file named - " + fileName + "is being uploaded");

		return fileName;
	}

	private boolean upLoadFile(String fileName, File location, String boundary,
			int[] next) throws IOException, IllegalRequestException {
		File file = new File(location.getAbsolutePath(), fileName);
		BufferedOutputStream newFile = null;
		try {
			try {
				newFile = new BufferedOutputStream(new FileOutputStream(file));
			} catch (FileNotFoundException e) {
				logger.error("file named " + fileName + ",can't be created");
				seakBoundary(byteStreamBufferedIn, boundary, next);
				return false; // indicates continue
			}
			searchBoundaryAndDumpTextInFile(byteStreamBufferedIn, newFile,
					boundary, next);

		} finally {
			if (newFile != null) {
				try {
					newFile.flush();
				} catch (IOException e) {
					logger.error("file named " + fileName
							+ ",written but could not flush");
				}
				try {
					newFile.close();
				} catch (IOException e) {
					logger.error("file named " + fileName
							+ ",could not be closed");
				}
			}
		}

		try {
			DeleteBoundaryFromFile(file, boundary);
		} catch (IOException e) {
			logger.error("file named "
					+ fileName
					+ ",uploaded but is appended a multipart boundary .. could not delete");
			return false; // indicates continue
		}

		logger.info("file created successfully at location - "
				+ file.getAbsolutePath());
		// indicates go forward
		return true;
	}

	/**
	 * reads multipart body. process it, and uploads files in 'location'
	 * provided.
	 * 
	 * @param byteStreamBufferedIn
	 * @param charStreamBufferedOut
	 * @param byteStreamBufferedOut
	 * @param boundary
	 *            is '--' + boundary(from header)
	 * @param location
	 *            the location to upload data
	 * @return list of file names successfully uploaded
	 * @throws IllegalRequestException
	 * @throws IOException
	 */
	ArrayList<String> handleMultipartBody(String boundary, File location)
			throws IllegalRequestException, IOException {

		int[] next = prekmp(boundary);
		seakBoundary(byteStreamBufferedIn, boundary, next);
		ArrayList<String> filesUploaded = new ArrayList<String>(5);

		while (!isMessageBodyEnd(byteStreamBufferedIn)) {

			String messageHeader = Helper
					.readHeaderFromClient(this.byteStreamBufferedIn);
			HashMap<String, String> headerMap = Helper
					.convertHTTPHeaderToHashMap(messageHeader);

			String fileName = analyseHeaders(headerMap);
			if (fileName == null)
				return null;

			// ignore this file
			if (fileName.length() == 0) {
				seakBoundary(byteStreamBufferedIn, boundary, next);
				continue;
			}

			fileName = Helper.getUniqueFileName(location.getAbsolutePath(),
					fileName);

			// get UUID based name if too many files with the same name
			if (fileName == null) {
				fileName = Helper.getUUIDBasedUniqueFileName(
						location.getAbsolutePath(), fileName);
			}

			if (!upLoadFile(fileName, location, boundary, next)) {
				continue;
			}

			filesUploaded.add(fileName);

		}// while

		return filesUploaded;

	}

}

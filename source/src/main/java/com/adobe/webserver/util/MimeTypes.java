package com.adobe.webserver.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * this class determines mime type of a file using its extension.
 * It reads mime type list from a file called mime.type and structures it into a HashMap
 * This is a singleton class
 * @author khemka
 *
 */
public class MimeTypes {
	
	private static MimeTypes singleInstance = null;
	private HashMap<String,String> mimeTypesMapping = null;
	
	
	/**
	 * return the instance of MimeTypes. This is a singleton class so new instances can't be created
	 * @return the MimeTypes Instance
	 * @throws IOException if some error occurs read/write of mime.types
	 */
	public static MimeTypes getInstance() throws IOException{
		if(singleInstance==null){
			synchronized (MimeTypes.class) {
				if(singleInstance==null){
					singleInstance = new MimeTypes();
				}
			}
		}
		
		return singleInstance;
	}
	
	private MimeTypes() throws IOException {
		mimeTypesMapping = new HashMap<String,String>(1000);
		BufferedReader mimeType = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/mime.types")));
				//new FileInputStream("mime.types")) );
		String next=null;
		String[] nextSplitted=null;
		while((next=mimeType.readLine())!=null ){
			next = next.trim();
			if(next.charAt(0)!='#'){
				nextSplitted = next.split("\\s+");
				for(int i=1;i<nextSplitted.length;i++){
					if(nextSplitted[i]!=null && nextSplitted[i].length()!=0){
						mimeTypesMapping.put(nextSplitted[i], nextSplitted[0]);
					}
				}
			}
		}

	}
			
	/**
	 * returns Mime type if found else null
	 * @param fileName name of file for which mime type has to be determined
	 * @return mime type 
	 */
	public String guessMimeTypeFromFileName(String fileName){
		String[] fileNameSplitted = fileName.split("\\.");
		String fileExtension =null;
		if(fileNameSplitted.length>0){
			fileExtension = fileNameSplitted[fileNameSplitted.length-1];
		}
		if(fileExtension!=null && fileExtension.length()!=0 && mimeTypesMapping.containsKey(fileExtension)){
			return mimeTypesMapping.get(fileExtension);
		}
		return null;
		
	}
	
}

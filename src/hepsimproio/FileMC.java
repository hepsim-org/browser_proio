/* ---------------------------------------------------------------------------
 ** This software is in the public domain, furnished "as is", without technical
 ** support, and with no warranty, express or implied, as to its usefulness for
 ** any purpose.
 **
 **  A library for HEP events storage and processing based on Google's ProtocolBuffers 
 **
 ** Author: S.Chekanov (ANL). chekanov@anl.gov
 ** Copyright  2018
 ** -------------------------------------------------------------------------*/

package hepsimproio;

import proio.*;
import java.io.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

/**
 * 
 * A class to read data structures from ProIO files.
 * <p>
 * 
 * @author S.Chekanov
 * 
 */
public class FileMC {

	private boolean isURL = false;
	private boolean debug=false;
	private String model;
	private proio.Reader reader;


	/**
	 * Open a file to write/read objects to/from a file in sequential order. If
	 * "w" option is set, the old file will be removed. Use close() to flash the
	 * buffer and close the file.
	 * 
	 * @param file
	 *            File name
	 * @param option
	 *            Option to create the file. If "w" - write a file (or read)
	 *            file, if "r" only read created file.
	 */
	public FileMC(String file, String option) {

		if (option.equalsIgnoreCase("r")) {

			if (file.startsWith("http") || file.startsWith("ftp")) {

				URL url = null;
				try {
					url = new URL(file);
				} catch (MalformedURLException e) {
					// System.out.println(e.toString());
					System.out.println("Error in accessing the URL="
					                   + url.toString());
				}
				try {
					URLConnection urlConn = url.openConnection();
					urlConn.setDoInput(true);
					urlConn.setUseCaches(false);
					InputStream inStream = urlConn.getInputStream();
					reader = new proio.Reader(inStream);
					isURL = true;

				} catch (MalformedURLException e) {
					System.out.println(e.toString());
				} catch (IOException e) {
					System.out.println(e.toString());
				}

			} else { // normal file

				try {
					reader = new proio.Reader(file);
					isURL = false;
				} catch (IOException e) {
					System.err.println("Error in opening=" + file);
					e.printStackTrace();
				}

			} // end normal file

		} else {

			System.err
			.println("Wrong option!. Only \"r\" or \"w\"  is allowed");
		}

	};


	/**
	 * Open file for reading objects from a serialized file in sequential order.
	 * 
	 * @param file
	 *            File name
	 */
	public FileMC(String file) {

		this(file, "r");

	};


	/**
	  * Skip some number of events. 
	  * @param number of events to skip. 
	  */
	public void skip(long nEvents){
		try {
			reader.skip(nEvents);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	/**
	  * Get the reader.
	  * 
	  * @return reader. 
	  */
	public proio.Reader reader(){
		return reader;
	}

	/**
	 * Is this a remote file?
	 * 
	 * @return true if remote
	 */
	public boolean getIsURL() {
		return isURL;
	}

	/**
	 * Get the description from the file.
	        * @return file description.
	 */
	public String getDescription() {
		String tmp="";
		return tmp;

	}


	/**
	 * Return info about the log file.
	 * 
	 * @return log file name if stored.
	 */
	public String getLogfile() {

		String logfile="None";
		int nn=0;
		String events="0";
		try {
			Event storeevent = null;
			Event tmpEvent;
			while (true) {
				tmpEvent = null;
				try {
					tmpEvent = reader.next(true);
				} catch (Throwable e) {
					;
				}
				if (tmpEvent == null) {
					break;
				}
				storeevent = tmpEvent;
				nn++;
			}
			Map<String, ByteString> metadata = storeevent.getMetadata();
			for (Map.Entry<String, ByteString> entry : metadata.entrySet()) {
				String skey=entry.getKey();
				String name=entry.getValue().toStringUtf8();
				if (!name.equals("logfile")) {
					if (skey.equals("meta:events")) events=name;
					else if (skey.equals("meta:logfile")) logfile=name;
				}
			}
			if (nn-1 != Integer.valueOf(events))
				System.out.println(HepSim.ANSI_RED+"####  Error: File is corrupted. Inconsistent number of events!  ####"+HepSim.ANSI_RESET );

		} catch (Throwable e) {
			System.out.println(e);
		}
		return logfile;
	}



	/**
	 * Returns model of this file
	 * 
	 * @return model 
	 */
	public String getModel() {

		String model="None";
		int nn=0;
		String events="0";
		try {
			Event storeevent = null;
			Event tmpEvent;
			while (true) {
				tmpEvent = null;
				try {
					tmpEvent = reader.next(true);
				} catch (Throwable e) {
					;
				}
				if (tmpEvent == null) {
					break;
				}
				storeevent = tmpEvent;
				nn++;
			}
			Map<String, ByteString> metadata = storeevent.getMetadata();
			for (Map.Entry<String, ByteString> entry : metadata.entrySet()) {
				String skey=entry.getKey();
				String name=entry.getValue().toStringUtf8();
				if (!name.equals("logfile")) {
					if (skey.equals("meta:events")) events=name;
					else if (skey.equals("meta:model.proto")) model=name;
				}
			}
			if (nn-1 != Integer.valueOf(events))
				System.out.println(HepSim.ANSI_RED+"####  Error: File is corrupted. Inconsistent number of events!  ####"+HepSim.ANSI_RESET );

		} catch (Throwable e) {
			System.out.println(e);
		}


		return model;
	}


	public long getEvents() {

		int nn=0;
		String events="0";
		try {
			Event storeevent = null;
			Event tmpEvent;
			while (true) {
				tmpEvent = null;
				try {
					tmpEvent = reader.next(true);
				} catch (Throwable e) {
					;
				}
				if (tmpEvent == null) {
					break;
				}
				storeevent = tmpEvent;
				nn++;
			}
			Map<String, ByteString> metadata = storeevent.getMetadata();
			for (Map.Entry<String, ByteString> entry : metadata.entrySet()) {
				String skey=entry.getKey();
				String name=entry.getValue().toStringUtf8();
				if (!name.equals("logfile")) {
					if (skey.equals("meta:events")) events=name;
				}
			}
			if (nn-1 != Integer.valueOf(events))
				System.out.println(HepSim.ANSI_RED+"####  Error: File is corrupted. Inconsistent number of events!  ####"+HepSim.ANSI_RESET );

		} catch (Throwable e) {
			System.out.println(e);
		}


		return nn-1;

	}



	/**
	 * Close the file. 
	 * 
	 * @return true if success. 
	 */
	public boolean close() {

		try {
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.gc();
		return true;
	};


	/**
	 * Generate error message
	 * 
	 * @param a
	 *            Message
	 **/

	private void ErrorMessage(String a) {
		System.err.println(a);

	}

}

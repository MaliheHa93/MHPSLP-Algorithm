package MyKnapsack;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.ParameterException;
import org.cloudbus.cloudsim.SanStorage;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;

public class MyStorage {

	/** a list storing the names of all the files on the harddrive. */
	private static List<String> nameList;

	/** a list storing all the files stored on the harddrive. */
	private static List<File> fileList;

	/** the name of the harddrive. */
	static String name;

	/** a generator required to randomize the seek time. */
	private static ContinuousDistribution gen;

	/** the current size of files on the harddrive. */
	static double currentSize;

	/** the total capacity of the harddrive in MB. */
	static double capacity;

	private static double maxTransferRate;
	 
	/** the latency of the harddrive in seconds. */
	static	private double latency;

	/** the average seek time in seconds. */
	static	private double avgSeekTime;

	public MyStorage(double storage) {
		MyStorage.capacity = storage;
		init();
	}

	private void init() {
		fileList = new ArrayList<File>();
		nameList = new ArrayList<String>();
		gen = null;
		currentSize = 0;
		latency = 0.00417;     // 4.17 ms in seconds
		avgSeekTime = 0.009;   // 9 ms
		maxTransferRate = 133;
	}	
	
	/**
	 * 
	 * @param file
	 * @return
	 */
	public static  double addFile(File file) {
		double result = 0.0;
		if (!isFileValid(file, "addFile()")) {
			return result;
		}
		// check the capacity				
				if (file.getSize() + currentSize > capacity) {
					Log.printLine(name + ".addFile(): Warning - not enough space" + " to store " + file.getName());
					return result;
				}
				// check if the same file name is alredy taken
				if (!contains(file.getName())) {
					double transferTime = getTransferTime(file.getSize());
					double seekTime = getSeekTime(file.getSize());
					fileList.add(file);               // add the file into the HD
					nameList.add(file.getName());     // add the name to the name list
					currentSize += file.getSize();    // increment the current HD size
					result = seekTime + transferTime;  // add total time
				}
				file.setTransactionTime(result);
				return result;
	}
	/**
	 * 
	 * @param fileSize
	 * @return
	 */
	public static double getTransferTime(double fileSize) {
		double result = 0.0;
		if (fileSize > 0 && capacity != 0) {
			result = (fileSize * maxTransferRate) / capacity;
		}
		return result;
	}

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public static  boolean contains(String filename) {
		boolean result = false;
		if (filename == null || filename.length() == 0) {
			Log.printLine(filename + ".contains(): Warning - invalid file name");
			return result;
		}
		// check each file in the list
		Iterator<String> it = nameList.iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (name.equals(filename)) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param rate
	 * @return
	 */
	public boolean setMaxTransferRate(int rate) {
		if (rate <= 0) {
			return false;
		}
		maxTransferRate = rate;
		return true;
	}
	
	/**
	 * Check if the file is valid or not. This method checks whether the given file or the file name
	 * of the file is valid. The method name parameter is used for debugging purposes, to output in
	 * which method an error has occured.
	 * 
	 * @param file the file to be checked for validity
	 * @param methodName the name of the method in which we check for validity of the file
	 * @return <tt>true</tt> if the file is valid, <tt>false</tt> otherwise
	 */
	 static boolean isFileValid(File file, String methodName) {

		if (file == null) {
			Log.printLine(name + "." + methodName + ": Warning - the given file is null.");
			return false;
		}

		String fileName = file.getName();
		if (fileName == null || fileName.length() == 0) {
			Log.printLine(name + "." + methodName + ": Warning - invalid file name.");
			return false;
		}

		return true;
	}

	 /**
	  * 
	  * @param fileName
	  * @return
	  */
		public  File getFile(String fileName) {
			// check first whether file name is valid or not
			File obj = null;
			if (fileName == null || fileName.length() == 0) {
				Log.printLine(name + ".getFile(): Warning - invalid " + "file name.");
				return obj;
			}

			Iterator<File> it = fileList.iterator();
			int size = 0;
			int index = 0;
			boolean found = false;
			File tempFile = null;

			// find the file in the disk
			while (it.hasNext()) {
				tempFile = it.next();
				size += tempFile.getSize();
				if (tempFile.getName().equals(fileName)) {
					found = true;
					obj = tempFile;
					break;
				}

				index++;
			}

			// if the file is found, then determine the time taken to get it
			if (found) {
				obj = fileList.get(index);
				double seekTime = getSeekTime(size);
				double transferTime = getTransferTime(obj.getSize());

				// total time for this operation
				obj.setTransactionTime(seekTime + transferTime);
			}
			return obj;
		}
		/**
		 * Get the seek time for a file with the defined size. Given a file size in MB, this method
		 * returns a seek time for the file in seconds.
		 * 
		 * @param d the size of a file in MB
		 * @return the seek time in seconds
		 */
		private static double getSeekTime(double d) {
			double result = 0;

			if (gen != null) {
				result += gen.sample();
			}

			if (d > 0 && capacity != 0) {
				result += (d / capacity);
			}

			return result;
		}
}

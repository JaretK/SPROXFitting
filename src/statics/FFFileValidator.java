package statics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility class to validate a SPROX file or Denaturants File
 * 
 * @author jkarnuta
 *
 */
public class FFFileValidator {

	/**
	 * For CSV file, return true if ends in .csv
	 * 
	 * @param file
	 * @return true iff the file ends in ".csv" (quotes for clarity)
	 */
	private static boolean validateCSV(File file) {
		String path = file.getAbsolutePath();
		return path.substring(path.length() - 4).equals(".csv");
	}

	/**
	 * 
	 * @param file
	 *            SPROX file to check
	 * @return Returns int == -1 if invalid Returns int = beginning of data > 0
	 *         if valid
	 */
	public static int VALIDATE_SPROX(File file) throws IOException {
		boolean validCSV = validateCSV(file);
		String line;
		int count = 0;
		BufferedReader br = new BufferedReader(new FileReader(file));
		while ((line = br.readLine()) != null) {
			String[] arr = line.split(",");
			if (arr[0].toLowerCase().contains("sequence") && validCSV) {
				br.close();
				return count;
			} else {
				count++;
			}
		}
		br.close();
		return -1;
	}

	/**
	 * 
	 * @param file
	 * @return true if denaturant file is the correct length (only one line)
	 */
	public static boolean VALIDATE_DENATURANTS(File file) throws IOException {
		boolean validCSV = validateCSV(file);
		BufferedReader br = new BufferedReader(new FileReader(file));
		br.readLine();
		boolean isOneLine = (br.readLine() == null);
		br.close();
		return isOneLine && validCSV;
	}
}

package org.healthnlp.deepphe.uima.drools.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocationHistogram {
	
	private Set<String> rowSet;
	private String prefix;

	public static final String BODYSITE_HEADING = "BodySite";
	public static final String LATERALITY_HEADING = "Laterality";
	public static final String COUNT_HEADING = "Count_Mentions";
	public static final String TUMOR_TYPE_HEADING = "TumorType";

	private static final String HEADER = BODYSITE_HEADING + "|" + LATERALITY_HEADING + "|" + COUNT_HEADING + "|" + TUMOR_TYPE_HEADING;
	private final String FILE_NAME_ENDING = "_Histogram.bsv";
	
	static private final String DEBUG_DIR = "../../histogram/";
	
	static private final String I = "|";    // separates fields
	static private final String NL = "\n";
	
	public static Map<String, LocationHistogram> patientHistMap = new HashMap<String, LocationHistogram>();
	public static String RAW = "_RAW";
	public static String MERGED_TUMOR = "_MERGED_TUMOR";
	
	public LocationHistogram(String prefix) {
		rowSet = new HashSet<String>();
		this.prefix = prefix;
	}
	
	public static String getKeyWord(String domain, String patientName, String namePrefix) {
		return domain+"_"+patientName+namePrefix;
	}
	
	public static LocationHistogram getHist(String domain, String patientName, String namePrefix) {
		String key = getKeyWord(domain, patientName, namePrefix);
		LocationHistogram toret = patientHistMap.get(key);
		if(toret == null) {
			toret = new LocationHistogram(key);
			patientHistMap.put(key, toret);
		}
		
		return toret;
	}
	
	public void addRow(String location, String laterality, int num, String tType) {
		rowSet.add(location+I+laterality+I+num+I+tType);
		
	}
	
	public void writeAll() {
		try ( Writer writer = createHistWriter( prefix ) ) {
			writer.write( HEADER + NL );
		    for ( String line : rowSet ) {
		    	writer.write(line + NL);
		    }
		 } catch ( IOException ioE ) {
		    ioE.printStackTrace();
		 }

	}
	
	private Writer createHistWriter( final String prefix ) throws IOException {
		 File directory = new File(DEBUG_DIR);
		    if (! directory.exists())
		        directory.mkdir();
			
	      String filename = DEBUG_DIR + prefix + FILE_NAME_ENDING;
	      final File file = new File( filename );
	      return  new BufferedWriter(new FileWriter(filename));
	   }

}

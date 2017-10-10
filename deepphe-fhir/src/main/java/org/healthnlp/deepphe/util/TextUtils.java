package org.healthnlp.deepphe.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.tools.TextTools;

public class TextUtils {
   /**
    * This method gets a text file (HTML too) from input stream
    * from given map
    *
    * @param InputStream text input
    * @return String that was produced
    * @throws IOException if something is wrong
    *                     WARNING!!! if you use this to read HTML text and want to put it somewhere
    *                     you should delete newlines
    */
   public static String getText( InputStream in ) throws IOException {
      StringBuffer strBuf = new StringBuffer();
      BufferedReader buf = new BufferedReader( new InputStreamReader( in ) );
      try {
         for ( String line = buf.readLine(); line != null; line = buf.readLine() ) {
            strBuf.append( line.trim() + "\n" );
         }
      } catch ( IOException ex ) {
         throw ex;
      } finally {
         buf.close();
      }
      return strBuf.toString();
   }


   /**
    * parse Date String
    *
    * @param dateString
    * @return
    */

   public static Date parseDateString( String dateString ) {
      return TextTools.parseDate( dateString );
      /*try {
         if(dateString == null){
				return null;
			}
			
			//Cleanup and trim excess of string
			dateString = dateString.trim();
			if(dateString.length()>13)
				dateString = dateString.substring(0,13);
			
			if (dateString.length() < 8) {
				return null;
			}

			
			SimpleDateFormat dateOnly = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat dateAndTime = new SimpleDateFormat("yyyyMMdd HHmm");
			
			SimpleDateFormat df = dateOnly;
			if (dateString.length() > 8) {
				df = dateAndTime;
			}
			return df.parse(dateString);
		} catch (ParseException e) {
			return null;
		}*/
   }


   public static double parseValue( String value ) {
      double val = 0;
      try {
         val = Double.parseDouble( value );
      } catch ( NumberFormatException ex ) {
         ex.printStackTrace();
      }
      return val;
   }

   public static List<Double> parseValues( String value ) {
      Pattern pt = Pattern.compile( "(\\d*\\.\\d+|\\d+)" );
      Matcher mt = pt.matcher( value );
      List<Double> values = new ArrayList<Double>();
      while ( mt.find() ) {
         try {
            values.add( Double.parseDouble( mt.group() ) );
         } catch ( NumberFormatException ex ) {
            ex.printStackTrace();
         }
      }
      return values;
   }


   /**
    * strip suffix
    *
    * @param name
    * @return
    */
   public static String stripSuffix( String name ) {
      int x = name.lastIndexOf( '.' );
      return x > -1 ? name.substring( 0, x ) : name;
   }

}

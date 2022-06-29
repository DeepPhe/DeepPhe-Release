package org.healthnlp.deepphe.util;

import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {6/2/2022}
 */
final public class TopoMajorLister {

   private TopoMajorLister() {

   }


   public static void main( String[] args ) {
      final File topoHistoFile = new File( "src/main/resources/org/healthnlp/deepphe/icdo/DpheHistologySites.bsv" );
      final File topoFile = new File( "src/main/resources/org/healthnlp/deepphe/icdo/DpheMajorSites.bsv" );
      final Map<String,String> topoMajorMap = new HashMap<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( topoHistoFile ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            if ( line.isEmpty() ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            topoMajorMap.put( splits[ 0 ].substring( 0, 3 ), splits[ 1 ] );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         //
      }
      final Collection<String> codes = topoMajorMap.keySet().stream().sorted().collect( Collectors.toList() );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( topoFile ) ) ) {
         for ( String code : codes ) {
            writer.write( code + "|" + topoMajorMap.get( code ) + "\n" );
         }
      } catch ( IOException ioE ) {
         //
      }
   }



}

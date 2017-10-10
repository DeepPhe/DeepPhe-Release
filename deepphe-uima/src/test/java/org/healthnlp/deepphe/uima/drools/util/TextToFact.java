package org.healthnlp.deepphe.uima.drools.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactFactory;


/**
 * recreates Facts from test file for testing
 *
 * @author opm1
 */
public class TextToFact {

   private static char F = File.separatorChar;
   private static final String TEST_DROOLS_DIR = System.getProperty( "user.dir" ) + F + "testdroolsdata" + F;

   public static List<Fact> factsFromFile( String fileSubPath ) {

      List<Fact> fList = new ArrayList<Fact>();
      BufferedReader br = null;

      try {

         String sCurrentLine;

         br = new BufferedReader( new FileReader( TEST_DROOLS_DIR + fileSubPath ) );

         while ( (sCurrentLine = br.readLine()) != null ) {
            if ( sCurrentLine.length() > 0 ) {
               Fact f = strToFact( sCurrentLine );
               fList.add( f );
               System.out.println( "STR:  " + sCurrentLine );
               System.out.println( "FACT: " + f.getInfo() );
            }
         }

      } catch ( IOException e ) {
         e.printStackTrace();
      } finally {
         try {
            if ( br != null ) br.close();
         } catch ( IOException ex ) {
            ex.printStackTrace();
         }
      }

      return fList;

   }

   public static Fact strToFact( String strFact ) {
      Map<String, String> map = new HashMap<String, String>();
      String[] pair = strFact.split( "\\|" );

      for ( String s : pair ) {
         String[] kv = s.split( ": " );
         System.out.println( "kv[0]: " + kv[ 0 ] + "    ***   kv[1]: " + kv[ 1 ] );

         map.put( kv[ 0 ], kv[ 1 ] );
         kv = null;
      }
      pair = null;
      System.out.println( "-" + map.get( "uri" ) + "-" );
      Fact f = FactFactory.createFact( map.get( "type" ), map.get( "uri" ) );
      f.setCategory( map.get( "category" ) );
      f.setIdentifier( map.get( "id" ) );
      f.setPatientIdentifier( map.get( "patient id" ) );
      f.setDocumentIdentifier( map.get( "document id" ) );
      f.setDocumentTitle( map.get( "document title" ) );
      String typoDocType = "document type";
      if ( map.get( typoDocType ) == null )
         typoDocType = "document tyoe";
      f.setDocumentType( map.get( typoDocType ) );


      List<String> containers = stringToList( map.get( "container ids" ) );
      if ( containers != null ) {
         for ( String s : containers )
            f.addContainerIdentifier( s );
         //containers.clear(); containers = null;
      }

      List<String> ancestors = stringToList( map.get( "ancestors" ) );
      if ( ancestors != null ) {
         for ( String a : ancestors )
            f.addAncestor( a );
         //ancestors.clear(); ancestors = null;
      }

      return f;

   }

   public static List<String> stringToList( String str ) {
      if ( str.startsWith( "[" ) )
         str = str.substring( 1 );
      if ( str.endsWith( "]" ) )
         str = str.substring( 0, str.length() - 1 );
      List<String> items = Arrays.asList( str.split( "\\s*,\\s*" ) );
      return items;
   }

   public static void main( String[] args ) {
      String fPath = "brca/tnm/M1data.txt";
      factsFromFile( fPath );
   }

}

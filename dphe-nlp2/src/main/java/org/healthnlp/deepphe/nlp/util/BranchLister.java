package org.healthnlp.deepphe.nlp.util;

import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.healthnlp.deepphe.nlp.uri.ShortUriUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Collection;


/**
 * @author SPF , chip-nlp
 * @since {12/5/2023}
 */
final public class BranchLister {
   static private final String DATABASE = "C:/Spiffy/output/mark_6_onto/test_runs/neo4j/DeepPhe_2023_v1.db";


//   static  private final String ROOT_WORD = "Non-Neoplastic Eye Disorder";
//   static private final String ROOT_URI  = "Non_NeoplasticEyeDisorder";
static private final String ROOT_WORD = "BRCA1 Gene";
   static private final String ROOT_URI = "Abdomen";

   public static void main( String[] args ) {
      final GraphDatabaseService graphDb
            = EmbeddedConnection.getInstance().connectToGraph( DATABASE );

      String rootUri = ROOT_URI;
      if ( ROOT_WORD != null && !ROOT_WORD.isEmpty() ) {
         rootUri = ShortUriUtil.createShortUri( ROOT_WORD );
         System.out.println( "URI for " + ROOT_WORD + " is " + rootUri );
      }

      final Collection<String> rootUris = SearchUtil.getRootUris( graphDb, rootUri );
      System.out.println( "Roots for " + rootUri );
      int count = 1;
      for ( String uri : rootUris ) {
         System.out.println( count + " " + uri );
//         System.out.println( count + " " + SearchUtil.getPreferredText( graphDb, uri ) );
         count++;
      }

      System.out.println( "\n");

      final Collection<String> branchUris = SearchUtil.getBranchUris( graphDb, rootUri );
      System.out.println( "Descendants for " + rootUri );
//      final Collection<String> prefNeoplasms = new HashSet<>();
      count = 1;
      for ( String uri : branchUris ) {
         System.out.println( count + " " + uri );
//         System.out.println( count + " " + SearchUtil.getPreferredText( graphDb, uri ) );
//         prefNeoplasms.add( SearchUtil.getPreferredText( graphDb, uri ) );
         count++;
      }

//      try ( BufferedReader reader = new BufferedReader(
//            new FileReader( "C:\\Spiffy\\output\\mark_6_onto\\Onto_7_0_short\\hsqldb\\DeepPhe_2023_v1\\DeepPhe_2023.txt" ) );
//      BufferedWriter writer = new BufferedWriter(
//            new FileWriter( "C:\\Spiffy\\output\\mark_6_onto\\Onto_7_0_short\\hsqldb\\DeepPhe_2023_v1\\short.txt" ) ) ) {
//         String line = reader.readLine();
//         while ( line != null ) {
//            final String[] splits = StringUtil.fastSplit( line, '\'' );
//            if ( splits.length < 7 ) {
//               System.err.println( "bad split : " + line );
//               line = reader.readLine();
//               continue;
//            }
//            final String className = splits[ 1 ];
//            if ( prefNeoplasms.contains( className ) ) {
//               final String synonym = splits[ 5 ];
//               writer.write( className +  " | " + synonym + "\n" );
//            }
//            line = reader.readLine();
//         }
//      } catch ( IOException ioE ) {
//         System.err.println( ioE.getMessage() );
//      }
   }

}

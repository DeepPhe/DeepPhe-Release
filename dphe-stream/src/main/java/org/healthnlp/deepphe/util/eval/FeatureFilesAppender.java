package org.healthnlp.deepphe.util.eval;

import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final public class FeatureFilesAppender {

   private FeatureFilesAppender() {}


   static public void initFeatureFiles( final File featureDir,
                                        final List<String> attributeNames ) throws IOException {
      featureDir.mkdirs();
      for ( String name : attributeNames ) {
         final File file = new File( featureDir, name );
         file.delete();
         file.createNewFile();
      }
   }

   static public void appendFeatureFiles( final String patientId,
                                          final NeoplasmSummary summary,
                                          final File featureDir,
                                          final List<String> attributeNames ) {
      final String summaryId = summary.getId();
      final List<NeoplasmAttribute> attributes = summary.getAttributes();
      final Map<String, String> featureLines = createFeatureLines( attributes );
      for ( String name : attributeNames ) {
         try ( final FileWriter writer = new FileWriter( new File( featureDir, name ), true ) ) {
            final String features = featureLines.get( name );
            writer.write( patientId + "|" + summaryId + "|" );
            if ( features == null ) {
               writer.write( "\n" );
            } else {
               writer.write( features );
            }
         } catch ( IOException ioE ) {
            //
         }
      }
   }

   static private Map<String, String> createFeatureLines( final List<NeoplasmAttribute> attributes ) {
      final Map<String,String> featureLines = new HashMap<>();
      for ( NeoplasmAttribute attribute : attributes ) {
         final StringBuilder sb = new StringBuilder();
         sb.append( attribute.getValue() ).append( '|' );
         attribute.getConfidenceFeatures().forEach( i -> sb.append( i ).append( '|' ) );
         sb.append( '\n' );
         featureLines.put( attribute.getName(), sb.toString() );
      }
      return featureLines;
   }


}

package org.apache.ctakes.cancer.summary.writer;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.summary.NeoplasmCiContainer;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/9/2019
 */
abstract public class AbstractNeoplasmBsvWriter {

   static public final String PATIENT_ID = "Patient_ID";
   static public final String SUMMARY_ID = "Summary_ID";

   static protected final Object WRITE_LOCK = new Object();

   static protected final char B = '|';

   private File _file;

   public AbstractNeoplasmBsvWriter( final String outputDir ) {
      _file = createFile( outputDir );
   }

   abstract protected String getFileName();

   abstract protected Collection<String> getSummaryTypes();

   abstract protected List<String> getPropertyNames();

   abstract protected List<String> getRelationNames();

   protected String getIdHeaders() {
      final StringBuilder sb = new StringBuilder();
      sb.append( PATIENT_ID ).append( B )
        .append( "Summary_Type" ).append( B )
        .append( SUMMARY_ID ).append( B )
        .append( "Summary_URI" ).append( B );
      return sb.toString();
   }

   protected String getIdValues( final String patientId, final NeoplasmCiContainer summary ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( patientId ).append( B )
        .append( summary.getType() ).append( B )
//        .append( summary.getId() ).append( B )
        .append( summary.getWorldId() ).append( B )
        .append( summary.getUri() ).append( B );
      return sb.toString();
   }

   public File createFile( final String outputDir ) {
      try {
         new File( outputDir ).mkdirs();
         final File file = new File( outputDir + "/" + getFileName() );
         file.createNewFile();

         final StringBuilder sb = new StringBuilder();
         sb.append( getIdHeaders() );

         final Collection<String> propertyNames = getPropertyNames();
         for ( String propertyName : propertyNames ) {
            sb.append( propertyName ).append( B );
         }

         final Collection<String> relationNames = getRelationNames();
         for ( String relationName : relationNames ) {
            sb.append( relationName ).append( B );
         }
         sb.append( "\n" );

         synchronized ( WRITE_LOCK ) {
            try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
               writer.write( sb.toString() );
            }
         }
         return file;
      } catch ( IOException ioE ) {
         Logger.getLogger( getClass().getSimpleName() )
               .error( "Cannot create Neoplasm Summary Eval File: " + ioE.getMessage() );
      }
      return null;
   }

   final public void writeNeoplasm( final String patientId, final NeoplasmCiContainer summary ) throws IOException {
      if ( !getSummaryTypes().contains( summary.getType() ) ) {
         return;
      }
      final StringBuilder sb = new StringBuilder();
      sb.append( getIdValues( patientId, summary ) );

      final Collection<String> propertyNames = getPropertyNames();
      final Map<String,String> properties = summary.getProperties();
      for ( String propertyName : propertyNames ) {
         sb.append( getProperty( properties, propertyName ) ).append( B );
      }

      final Collection<String> relationNames = getRelationNames();
      final Map<String,Collection<ConceptInstance>> relations = summary.getRelations();
      for ( String relationName : relationNames ) {
         sb.append( getRelated( relations, properties, relationName ) ).append( B );
      }
      sb.append( "\n" );

      synchronized ( WRITE_LOCK ) {
         try ( Writer writer = new BufferedWriter( new FileWriter( _file, true ) ) ) {
            writer.write( sb.toString() );
         }
      }
   }

   static private String getProperty( final Map<String,String> properties, final String propertyName ) {
      final String property = properties.get( propertyName );
      if ( property == null || property.isEmpty() ) {
         return "";
      }
      return property;
   }


   static private String getRelated( final Map<String,Collection<ConceptInstance>> relations,
                                     final Map<String,String> properties,
                                     final String relationName ) {
      final Collection<ConceptInstance> related = relations.get( relationName );
      if ( related == null || related.isEmpty() ) {
         final String property = properties.get( relationName );
         if ( property != null ) {
            return property;
         }
         return "";
      }
      return related.stream()
                    .map( ConceptInstance::getUri )
                    .distinct()
                    .sorted()
                    .collect( Collectors.joining( ";" ) );
   }

}

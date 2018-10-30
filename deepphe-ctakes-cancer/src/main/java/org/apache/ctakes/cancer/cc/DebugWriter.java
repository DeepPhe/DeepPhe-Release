package org.apache.ctakes.cancer.cc;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.cc.AbstractOutputFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/21/2017
 */
@PipeBitInfo(
      name = "cTakes debug writer",
      description = "Writes to file Cancer and Tumor ConceptInstances and their Relations.",
      dependencies = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION,
      role = PipeBitInfo.Role.WRITER
)
public class DebugWriter extends AbstractOutputFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "DebugWriter" );


   private Collection<String> _cancerUris;
   private Collection<String> _tumorUris;
   private Collection<String> _metastasisUris;

   private void collectUris() {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      _cancerUris = UriConstants.getCancerUris();
      _tumorUris = UriConstants.getTumorUris();
      _metastasisUris = SearchUtil.getBranchUris( graphDb, UriConstants.METASTASIS );
      _cancerUris.removeAll( _tumorUris );
      _cancerUris.removeAll( _metastasisUris );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( JCas jCas, String outputDir, String documentId, String fileName ) throws IOException {
      File debugFile = new File( outputDir, fileName + ".CTAKES.debug.txt" );
      LOGGER.info( "Writing Cancer Debug Data to " + debugFile.getAbsolutePath() + " ..." );
      if ( _cancerUris == null ) {
         collectUris();
      }

      final StringBuilder sb = new StringBuilder();
      final Function<ConceptInstance, ConceptInstance> printPrimaryLabel = ci -> {
         sb.append( "Primary:" ).append( getPadding( getLineLength( sb ), 20 ) );
         return ci;
      };
      final Function<ConceptInstance, ConceptInstance> printTumorLabel = ci -> {
         sb.append( "Tumor:" ).append( getPadding( getLineLength( sb ), 20 ) );
         return ci;
      };
      final Function<ConceptInstance, ConceptInstance> printMetastasisLabel = ci -> {
         sb.append( "Metastasis:" ).append( getPadding( getLineLength( sb ), 20 ) );
         return ci;
      };
      final Function<ConceptInstance, ConceptInstance> printSizeLabel = ci -> {
         sb.append( "Tumor Size:" ).append( getPadding( getLineLength( sb ), 20 ) );
         return ci;
      };
      final Function<ConceptInstance, ConceptInstance> printTnmLabel = ci -> {
         sb.append( "TNM:" ).append( getPadding( getLineLength( sb ), 20 ) );
         return ci;
      };
      final Function<ConceptInstance, ConceptInstance> printStageLabel = ci -> {
         sb.append( "Stage:" ).append( getPadding( getLineLength( sb ), 20 ) );
         return ci;
      };
      final Function<ConceptInstance, ConceptInstance> printStatusLabel = ci -> {
         sb.append( "Receptor Status:" ).append( getPadding( getLineLength( sb ), 20 ) );
         return ci;
      };

      final Function<ConceptInstance, ConceptInstance> printConceptText = ci -> {
         final String uri = ci.getUri();
         sb.append( uri.substring( uri.indexOf( '#' ) + 1 ) )
               .append( getPadding( getLineLength( sb ), 50 ) ).append( "\"" ).append( ci.getCoveredText().replaceAll( "\r?\n", " " ).trim() )
               .append( "\"\r\n" );
         return ci;
      };

      final Function<Map.Entry<String, Collection<ConceptInstance>>, String> getRelationTexts = e -> {
         final StringBuilder sb2 = new StringBuilder();
         e.getValue().forEach( ci -> sb2.append( "  " ).append( e.getKey() ).append( ":" ).append( getPadding( sb2.length(), 20 ) )
               .append( ci.getUri().substring( ci.getUri().indexOf( '#' ) + 1 ) ).append( getPadding( sb2.length(), 50 ) )
               .append( "\"" ).append( ci.getCoveredText().replaceAll( "\r?\n", " " ).trim() ).append( "\"\r\n" ) );
         return sb2.toString();
      };


      final Predicate<Map.Entry<String, Collection<ConceptInstance>>> filterCategory
            = e -> !e.getKey().contains( "Metastasis" )
            && !e.getKey().contains( "Diagnosis" )
            && !e.getKey().contains( "Method" )
            && !e.getKey().contains( "Extent" )
            && !e.getKey().contains( "ReasonForUse" );

      final Map<String,Collection<ConceptInstance>> uriConceptInstances = ConceptInstanceFactory.createUriConceptInstanceMap( jCas );

      final Writer writer = new BufferedWriter( new FileWriter( debugFile ) );
      writer.write( "DOCUMENT : " + DocumentIDAnnotationUtil.getDocumentID( jCas ) + "\r\n\r\n" );
      writer.write( sb.toString() );
      writer.close();

      LOGGER.info( "Finished." );
   }

   /**
    * @param sb -
    * @return length of last line in builder
    */
   static private int getLineLength( final StringBuilder sb ) {
      final int index = sb.lastIndexOf( "\r\n" );
      if ( index < 0 ) {
         return sb.length();
      }
      return sb.length() - index - 2;
   }

   /**
    * 20, 75
    *
    * @param currentLength -
    * @param wantedLength  -
    * @return -
    */
   static private String getPadding( final int currentLength, final int wantedLength ) {
      if ( wantedLength <= currentLength ) {
         return " ";
      }
      final char[] chars = new char[ wantedLength - currentLength ];
      Arrays.fill( chars, ' ' );
      return new String( chars );
   }


}

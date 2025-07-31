package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.neo4j.constant.UriConstants2;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;


/**
 * @author SPF , chip-nlp
 * @since {3/28/2024}
 */
@PipeBitInfo (
      name = "UAB Patient Recurrence Table Writer",
      description = "Writes a table of Patient recurrence information to file.",
      role = PipeBitInfo.Role.WRITER
)
public class UabRecurrenceTableWriter extends UabPatientTableWriter {

   static private final List<String> BASIC_LABEL = Arrays.asList( "", "", "Basic", "", "", "", "", "", "", "", "" );
   static private final List<String> RECURRENT_LABEL = Arrays.asList( "", "", "Recur", "", "", "", "", "", "", "", "" );
   static private final List<String> REFRACTORY_LABEL =
         Arrays.asList( "", "", "Refract", "", "", "", "", "", "", "", "" );
   static private final List<String> REMISSION_LABEL =
         Arrays.asList( "", "", "Remiss", "", "", "", "", "", "", "", "" );
   static private final List<String> REPORTABLE_LABEL =
         Arrays.asList( "", "", "Report", "", "", "", "", "", "", "", "" );

   static private List<String> HEADER = Arrays.asList( " URI ", " CUI ", " TUI ", " Group ", " Type ", " Pref. Text ",
         " Negated ", " Uncertain ", " Historic ", " Mentions ", " Confidence " );


   static private final String RECURRENT = "Recurrent";
   static private final String REFRACTORY = "Refractory";
   static private final String REMISSION = "Remission";

   static private final String RECURRENT_DISEASE = "RecurrentDisease";
   static private final String RECURRENT_CANCER = "RecurrentNeoplasm";
   static private final String REFRACTORY_CANCER = "RefractoryNeoplasm";

   private final Collection<String> _basicUris = new HashSet<>();
   private final Collection<String> _recurrenceUris = new HashSet<>();
   private final Collection<String> _refractoryUris = new HashSet<>();
   private final Collection<String> _remissionUris = new HashSet<>();
   private final Collection<String> _reportableUris = new HashSet<>();


   /**
    * Save the time from the beginning of the run.
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _basicUris.add( RECURRENT );
      _basicUris.add( REFRACTORY );
      _basicUris.add( REMISSION );
      // Add mentions of recurrence, such as "Relapse"
      _basicUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( RECURRENT_DISEASE ) );
      // Add all cancers that are in the branch holding recurrent cancers.
      _recurrenceUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( RECURRENT_CANCER ) );
      // Add all cancers that are in the branch holding refractory cancers.
      _refractoryUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( REFRACTORY_CANCER ) );
      // Kludgy.  Add cancers that have "Remission" in their URI.
      Neo4jOntologyConceptUtil.getBranchUris( "Neoplasm" )
                              .stream()
                              .filter( u -> u.contains( REMISSION ) )
                              .filter( u -> !u.equals( REMISSION ) )
                              .forEach( _remissionUris::add );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      _reportableUris.addAll( UriConstants2.getReportableUris( graphDb ) );

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      final List<List<String>> allRows = super.createDataRows( jCas );
      if ( allRows.isEmpty() ) {
         // The patient isn't done or is empty.
         return allRows;
      }
      final List<List<String>> myRows = new ArrayList<>();
      //Stream through all the rows (from super.createDataRows) and pull out the ones that we want.
//      allRows.stream()
//                  .filter( r -> _recurrenceUris.contains( r.get( 0 ).toLowerCase() ) )
//                  .forEach( myRows::add );
//      allRows.stream()
//                  .filter( r -> _refractoryUris.contains( r.get( 0 ).toLowerCase() ) )
//             .forEach( myRows::add );
//      allRows.stream()
//                  .filter( r -> _remissionUris.contains( r.get( 0 ).toLowerCase() ) )
//             .forEach( myRows::add );
//      allRows.stream()
//             .filter( r -> _basicUris.contains( r.get( 0 ).toLowerCase() ) )
//             .forEach( myRows::add );
//      allRows.stream()
//             .filter( r -> _reportableUris.contains( r.get( 0 ).toLowerCase() ) )
//             .forEach( myRows::add );
      myRows.add( RECURRENT_LABEL );
      allRows.stream()
             .filter( r -> _recurrenceUris.contains( r.get( 0 ) ) )
             .forEach( myRows::add );
      myRows.add( REFRACTORY_LABEL );
      allRows.stream()
             .filter( r -> _refractoryUris.contains( r.get( 0 ) ) )
             .forEach( myRows::add );
      myRows.add( REMISSION_LABEL );
      allRows.stream()
             .filter( r -> _remissionUris.contains( r.get( 0 ) ) )
             .forEach( myRows::add );
      myRows.add( BASIC_LABEL );
      allRows.stream()
             .filter( r -> _basicUris.contains( r.get( 0 ) ) )
             .forEach( myRows::add );
      myRows.add( REPORTABLE_LABEL );
      allRows.stream()
             .filter( r -> _reportableUris.contains( r.get( 0 ) ) )
             .forEach( myRows::add );
      return myRows;
   }

   /**
    * @return a filename for the table made from the patient ID and the startup time, plus "_Recurrence".
    */
   @Override
   protected String getFilename() {
      return super.getFilename() + "_Recurrence";
   }


}

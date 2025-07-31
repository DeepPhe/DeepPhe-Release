package org.healthnlp.deepphe.neo4j.constant;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author SPF , chip-nlp
 * @since {11/12/2023}
 */
final public class UriConstants2 {

   private UriConstants2() {}

   static public final String PATIENT_CONCEPT_SCHEMA = "DeepPheXn";

   static public final String BREAST_QUADRANT = "BreastQuadrant";
   static public final Collection<String> QUADRANT_URIS = new HashSet<>();
   static public Collection<String> getBreastQuadrants( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return QUADRANT_URIS;
   }

   static private final Collection<String> LEFT_LUNG_LOBES = new HashSet<>();
   static public Collection<String> getLeftLungLobes( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return LEFT_LUNG_LOBES;
   }

   static private final Collection<String> RIGHT_LUNG_LOBES = new HashSet<>();

   static public Collection<String> getRightLungLobes( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return RIGHT_LUNG_LOBES;
   }


   static private final Collection<String> REPORTABLE_URIS = new HashSet<>();

   static public Collection<String> getReportableUris( final GraphDatabaseService graphDb ) {
      initializeUris( graphDb );
      return REPORTABLE_URIS;
   }


   static private final Object URI_LOCK = new Object();

   static private void initializeUris( final GraphDatabaseService graphDb ) {
      synchronized ( URI_LOCK ) {
         if ( !QUADRANT_URIS.isEmpty() ) {
            return;
         }
         QUADRANT_URIS.addAll( SearchUtil.getBranchUris( graphDb, BREAST_QUADRANT ) );
         QUADRANT_URIS.remove( BREAST_QUADRANT );
//         QUADRANT_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Nipple" ) );
//         QUADRANT_URIS.addAll( SearchUtil.getBranchUris( graphDb, "Areola" ) );
//         QUADRANT_URIS.addAll( SearchUtil.getBranchUris( graphDb, "CentralPortionOfTheBreast" ) );
//         QUADRANT_URIS.addAll( SearchUtil.getBranchUris( graphDb, "SubareolarRegion" ) );
//         QUADRANT_URIS.addAll( SearchUtil.getBranchUris( graphDb, "AxillaryTailOfTheBreast" ) );

         final Collection<String> lungLobes = SearchUtil.getBranchUris( graphDb, "LungLobe" );
         lungLobes.stream().filter( u -> u.contains( "Left" ) ).forEach( LEFT_LUNG_LOBES::add );
         lungLobes.stream().filter( u -> u.contains( "Right" ) ).forEach( RIGHT_LUNG_LOBES::add );

         try ( Transaction tx = graphDb.beginTx() ) {
            final ResourceIterator<Node> nodes = graphDb.findNodes( Label.label( "Reportable" ) );
            nodes.stream()
                 .map( n -> n.getProperty( Neo4jConstants.NAME_KEY ) )
                 .filter( Objects::nonNull )
                 .map( Object::toString )
                 .forEach( REPORTABLE_URIS::add );
            nodes.close();
            tx.success();
         } catch ( MultipleFoundException mfE ) {
            Logger.getLogger( "UriConstants2" ).error( mfE.getMessage(), mfE );
         }
      }
   }



}

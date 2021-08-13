package org.healthnlp.deepphe.summary.concept.bin;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @since {6/9/2021}
 */
enum SiteType {
//   ORGAN( DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
//          DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
//          DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
//          Finding_Has_Associated_Site ),
//   REGION( Disease_Has_Associated_Region,
//           Disease_Has_Associated_Cavity,
//           Disease_Has_Associated_System,
//           Finding_Has_Associated_Region,
//           Finding_Has_Associated_Cavity,
//           Finding_Has_Associated_System ),
   ALL_SITES( DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
              DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
              DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
              Finding_Has_Associated_Site,
              Disease_Has_Associated_Region,
              Disease_Has_Associated_Cavity,
              Disease_Has_Associated_System,
              Finding_Has_Associated_Region,
              Finding_Has_Associated_Cavity,
              Finding_Has_Associated_System ),
   NO_SITE;

   final Collection<String> _relationTypes;
   SiteType( final String... relationTypes ) {
      _relationTypes = Arrays.stream( relationTypes )
                             .collect( Collectors.toSet() );
   }

   Collection<String> getMatchingTypes( final Collection<String> relationTypes ) {
      final Collection<String> matches = new HashSet<>( relationTypes );
      matches.retainAll( _relationTypes );
      return matches;
   }


   Collection<String> getMatchingSiteUris( final Map<String,Collection<Mention>> relations ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
      final Collection<String> validLocations = new HashSet<>( UriConstants.getLocationUris( graphDb ) );
      // TODO - Move this to URI Constants.
      validLocations.removeAll( Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT ) );
      return relations.entrySet()
                      .stream()
                      .filter( e -> _relationTypes.contains( e.getKey() ) )
                      .map( Map.Entry::getValue )
                      .flatMap( Collection::stream )
                      .map( Mention::getClassUri )
                      .filter( validLocations::contains )
                      .collect( Collectors.toSet() );
   }


   boolean hasSiteType( final Collection<String> relationTypes ) {
      return relationTypes.stream().anyMatch( _relationTypes::contains );
   }

   static boolean isNoSiteType( final Collection<String> relationTypes ) {
      return ALL_SITES.hasSiteType( relationTypes );
   }

   static SiteType getSiteType( final Collection<String> relationTypes ) {
      if ( relationTypes == null || relationTypes.isEmpty() ) {
         return NO_SITE;
      }
      if ( ALL_SITES.hasSiteType( relationTypes ) ) {
         return ALL_SITES;
      }
      return NO_SITE;
   }

}

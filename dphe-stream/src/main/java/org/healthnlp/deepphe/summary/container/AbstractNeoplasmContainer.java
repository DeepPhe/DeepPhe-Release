package org.healthnlp.deepphe.summary.container;

import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2020
 */
public abstract class AbstractNeoplasmContainer {


//   abstract public Collection<ConceptAggregate> getRelated( final String relationName );

   abstract protected Collection<String> getRelatedUris( final String relationName ); // {
//      return getRelated( relationName ).stream().map( ConceptAggregate::getUri ).collect( Collectors.toSet() );
//   }

   abstract public ConceptAggregate createMergedConcept( final Collection<ConceptAggregate> conceptAggregates  );

   protected Collection<String> getRelatedSiteUris( final Map<String,Collection<String>> relatedUris ) {
      return relatedUris.entrySet()
                        .stream()
                        .filter( e -> RelationConstants.isHasSiteRelation( e.getKey() ) )
                        .map( Map.Entry::getValue)
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
   }

   boolean hasRelated( final String relationName, final ConceptAggregate neoplasm ) {
      return hasRelated( relationName, neoplasm.getRelatedConceptMap().get( relationName ) );
   }

   boolean hasRelated( final String relationName, final Collection<ConceptAggregate> relations ) {
      final boolean theyEmpty = relations == null || relations.isEmpty();
      final Collection<String> relatedUris = getRelatedUris( relationName );
      final boolean weEmpty = relatedUris == null || relatedUris.isEmpty();
      if ( theyEmpty || weEmpty ) {
         // It is a match if both relation sets are empty.
         return theyEmpty && weEmpty;
      }
      for ( ConceptAggregate relation : relations ) {
         final String relationUri = relation.getUri();
         if ( UriUtil.isUriBranchMatch( Collections.singletonList( relationUri ), relatedUris ) ) {
            return true;
         }
      }
      return false;
   }

   void updateAllInstances( final Collection<ConceptAggregate> allInstances,
                            final Collection<ConceptAggregate> mergedNeoplasms,
                            final ConceptAggregate newNeoplasm ) {
      allInstances.add( newNeoplasm );
      for ( ConceptAggregate instance : allInstances ) {
         if ( mergedNeoplasms.contains( instance ) ) {
            continue;
         }
         final Map<String, Collection<ConceptAggregate>> oldRelated = instance.getRelatedConceptMap();
         for ( Map.Entry<String, Collection<ConceptAggregate>> oldRelations : oldRelated.entrySet() ) {
            if ( oldRelations.getValue().removeAll( mergedNeoplasms ) ) {
               oldRelations.getValue().add( newNeoplasm );
            }
         }
      }
      allInstances.removeAll( mergedNeoplasms );


//      mergedNeoplasms.forEach( n -> Logger.getLogger( "AbstractNeoplasmContainer" ).info( "Merged " + n.getUri() + " " + n.getId() ) );

   }


}

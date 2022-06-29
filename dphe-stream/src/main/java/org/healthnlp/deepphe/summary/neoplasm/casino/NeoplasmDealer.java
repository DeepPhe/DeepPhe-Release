package org.healthnlp.deepphe.summary.neoplasm.casino;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateHandler;
import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;

import java.util.*;

import static org.healthnlp.deepphe.summary.neoplasm.casino.SiteTable.ADJUSTED_SITE_RELATION;

/**
 * @author SPF , chip-nlp
 * @since {6/22/2022}
 */
final class NeoplasmDealer {



   /**
    * For all the neoplasms with an undetermined (or minority) site and nos histology,
    * distribute all the relations except for sites.
    * @param patientId -
    * @param undeterminedNosNeoplasms -
    * @param bestSiteNeoplasmsMap -
    */
   static void relateUndeterminedSiteNos( final String patientId,
                                                  final Collection<ConceptAggregate> undeterminedNosNeoplasms,
                                                  final Map<String,Collection<ConceptAggregate>> bestSiteNeoplasmsMap,
                                                  final Collection<ConceptAggregate> allConcepts ) {
      final Map<String,Collection<ConceptAggregate>> adjustedRelations = new HashMap<>();
      for ( ConceptAggregate neoplasm : undeterminedNosNeoplasms ) {
         final Map<String,Collection<ConceptAggregate>> relatedConceptMap = neoplasm.getRelatedConceptMap();
         for ( Map.Entry<String,Collection<ConceptAggregate>> relatedConcepts : relatedConceptMap.entrySet() ) {
            final String name = RelationConstants.isHasSiteRelation( relatedConcepts.getKey() )
                                ? ADJUSTED_SITE_RELATION : relatedConcepts.getKey();
            adjustedRelations.computeIfAbsent( name, r -> new HashSet<>() )
                             .addAll( relatedConcepts.getValue() );
         }
      }
      for ( Map.Entry<String,Collection<ConceptAggregate>> relations : adjustedRelations.entrySet() ) {
         bestSiteNeoplasmsMap.values().stream()
                             .flatMap( Collection::stream )
                             .forEach( n -> n.addRelated( relations.getKey(), relations.getValue() ) );
      }
      final ConceptAggregate undeterminedNosNeoplasm
            = createMergedNeoplasm( patientId, undeterminedNosNeoplasms, Collections.emptyList() );
      bestSiteNeoplasmsMap.values().forEach( c -> c.add( undeterminedNosNeoplasm ) );
      replaceRelated( undeterminedNosNeoplasm, undeterminedNosNeoplasms, allConcepts );
      allConcepts.removeAll( undeterminedNosNeoplasms );
      allConcepts.add( undeterminedNosNeoplasm );
   }


   static void replaceRelated( final ConceptAggregate replacement,
                               final Collection<ConceptAggregate> toReplace,
                               final Collection<ConceptAggregate> allConcepts ) {
      for ( ConceptAggregate concept : allConcepts ) {
         final Map<String, Collection<ConceptAggregate>> relationsMap = concept.getRelatedConceptMap();
         for ( Map.Entry<String, Collection<ConceptAggregate>> relations : relationsMap.entrySet() ) {
            if ( relations.getValue().removeAll( toReplace ) ) {
               relations.getValue().add( replacement );
            }
         }
      }
   }





   static private Map<String,Collection<String>> collectMergingUriRoots(
         final Collection<ConceptAggregate> mergingSiteNeoplasms ) {
      final Map<String,Collection<String>> newNeoplasmUriRoots = new HashMap<>();
      mergingSiteNeoplasms.stream()
                          .map( ConceptAggregate::getUriRootsMap )
                          .map( Map::entrySet )
                          .flatMap( Collection::stream )
                          .filter( e -> !newNeoplasmUriRoots.containsKey( e.getKey() ) )
                          .forEach( e -> newNeoplasmUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() )
                                                            .addAll( e.getValue() ) );
      return newNeoplasmUriRoots;
   }




   static private Map<String,Collection<String>> collectMergingUriRoots(
         final Collection<ConceptAggregate> mergingSiteNeoplasms,
         final Collection<ConceptAggregate> unwantedSiteNeoplasms ) {
      final Map<String,Collection<String>> newNeoplasmUriRoots = new HashMap<>();
      mergingSiteNeoplasms.stream()
                          .map( ConceptAggregate::getUriRootsMap )
                          .map( Map::entrySet )
                          .flatMap( Collection::stream )
                          .filter( e -> !newNeoplasmUriRoots.containsKey( e.getKey() ) )
                          .forEach( e -> newNeoplasmUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() )
                                                            .addAll( e.getValue() ) );
      unwantedSiteNeoplasms.stream()
                           .map( ConceptAggregate::getUriRootsMap )
                           .map( Map::entrySet )
                           .flatMap( Collection::stream )
                           .filter( e -> !newNeoplasmUriRoots.containsKey( e.getKey() ) )
                           .forEach( e -> newNeoplasmUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() )
                                                             .addAll( e.getValue() ) );
//      undeterminedSiteNosNeoplasms.stream()
//                           .map( ConceptAggregate::getUriRootsMap )
//                           .map( Map::entrySet )
//                           .flatMap( Collection::stream )
//                           .forEach( e -> siteUriRoots.computeIfAbsent( e.getKey(), s -> new HashSet<>() )
//                           .addAll( e.getValue() ) );
      return newNeoplasmUriRoots;
   }

   static ConceptAggregate createMergedNeoplasm( final String patientId,
                                                 final Collection<ConceptAggregate> mergingSiteNeoplasms,
                                                 final Collection<ConceptAggregate> unwantedSiteNeoplasms ) {
      final Map<String,Collection<String>> newNeoplasmUriRoots = collectMergingUriRoots( mergingSiteNeoplasms,
                                                                                         unwantedSiteNeoplasms );
      final Map<String, Collection<Mention>> newNeoplasmDocMentions
            = ConceptAggregateHandler.collectDocMentions( mergingSiteNeoplasms );
      ConceptAggregateHandler.appendDocMentions( unwantedSiteNeoplasms, newNeoplasmDocMentions );
      return new DefaultConceptAggregate( patientId, newNeoplasmUriRoots, newNeoplasmDocMentions );
   }


   static ConceptAggregate createMergedNeoplasm( final String patientId,
                                                 final Collection<ConceptAggregate> mergingNeoplasms ) {
      final Map<String,Collection<String>> newNeoplasmUriRoots = collectMergingUriRoots( mergingNeoplasms );
      final Map<String, Collection<Mention>> newNeoplasmDocMentions
            = ConceptAggregateHandler.collectDocMentions( mergingNeoplasms );
      return new DefaultConceptAggregate( patientId, newNeoplasmUriRoots, newNeoplasmDocMentions );
   }



   /**
    * Merge all of the tumor instances and their cancer diagnoses into a single neoplasm.
    * !!! Note !!!   This undoes previous concept separations, including those based upon site and laterality.
    *
    * @param patientId    -
    * @param siteNeoplasmsMap -
    * @param allConcepts -
    * @return -
    */
   static public Map<String,ConceptAggregate> mergeSiteNeoplasms( final String patientId,
                                                  final Map<String,Collection<ConceptAggregate>> siteNeoplasmsMap,
                                                  final Collection<ConceptAggregate> allConcepts ) {
      final Map<ConceptAggregate,ConceptAggregate> originalToMergedMap = new HashMap<>();
      final Map<String,ConceptAggregate> newSiteNeoplasms = new HashMap<>( siteNeoplasmsMap.size() );
      for ( Map.Entry<String,Collection<ConceptAggregate>> siteNeoplasms : siteNeoplasmsMap.entrySet() ) {
         final ConceptAggregate mergedNeoplasm = createMergedNeoplasm( patientId, siteNeoplasms.getValue() );
         siteNeoplasms.getValue().forEach( n -> originalToMergedMap.put( n, mergedNeoplasm ) );
         newSiteNeoplasms.put( siteNeoplasms.getKey(), mergedNeoplasm );
      }

      // Redo relation sources and targets based upon originalToMergedMap.  Set for each new site merged neoplasm.
      for ( Map.Entry<String,Collection<ConceptAggregate>> siteNeoplasms : siteNeoplasmsMap.entrySet() ) {
         final Map<String, Collection<ConceptAggregate>> newNeoplasmRelations = new HashMap<>();
         for ( ConceptAggregate siteNeoplasm : siteNeoplasms.getValue() ) {
            final Map<String, Collection<ConceptAggregate>> originalRelationsMap
                  = siteNeoplasm.getRelatedConceptMap();
            for ( Map.Entry<String, Collection<ConceptAggregate>> originalRelations : originalRelationsMap.entrySet() ) {
               final String relationName = originalRelations.getKey();
               final Collection<ConceptAggregate> newRelations
                     = newNeoplasmRelations.computeIfAbsent( relationName, r -> new HashSet<>() );
               for ( ConceptAggregate originalTarget : originalRelations.getValue() ) {
                  final ConceptAggregate newTarget = originalToMergedMap.getOrDefault( originalTarget, originalTarget );
                  newRelations.add( newTarget );
               }
            }
         }
         final ConceptAggregate newSiteNeoplasm = newSiteNeoplasms.get( siteNeoplasms.getKey() );
         newSiteNeoplasm.setRelated( newNeoplasmRelations );
      }
      allConcepts.removeAll( originalToMergedMap.keySet() );
      allConcepts.addAll( originalToMergedMap.values() );
      return newSiteNeoplasms;
   }



}

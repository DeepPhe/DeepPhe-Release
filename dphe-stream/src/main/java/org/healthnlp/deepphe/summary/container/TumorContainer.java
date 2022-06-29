package org.healthnlp.deepphe.summary.container;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_LATERALITY;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2020
 */
public class TumorContainer extends AbstractNeoplasmContainer {

   static private final org.apache.log4j.Logger LOGGER = Logger.getLogger( "TumorContainer" );


   final public Collection<ConceptAggregate> _neoplasms = new HashSet<>();
   final private Map<String, Collection<String>> _relatedUris = new HashMap<>();
   final private Map<String, Collection<String>> _allRelatedUris = new HashMap<>();

   public TumorContainer( final ConceptAggregate neoplasm ) {
      addNeoplasm( neoplasm );
   }

//   public TumorContainer( final ConceptAggregate... neoplasms ) {
//      Arrays.stream( neoplasms ).forEach( this::addNeoplasm );
//   }

   public TumorContainer( final Collection<ConceptAggregate> neoplasms ) {
      neoplasms.forEach( this::addNeoplasm );
   }

   protected Collection<String> getRelatedUris( final String relationName ) {
      return _relatedUris.getOrDefault( relationName, Collections.emptyList() );
   }

   protected Collection<String> getAllRelatedUris( final String relationName ) {
      return _allRelatedUris.getOrDefault( relationName, Collections.emptyList() );
   }


   public boolean isLocationMatch( final ConceptAggregate neoplasm ) {
      final Collection<String> neoplasmSiteUris = neoplasm.getRelatedSiteMainUris();
      if ( neoplasmSiteUris.isEmpty() ) {
         return false;
      }
//         TODO - Check in order
//         DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
//         DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
//         DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
//         Disease_Has_Associated_Region,
//         Disease_Has_Associated_Cavity,
//         Finding_Has_Associated_Site,
//         Finding_Has_Associated_Region,
//         Finding_Has_Associated_Cavity

      final Collection<String> mySiteUris = getRelatedSiteUris( _relatedUris );
//      if ( neoplasmSiteUris.stream().anyMatch( mySiteUris::contains ) ) {
//         return true;
//      }

      final Collection<String> allMySiteUris = getRelatedSiteUris( _allRelatedUris );
      if ( neoplasmSiteUris.stream().anyMatch( allMySiteUris::contains ) ) {
         return true;
      }

      if ( UriUtil.isUriBranchMatch( mySiteUris, neoplasmSiteUris ) ) {
         return true;
      }
      // TODO - with v5 is this now just garbage?  Run and score with, then without and see if it matters.
      for ( String mySiteUri : mySiteUris ) {
         for ( String neoplasmSiteUri : neoplasmSiteUris ) {
            if ( UriUtil.getCloseUriLeaf( mySiteUri, neoplasmSiteUri ) != null ) {
               return true;
            }
         }
      }
      return false;
   }

   public boolean isLateralityMatch( final ConceptAggregate neoplasm ) {
      if ( hasRelated( HAS_LATERALITY, neoplasm ) ) {
         return true;
      }
      final Collection<ConceptAggregate> lateralities = neoplasm.getRelatedConceptMap().get( HAS_LATERALITY );
      if ( lateralities == null || lateralities.isEmpty() ) {
         return false;
      }
      final Collection<String> lateralityUris = _relatedUris.get( HAS_LATERALITY );
      for ( String weUri : lateralityUris ) {
         for ( ConceptAggregate laterality : lateralities ) {
            final String theyUri = laterality.getUri();
            if ( UriUtil.isBilaterality( weUri, theyUri ) ) {
               return true;
            }
         }
      }
      return false;
   }


   public void addNeoplasm( final ConceptAggregate neoplasm ) {
      _neoplasms.add( neoplasm );
      final Map<String, Collection<ConceptAggregate>> related = neoplasm.getRelatedConceptMap();
      for ( Map.Entry<String, Collection<ConceptAggregate>> relation : related.entrySet() ) {
         final Collection<String> relationUris = relation.getValue()
                                                         .stream()
                                                         .map( ConceptAggregate::getUri )
                                                         .collect( Collectors.toSet() );
         _relatedUris.computeIfAbsent( relation.getKey(), r -> new HashSet<>() )
                     .addAll( relationUris );
         final Collection<String> allRelationUris = relation.getValue()
                                                            .stream()
                                                            .map( ConceptAggregate::getAllUris )
                                                            .flatMap( Collection::stream )
                                                            .collect( Collectors.toSet() );
         _allRelatedUris.computeIfAbsent( relation.getKey(), r -> new HashSet<>() )
                     .addAll( allRelationUris );
      }
   }

   @Override
   public ConceptAggregate createMergedConcept( final Collection<ConceptAggregate> allInstances ) {
      if ( _neoplasms.size() == 1 ) {
         return new ArrayList<>( _neoplasms ).get( 0 );
      }
      final String patientId = new ArrayList<>( _neoplasms ).get( 0 ).getPatientId();
      final Map<String, Collection<Mention>> allDocMentions = new HashMap<>();
      final Map<String, Collection<ConceptAggregate>> allRelated = new HashMap<>();
      final Map<String,Collection<String>> allUriRoots = new HashMap<>();
      for ( ConceptAggregate neoplasm : _neoplasms ) {
         final Map<String, Collection<Mention>> docMentions = neoplasm.getNoteMentions();
         for ( Map.Entry<String, Collection<Mention>> docMention : docMentions.entrySet() ) {
            allDocMentions.computeIfAbsent( docMention.getKey(), d -> new HashSet<>() )
                          .addAll( docMention.getValue() );
         }
         final Map<String, Collection<ConceptAggregate>> related = neoplasm.getRelatedConceptMap();
         for ( Map.Entry<String, Collection<ConceptAggregate>> relation : related.entrySet() ) {
            allRelated.computeIfAbsent( relation.getKey(), r -> new HashSet<>() ).addAll( relation.getValue() );
         }
         neoplasm.getUriRootsMap().forEach( (k,v) -> allUriRoots.computeIfAbsent( k, s -> new HashSet<>() ).addAll( v ) );
      }

      //  TODO : In dphe-summary only the bestUri was used in the CorefConceptInstance, all other uris ignored.
//         final ConceptAggregate merge = new CorefConceptInstance( patientId, bestUri, allDocMentions );

//      final String bestUri = UriUtil.getMostSpecificUri( _neoplasms.stream()
//                                    .map( ConceptAggregate::getUri )
//                                    .collect( Collectors.toSet() ) );
//      final Map<String, Collection<String>> mapOfBestUri = new HashMap<>();
//      mapOfBestUri.put( bestUri, Collections.singletonList( bestUri ) );
      final ConceptAggregate merge = new DefaultConceptAggregate( patientId, allUriRoots, allDocMentions );
      merge.setRelated( allRelated );
      updateAllInstances( allInstances, _neoplasms, merge );

//      LOGGER.info( "\nRecomputed best URI " + merge.getUri() + " for tumor " + merge.getId()  + " scored: " + merge.getUriScore() + ":" );
//      _neoplasms.forEach( ca -> LOGGER.info( ca.getUri() + " (" + ca.getMentions()
//                                                                                  .stream()
//                                                                                  .map( Mention::getClassUri )
//                                                                                  .collect( Collectors.joining( "," ) ) + ")" ) );
//      LOGGER.info( " and added a new ConceptAggregate replacing all constituent Tumor Container ConceptAggregates " );


      return merge;
   }
}

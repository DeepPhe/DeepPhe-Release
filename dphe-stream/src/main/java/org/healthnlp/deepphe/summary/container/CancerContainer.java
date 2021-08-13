package org.healthnlp.deepphe.summary.container;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;
import org.healthnlp.deepphe.util.CancerMatchUtil;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2020
 */
public class CancerContainer extends AbstractNeoplasmContainer {

   static private final Logger LOGGER = Logger.getLogger( "CancerContainer" );


   final private ConceptAggregate _primary;
   final private boolean _isMetastatic;
   final private Collection<ConceptAggregate> _metastases = new HashSet<>();
   final private Collection<ConceptAggregate> _neoplasms = new HashSet<>();
   final private Map<String, Collection<String>> _nonSiteRelatedUris = new HashMap<>();

   public CancerContainer( final ConceptAggregate primary ) {
      this( primary, false );
   }

   public CancerContainer( final ConceptAggregate primary, final boolean isMetastatic ) {
      _primary = primary;
      addRelatedUris( primary );
      _isMetastatic = isMetastatic;
   }

   public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append( "CancerContainer " ).append( hashCode() ).append( "\n" );
      if ( _primary == null ) {
         sb.append( "Null Primary\n" );
      } else {
         sb.append( _primary.toShortText() ).append( "\n" );
      }
      if ( !_neoplasms.isEmpty() ) {
         sb.append( "Neoplasms\n" );
         _neoplasms.forEach( n -> sb.append( n.toShortText() ).append( "\n" ) );
      }
      if ( !_metastases.isEmpty() ) {
         sb.append( "Metastases\n" );
         _metastases.forEach( n -> sb.append( n.toShortText() ).append( "\n" ) );
      }
      _nonSiteRelatedUris.forEach( ( k, v ) -> sb.append( k )
                                                 .append( " : " )
                                                 .append( String.join( ";", v ) )
                                                 .append( "\n" ) );
      return sb.toString();
   }


   protected Collection<String> getRelatedUris( final String relationName ) {
      final Collection<String> related = _nonSiteRelatedUris.get( relationName );
      if ( related == null ) {
         return Collections.emptyList();
      }
      return related;
   }

   /**
    * @param metastasis has a site
    */
   public void addMetastasis( final ConceptAggregate metastasis ) {
//      LOGGER.info( "Adding metastasis " + metastasis.getUri() + " to cancer " + getPrimary().getUri() );
      _metastases.add( metastasis );
      addRelatedUris( metastasis );
   }

   public void addNeoplasm( final ConceptAggregate neoplasm ) {
//      LOGGER.info( "Adding neoplasm " + neoplasm.getUri() + " to cancer " + getPrimary().getUri() );
      _neoplasms.add( neoplasm );
      addRelatedUris( neoplasm );
   }

   private void addRelatedUris( final ConceptAggregate neoplasm ) {
      final Map<String, Collection<ConceptAggregate>> nonSiteRelated = neoplasm.getNonLocationRelationMap();
      for ( Map.Entry<String, Collection<ConceptAggregate>> relation : nonSiteRelated.entrySet() ) {
         final Collection<String> relationUris = relation.getValue()
                                                         .stream()
                                                         .map( ConceptAggregate::getUri )
                                                         .collect( Collectors.toSet() );
         _nonSiteRelatedUris.computeIfAbsent( relation.getKey(), r -> new HashSet<>() )
                     .addAll( relationUris );
      }
   }

   private Collection<ConceptAggregate> getAllTumors() {
      final Collection<ConceptAggregate> tumors = new HashSet<>( _metastases );
      tumors.add( _primary );
      return tumors;
   }

   public Collection<ConceptAggregate> getAllLocatedTumors() {
      final Collection<ConceptAggregate> tumors = new HashSet<>( _metastases );
      tumors.add( _primary );
      final Collection<ConceptAggregate> removals = new ArrayList<>();
      for ( ConceptAggregate tumor : tumors ) {
         final Collection<ConceptAggregate> locations = tumor.getRelatedSites();
         if ( locations.isEmpty() ) {
            removals.add( tumor );
            continue;
         }
//         if ( tumor.getMentions().size() == 1 ) {
//            LOGGER.info( "Only 1 mention : " + tumor.getUri() + " " + tumor.getCoveredText() + " Loci " +
//                                       locations.stream().map( ConceptAggregate::getCoveredText ).collect( Collectors.joining() ) +
//                                       " " + _primary.equals( tumor ) );
//         }
         if ( locations.size() > 1 ) {
            continue;
         }
         for ( ConceptAggregate location : locations ) {
            if ( location.getMentions().size() == 1 && !_primary.equals( tumor ) ) {
//               LOGGER.info( "Only 1 loci : " + tumor.getUri() + " " + tumor.getCoveredText() + " of " +
//                                          location.getUri() + " " + location.getCoveredText() );
               removals.add( tumor );
            }
         }
      }
      tumors.removeAll( removals );
      final Collection<ConceptAggregate> diagnoses = _primary.getRelated( HAS_DIAGNOSIS );
      if ( !diagnoses.isEmpty() ) {
         for ( ConceptAggregate tumor : tumors ) {
            final Collection<ConceptAggregate> diagnosis = tumor.getRelated( HAS_DIAGNOSIS );
            if ( diagnosis == null || diagnosis.isEmpty() ) {
               for ( ConceptAggregate diag : diagnoses ) {
//                  LOGGER.info(
//                        "Adding diagnosis " + diag.getUri() + " to tumor " + tumor.getUri() + " for primary " +
//                        _primary.getUri() );
                  tumor.addRelated( HAS_DIAGNOSIS, diag );
               }
            }
         }
      }
      return tumors;
   }

   /**
    * compares main uris to diagnoses to stages.
    *
    * @param neoplasm -
    * @return -
    */
   private boolean isDiagnosisMatch( final ConceptAggregate neoplasm ) {
      final Collection<String> containerUris = _neoplasms.stream()
                                                         .map( ConceptAggregate::getUri )
                                                         .collect( Collectors.toSet() );
      containerUris.add( _primary.getUri() );
      containerUris.addAll( getRelatedUris( HAS_DIAGNOSIS ) );
      final Collection<String> neoplasmUris = new HashSet<>();
      neoplasmUris.add( neoplasm.getUri() );
      neoplasmUris.addAll( neoplasm.getRelatedUris( HAS_DIAGNOSIS ) );
      if ( neoplasmUris.stream().anyMatch( containerUris::contains ) ) {
         return true;
      }
      if ( containerUris.stream().anyMatch( neoplasmUris::contains ) ) {
         return true;
      }
      final Collection<String> containerBranch = containerUris.stream()
                                                              .map( Neo4jOntologyConceptUtil::getBranchUris )
                                                              .flatMap( Collection::stream )
                                                              .collect( Collectors.toSet() );
      if ( neoplasmUris.stream().anyMatch( containerBranch::contains ) ) {
         return true;
      }
      final Collection<String> neoplasmBranch = neoplasmUris.stream()
                                                            .map( Neo4jOntologyConceptUtil::getBranchUris )
                                                            .flatMap( Collection::stream )
                                                            .collect( Collectors.toSet() );
      if ( containerUris.stream().anyMatch( neoplasmBranch::contains ) ) {
         return true;
      }
      return false;
   }

   /**
    * compares main uris to diagnoses to stages.
    *
    * @param neoplasm -
    * @return -
    */
   public int rateDiagnosisMatch( final ConceptAggregate neoplasm ) {
      final Collection<String> neoplasmUris = neoplasm.getRelatedUris( HAS_DIAGNOSIS );
      if ( neoplasmUris.contains( _primary.getUri() ) ) {
//         LOGGER.info( "Diagnostic Match rated 10 because the neoplasm " + neoplasm.getUri() + " has this cancer " + _primary.getUri() + " as a diagnosis." );
         return 10;
      }

      final Collection<String> containerUris = new HashSet<>();
      containerUris.add( _primary.getUri() );
      _neoplasms.stream()
                .map( ConceptAggregate::getUri )
                .forEach( containerUris::add );

      if ( neoplasmUris.stream().anyMatch( containerUris::contains ) ) {
//         LOGGER.info( "Diagnostic Match rated 9 because the neoplasm " + neoplasm.getUri() + " has a matching diagnosis as one of this cancer's neoplasm URIs " + _primary.getUri() + "." );
         return 9;
      }

      containerUris.addAll( getRelatedUris( HAS_DIAGNOSIS ) );
      neoplasmUris.add( neoplasm.getUri() );
      if ( neoplasmUris.stream().anyMatch( containerUris::contains ) ) {
//         LOGGER.info( "Diagnostic Match rated 7 because the neoplasm " + neoplasm.getUri() + " has a matching diagnosis as one of this cancer's neoplasms " + _primary.getUri() + " diagnoses." );
         return 7;
      }

      if ( containerUris.stream().anyMatch( neoplasmUris::contains ) ) {
//         LOGGER.info( "Diagnostic Match rated 5 because the neoplasm " + neoplasm.getUri() + " has a matching diagnosis as one of this cancer's neoplasms " + _primary.getUri() + " diagnoses (reverse)." );
         return 5;
      }
//         for ( String uri : containerUris ) {
//            LOGGER.warn( "Container: " + uri );
//         }
         final Collection<String> containerBranch = containerUris.stream()
                                                                 .filter( Objects::nonNull )
                                                                 .map( Neo4jOntologyConceptUtil::getBranchUris )
                                                                 .flatMap( Collection::stream )
                                                                 .collect( Collectors.toSet() );
         if ( neoplasmUris.stream()
                          .anyMatch( containerBranch::contains ) ) {
//         LOGGER.info( "Diagnostic Match rated 3 because the neoplasm " + neoplasm.getUri() + " has a diagnosis as one of this cancer's neoplasms " + _primary.getUri() + " diagnoses branch URIs." );
            return 3;
         }
      final Collection<String> neoplasmBranch = neoplasmUris.stream()
                                                            .map( Neo4jOntologyConceptUtil::getBranchUris )
                                                            .flatMap( Collection::stream )
                                                            .collect( Collectors.toSet() );
      if ( containerUris.stream()
                        .anyMatch( neoplasmBranch::contains ) ) {
//         LOGGER.info( "Diagnostic Match rated 1 because the neoplasm " + neoplasm.getUri() + " has a diagnosis branch URI matching one of this cancer's neoplasms " + _primary.getUri() + " diagnoses branch URIs." );
         return 1;
      }
//         return isTnmMatch( neoplasm );
      return 0;
   }

//   private boolean isStageMatch( final ConceptAggregate neoplasm ) {
//      return CancerMatchUtil.isStageMatch( neoplasm, Collections.singletonList( _primary ) );
//   }
//
//   private boolean isTnmMatch( final ConceptAggregate neoplasm ) {
//      CancerMatchUtil.MatchType match
//            = CancerMatchUtil.getTnmMatchType( neoplasm, Collections.singletonList( _primary ) );
//      if ( match == CancerMatchUtil.MatchType.MATCH ) {
//         return true;
//      }
//      match = CancerMatchUtil.getTnmMatchType( neoplasm, _neoplasms );
//      if ( match == CancerMatchUtil.MatchType.MATCH ) {
//         return true;
//      }
//      match = CancerMatchUtil.getTnmMatchType( neoplasm, _metastases );
//      return match == CancerMatchUtil.MatchType.MATCH;
//   }
//
//   private boolean isCellMatch( final ConceptAggregate neoplasm ) {
//      CancerMatchUtil.MatchType match
//            = CancerMatchUtil.getHistologyMatchType( neoplasm, Collections.singletonList( _primary ) );
//      if ( match == CancerMatchUtil.MatchType.MATCH ) {
//         return true;
//      }
//      boolean empty = match == CancerMatchUtil.MatchType.EMPTY;
//
//      match = CancerMatchUtil.getHistologyMatchType( neoplasm, _metastases );
//      if ( match == CancerMatchUtil.MatchType.MATCH ) {
//         return true;
//      }
//      empty = empty && match == CancerMatchUtil.MatchType.EMPTY;
//
//      match = CancerMatchUtil.getHistologyMatchType( neoplasm, _neoplasms );
//      if ( match == CancerMatchUtil.MatchType.MATCH ) {
//         return true;
//      }
//      empty = empty && match == CancerMatchUtil.MatchType.EMPTY;
//
//      match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, Collections.singletonList( _primary ) );
//      if ( match == CancerMatchUtil.MatchType.MATCH ) {
//         return true;
//      }
//      empty = empty && match == CancerMatchUtil.MatchType.EMPTY;
//
//      match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, _metastases );
//      if ( match == CancerMatchUtil.MatchType.MATCH ) {
//         return true;
//      }
//      empty = empty && match == CancerMatchUtil.MatchType.EMPTY;
//
//      match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, _neoplasms );
//      if ( match == CancerMatchUtil.MatchType.MATCH ) {
//         return true;
//      }
//      return empty && match == CancerMatchUtil.MatchType.EMPTY;
//   }

   public int rateCellMatch( final ConceptAggregate neoplasm ) {
      CancerMatchUtil.MatchType match
            = CancerMatchUtil.getHistologyMatchType( neoplasm, Collections.singletonList( _primary ) );
      if ( match == CancerMatchUtil.MatchType.MATCH ) {
         return 10;
      }
      boolean empty = match == CancerMatchUtil.MatchType.EMPTY;

      match = CancerMatchUtil.getHistologyMatchType( neoplasm, _metastases );
      if ( match == CancerMatchUtil.MatchType.MATCH ) {
         return 8;
      }
      empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

      match = CancerMatchUtil.getHistologyMatchType( neoplasm, _neoplasms );
      if ( match == CancerMatchUtil.MatchType.MATCH ) {
         return 6;
      }
      empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

      match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, Collections.singletonList( _primary ) );
      if ( match == CancerMatchUtil.MatchType.MATCH ) {
         return 4;
      }
      empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

      match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, _metastases );
      if ( match == CancerMatchUtil.MatchType.MATCH ) {
         return 3;
      }
      empty = empty && match == CancerMatchUtil.MatchType.EMPTY;

      match = CancerMatchUtil.getCancerTypeMatchType( neoplasm, _neoplasms );
      if ( match == CancerMatchUtil.MatchType.MATCH ) {
         return 2;
      }
      if ( empty && match == CancerMatchUtil.MatchType.EMPTY ) {
         return 1;
      }
      return 0;
   }

   private Collection<ConceptAggregate> getMetastases() {
      return _metastases;
   }

   private ConceptAggregate getPrimary() {
      return _primary;
   }

   // TODO for -classic- there was logic to separate tumors from cancers by having tumor-only facts or cancer-only facts.
   // Should still be in old dphe3 code (though commented)
   @Override
   public ConceptAggregate createMergedConcept( final Collection<ConceptAggregate> conceptAggregates ) {
      final ConceptAggregate primary = getPrimary();
      String patientId = primary.getPatientId();

      final Collection<ConceptAggregate> cancerContributors = new HashSet<>( _neoplasms );
      final Map<String, Collection<Mention>> allDocMentions = new HashMap<>();
      final Map<String,Collection<String>> allUriRoots = new HashMap<>();

      // Best URI for Cancer derived from primary and all non-sited neoplasms.  Don't use metastasis uris.
      final Map<String, Collection<ConceptAggregate>> allRelated = new HashMap<>();
      for ( ConceptAggregate neoplasm : cancerContributors ) {
         final Map<String, Collection<Mention>> docMentions = neoplasm.getNoteMentions();
         for ( Map.Entry<String, Collection<Mention>> docAnnotation : docMentions.entrySet() ) {
            allDocMentions.computeIfAbsent( docAnnotation.getKey(), d -> new HashSet<>() )
                             .addAll( docAnnotation.getValue() );
         }
         neoplasm.getUriRootsMap().forEach( (k,v) -> allUriRoots.computeIfAbsent( k, s -> new HashSet<>() ).addAll( v ) );

         final Map<String, Collection<ConceptAggregate>> related = neoplasm.getRelatedConceptMap();
         for ( Map.Entry<String, Collection<ConceptAggregate>> relation : related.entrySet() ) {
            final String relationName = relation.getKey();
//            LOGGER.info( relationName + " for cancer ... " );
            if ( !RelationConstants.isLocationRelation( relationName ) ) {
//               LOGGER.info( "Yes" );
               allRelated.computeIfAbsent( relation.getKey(), r -> new HashSet<>() )
                         .addAll( relation.getValue() );
            }
         }
      }

      primary.getNoteMentions()
             .forEach( (k,v) -> allDocMentions.computeIfAbsent( k, d -> new HashSet<>() )
                                              .addAll( v ) );

      primary.getUriRootsMap()
             .forEach( (k,v) -> allUriRoots.computeIfAbsent( k, d -> new HashSet<>() )
                                           .addAll( v ) );

      primary.getRelatedConceptMap()
             .forEach( (k,v) -> allRelated.computeIfAbsent( k, r -> new HashSet<>() )
                                          .addAll( v ) );

      // Best URI for Cancer derived from primary and all non-sited neoplasms.  Don't use metastasis uris.        ---> Wasn't this already done above?
      cancerContributors.add( primary );
//      String bestUri = primary.getUri();
//      if ( _isMetastatic || UriConstants.METASTASIS.equals( bestUri ) ) {
//         final Collection<String> diagnoses = new HashSet<>();
//         diagnoses.add( bestUri );
//         diagnoses.addAll( getRelatedUris( HAS_DIAGNOSIS ) );
//         diagnoses.addAll( getRelatedUris( RelationConstants.METASTASIS_OF ) );
//         bestUri = UriUtil.getMostSpecificUri( diagnoses );
//      } else {
//         bestUri = UriUtil.getMostSpecificUri( cancerContributors.stream()
//                                                                 .map( ConceptAggregate::getUri )
//                                                                 .collect( Collectors.toSet() ) );
//      }

      getCancerOnlyFacts( cancerContributors )
            .forEach( (k,v) -> allRelated.computeIfAbsent( k, r -> new HashSet<>() )
                                         .addAll( v ) );

      getCancerOnlyFacts( _metastases )
            .forEach( (k,v) -> allRelated.computeIfAbsent( k, r -> new HashSet<>() )
                                         .addAll( v ) );

      getCancerFacts( Collections.singletonList( primary ) )
            .forEach( (k,v) -> allRelated.computeIfAbsent( k, r -> new HashSet<>() )
                                         .addAll( v ) );

//      final Map<String, Collection<String>> mappedBestUri = new HashMap<>( 1 );
//      mappedBestUri.put( bestUri, Collections.singletonList( bestUri ) );
//      final ConceptAggregate cancer = new DefaultConceptAggregate( patientId, mappedBestUri, allDocAnnotations );
//      final ConceptAggregate merge = new DefaultConceptAggregate( patientId, _primary.getUriRootsMap(), allDocMentions );
      final ConceptAggregate merge = new DefaultConceptAggregate( patientId, allUriRoots, allDocMentions );
      merge.setRelated( allRelated );
      updateAllInstances( conceptAggregates, Collections.singletonList( _primary ), merge );
      conceptAggregates.add( _primary );
      // Need to create merged primary?

//      LOGGER.info( "\nRecomputed best URI " + merge.getUri() + " for cancer " + merge.getId() + " scored: " + merge.getUriScore() + ":" );
//      _neoplasms.forEach( ca -> LOGGER.info( ca.getUri() + " (" + ca.getMentions()
//                                                                    .stream()
//                                                                    .map( Mention::getClassUri )
//                                                                    .collect( Collectors.joining( "," ) ) + ")" ) );
//      LOGGER.info( " and added a new ConceptAggregate replacing all constituent Cancer Container ConceptAggregates " );

      return merge;
   }




   static private final Collection<String> CANCER_ONLY_FACT
         = Arrays.asList( HAS_CLINICAL_T,
         HAS_CLINICAL_N,
         HAS_CLINICAL_M,
         HAS_PATHOLOGIC_T,
         HAS_PATHOLOGIC_N,
         HAS_PATHOLOGIC_M,
         HAS_STAGE );
   static public boolean isCancerOnlyFact( final String category ) {
      return CANCER_ONLY_FACT.contains( category );
   }

   static public Map<String,Collection<ConceptAggregate>> getCancerOnlyFacts(
         final Collection<ConceptAggregate> neoplasms ) {
      final Map<String,Collection<ConceptAggregate>> allRelated = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         for ( String fact : CANCER_ONLY_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( neoplasm.getRelated( fact ) );
         }
      }
      return allRelated;
   }


   static private final  Collection<String> CANCER_FACT
         = Arrays.asList(
         HAS_BODY_MODIFIER,
         HAS_LATERALITY,
         HAS_CANCER_TYPE,
         HAS_HISTOLOGY
   );
   static private boolean isCancerFact( final String category ) {
      return CANCER_FACT.contains( category );
   }

   static public Map<String,Collection<ConceptAggregate>> getCancerFacts(
         final Collection<ConceptAggregate> neoplasms ) {
      final Map<String,Collection<ConceptAggregate>> allRelated = new HashMap<>();
      for ( ConceptAggregate neoplasm : neoplasms ) {
         neoplasm.getRelatedConceptMap().entrySet()
                 .stream()
                 .filter( e -> RelationConstants.isHasSiteRelation( e.getKey() ) )
                 .forEach( e -> allRelated.computeIfAbsent( e.getKey(), s -> new HashSet<>() )
                                          .addAll( e.getValue() ) );
         for ( String fact : CANCER_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( neoplasm.getRelated( fact ) );
         }
         for ( String fact : CANCER_ONLY_FACT ) {
            allRelated.computeIfAbsent( fact, f -> new HashSet<>() )
                      .addAll( neoplasm.getRelated( fact ) );
         }
      }
      return allRelated;
   }


}

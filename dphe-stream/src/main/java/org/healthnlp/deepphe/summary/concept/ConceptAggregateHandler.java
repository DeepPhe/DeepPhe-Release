package org.healthnlp.deepphe.summary.concept;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.util.MentionUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/15/2020
 */
final public class ConceptAggregateHandler {

   static private final Logger LOGGER = Logger.getLogger( "ConceptAggregateHandler" );

   private ConceptAggregateHandler() {}

   /**
    * @param patientId      -
    * @param patientMentionNoteIds -
    * @param patientRelations   -
    * @return map of best uri to its concept instances
    */
   static public Map<String, Collection<ConceptAggregate>> createUriConceptAggregateMap(
         final String patientId,
         final Map<Mention, String> patientMentionNoteIds,
         final Collection<MentionRelation> patientRelations ) {

      final Map<String,Mention> mentionIdMap
            = patientMentionNoteIds.keySet()
                                   .stream()
                                   .collect( Collectors.toMap( Mention::getId, Function.identity() ) );

      // Map of unique URIs to all Mentions with that URI.
      final Map<String, List<Mention>> uriMentionsMap = mapUriMentions( patientMentionNoteIds.keySet() );

      // Map of unique xDoc URIs to URIs that are associated (e.g. same branch).
      // Has nothing to do with previously determined in-doc coreference chains.
      final Map<String, Collection<String>> associatedUrisMap = UriUtil.getAssociatedUriMap( uriMentionsMap.keySet() );


//      LOGGER.info( "\nMap of unique xDoc URIs to URIs that are associated (e.g. same branch)." );
//      associatedUrisMap.forEach( (k,v) -> LOGGER.info( k + ": (" + String.join( ",", v ) + ")" ) );


      // TODO - jinky.  Relies upon corefs that may be incorrect.
      // Map of unique URIs to all connected URIs from any in-doc coreference chain.
//      final Map<String, Collection<String>> inDocChainedUrisMap = mapChainedUris( patientCorefs, mentionIdMap );
      // Map of uri branches to their best root according to previously determined coreference chains.
//      final Map<String, Collection<String>> bestRootsMap = mapBestRoots( associatedUrisMap, inDocChainedUrisMap );
//      LOGGER.info( "\nMap of Uri branches to their best roots for the in-doc chains." );
//      bestRootsMap.forEach( (k,v) -> LOGGER.info( k + ": (" + String.join( ",", v ) + ")" ) );
      // Collection of all the best URI branches according to the best root URIs.
//      final Collection<Collection<String>> finalBranches = collectFinalBranches( associatedUrisMap, bestRootsMap );
//      LOGGER.info( "\nxDoc Coref URI Chains.  All Mentions in all Docs with these URIs are now coreferrent.  This is not final." );
//      finalBranches.forEach( c -> LOGGER.info( "Chain: (" + String.join( ",", c ) + ")" ) );


//      final Map<Mention, Collection<String>> locationUris = new HashMap<>();
//      "Disease_Has_Primary_Anatomic_Site", "Disease_Has_Associated_Anatomic_Site", "Disease_Has_Metastatic_Anatomic_Site",
//      "Disease_Has_Associated_Region", "Disease_Has_Associated_Cavity",
//      "Finding_Has_Associated_Site", "Finding_Has_Associated_Region", "Finding_Has_Associated_Cavity"
      final Map<String,Map<Mention,Collection<String>>> typeLocationUris = new HashMap<>();
      final Map<Mention, Collection<String>> lateralityUris = new HashMap<>();
//      buildPlacements( patientRelations, mentionIdMap, locationUris, lateralityUris );
      buildPlacements( patientRelations, mentionIdMap, typeLocationUris, lateralityUris );


//      LOGGER.info( "!!!    Determined Locations for all Mentions." );
//      locationUris.forEach( (k,v) -> LOGGER.info( "Mention " + k.getClassUri() +" "+ k.getId() + " at (" + String.join( ",", v ) + ")" ) );
//      lateralityUris.forEach( (k,v) -> LOGGER.info( "Mention " + k.getClassUri() +" "+ k.getId() + " on (" + String.join( ",", v ) + ")" ) );



      // Get roots of all uris here to prevent repeated lookup for mentions in different chains.
      final Map<String,Collection<String>> allUriRoots = UriUtil.mapUriRoots( uriMentionsMap.keySet() );

      final Map<String, Collection<ConceptAggregate>> conceptAggregates = new HashMap<>();
      final Collection<Mention> usedMentions = new ArrayList<>();

//      for ( Collection<String> finalBranch : finalBranches ) {
      for ( Collection<String> finalBranch : associatedUrisMap.values() ) {
         final Collection<Mention> mentionGroup = finalBranch.stream()
                                                             .map( uriMentionsMap::get )
                                                             .flatMap( Collection::stream )
                                                             .collect( Collectors.toSet() );
         final List<List<Mention>> chains = new ArrayList<>();
//         MentionUtil.collateCoref( chains, mentionGroup, locationUris, lateralityUris );
         MentionUtil.collateCoref( chains, mentionGroup, typeLocationUris, lateralityUris );
         for ( List<Mention> chain : chains ) {
            if ( chain.size() > 1 ) {
               final Map<String, Collection<Mention>> noteIdMentionsMap = new HashMap<>();
               for ( Mention mention : chain ) {
                  noteIdMentionsMap.computeIfAbsent( patientMentionNoteIds.get( mention ), d -> new HashSet<>() )
                                   .add( mention );
               }
               // Create a concept aggregate with each annotation assigned to the appropriate docId.
               final Collection<String> uris = chain.stream()
                                                    .map( Mention::getClassUri )
                                                    .collect( Collectors.toSet() );
               // smaller map of uris to roots that only contains pertinent uris.
               final Map<String,Collection<String>> uriRoots = new HashMap<>( allUriRoots );
               uriRoots.keySet().retainAll( uris );
               final ConceptAggregate concept
                     = new DefaultConceptAggregate( patientId, uriRoots, noteIdMentionsMap );



//               LOGGER.info( "Created " + chain.size() + "mention ConceptAggregate " + concept.getUri() + " " + concept.getId() + " "
//                                             + "scored:"
//                            + " " + concept.getUriScore() );
//               uriRoots.forEach( (k,v) -> LOGGER.info( "URI " + k + " with -unordered- root URIs (" + String.join( ",", v ) + ")" ) );



               conceptAggregates.computeIfAbsent( concept.getUri(), ci -> new ArrayList<>() ).add( concept );
               usedMentions.addAll( chain );
            }
         }
      }
      usedMentions.forEach( patientMentionNoteIds.keySet()::remove );
      for ( Map.Entry<Mention, String> mentionNoteId : patientMentionNoteIds.entrySet() ) {
         final String bestUri = mentionNoteId.getKey().getClassUri();
         if ( bestUri.isEmpty() ) {
            continue;
         }
//         final Map<String,Collection<String>> bestUriMap = new HashMap<>( 1 );
//          bestUriMap.put( bestUri, Collections.singletonList( bestUri ) );
         final Map<String,Collection<String>> uriRoots = new HashMap<>( 1 );
         uriRoots.put( bestUri, allUriRoots.get( bestUri ) );

         conceptAggregates
               .computeIfAbsent( bestUri, ci -> new HashSet<>() )
               .add( new DefaultConceptAggregate( patientId,
                     uriRoots,
                     Collections.singletonMap( mentionNoteId.getValue(),
                           Collections.singletonList( mentionNoteId.getKey() ) ) ) );


//         LOGGER.info( "Created Simple ConceptAggregate of " + bestUri );


      }


      addRelations( conceptAggregates.values(), patientRelations );


//      LOGGER.info( "\n!!!     All Concept Aggregates:" );
//      conceptAggregates.values().stream().flatMap( Collection::stream ).forEach( c -> LOGGER.info( c + "\n" ) );



      return conceptAggregates;
   }




   static private Map<String,List<Mention>> mapUriMentions( final Collection<Mention> mentions ) {
      return mentions.stream().collect( Collectors.groupingBy( Mention::getClassUri ) );
   }


//   // TODO Move to AnatomyUtil ?
//   static private void buildPlacements( final Collection<MentionRelation> relations,
//                                        final Map<String,Mention> mentionIdMap,
//                                        final Map<Mention, Collection<String>> locationUris,
//                                        final Map<Mention, Collection<String>> lateralityUris ) {
//      for ( MentionRelation relation : relations ) {
//         if ( isLocationOf( relation ) ) {
//            final String targetId = relation.getTargetId();
//            final Mention target = mentionIdMap.get( targetId );
//            if ( target == null ) {
//               LOGGER.error( "No Target for relation." );
//               continue;
//            }
//            final String uri = target.getClassUri();
//            final String sourceId = relation.getSourceId();
//            final Mention source = mentionIdMap.get( sourceId );
//            if ( source == null ) {
//               LOGGER.error( "No Source for relation." );
//               continue;
//            }
//            locationUris.computeIfAbsent( source, a -> new HashSet<>() ).add( uri );
//         } else if ( relation.getType().equals( RelationConstants.HAS_LATERALITY ) ) {
//            final String targetId = relation.getTargetId();
//            final Mention target = mentionIdMap.get( targetId );
//            if ( target == null ) {
//               LOGGER.error( "No Target for relation." );
//               continue;
//            }
//            final String uri = target.getClassUri();
//            final String sourceId = relation.getSourceId();
//            final Mention source = mentionIdMap.get( sourceId );
//            if ( source == null ) {
//               LOGGER.error( "No Source for relation." );
//               continue;
//            }
//            lateralityUris.computeIfAbsent( source, a -> new HashSet<>() ).add( uri );
//         }
//      }
//   }

   static private void buildPlacements( final Collection<MentionRelation> relations,
                                        final Map<String,Mention> mentionIdMap,
                                        final Map<String,Map<Mention,Collection<String>>> typeLocationUris,
                                        final Map<Mention, Collection<String>> lateralityUris ) {
      for ( MentionRelation relation : relations ) {
         if ( RelationConstants.isHasSiteRelation( relation.getType() ) ) {
            final String targetId = relation.getTargetId();
            final Mention target = mentionIdMap.get( targetId );
            if ( target == null ) {
               LOGGER.error( "No Target for relation." );
               continue;
            }
            final String uri = target.getClassUri();
            final String sourceId = relation.getSourceId();
            final Mention source = mentionIdMap.get( sourceId );
            if ( source == null ) {
               LOGGER.error( "No Source for relation." );
               continue;
            }
            Map<Mention,Collection<String>> locationUris
                  = typeLocationUris.computeIfAbsent( relation.getType(), t -> new HashMap<>() );
            locationUris.computeIfAbsent( source, a -> new HashSet<>() ).add( uri );
         } else if ( relation.getType().equals( RelationConstants.HAS_LATERALITY ) ) {
            final String targetId = relation.getTargetId();
            final Mention target = mentionIdMap.get( targetId );
            if ( target == null ) {
               LOGGER.error( "No Target for relation." );
               continue;
            }
            final String uri = target.getClassUri();
            final String sourceId = relation.getSourceId();
            final Mention source = mentionIdMap.get( sourceId );
            if ( source == null ) {
               LOGGER.error( "No Source for relation." );
               continue;
            }
            lateralityUris.computeIfAbsent( source, a -> new HashSet<>() ).add( uri );
         }
      }
   }






   static private void addRelations(
         final Collection<Collection<ConceptAggregate>> conceptAggregateSet,
         final Collection<MentionRelation> patientRelations ) {

//      LOGGER.info( "Adding relations to Concept Aggregates ..." );


      final Map<Mention, ConceptAggregate> mentionConceptAggregateMap
            = mapMentionConceptAggregates( conceptAggregateSet );

      // TODO  --- Do we need to add the xdoc relations?  Relations within same section type?
//      CiUriRelationFinder.addRelations( docJcases, conceptAggregateSet );

      for ( Collection<ConceptAggregate> conceptAggregates : conceptAggregateSet ) {
         for ( ConceptAggregate conceptAggregate : conceptAggregates ) {
            final Map<String, Collection<ConceptAggregate>> relatedCis
                  = getCategoryTargetConceptMap( conceptAggregate, mentionConceptAggregateMap, patientRelations );
            for ( Map.Entry<String, Collection<ConceptAggregate>> related : relatedCis.entrySet() ) {
               final String type = related.getKey();
               for ( ConceptAggregate target : related.getValue() ) {



//                  LOGGER.info( "Adding Concept Aggregate Relation " + conceptAggregate.getUri() + " " + conceptAggregate.getId() + " " + type + " " + target.getUri() + " " + target.getId() );



                  conceptAggregate.addRelated( type, target );
               }
            }
         }
      }
   }


   static public Map<Mention, ConceptAggregate> mapMentionConceptAggregates(
         final Collection<Collection<ConceptAggregate>> conceptAggregateSet ) {
      final Map<Mention, ConceptAggregate> map = new HashMap<>();
      for ( Collection<ConceptAggregate> conceptAggregates : conceptAggregateSet ) {
         for ( ConceptAggregate ci : conceptAggregates ) {
            ci.getMentions().forEach( m -> map.put( m, ci ) );
         }
      }
      return map;
   }


   /**
    * @param conceptAggregate a concept instance that may have relations to other instances
    * @param conceptMap      map of identified annotations to their concept instances
    * @param patientRelations       relations of interest
    * @return relation categories (names) and instances that are targets in such to the given Concept Instance
    */
   static private Map<String, Collection<ConceptAggregate>> getCategoryTargetConceptMap(
         final ConceptAggregate conceptAggregate,
         final Map<Mention, ConceptAggregate> conceptMap,
         final Collection<MentionRelation> patientRelations ) {
      final Map<String, Collection<ConceptAggregate>> map = new HashMap<>();
      final Map<MentionRelation, ConceptAggregate> related
            = getRelatedTargetConceptMap( conceptAggregate, conceptMap, patientRelations );
      for ( Map.Entry<MentionRelation, ConceptAggregate> entry : related.entrySet() ) {
         final String name = entry.getKey().getType();
         map.computeIfAbsent( name, c -> new HashSet<>() )
            .add( entry.getValue() );
      }
      return map;
   }



   /**
    * @param conceptAggregate a concept instance that may have relations to other instances
    * @param conceptMap      map of identified annotations to their concept instances
    * @param patientRelations       relations of interest
    * @return relations and instances related as targets to the given Concept Instance
    */
   static private Map<MentionRelation, ConceptAggregate> getRelatedTargetConceptMap(
         final ConceptAggregate conceptAggregate,
         final Map<Mention, ConceptAggregate> conceptMap,
         final Collection<MentionRelation> patientRelations ) {
      final Map<MentionRelation, ConceptAggregate> map = new HashMap<>();
      for ( Mention mention : conceptAggregate.getMentions() ) {
         final Map<MentionRelation, String> relatedMentionIds
               = MentionUtil.getRelatedTargetIdsMap( patientRelations, mention );
         for ( Map.Entry<MentionRelation, String> entry : relatedMentionIds.entrySet() ) {
            final ConceptAggregate conceptEntry = getMentionAggregate( conceptMap, entry.getValue() );
            if ( conceptEntry != null ) {
               map.put( entry.getKey(), conceptEntry );
            }
         }
      }
      return map;
   }







   static private ConceptAggregate getMentionAggregate( final Map<Mention, ConceptAggregate> conceptMap,
                                                        final String mentionId ) {
      return conceptMap.entrySet()
                       .stream()
                       .filter( e -> e.getKey().getId().equals( mentionId ) )
                       .map( Map.Entry::getValue )
                       .findFirst()
                       .orElse( null );
   }





   static public Map<String, Collection<Mention>> collectDocMentions(
         final Collection<ConceptAggregate> concepts ) {
      final Map<String, Collection<Mention>> docMentions = new HashMap<>();
      for ( ConceptAggregate concept : concepts ) {
         concept.getNoteMentions()
                .forEach( (k,v) -> docMentions.computeIfAbsent( k, s -> new HashSet<>() )
                                              .addAll( v ) );
      }
      return docMentions;
   }

   static public void appendDocMentions( final Collection<ConceptAggregate> concepts,
                                         final Map<String, Collection<Mention>> docMentions ) {
      for ( ConceptAggregate concept : concepts ) {
         concept.getNoteMentions()
                .forEach( (k,v) -> docMentions.computeIfAbsent( k, s -> new HashSet<>() )
                                              .addAll( v ) );
      }
   }


}

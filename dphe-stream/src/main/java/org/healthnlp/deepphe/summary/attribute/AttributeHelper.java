package org.healthnlp.deepphe.summary.attribute;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;
import java.util.stream.Collectors;

public interface AttributeHelper {

   ConceptAggregate getNeoplasm();
   Collection<ConceptAggregate> getAllPatientConcepts();


   //////////////////////////////////////////////////////
   //
   //       Neoplasm Attribute Best Value, URI
   //

   String getBestUriForType();

   double getBestUriScoreForType();

   String getRunnerUpUriForType();

   double getRunnerUpUriScoreForType();


   //////////////////////////////////////////////////////
   //
   //       Neoplasm Attribute Best Value, Concepts
   //

   Collection<ConceptAggregate> getBestConceptsForType();

   default Collection<String> getAllBestUrisForType() {
      return getAllUris( getBestConceptsForType() );
   }

   default Collection<Mention> getAllBestMentionsForType() {
      return getAllMentions( getBestConceptsForType() );
   }


   //////////////////////////////////////////////////////
   //
   //       Attribute, All Values in Neoplasm
   //

   Collection<ConceptAggregate> getAllNeoplasmConceptsForType();

   default Collection<String> getAllNeoplasmUrisForType() {
      return getAllUris( getAllNeoplasmConceptsForType() );
   }

   default Collection<Mention> getAllNeoplasmMentionsForType() {
      return getAllMentions( getAllNeoplasmConceptsForType() );
   }


   //////////////////////////////////////////////////////
   //
   //       Attribute, All Values in Patient
   //

   Collection<ConceptAggregate> getAllPatientConceptsForType();

   default Collection<String> getAllPatientUrisForType() {
      return getAllUris( getAllPatientConceptsForType() );
   }

   default Collection<Mention> getAllPatientMentionsForType() {
      return getAllMentions( getAllPatientConceptsForType() );
   }

   Map<String,Collection<String>> getAllPatientUriRootsForType();


   //////////////////////////////////////////////////////
   //
   //       Features
   //

   List<Integer>  createFeatures();

   List<Integer> createEmptyFeatures();



   //////////////////////////////////////////////////////
   //
   //       Utility
   //

   default Collection<String> getAllUris( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getAllUris )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }


   default Collection<Mention> getAllMentions( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getMentions )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }


   default Map<String,Collection<String>> mapAllConceptsUriRoots( final Collection<ConceptAggregate> concepts ) {
      return mapAllUriRoots( getAllUris( concepts ) );
   }

   default Map<String,Collection<String>> mapAllUriRoots( final Collection<String> uris ) {
      final Map<String,Collection<String>> rootsMap = new HashMap<>();
      uris.forEach( u -> rootsMap.put( u,
                                       getAllPatientUriRootsForType()
                                             .getOrDefault( u, Collections.emptyList() ) ) );
      return rootsMap;
   }




}

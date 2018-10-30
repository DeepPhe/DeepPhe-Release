package org.apache.ctakes.cancer.summary;


import org.apache.ctakes.cancer.ae.ByUriRelationFinder;
import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceMerger;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/10/2018
 */
final public class CiCollapser {

   static private final Logger LOGGER = Logger.getLogger( "CiCollapser" );


   private CiCollapser() {
   }

   static public Collection<ConceptInstance> collapse( final Collection<ConceptInstance> oldInstances ) {
      final Collection<ConceptInstance> allInstances = new HashSet<>( oldInstances );
      for ( ConceptInstance oldInstance : oldInstances ) {
         oldInstance.getRelated().values().forEach( allInstances::addAll );
         oldInstance.getReverseRelated().values().forEach( allInstances::addAll );
      }
      // Map of all instances to new collapsed instances.
      final Map<ConceptInstance, ConceptInstance> collapsedMap = createCollapsedMap( allInstances );

      final Map<ConceptInstance, Map<String, Collection<ConceptInstance>>> newRelationsMap = new HashMap<>();
      final Map<ConceptInstance, Map<String, Collection<ConceptInstance>>> newReverseMap = new HashMap<>();
      for ( ConceptInstance oldInstance : oldInstances ) {
         final ConceptInstance mainNewInstance = collapsedMap.get( oldInstance );
         final Map<String, Collection<ConceptInstance>> oldRelations = oldInstance.getRelated();
         for ( Map.Entry<String, Collection<ConceptInstance>> oldRelationType : oldRelations.entrySet() ) {
            final String relationName = oldRelationType.getKey();
            final Collection<ConceptInstance> newRelated
                  = oldRelationType.getValue().stream()
                                   .map( collapsedMap::get )
                                   .distinct()
                                   .collect( Collectors.toList() );
            newRelationsMap.computeIfAbsent( mainNewInstance, c -> new HashMap<>() );
            final Map<String, Collection<ConceptInstance>> newRelations = newRelationsMap.get( mainNewInstance );
            newRelations.computeIfAbsent( relationName, r -> new ArrayList<>() ).addAll( newRelated );
         }
         final Map<String,Collection<ConceptInstance>> oldReversed = oldInstance.getReverseRelated();
         for ( Map.Entry<String, Collection<ConceptInstance>> oldReverseType : oldReversed.entrySet() ) {
            final String relationName = oldReverseType.getKey();
            final Collection<ConceptInstance> newReversed
                  = oldReverseType.getValue().stream()
                                   .map( collapsedMap::get )
                                   .distinct()
                                   .collect( Collectors.toList() );
            newReverseMap.computeIfAbsent( mainNewInstance, c -> new HashMap<>() );
            final Map<String, Collection<ConceptInstance>> newReverses = newReverseMap.get( mainNewInstance );
            newReverses.computeIfAbsent( relationName, r -> new ArrayList<>() ).addAll( newReversed );
         }
      }

      newRelationsMap.forEach( ConceptInstance::setRelated );
      newReverseMap.forEach( ConceptInstance::setReverseRelated );

      return oldInstances.stream().map( collapsedMap::get ).distinct().collect( Collectors.toList() );
   }

   /**
    * Collapses a collection of concept instances so that alike concepts become one.
    * @param oldInstances All ConceptInstances in the document(s)
    * @return map of all original concept instances to collapsed ConceptInstances
    */
   static private Map<ConceptInstance, ConceptInstance> createCollapsedMap(
         final Collection<ConceptInstance> oldInstances ) {
      final Map<String, Collection<ConceptInstance>> collatedInstances
            = ByUriRelationFinder.collateUriConceptsCloseEnough( oldInstances );

      final Map<ConceptInstance, ConceptInstance> toCollapsedMap = new HashMap<>();
      for ( Collection<ConceptInstance> collatedInstance : collatedInstances.values() ) {
         final ConceptInstance newInstance = collapseConcepts( collatedInstance );
         for ( ConceptInstance oldInstance : collatedInstance ) {
            toCollapsedMap.put( oldInstance, newInstance );
         }
      }
      return toCollapsedMap;
   }

   static public ConceptInstance collapseConcepts( final Collection<ConceptInstance> concepts ) {
      final String patient = concepts.stream()
                                     .map( ConceptInstance::getPatientId )
                                     .distinct()
                                     .sorted()
                                     .collect( Collectors.joining( "_" ) );
      final Map<String, Collection<IdentifiedAnnotation>> docAnnotations
            = ConceptInstanceMerger.mergeDocAnnotations( concepts );
      return ConceptInstanceFactory.createConceptInstance( patient, docAnnotations );
   }

   static public ConceptInstance collapseConcepts( final String uri, final Collection<ConceptInstance> concepts ) {
      final String patient = concepts.stream()
                                       .map( ConceptInstance::getPatientId )
                                       .distinct()
                                       .sorted()
                                       .collect( Collectors.joining( "_" ) );
      final Map<String, Collection<IdentifiedAnnotation>> docAnnotations
            = ConceptInstanceMerger.mergeDocAnnotations( concepts );
      return ConceptInstanceFactory.createConceptInstance( patient, uri, docAnnotations );
   }


   static private void collapseRelations( final Map<ConceptInstance,ConceptInstance> collapsedMap ) {

   }

}


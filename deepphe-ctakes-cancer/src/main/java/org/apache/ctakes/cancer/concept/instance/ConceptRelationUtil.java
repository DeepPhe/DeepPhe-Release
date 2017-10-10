package org.apache.ctakes.cancer.concept.instance;


import org.apache.ctakes.cancer.owl.UriAnnotationCache;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/5/2017
 */
final public class ConceptRelationUtil {

   static private final Logger LOGGER = Logger.getLogger( "RelationUtil" );

   private ConceptRelationUtil() {
   }

//   /**
//    * @param jcas            ye olde ...
//    * @param relationClass   relation type of interest
//    * @param conceptInstance a concept instance that may have relations to other instances
//    * @return instances related to the given Concept Instance
//    */
//   static public Collection<ConceptInstance> getRelatedConcepts( final JCas jcas,
//                                                                 final Class<? extends BinaryTextRelation> relationClass,
//                                                                 final ConceptInstance conceptInstance ) {
//      final Collection<? extends BinaryTextRelation> relations = JCasUtil.select( jcas, relationClass );
//      final Collection<IdentifiedAnnotation> annotations = conceptInstance.getAnnotations().stream()
//            .map( a -> RelationUtil.getAllRelated( relations, a ) )
//            .flatMap( Collection::stream )
//            .distinct()
//            .collect( Collectors.toList() );
//      return ConceptInstanceFactory.createConceptInstances( jcas, annotations );
//   }

   /**
    * @param jcas            ye olde ...
    * @param relationClass   relation type of interest
    * @param conceptInstance a concept instance that may have relations to other instances
    * @return instances related to the given Concept Instance
    */
   static public Map<BinaryTextRelation, ConceptInstance> getRelatedConceptMap( final JCas jcas,
                                                                                final Class<? extends BinaryTextRelation> relationClass,
                                                                                final ConceptInstance conceptInstance ) {
      final Collection<BinaryTextRelation> relations = new ArrayList<>( JCasUtil.select( jcas, relationClass ) );
      final Map<IdentifiedAnnotation, ConceptInstance> conceptMap = ConceptInstanceFactory.createConceptInstanceMap( jcas );
      return getRelatedConceptMap( conceptInstance, conceptMap, relations );
//      final Map<BinaryTextRelation, ConceptInstance> map = new HashMap<>();
//      for ( IdentifiedAnnotation annotation : conceptInstance.getAnnotations() ) {
//         final Map<BinaryTextRelation, IdentifiedAnnotation> relatedAnnotations
//               = RelationUtil.getAllRelatedMap( relations, annotation );
//         for ( Map.Entry<BinaryTextRelation, IdentifiedAnnotation> entry : relatedAnnotations.entrySet() ) {
//            map.put( entry.getKey(), new SimpleConceptInstance( entry.getValue() ) );
//         }
//      }
//      return map;
   }

//   /**
//    * @param jcas            ye olde ...
//    * @param conceptInstance a concept instance that may have relations to other instances
//    * @return instances related to the given Concept Instance
//    */
//   static public Collection<ConceptInstance> getRelatedConcepts( final JCas jcas,
//                                                                 final ConceptInstance conceptInstance ) {
//      return getRelatedConcepts( jcas, BinaryTextRelation.class, conceptInstance );
//   }

   /**
    * @param jcas            ye olde ...
    * @param conceptInstance a concept instance that may have relations to other instances
    * @return instances related to the given Concept Instance
    */
   static public Map<BinaryTextRelation, ConceptInstance> getRelatedConceptMap( final JCas jcas,
                                                                                final ConceptInstance conceptInstance ) {
      return getRelatedConceptMap( jcas, BinaryTextRelation.class, conceptInstance );
   }

   /**
    * @param conceptInstance a concept instance that may have relations to other instances
    * @param conceptMap      map of identified annotations to their concept instances
    * @param relations       relations of interest
    * @return relations and instances related to the given Concept Instance
    */
   static public Map<BinaryTextRelation, ConceptInstance> getRelatedConceptMap( final ConceptInstance conceptInstance,
                                                                                final Map<IdentifiedAnnotation, ConceptInstance> conceptMap,
                                                                                final Collection<BinaryTextRelation> relations ) {
      final Map<BinaryTextRelation, ConceptInstance> map = new HashMap<>();
      for ( IdentifiedAnnotation annotation : conceptInstance.getAnnotations() ) {
         final Map<BinaryTextRelation, IdentifiedAnnotation> relatedAnnotations
               = RelationUtil.getAllRelatedMap( relations, annotation );
         for ( Map.Entry<BinaryTextRelation, IdentifiedAnnotation> entry : relatedAnnotations.entrySet() ) {
            final ConceptInstance conceptEntry = conceptMap.get( entry.getValue() );
            if ( conceptEntry == null ) {
               map.put( entry.getKey(), new SimpleConceptInstance( entry.getValue() ) );
            } else {
               map.put( entry.getKey(), conceptEntry );
            }
         }
      }
      return map;
   }


   /**
    * @param conceptInstance a concept instance that may have relations to other instances
    * @param conceptMap      map of identified annotations to their concept instances
    * @param relations       relations of interest
    * @return relation categories (names) and instances related to the given Concept Instance
    */
   static public Map<String, Collection<ConceptInstance>> getCategoryConceptMap( final ConceptInstance conceptInstance,
                                                                                 final Map<IdentifiedAnnotation, ConceptInstance> conceptMap,
                                                                                 final Collection<BinaryTextRelation> relations ) {
      final Map<String, Collection<ConceptInstance>> map = new HashMap<>();
      final Map<BinaryTextRelation, ConceptInstance> related
            = getRelatedConceptMap( conceptInstance, conceptMap, relations );
      for ( Map.Entry<BinaryTextRelation, ConceptInstance> entry : related.entrySet() ) {
         final String name = entry.getKey().getCategory();
         map.putIfAbsent( name, new HashSet<>() );
         map.get( name ).add( entry.getValue() );
      }
      return map;
   }

   /**
    * @param jcas             ye olde ...
    * @param relationClass    relation type of interest
    * @param conceptInstance  a concept instance that may have relations to other instances
    * @param relatedParentUri a uri of interest
    * @return instances related to the given Concept Instance within the given uri's branch
    */
   static public Collection<ConceptInstance> getRelatedConcepts( final JCas jcas,
                                                                 final Class<? extends BinaryTextRelation> relationClass,
                                                                 final ConceptInstance conceptInstance,
                                                                 final String relatedParentUri ) {
      final Collection<IdentifiedAnnotation> uriAnnotations
            = UriAnnotationCache.getInstance().getBranchAnnotations( jcas, relatedParentUri );
      final Collection<? extends BinaryTextRelation> relations = JCasUtil.select( jcas, relationClass );
      final Collection<IdentifiedAnnotation> annotations = conceptInstance.getAnnotations().stream()
            .map( a -> RelationUtil.getAllRelated( relations, a ) )
            .flatMap( Collection::stream )
            .filter( uriAnnotations::contains )
            .distinct()
            .collect( Collectors.toList() );
      return ConceptInstanceFactory.createConceptInstances( jcas, annotations );
   }

//   /**
//    * @param jcas             ye olde ...
//    * @param conceptInstance  a concept instance that may have relations to other instances
//    * @param relatedParentUri a uri of interest
//    * @return instances related to the given Concept Instance within the given uri's branch
//    */
//   static public Collection<ConceptInstance> getRelatedConcepts( final JCas jcas,
//                                                                 final ConceptInstance conceptInstance,
//                                                                 final String relatedParentUri ) {
//      return getRelatedConcepts( jcas, BinaryTextRelation.class, conceptInstance, relatedParentUri );
//   }

   // TODO refactor all of this to utilize the new Map methods and pass previously created collections
   // TODO should be much faster and more accurate wrt related corefs

   /**
    * @param jcas              ye olde ...
    * @param relationClass     relation type of interest
    * @param conceptInstance   a concept instance that may have relations to other instances
    * @param relatedParentUris uris of interest
    * @return instances related to the given Concept Instance within the given uri's branch
    */
   static public Collection<ConceptInstance> getRelatedConcepts( final JCas jcas,
                                                                 final Class<? extends BinaryTextRelation> relationClass,
                                                                 final ConceptInstance conceptInstance,
                                                                 final String[] relatedParentUris ) {
      return Arrays.stream( relatedParentUris )
            .map( u -> getRelatedConcepts( jcas, relationClass, conceptInstance, u ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );
   }

//   /**
//    * @param jcas             ye olde ...
//    * @param conceptInstance  a concept instance that may have relations to other instances
//    * @param relatedParentUris uris of interest
//    * @return instances related to the given Concept Instance within the given uri's branch
//    */
//   static public Collection<ConceptInstance> getRelatedConcepts( final JCas jcas,
//                                                                 final ConceptInstance conceptInstance,
//                                                                 final String[] relatedParentUris ) {
//      return getRelatedConcepts( jcas, BinaryTextRelation.class, conceptInstance, relatedParentUris );
//   }

   /**
    * @param relationClass     relation type of interest
    * @param conceptInstance   a concept instance that may have relations to other instances
    * @param relatedParentUris uris of interest
    * @return instances related to the given Concept Instance within the given uri's branch
    */
   static public Collection<ConceptInstance> getRelatedConcepts( final Class<? extends BinaryTextRelation> relationClass,
                                                                 final ConceptInstance conceptInstance,
                                                                 final String... relatedParentUris ) {
      final JCas jCas = ConceptInstanceUtil.getJcas( conceptInstance );
      return getRelatedConcepts( jCas, relationClass, conceptInstance, relatedParentUris );
   }

   /**
    * @param conceptInstance   a concept instance that may have relations to other instances
    * @param relatedParentUris uris of interest
    * @return instances related to the given Concept Instance within the given uri's branch
    */
   static public Collection<ConceptInstance> getRelatedConcepts( final ConceptInstance conceptInstance,
                                                                 final String... relatedParentUris ) {
      return getRelatedConcepts( BinaryTextRelation.class, conceptInstance, relatedParentUris );
   }

}

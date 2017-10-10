package org.apache.ctakes.cancer.concept.instance;


import org.apache.ctakes.cancer.owl.OwlUriUtil;
import org.apache.ctakes.cancer.owl.UriAnnotationCache;
import org.apache.ctakes.cancer.util.MarkableHolder;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/9/2016
 */
final public class ConceptInstanceFactory {

   static private final Logger LOGGER = Logger.getLogger( "ConceptInstanceFactory" );

   private ConceptInstanceFactory() {
   }

   /**
    * @param jCas ye olde ...
    * @param uri  uri for instance of interest
    * @return collection of instances with the given uri
    */
   static public Collection<ConceptInstance> createExactConceptInstances( final JCas jCas, final String uri ) {
      return createConceptInstances( jCas, OwlOntologyConceptUtil.getAnnotationsByUri( jCas, uri ) );
   }

   /**
    * @param jCas ye olde ...
    * @param uri  uri for instance of interest
    * @return collection of instances with the given uri and all of its children
    */
   static public Collection<ConceptInstance> createBranchConceptInstances( final JCas jCas, final String uri ) {
      return createConceptInstances( jCas, UriAnnotationCache.getInstance().getBranchAnnotations( jCas, uri ) );
   }


   /**
    * @param jCas ye olde
    * @return Collection of ConceptInstances based upon single Annotations and/or Coreferent Annotations
    */
   static public Collection<ConceptInstance> createConceptInstances( final JCas jCas ) {
      return createConceptInstances( jCas, JCasUtil.select( jCas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jCas        ye olde
    * @param annotations -
    * @return Collection of ConceptInstances based upon single Annotations and/or Coreferent Annotations
    */
   static public Collection<ConceptInstance> createConceptInstances( final JCas jCas,
                                                                     final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations.isEmpty() ) {
         return Collections.emptyList();
      }
      final Collection<CollectionTextRelation> corefRelations = JCasUtil.select( jCas, CollectionTextRelation.class );
      if ( corefRelations == null || corefRelations.isEmpty() || annotations.size() == 1 ) {
         return annotations.stream()
               .map( SimpleConceptInstance::new )
               .collect( Collectors.toSet() );
      }
      final CollectionMap<Paragraph, IdentifiedAnnotation, Set<IdentifiedAnnotation>> paragraphCorefs = new HashSetMap<>();
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jCas );
      final Collection<IdentifiedAnnotation> usedAnnotations = new HashSet<>();
      final Collection<ConceptInstance> conceptInstances = new HashSet<>();
      for ( CollectionTextRelation corefRelation : corefRelations ) {
         final FSList chainHead = corefRelation.getMembers();
         final Collection<IdentifiedAnnotation> markables
               = FSCollectionFactory.create( chainHead, IdentifiedAnnotation.class );
         for ( IdentifiedAnnotation markable : markables ) {
            final Paragraph paragraph = getParagraph( jCas, markable );
            if ( paragraph != null ) {
               paragraphCorefs.placeValue( paragraph, markable );
            } else {
               LOGGER.error( "No paragraph for " + markable.getCoveredText() );
            }
         }
         for ( Collection<IdentifiedAnnotation> paragraphMarkables : paragraphCorefs.getAllCollections() ) {
            final Collection<IdentifiedAnnotation> docAnnotations = MarkableHolder.getAnnotations( documentId, paragraphMarkables );
            if ( docAnnotations != null && !docAnnotations.isEmpty() && docAnnotations.stream().anyMatch( annotations::contains ) ) {
               if ( docAnnotations.size() == 1 ) {
                  docAnnotations.stream()
                        .findFirst()
                        .map( SimpleConceptInstance::new )
                        .ifPresent( conceptInstances::add );
               } else {
                  final String stemUri = OwlUriUtil.getStemUri( docAnnotations );
                  conceptInstances.add( new CorefConceptInstance( stemUri, docAnnotations ) );
               }
               usedAnnotations.addAll( docAnnotations );
            }
         }
         paragraphCorefs.clear();
      }
      annotations.stream()
            .filter( a -> !usedAnnotations.contains( a ) )
            .map( SimpleConceptInstance::new )
            .forEach( conceptInstances::add );
      return conceptInstances;
   }

   /**
    * @param jCas            ye olde
    * @param rootAnnotations annotations serving as concept instance roots
    * @param allAnnotations  all annotation candidates for corefs
    * @return Collection of ConceptInstances based upon single Annotations and/or Coreferent Annotations
    */
   static public Collection<ConceptInstance> createConceptInstances( final JCas jCas,
                                                                     final Collection<IdentifiedAnnotation> rootAnnotations,
                                                                     final Collection<IdentifiedAnnotation> allAnnotations ) {
      final Predicate<ConceptInstance> isWanted = ci -> ci.getAnnotations().stream().anyMatch( rootAnnotations::contains );
      final Collection<ConceptInstance> allInstances = createConceptInstances( jCas, allAnnotations );
      return allInstances.stream().filter( isWanted ).collect( Collectors.toList() );
   }

   /**
    * @param jCas ye olde
    * @return Collection of ConceptInstances based upon single Annotations and/or Coreferent Annotations
    */
   static public Map<IdentifiedAnnotation, ConceptInstance> createConceptInstanceMap( final JCas jCas ) {
      return createConceptInstanceMap( jCas, JCasUtil.select( jCas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jCas        ye olde
    * @param annotations -
    * @return Collection of ConceptInstances based upon single Annotations and/or Coreferent Annotations
    */
   static public Map<IdentifiedAnnotation, ConceptInstance> createConceptInstanceMap( final JCas jCas,
                                                                                      final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Collection<CollectionTextRelation> corefRelations = JCasUtil.select( jCas, CollectionTextRelation.class );
      if ( corefRelations == null || corefRelations.isEmpty() || annotations.size() == 1 ) {
         return annotations.stream().collect( Collectors.toMap( Function.identity(), SimpleConceptInstance::new ) );
      }
      final Map<Paragraph, Collection<IdentifiedAnnotation>> paragraphs
            = JCasUtil.indexCovered( jCas, Paragraph.class, IdentifiedAnnotation.class );
      final Collection<Collection<IdentifiedAnnotation>> chains = new ArrayList<>();
      for ( Collection<IdentifiedAnnotation> paragraph : paragraphs.values() ) {
         for ( CollectionTextRelation corefRelation : corefRelations ) {
            final FSList chainHead = corefRelation.getMembers();
            final Collection<IdentifiedAnnotation> markables
                  = FSCollectionFactory.create( chainHead, IdentifiedAnnotation.class );
            final Collection<IdentifiedAnnotation> paragraphCorefs
                  = markables.stream().filter( paragraph::contains ).collect( Collectors.toList() );
            if ( !paragraphCorefs.isEmpty() ) {
               chains.add( paragraphCorefs );
            }
         }
      }
      final Collection<IdentifiedAnnotation> usedAnnotations = new HashSet<>();
      final Map<IdentifiedAnnotation, ConceptInstance> map = new HashMap<>();
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jCas );
      for ( Collection<IdentifiedAnnotation> chain : chains ) {
         final Collection<IdentifiedAnnotation> docAnnotations = MarkableHolder.getAnnotations( documentId, chain );
         if ( docAnnotations.size() == 1 ) {
            docAnnotations.forEach( a -> map.put( a, new SimpleConceptInstance( a ) ) );
         } else {
            final String stemUri = OwlUriUtil.getStemUri( docAnnotations );
            final ConceptInstance corefConcept = new CorefConceptInstance( stemUri, docAnnotations );
            docAnnotations.forEach( a -> map.put( a, corefConcept ) );
         }
         usedAnnotations.addAll( docAnnotations );
      }
      annotations.stream()
            .filter( a -> !usedAnnotations.contains( a ) )
            .forEach( a -> map.put( a, new SimpleConceptInstance( a ) ) );
      return map;
   }

   static private Paragraph getParagraph( final JCas jCas, final IdentifiedAnnotation markable ) {
      return JCasUtil.selectCovering( jCas, Paragraph.class, markable ).stream().findFirst().orElse( null );
   }

   // TODO run a sample note from Alon and output Paul's "observation_fact" table with adverse event relations
   //  - Relation as an "entity" row, with some sort of tuple and the text surrounding the relation entities

   // TODO store values in concept instance
   static private Collection<IdentifiedAnnotation> createValues( final JCas jCas,
                                                                 final Collection<IdentifiedAnnotation> annotations ) {
      final Collection<String> annotationUriRoots =
            annotations.stream()
                  .map( OwlOntologyConceptUtil::getUris )
                  .map( Collection::stream )
                  .flatMap( Function.identity() )
                  .map( OwlOntologyConceptUtil::getUriRootsStream )
                  .flatMap( Function.identity() )
                  .collect( Collectors.toSet() );
      final Collection<String> valueTypes = getValueTypes( annotationUriRoots );
      if ( valueTypes.isEmpty() ) {
         return Collections.emptySet();
      }
      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      if ( relations.isEmpty() ) {
         return Collections.emptySet();
      }
      return annotations.stream()
            .map( a -> RelationUtil.getSecondArguments( relations, a ) )
            .map( Collection::stream )
            .flatMap( Function.identity() )
            .collect( Collectors.toSet() );
      // TODO - create values from relations based upon uri(s) of annotations.
      // Neoplasms get size, devices do not, etc.  hunt Binaries by name?
      //  Make a static group of relation names (size_of, metastasis_of) ...  below
   }

   static private Collection<String> getValueTypes( final Collection<String> annotationUris ) {
      return annotationUris.stream()
            .map( ConceptInstanceFactory::getValueTypes )
            .filter( Objects::nonNull )
            .map( Collection::stream )
            .flatMap( Function.identity() )
            .collect( Collectors.toSet() );
   }

   static private Collection<String> getValueTypes( final String annotationUri ) {
//      //  TODO better as a hashmap<uri,collection<name>>
      return null;
   }


}

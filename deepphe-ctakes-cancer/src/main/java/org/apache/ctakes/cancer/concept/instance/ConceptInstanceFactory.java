package org.apache.ctakes.cancer.concept.instance;


import org.apache.ctakes.cancer.ae.coref.CorefUtil;
import org.apache.ctakes.cancer.uri.UriAnnotationUtil;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.core.util.*;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.RelationConstants;

import java.util.*;
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



   static private void addRelations( final Collection<Collection<ConceptInstance>> conceptInstanceSet,
                                     final Collection<BinaryTextRelation> relations ) {
      final Map<IdentifiedAnnotation,ConceptInstance> annotationConceptInstances
            = createAnnotationConceptInstanceMap( conceptInstanceSet );
      for ( Collection<ConceptInstance> conceptInstances : conceptInstanceSet ) {
         for ( ConceptInstance conceptInstance : conceptInstances ) {
            final Map<String, Collection<ConceptInstance>> relatedCis
                  = getCategoryTargetConceptMap( conceptInstance, annotationConceptInstances, relations );
            for ( Map.Entry<String,Collection<ConceptInstance>> related : relatedCis.entrySet() ) {
               final String type = related.getKey();
               for ( ConceptInstance target : related.getValue() ) {
                  conceptInstance.addRelated( type, target );
               }
            }
         }
      }
   }



   /**
    *
    * @param patientId -
    * @param annotationDocs map of annotations to their enclosing documents
    * @param relations -
    * @return map of best uri to its concept instances
    */
   static public Map<String,Collection<ConceptInstance>> createUriConceptInstanceMap(
         final String patientId,
         final Map<IdentifiedAnnotation,String> annotationDocs,
         final Collection<BinaryTextRelation> relations ) {

      final Map<String,List<IdentifiedAnnotation>> uriAnnotationGroups = UriAnnotationUtil.getAssociatedUriAnnotations( annotationDocs.keySet() );

      final Map<Annotation,Collection<String>> locationUris = new HashMap<>();
      final Map<Annotation,Collection<String>> lateralityUris = new HashMap<>();
      buildPlacements( relations, locationUris, lateralityUris );

      final Map<String,Collection<ConceptInstance>> conceptInstances = new HashMap<>();
      final Collection<IdentifiedAnnotation> usedAnnotations = new ArrayList<>();
      for ( List<IdentifiedAnnotation> uriAnnotationGroup : uriAnnotationGroups.values() ) {
         final List<List<IdentifiedAnnotation>> chains = new ArrayList<>();
         CorefUtil.collateCoref( chains, uriAnnotationGroup, locationUris, lateralityUris );
         for ( List<IdentifiedAnnotation> chain : chains ) {
            if ( chain.size() > 1 ) {
               final String bestUri = UriAnnotationUtil.getMostSpecificUri( chain );
               final Map<String, Collection<IdentifiedAnnotation>> docAnnotations = new HashMap<>();
               for ( IdentifiedAnnotation annotation : chain ) {
                  docAnnotations.computeIfAbsent( annotationDocs.get( annotation ), d -> new HashSet<>() )
                                .add( annotation );
               }
               conceptInstances.computeIfAbsent( bestUri, ci -> new ArrayList<>() )
                               .add( new CorefConceptInstance( patientId, bestUri, docAnnotations ) );
               usedAnnotations.addAll( chain );
            }
         }
      }
      annotationDocs.keySet().removeAll( usedAnnotations );
      for ( Map.Entry<IdentifiedAnnotation,String> annotationDoc : annotationDocs.entrySet() ) {
         final String bestUri = Neo4jOntologyConceptUtil.getUri( annotationDoc.getKey() );
         if ( bestUri.isEmpty() ) {
            continue;
         }
         conceptInstances
               .computeIfAbsent( bestUri, ci -> new HashSet<>() )
               .add( new SimpleConceptInstance( patientId, annotationDoc.getValue(), annotationDoc.getKey() ) );
      }

      addRelations( conceptInstances.values(), relations );
      return conceptInstances;
   }

   /**
    *
    * @param patientId -
    * @param annotationDocs map of annotations to their enclosing documents
    * @param relations -
    * @return map of best uri to its concept instances
    */
   static public Map<String,Collection<ConceptInstance>> createUriConceptInstanceMap2(
         final String patientId,
         final Map<IdentifiedAnnotation,String> annotationDocs,
         final Collection<BinaryTextRelation> relations,
         final Collection<CollectionTextRelation> corefs,
         final Map<Markable, Collection<IdentifiedAnnotation>> markableAnnotations ) {

      final Map<String,Collection<String>> corefUriLists = new HashMap<>();
      for ( CollectionTextRelation coref : corefs ) {
         final FSList chainHead = coref.getMembers();
         final Collection<Markable> markables = FSCollectionFactory.create( chainHead, Markable.class );
         final Collection<String> corefUris = markables.stream()
                                                       .map( markableAnnotations::get )
                                                       .flatMap( Collection::stream )
                                                       .map( Neo4jOntologyConceptUtil::getUri )
                                                       .collect( Collectors.toSet() );
         for ( String uri : corefUris ) {
            corefUriLists.computeIfAbsent( uri, u -> new HashSet<>() ).addAll( corefUris );
         }
      }

      final Map<String,List<IdentifiedAnnotation>> uriAnnotationMap = UriAnnotationUtil.mapUriAnnotations( annotationDocs.keySet() );

      final Map<String,Collection<String>> associatedUris = UriUtil.getAssociatedUriMap( uriAnnotationMap.keySet() );

      // TODO associatedUris is a map of uri branches to their best root.
      // TODO corefUriLists is a map of all uris in a coref to a chain of all uris in corefs.

      final Map<String,Collection<String>> associatedRoots = new HashMap<>( associatedUris.size() );

      for ( Map.Entry<String,Collection<String>> associated : associatedUris.entrySet() ) {
         final String root = associated.getKey();
         for ( String uri : associated.getValue() ) {
            final Collection<String> corefUriList = corefUriLists.get( uri );
            if ( corefUriList != null ) {
               for ( String corefUri : corefUriList ) {
                  for ( Map.Entry<String, Collection<String>> associated2 : associatedUris.entrySet() ) {
                     if ( associated2.getValue().stream().anyMatch( corefUri::equals ) ) {
                        associatedRoots.computeIfAbsent( root, u -> new HashSet<>() )
                                      .add( associated2.getKey() );
                        associatedRoots.computeIfAbsent( associated2.getKey(), u -> new HashSet<>() )
                                       .add( root );
                     }
                  }
               }
            }
         }
      }

      final Collection<Collection<String>> finalBranches = new ArrayList<>( associatedUris.size() );
      final Collection<String> usedRoots = new HashSet<>( associatedRoots.size() );
      for ( Map.Entry<String,Collection<String>> associated : associatedUris.entrySet() ) {
         if ( usedRoots.contains( associated.getKey() ) ) {
            continue;
         }
         final Collection<String> corefRoots = associatedRoots.get( associated.getKey() );
         if ( corefRoots == null || corefRoots.isEmpty() ) {
            finalBranches.add( associated.getValue() );
         } else {
            final Collection<String> finalCoref = new HashSet<>( associated.getValue() );
            for ( String corefRoot : corefRoots ) {
               finalCoref.addAll( associatedUris.get( corefRoot ) );
               usedRoots.add( corefRoot );
            }
            finalBranches.add( finalCoref );
         }
      }


      final Map<Annotation,Collection<String>> locationUris = new HashMap<>();
      final Map<Annotation,Collection<String>> lateralityUris = new HashMap<>();
      buildPlacements( relations, locationUris, lateralityUris );

      final Map<String,Collection<ConceptInstance>> conceptInstances = new HashMap<>();
      final Collection<IdentifiedAnnotation> usedAnnotations = new ArrayList<>();

      for ( Collection<String> finalBranch : finalBranches ) {
         final Collection<IdentifiedAnnotation> annotationGroup = finalBranch.stream().map( uriAnnotationMap::get ).flatMap( Collection::stream ).collect( Collectors.toList() );
         final List<List<IdentifiedAnnotation>> chains = new ArrayList<>();
         CorefUtil.collateCoref( chains, annotationGroup, locationUris, lateralityUris );
         for ( List<IdentifiedAnnotation> chain : chains ) {
            if ( chain.size() > 1 ) {
               final String bestUri = UriAnnotationUtil.getMostSpecificUri( chain );
               final Map<String, Collection<IdentifiedAnnotation>> docAnnotations = new HashMap<>();
               for ( IdentifiedAnnotation annotation : chain ) {
                  docAnnotations.computeIfAbsent( annotationDocs.get( annotation ), d -> new HashSet<>() )
                                .add( annotation );
               }
               conceptInstances.computeIfAbsent( bestUri, ci -> new ArrayList<>() )
                               .add( new CorefConceptInstance( patientId, bestUri, docAnnotations ) );
               usedAnnotations.addAll( chain );
            }
         }
      }
      annotationDocs.keySet().removeAll( usedAnnotations );
      for ( Map.Entry<IdentifiedAnnotation,String> annotationDoc : annotationDocs.entrySet() ) {
         final String bestUri = Neo4jOntologyConceptUtil.getUri( annotationDoc.getKey() );
         if ( bestUri.isEmpty() ) {
            continue;
         }
         conceptInstances
               .computeIfAbsent( bestUri, ci -> new HashSet<>() )
               .add( new SimpleConceptInstance( patientId, annotationDoc.getValue(), annotationDoc.getKey() ) );
      }

      addRelations( conceptInstances.values(), relations );
      return conceptInstances;
   }


   static private void buildPlacements( final Collection<BinaryTextRelation> relations,
                                        final Map<Annotation,Collection<String>> locationUris,
                                        final Map<Annotation,Collection<String>> lateralityUris ) {
      for ( BinaryTextRelation relation : relations ) {
         if ( relation.getCategory().equals( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE ) ) {
            final String uri = Neo4jOntologyConceptUtil.getUri( (IdentifiedAnnotation)relation.getArg2().getArgument() );
            locationUris.computeIfAbsent( relation.getArg1().getArgument(), a -> new HashSet<>() ).add( uri );
         } else if ( relation.getCategory().equals( RelationConstants.HAS_LATERALITY ) ) {
            final String uri = Neo4jOntologyConceptUtil.getUri( (IdentifiedAnnotation)relation.getArg2().getArgument() );
            lateralityUris.computeIfAbsent( relation.getArg1().getArgument(), a -> new HashSet<>() ).add( uri );
         }
      }
   }




   static public ConceptInstance createConceptInstance( final String patientId,
                                                        final Map<String, Collection<IdentifiedAnnotation>> docAnnotations ) {
      if ( docAnnotations.size() == 1 ) {
         final String documentId = new ArrayList<>( docAnnotations.keySet() ).get( 0 );
         final List<IdentifiedAnnotation> annotations = new ArrayList<>( docAnnotations.get( documentId ) );
         if ( annotations.size() == 1 ) {
            return new SimpleConceptInstance( patientId, documentId, annotations.get( 0 ) );
         }
      }
      final Collection<IdentifiedAnnotation> allAnnotations
            = docAnnotations.values().stream()
                            .flatMap( Collection::stream )
                            .collect( Collectors.toList() );
      final String bestUri = UriAnnotationUtil.getMostSpecificUri( allAnnotations );
      return new CorefConceptInstance( patientId, bestUri, docAnnotations );
   }

   static public ConceptInstance createConceptInstance( final String patientId,
                                                        final String uri,
                                                        final Map<String, Collection<IdentifiedAnnotation>> docAnnotations ) {
      if ( docAnnotations.size() == 1 ) {
         final String documentId = new ArrayList<>( docAnnotations.keySet() ).get( 0 );
         final List<IdentifiedAnnotation> annotations = new ArrayList<>( docAnnotations.get( documentId ) );
         if ( annotations.size() == 1 ) {
            return new SimpleConceptInstance( patientId, documentId, annotations.get( 0 ) );
         }
      }
      return new CorefConceptInstance( patientId, uri, docAnnotations );

   }




   static public Map<IdentifiedAnnotation,ConceptInstance> createAnnotationConceptInstanceMap(
         final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
      return createAnnotationConceptInstanceMap( uriConceptInstances.values() );
   }

   static public Map<IdentifiedAnnotation,ConceptInstance> createAnnotationConceptInstanceMap(
         final Collection<Collection<ConceptInstance>> conceptInstanceSet ) {
      final Map<IdentifiedAnnotation,ConceptInstance> map = new HashMap<>();
      for ( Collection<ConceptInstance> conceptInstances : conceptInstanceSet ) {
         for ( ConceptInstance ci : conceptInstances ) {
            ci.getAnnotations().forEach( a -> map.put( a, ci ) );
         }
      }
      return map;
   }





   /**
    * @param conceptInstance a concept instance that may have relations to other instances
    * @param conceptMap      map of identified annotations to their concept instances
    * @param relations       relations of interest
    * @return relation categories (names) and instances that are targets in such to the given Concept Instance
    */
   static private Map<String, Collection<ConceptInstance>> getCategoryTargetConceptMap( final ConceptInstance conceptInstance,
                                                                                       final Map<IdentifiedAnnotation, ConceptInstance> conceptMap,
                                                                                       final Collection<BinaryTextRelation> relations ) {
      final Map<String, Collection<ConceptInstance>> map = new HashMap<>();
      final Map<BinaryTextRelation, ConceptInstance> related
            = getRelatedTargetConceptMap( conceptInstance, conceptMap, relations );
      for ( Map.Entry<BinaryTextRelation, ConceptInstance> entry : related.entrySet() ) {
         final String name = entry.getKey().getCategory();
         map.computeIfAbsent( name, c -> new HashSet<>() )
            .add( entry.getValue() );
      }
      return map;
   }

   /**
    * @param conceptInstance a concept instance that may have relations to other instances
    * @param conceptMap      map of identified annotations to their concept instances
    * @param relations       relations of interest
    * @return relations and instances related as targets to the given Concept Instance
    */
   static private Map<BinaryTextRelation, ConceptInstance> getRelatedTargetConceptMap( final ConceptInstance conceptInstance,
                                                                                       final Map<IdentifiedAnnotation, ConceptInstance> conceptMap,
                                                                                       final Collection<BinaryTextRelation> relations ) {
      final Map<BinaryTextRelation, ConceptInstance> map = new HashMap<>();
      for ( IdentifiedAnnotation annotation : conceptInstance.getAnnotations() ) {
         final Map<BinaryTextRelation, IdentifiedAnnotation> relatedAnnotations
               = RelationUtil.getRelatedTargetsMap( relations, annotation );
         for ( Map.Entry<BinaryTextRelation, IdentifiedAnnotation> entry : relatedAnnotations.entrySet() ) {
            final ConceptInstance conceptEntry = conceptMap.get( entry.getValue() );
            if ( conceptEntry != null ) {
               map.put( entry.getKey(), conceptEntry );
            }
         }
      }
      return map;
   }


}

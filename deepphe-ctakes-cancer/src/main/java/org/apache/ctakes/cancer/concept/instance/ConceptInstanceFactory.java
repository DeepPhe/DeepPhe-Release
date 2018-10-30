package org.apache.ctakes.cancer.concept.instance;


import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.semantic.SemanticTui;
import org.apache.ctakes.core.util.*;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Function;
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
    * This method does not separate CIs by Paragraph like the v1 method.  We trust the corefs to be accurate.
    * @param jCas ye olde
    * @return a map of uris and all Concept Instances in the document
    */
   static public Map<String,Collection<ConceptInstance>> createUriConceptInstanceMap( final JCas jCas ) {
      return createUriConceptInstanceMap( jCas, JCasUtil.select( jCas, IdentifiedAnnotation.class ) );
   }

   /**
    * This method does not separate CIs by Paragraph like the v1 method.  We trust the corefs to be accurate.
    * @param jCas ye olde
    * @param annotations all annotations
    * @return a map of uris and all Concept Instances in the document
    */
   static private Map<String,Collection<ConceptInstance>> createUriConceptInstanceMap( final JCas jCas,
                                                                     final Collection<IdentifiedAnnotation> annotations ) {
      final Map<IdentifiedAnnotation, Collection<Integer>> markableCorefs
            = EssentialAnnotationUtil.createMarkableCorefs( jCas );
      // essentials contains all EntityMentions, EventMentions, TimeMentions,
      // plus Annotations required for coreferences and relations
      final Collection<IdentifiedAnnotation> essentials = EssentialAnnotationUtil.getRequiredAnnotations( jCas,
            annotations,
            markableCorefs );
      for ( IdentifiedAnnotation essential : essentials ) {
         if ( !Neo4jOntologyConceptUtil.getUris( essential ).isEmpty() ) {
            // already have uri
            continue;
         }
         if ( essential instanceof TimeMention ) {
            addUmlsConcept( jCas, essential, UriConstants.CUI_TIME, SemanticTui.T079.name(),
                  SemanticTui.T079.getSemanticType(), UriConstants.UNKNOWN );
         } else if ( essential instanceof EventMention ) {
            addUmlsConcept( jCas, essential, UriConstants.CUI_EVENT, SemanticTui.T051.name(),
                  SemanticTui.T051.getSemanticType(), UriConstants.EVENT );
         } else {
            final String cui = OntologyConceptUtil.getCuis( essential )
                                                  .stream()
                                                  .findAny()
                                                  .orElse( UriConstants.CUI_UNKNOWN );
            final SemanticTui tui = SemanticTui.getTuis( essential ).stream()
                                               .findFirst().orElse( SemanticTui.UNKNOWN );
            addUmlsConcept( jCas, essential, cui, tui.name(), tui.getSemanticType(), UriConstants.UNKNOWN );
         }
      }
      return createUriConceptInstanceMap( jCas, markableCorefs, essentials );
   }

   /**
    * This method does not separate CIs by Paragraph like the v1 method.  We trust the corefs to be accurate.
    * @param markableCorefs -
    * @param essentials -
    * @return -
    */
   static private Map<String,Collection<ConceptInstance>> createUriConceptInstanceMap( final JCas jCas,
         final Map<IdentifiedAnnotation, Collection<Integer>> markableCorefs,
         final Collection<IdentifiedAnnotation> essentials ) {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jCas );
      // Create coref CIs
      final Map<Integer,Collection<IdentifiedAnnotation>> chains = new HashMap<>();
      for ( Map.Entry<IdentifiedAnnotation,Collection<Integer>> corefEntry : markableCorefs.entrySet() ) {
         for ( Integer index : corefEntry.getValue() ) {
            chains.computeIfAbsent( index, a -> new ArrayList<>() ).add( corefEntry.getKey() );
         }
      }
      final Map<String,Collection<ConceptInstance>> conceptInstances = new HashMap<>();
      for ( Collection<IdentifiedAnnotation> chain : chains.values() ) {
         final String bestUri = getBestIaUri( chain );
         final Map<String, Collection<IdentifiedAnnotation>> docAnnotations = new HashMap<>( 1 );
         docAnnotations.put( documentId, chain );
         conceptInstances.computeIfAbsent( bestUri, ci -> new HashSet<>() )
                         .add( new CorefConceptInstance( patientId, bestUri, docAnnotations ) );
         essentials.removeAll( chain );
      }
      essentials
            .forEach( a -> conceptInstances
                  .computeIfAbsent( Neo4jOntologyConceptUtil.getUri( a ), ci -> new HashSet<>() )
                  .add( new SimpleConceptInstance( patientId, documentId, a ) ) );
      addRelations( jCas, conceptInstances );
      return conceptInstances;
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
      final String bestUri = getBestIaUri( allAnnotations );
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


   static private void addRelations( final JCas jCas,
                                     final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
      final Map<IdentifiedAnnotation,ConceptInstance> annotationConceptInstances
            = createAnnotationConceptInstanceMap( uriConceptInstances );
      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      for ( Collection<ConceptInstance> conceptInstances : uriConceptInstances.values() ) {
         for ( ConceptInstance conceptInstance : conceptInstances ) {
            final Map<String, Collection<ConceptInstance>> relatedCis
                  = getCategoryTargetConceptMap( conceptInstance, annotationConceptInstances, relations );
            for ( Map.Entry<String,Collection<ConceptInstance>> related : relatedCis.entrySet() ) {
               final String type = related.getKey();
               for ( ConceptInstance target : related.getValue() ) {
                  conceptInstance.addRelated( type, target );
                  target.addReverseRelated( type, conceptInstance );
               }
            }
         }
      }
   }


   static public Map<IdentifiedAnnotation,ConceptInstance> createAnnotationConceptInstanceMap(
         final Map<String,Collection<ConceptInstance>> uriConceptInstances ) {
      final Map<IdentifiedAnnotation,ConceptInstance> map = new HashMap<>();
      for ( Collection<ConceptInstance> conceptInstances : uriConceptInstances.values() ) {
         for ( ConceptInstance ci : conceptInstances ) {
            ci.getAnnotations().forEach( a -> map.put( a, ci ) );
         }
      }
      return map;
   }


   // TODO move

   /**
    * Gets a stem uri.  For example, when given "left breast", "upper right breast"
    * it will return the common parent "breast".
    *
    * @param annotations -
    * @return a uri that covers all the leaves for all the given annotations
    */
   static private String getStemIaUri( final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return null;
      }
      final Collection<String> allUris = annotations.stream()
                                                    .map( Neo4jOntologyConceptUtil::getUris )
                                                    .flatMap( Collection::stream )
                                                    .distinct()
                                                    .collect( Collectors.toList() );
      final String mostContainedUri = getMostContainedUri( allUris );
      if ( mostContainedUri != null ) {
         return mostContainedUri;
      }
      return getSmallestRootUri( allUris );
   }

   /**
    * Gets a stem uri.  For example, when given "left breast", "upper right breast"
    * it will return the common parent "breast".
    *
    * @param annotations -
    * @return a uri that covers all the leaves for all the given annotations
    */
   static private String getMostContainedIaUri( final Collection<IdentifiedAnnotation> annotations ) {
      final Collection<String> allUris = annotations.stream()
                                                    .map( Neo4jOntologyConceptUtil::getUris )
                                                    .flatMap( Collection::stream )
                                                    .distinct()
                                                    .collect( Collectors.toList() );
      return getMostContainedUri( allUris );
   }

   /**
    * Gets a stem uri.  For example, when given "left breast", "upper right breast"
    * it will return the common parent "breast".
    *
    * @param allUris -
    * @return a uri that covers all the leaves for all the given annotations
    */
   static private String getMostContainedUri( final Collection<String> allUris ) {
      if ( allUris.size() == 1 ) {
         return allUris.stream()
                       .findFirst()
                       .get();
      }
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      String mostContainmentUri = "";
      long mostContainment = 0;
      for ( String uri : allUris ) {
         final Collection<String> branch = SearchUtil.getBranchUris( graphDb, uri );
         final long containment = allUris.stream()
                                         .filter( branch::contains )
                                         .count();
         if ( containment > mostContainment ) {
            if ( containment == allUris.size() ) {
               return uri;
            }
            mostContainmentUri = uri;
            mostContainment = containment;
         }
      }
      if ( mostContainment == allUris.size() - 1 ) {
         return mostContainmentUri;
      }
      return null;
   }


   /**
    * Gets a stem uri.  For example, when given "left breast", "upper right breast"
    * it will return the common parent "breast".
    *
    * @param annotations -
    * @return a uri that covers all the leaves for all the given annotations
    */
   static private String getBestIaUri( final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return null;
      }
      final Collection<String> allUris = annotations.stream()
                                                    .map( Neo4jOntologyConceptUtil::getUris )
                                                    .flatMap( Collection::stream )
                                                    .distinct()
                                                    .collect( Collectors.toList() );
      final String bestUri = getMostSpecificUri( allUris );
      if ( bestUri != null ) {
         return bestUri;
      }
      return getSmallestRootUri( allUris );
   }

   /**
    * Gets a "best leaf" uri.  For example, when given "left breast", "breast"
    * it will return the common leaf "left breast".
    * This assumes that the uris are in a common branch.
    * Something like "right breast" "left breast" "breast" would result in the return of "right breast"
    *
    * @param allUris -
    * @return a uri that is the most "specific"
    */
   static public String getMostSpecificUri( final Collection<String> allUris ) {
      if ( allUris.size() == 1 ) {
         return new ArrayList<>( allUris ).get( 0 );
      }
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance()
                                                                 .getGraph();
      final Map<String, Collection<String>> branchMap
            = allUris.stream()
                     .distinct()
                     .collect( Collectors.toMap( Function.identity(), u -> SearchUtil.getBranchUris( graphDb, u ) ) );
      long bestCount = 0;
      String bestUri = "";
      for ( String uri : allUris ) {
         long count = branchMap.entrySet().stream()
                               .filter( e -> !uri.equals( e.getKey() ) )
                               .map( Map.Entry::getValue )
                               .filter( uris -> uris.contains( uri ) )
                               .count();
         if ( count > bestCount ) {
            bestUri = uri;
            bestCount = count;
         } else if ( count == bestCount && uri.length() > bestUri.length() ) {
            // Not exactly brillant, but assume that something with a longer name is more specific
            bestUri = uri;
         }
      }
      return bestUri;
   }


   static private String getSmallestRootIaUri( final Collection<IdentifiedAnnotation> annotations ) {
      final Collection<String> allUris = annotations.stream()
                                                 .map( Neo4jOntologyConceptUtil::getUris )
                                                 .flatMap( Collection::stream )
                                                 .distinct()
                                                 .collect( Collectors.toList() );
      return getSmallestRootUri( allUris );
   }

   static private String getSmallestRootUri( final Collection<String> allUris ) {
      long leastLength = Integer.MAX_VALUE;
      String bestUri = "";
      for ( String uri : allUris ) {
         long length = Neo4jOntologyConceptUtil.getRootUris( uri ).size();
         if ( length < leastLength ) {
            leastLength = length;
            bestUri = uri;
         }
      }
      return bestUri;
   }

   static private String getLongestRootUri( final Collection<String> allUris ) {
      long longestLength = 0;
      String bestUri = "";
      for ( String uri : allUris ) {
         long length = Neo4jOntologyConceptUtil.getRootUris( uri ).size();
         if ( length > longestLength ) {
            longestLength = length;
            bestUri = uri;
         }
      }
      return bestUri;
   }

   private static void addUmlsConcept( final JCas jCas, final IdentifiedAnnotation annotation, final String cui,
                                       final String tui, final String prefText, final String uri ) {
      int newSize = 1;
      FSArray newConcepts;
      final FSArray oldConcepts = annotation.getOntologyConceptArr();
      if ( oldConcepts != null ) {
         newConcepts = new FSArray( jCas, oldConcepts.size()+1 );
         for ( int i=0; i<oldConcepts.size(); i++ ) {
            newConcepts.set( i, oldConcepts.get( i ) );
         }
      } else {
         newConcepts = new FSArray( jCas, 1 );
      }
      final UmlsConcept concept = createUmlsConcept( jCas, UriConstants.DPHE_SCHEME, cui, tui, prefText, uri );
      newConcepts.set( newConcepts.size()-1, concept );
      annotation.setOntologyConceptArr( newConcepts );
   }

   //TODO  Copied this from DefaultUmlsConceptCreator in ctakes where it is private.  Make public and call.
   private static UmlsConcept createUmlsConcept(JCas jcas, String codingScheme, String cui, String tui, String preferredText, String code) {
      UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCodingScheme( codingScheme );
      umlsConcept.setCui(cui);
      if (tui != null) {
         umlsConcept.setTui(tui);
      }
      if (preferredText != null && !preferredText.isEmpty()) {
         umlsConcept.setPreferredText(preferredText);
      }
      if (code != null) {
         umlsConcept.setCode(code);
      }
      return umlsConcept;
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

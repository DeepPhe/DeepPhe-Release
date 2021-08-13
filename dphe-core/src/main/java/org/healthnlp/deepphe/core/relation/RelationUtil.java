package org.healthnlp.deepphe.core.relation;


import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
//import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
//import org.healthnlp.deepphe.neo4j.RelationConstants;

//import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.ctakes.typesystem.type.constants.CONST.NE_DISCOVERY_TECH_EXPLICIT_AE;

//import org.healthnlp.deepphe.neo4j.UriConstants;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/25/2016
 */
//@Immutable
final public class RelationUtil {
   private RelationUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "RelationUtil" );

   static private final Pattern WHITESPACE = Pattern.compile( "\\s+" );

   static public <T extends IdentifiedAnnotation> Map<T, Collection<BinaryTextRelation>> getRelatedAsFirst(
         final JCas jCas,
         final Collection<T> annotations, final String relationName ) {
      final Collection<BinaryTextRelation> relations
            = JCasUtil.select( jCas, BinaryTextRelation.class ).stream()
                      .filter( r -> r.getCategory().equals( relationName ) )
                      .collect( Collectors.toList() );
      if ( relations.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<T, Collection<BinaryTextRelation>> relatedMap = new HashMap<>();
      for ( T annotation : annotations ) {
         final Collection<BinaryTextRelation> related = getRelationsAsFirst( relations, annotation );
         if ( related != null && !related.isEmpty() ) {
            relatedMap.put( annotation, related );
         }
      }
      return relatedMap;
   }


   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation as the first argument
    */
   static public Collection<IdentifiedAnnotation> getFirstArguments(
         final Collection<? extends BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
                      .filter( r -> r.getArg2().getArgument().equals( annotation ) )
                      .map( r -> r.getArg1().getArgument() )
                      .filter( IdentifiedAnnotation.class::isInstance )
                      .map( a -> (IdentifiedAnnotation)a )
                      .collect( Collectors.toList() );
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation as the second argument
    */
   static public Collection<IdentifiedAnnotation> getSecondArguments(
         final Collection<? extends BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
                      .filter( r -> r.getArg1().getArgument().equals( annotation ) )
                      .map( r -> r.getArg2().getArgument() )
                      .filter( IdentifiedAnnotation.class::isInstance )
                      .map( a -> (IdentifiedAnnotation)a )
                      .collect( Collectors.toList() );
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation
    */
   static public Collection<IdentifiedAnnotation> getAllRelated(
         final Collection<? extends BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
                      .map( r -> getRelated( r, annotation ) )
                      .filter( Objects::nonNull )
                      .collect( Collectors.toList() );
   }

   static private Map.Entry<BinaryTextRelation, IdentifiedAnnotation> getRelationEntry(
         final BinaryTextRelation relation,
         final IdentifiedAnnotation annotation ) {
      final IdentifiedAnnotation related = getRelated( relation, annotation );
      if ( related == null ) {
         return null;
      }
      return new AbstractMap.SimpleEntry<>( relation, related );
   }


   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation
    */
   static public Map<BinaryTextRelation, IdentifiedAnnotation> getAllRelatedMap(
         final Collection<BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      final Map<BinaryTextRelation, IdentifiedAnnotation> map = new HashMap<>();
      relations.stream()
               .map( r -> getRelationEntry( r, annotation ) )
               .filter( Objects::nonNull )
               .forEach( e -> map.put( e.getKey(), e.getValue() ) );
      return map;
   }


   /**
    * @param relation   relation of interest
    * @param annotation some annotation that might be in the relation
    * @return the other annotation in the relation if the first is present
    */
   static private IdentifiedAnnotation getRelated( final BinaryTextRelation relation,
                                                   final IdentifiedAnnotation annotation ) {
      if ( relation == null
           || relation.getArg1() == null
           || relation.getArg1().getArgument() == null
           || relation.getArg2() == null
           || relation.getArg2().getArgument() == null ) {
         return null;
      }
      Annotation argument = null;
      if ( relation.getArg1().getArgument().equals( annotation ) ) {
         argument = relation.getArg2().getArgument();
      } else if ( relation.getArg2().getArgument().equals( annotation ) ) {
         argument = relation.getArg1().getArgument();
      }
      if ( argument != null && IdentifiedAnnotation.class.isInstance( argument ) ) {
         return (IdentifiedAnnotation)argument;
      }
      return null;
   }


   /**
    * @param relation   relation of interest
    * @param annotation some annotation that might be in the relation
    * @return the target annotation in the relation if the first is present
    */
   static public IdentifiedAnnotation getTarget( final BinaryTextRelation relation,
                                                 final IdentifiedAnnotation annotation ) {
      if ( !relation.getArg1()
                    .getArgument()
                    .equals( annotation ) ) {
         return null;
      }
      final Annotation argument = relation.getArg2()
                                          .getArgument();
      if ( argument != null && IdentifiedAnnotation.class.isInstance( argument ) ) {
         return (IdentifiedAnnotation)argument;
      }
      return null;
   }

   static private Map.Entry<BinaryTextRelation, IdentifiedAnnotation> getRelationTargetEntry(
         final BinaryTextRelation relation,
         final IdentifiedAnnotation annotation ) {
      final IdentifiedAnnotation related = getTarget( relation, annotation );
      if ( related == null ) {
         return null;
      }
      return new AbstractMap.SimpleEntry<>( relation, related );
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation
    */
   static public Map<BinaryTextRelation, IdentifiedAnnotation> getRelatedTargetsMap(
         final Collection<BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      final Map<BinaryTextRelation, IdentifiedAnnotation> map = new HashMap<>();
      relations.stream()
               .map( r -> getRelationTargetEntry( r, annotation ) )
               .filter( Objects::nonNull )
               .forEach( e -> map.put( e.getKey(), e.getValue() ) );
      return map;
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all relations in the given relations where the first argument is the given annotation
    */
   static public <T extends BinaryTextRelation> Collection<T> getRelationsAsFirst(
         final Collection<T> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
                      .filter( r -> r.getArg1().getArgument().equals( annotation ) )
                      .collect( Collectors.toList() );
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all relations in the given relations where the second argument is the given annotation
    */
   static public <T extends BinaryTextRelation> Collection<T> getRelationsAsSecond(
         final Collection<T> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
                      .filter( r -> r.getArg2().getArgument().equals( annotation ) )
                      .collect( Collectors.toList() );
   }


   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> createSourceTargetMap(
         final List<IdentifiedAnnotation> sources,
         final List<IdentifiedAnnotation> targets,
         final boolean onlyOneTarget ) {
      if ( sources.isEmpty() || targets.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargets
            = new HashMap<>( sources.size() );

      for ( IdentifiedAnnotation target : targets ) {
         Pair<Integer> bestSpan = null;
         for ( IdentifiedAnnotation source : sources ) {
            if ( source.equals( target )
                 || (source.getBegin() == target.getBegin() && source.getEnd() == target.getEnd()) ) {
               // source and target are same OR duplicates formed for conjunctions
               continue;
            }
            if ( onlyOneTarget && sourceTargets.containsKey( source ) ) {
               continue;
            }
            final String sourceSentenceId = source.getSentenceID();
            if ( source.getBegin() > target.getEnd() ) {
               if ( !onlyOneTarget ) {
                  if ( bestSpan == null ) {
                     if ( sourceSentenceId.equals( target.getSentenceID() ) ) {
                        bestSpan = new Pair<>( source.getBegin(), source.getEnd() );
                     }
                  }
                  break;
               }
               if ( bestSpan != null ) {
                  break;
               }
            }
            bestSpan = new Pair<>( source.getBegin(), source.getEnd() );
         }
         if ( bestSpan != null ) {
            for ( IdentifiedAnnotation source : sources ) {
               if ( source.getBegin() > bestSpan.getValue1() ) {
                  break;
               }
               if ( source.getBegin() == bestSpan.getValue1() && source.getEnd() == bestSpan.getValue2() ) {
                  sourceTargets.computeIfAbsent( source, a -> new ArrayList<>() ).add( target );
               }
            }
         }
      }
      return sourceTargets;
   }


   // Attributes is anatomical site (location), owners is finding, disorder, procedure (thing at location)
   // In other words, attributeOwner hasRelationTo attribute
   // TODO for some reason this seems to randomly take a -long- time
   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> createReverseAttributeMap(
         final JCas jcas,
         final List<IdentifiedAnnotation> attributeOwners,
         final List<IdentifiedAnnotation> attributes,
         final boolean onlyOneOwner ) {
      if ( attributeOwners.isEmpty() || attributes.isEmpty() ) {
         return Collections.emptyMap();
      }
//      final Collection<IdentifiedAnnotation> modifiers
//            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jcas, UriConstants.BODY_MODIFIER );
//      attributeOwners.removeAll( modifiers );
//      if ( attributeOwners.isEmpty() || attributes.isEmpty() ) {
//         return Collections.emptyMap();
//      }

      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> conjoinedAttributes
            = getConjoinedAttributes( jcas.getDocumentText(), attributes );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> ownerMap = new HashMap<>( attributeOwners
            .size() );
      for ( IdentifiedAnnotation owner : attributeOwners ) {
         if ( onlyOneOwner && ownerMap.containsKey( owner ) ) {
            continue;
         }
         final String ownerSentenceId = owner.getSentenceID();
         final Collection<IdentifiedAnnotation> sameSentenceAttributes = new ArrayList<>();
         for ( IdentifiedAnnotation attribute : attributes ) {
            if ( owner.equals( attribute )
                 || (owner.getSubject() != null && !owner.getSubject().equals( attribute.getSubject() )) ) {
               continue;
            }
            if ( ownerSentenceId.equals( attribute.getSentenceID() ) ) {
               sameSentenceAttributes.add( attribute );
            }
         }
         final Collection<IdentifiedAnnotation> assignedAttributes = new ArrayList<>( attributes.size() );
         IdentifiedAnnotation bestAttribute = null;
         for ( IdentifiedAnnotation attribute : sameSentenceAttributes ) {
            // if location is after locatable thing then it is the best location?
            if ( attribute.getBegin() > owner.getEnd() ) {
               if ( bestAttribute == null ) {
                  bestAttribute = attribute;
               }
               break;
            }
            bestAttribute = attribute;
         }
         if ( bestAttribute != null ) {
            ownerMap.computeIfAbsent( owner, a -> new ArrayList<>() ).add( bestAttribute );
            assignedAttributes.add( bestAttribute );

            // Check conjoined, adding a duplicate owner to each conjoined attribute
            final Collection<IdentifiedAnnotation> conjoinedList = conjoinedAttributes.get( bestAttribute );
            if ( conjoinedList != null ) {
               for ( IdentifiedAnnotation conjoined : conjoinedList ) {
                  if ( !conjoined.equals( bestAttribute ) ) {
                     final IdentifiedAnnotation duplicate = createDuplicate( jcas, owner );
                     ownerMap.computeIfAbsent( duplicate, a -> new ArrayList<>() ).add( conjoined );
                  }
               }
            }
            continue;
         }
         // get best attribute by those in preceding sentences
         for ( IdentifiedAnnotation attribute : attributes ) {
            if ( owner.equals( attribute ) || assignedAttributes.contains( attribute ) ) {
               continue;
            }
            if ( attribute.getBegin() > owner.getEnd() ) {
               break;
            }
            bestAttribute = attribute;
         }
         if ( bestAttribute != null ) {
            ownerMap.computeIfAbsent( owner, a -> new ArrayList<>() ).add( bestAttribute );

            // Check conjoined, adding a duplicate owner to each conjoined attribute
            final Collection<IdentifiedAnnotation> conjoinedList = conjoinedAttributes.get( bestAttribute );
            if ( conjoinedList != null ) {
               for ( IdentifiedAnnotation conjoined : conjoinedList ) {
                  if ( !conjoined.equals( bestAttribute ) ) {
                     final IdentifiedAnnotation duplicate = createDuplicate( jcas, owner );
                     ownerMap.computeIfAbsent( duplicate, a -> new ArrayList<>() ).add( conjoined );
                  }
               }
            }
         }
      }
      return ownerMap;
   }

   // Attributes is anatomical site (location), owners is finding, disorder, procedure (thing at location)
   // In other words, attributeOwner hasRelationTo attribute
   // TODO for some reason this seems to randomly take a -long- time
   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> createReverseAttributeMapSingle(
         final List<IdentifiedAnnotation> attributeOwners,
         final List<IdentifiedAnnotation> attributes,
         final boolean onlyOneOwner ) {
      if ( attributeOwners.isEmpty() || attributes.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> ownerMap = new HashMap<>( attributeOwners
            .size() );
      for ( IdentifiedAnnotation owner : attributeOwners ) {
         if ( onlyOneOwner && ownerMap.containsKey( owner ) ) {
            continue;
         }
         final String ownerSentenceId = owner.getSentenceID();

         final Collection<IdentifiedAnnotation> sentenceAttributes = new ArrayList<>();
         for ( IdentifiedAnnotation attribute : attributes ) {
            if ( owner.equals( attribute )
                 || (owner.getSubject() != null && !owner.getSubject().equals( attribute.getSubject() ))
                 || (owner.getBegin() == attribute.getBegin() && owner.getEnd() == attribute.getEnd()) ) {
               continue;
            }
            if ( ownerSentenceId.equals( attribute.getSentenceID() ) ) {
               sentenceAttributes.add( attribute );
            }
         }
         IdentifiedAnnotation bestAttribute = null;
         for ( IdentifiedAnnotation attribute : sentenceAttributes ) {
            // if location is after locatable thing then it is the best location?
            if ( attribute.getBegin() > owner.getEnd() ) {
               if ( bestAttribute == null ) {
                  bestAttribute = attribute;
               }
               break;
            }
            bestAttribute = attribute;
         }
         if ( bestAttribute != null ) {
            ownerMap.computeIfAbsent( owner, a -> new ArrayList<>() ).add( bestAttribute );
            continue;
         }
         // get best attribute by those in preceding sentences
         for ( IdentifiedAnnotation attribute : attributes ) {
            if ( owner.equals( attribute ) ) {
               continue;
            }
            if ( attribute.getBegin() > owner.getEnd() ) {
               break;
            }
            bestAttribute = attribute;
         }
         if ( bestAttribute != null ) {
            ownerMap.computeIfAbsent( owner, a -> new ArrayList<>() ).add( bestAttribute );
         }
      }
      return ownerMap;
   }


   static private Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> getConjoinedAttributes(
         final String docText,
         final List<IdentifiedAnnotation> attributes ) {
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> conjoinedAttributeLists = new HashMap<>();
      IdentifiedAnnotation previousAttribute = null;
      String previousSentenceId = null;
      Collection<IdentifiedAnnotation> conjoinedAttributes = new ArrayList<>();
      for ( IdentifiedAnnotation attribute : attributes ) {
         if ( previousAttribute == null ) {
            previousAttribute = attribute;
            previousSentenceId = previousAttribute.getSentenceID();
            continue;
         }
         if ( attribute.getBegin() <= previousAttribute.getEnd() ) {
            // The previous attribute overlaps and is longer than this attribute.
            // Since the are the same type, this attribute should be subsumed.  Skip it.
            continue;
         }
         final String attributeSentenceId = attribute.getSentenceID();
         if ( !attributeSentenceId.equals( previousSentenceId ) ) {
            if ( !conjoinedAttributes.isEmpty() ) {
               final Collection<IdentifiedAnnotation> conjoined = new ArrayList<>( conjoinedAttributes );
               conjoinedAttributes.forEach( a -> conjoinedAttributeLists.put( a, conjoined ) );
               conjoinedAttributes = new ArrayList<>();
            }
            previousAttribute = attribute;
            previousSentenceId = attributeSentenceId;
            continue;
         }
         final String textBetween = docText.substring( previousAttribute.getEnd(), attribute.getBegin() );
         final String[] splits = WHITESPACE.split( textBetween );
         if ( splits.length < 5
              && (textBetween.contains( "," ) || textBetween.toLowerCase().contains( " and " )) ) {
            if ( conjoinedAttributes.isEmpty() ) {
               conjoinedAttributes.add( previousAttribute );
            }
            conjoinedAttributes.add( attribute );
         } else {
            if ( !conjoinedAttributes.isEmpty() ) {
               final Collection<IdentifiedAnnotation> conjoined = new ArrayList<>( conjoinedAttributes );
               conjoinedAttributes.forEach( a -> conjoinedAttributeLists.put( a, conjoined ) );
               conjoinedAttributes = new ArrayList<>();
            }
         }
         previousAttribute = attribute;
         previousSentenceId = attributeSentenceId;
      }
      if ( !conjoinedAttributes.isEmpty() ) {
         final Collection<IdentifiedAnnotation> conjoined = new ArrayList<>( conjoinedAttributes );
         conjoinedAttributes.forEach( a -> conjoinedAttributeLists.put( a, conjoined ) );
      }

      return conjoinedAttributeLists;
   }

   static private IdentifiedAnnotation createDuplicate( final JCas jCas, final IdentifiedAnnotation annotation ) {
      final SemanticGroup semanticGroup = SemanticGroup.getBestGroup( annotation );
      final IdentifiedAnnotation duplicate = semanticGroup.getCreator().apply( jCas );
      duplicate.setBegin( annotation.getBegin() );
      duplicate.setEnd( annotation.getEnd() );
      duplicate.setTypeID( annotation.getTypeID() );
      duplicate.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
      final Collection<UmlsConcept> umlsConcepts = OntologyConceptUtil.getUmlsConcepts( annotation );
      if ( !umlsConcepts.isEmpty() ) {
         final FSArray conceptArray = new FSArray( jCas, umlsConcepts.size() );
         int i = 0;
         for ( UmlsConcept umlsConcept : umlsConcepts ) {
            conceptArray.set( i, umlsConcept );
            i++;
         }
         duplicate.setOntologyConceptArr( conceptArray );
      }
      duplicate.setConditional( annotation.getConditional() );
      duplicate.setConfidence( annotation.getConfidence() );
      duplicate.setGeneric( annotation.getGeneric() );
      duplicate.setHistoryOf( annotation.getHistoryOf() );
      duplicate.setPolarity( annotation.getPolarity() );
      duplicate.setSegmentID( annotation.getSegmentID() );
      duplicate.setSentenceID( annotation.getSentenceID() );
      duplicate.setSubject( annotation.getSubject() );
      duplicate.setUncertainty( annotation.getUncertainty() );
      duplicate.addToIndexes( jCas );
      return duplicate;
   }

   // TODO : Move to dphe-neo4j  RelationConstants
//   static public boolean isHasSiteRelation( final String relationName ) {
//      return relationName.equals( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE )
//             || relationName.equals( RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE )
//             || relationName.equals( RelationConstants.DISEASE_HAS_METASTATIC_ANATOMIC_SITE )
//             || relationName.equals( RelationConstants.Disease_Has_Associated_Region )
//             || relationName.equals( RelationConstants.Disease_Has_Associated_Cavity )
//             || relationName.equals( RelationConstants.Finding_Has_Associated_Site )
//             || relationName.equals( RelationConstants.Finding_Has_Associated_Region )
//             || relationName.equals( RelationConstants.Finding_Has_Associated_Cavity );
//   }

   /**
    * @param jCas     ye olde ...
    * @param argument -
    * @param target   -
    * @param name     name of relation type
    * @return created relation or null if there was a problem
    */
   static public BinaryTextRelation createRelation( final JCas jCas,
                                                    final IdentifiedAnnotation argument,
                                                    final IdentifiedAnnotation target,
                                                    final String name ) {
//      if ( isHasSiteRelation( name ) ) {
//         return createRelation( jCas, new LocationOfTextRelation( jCas ), argument, target, name );
//      }
      return createRelation( jCas, new BinaryTextRelation( jCas ), argument, target, name );
   }

   /**
    * @param jCas     ye olde ...
    * @param relation -
    * @param argument -
    * @param target   -
    * @param name     name of relation type
    * @return created relation or null if there was a problem
    */
   static public <T extends BinaryTextRelation> T createRelation( final JCas jCas,
                                                                  T relation,
                                                                  final IdentifiedAnnotation argument,
                                                                  final IdentifiedAnnotation target,
                                                                  final String name ) {
      if ( argument == null ) {
         LOGGER.info( "No argument for " + ((target != null) ? target.getCoveredText() : "") );
         return null;
      }
      if ( target == null ) {
         LOGGER.info( "No target to relate to " + argument.getCoveredText() );
         return null;
      }
      if ( argument.equals( target ) ) {
         LOGGER.warn( "Argument and target are identical " + argument.getCoveredText() );
         return null;
      }
      final RelationArgument relationArgument = new RelationArgument( jCas );
      relationArgument.setArgument( argument );
      relationArgument.setRole( "Argument" );
      relationArgument.addToIndexes();
      final RelationArgument relationTarget = new RelationArgument( jCas );
      relationTarget.setArgument( target );
      relationTarget.setRole( "Related_to" );
      relationTarget.addToIndexes();
      relation.setArg1( relationArgument );
      relation.setArg2( relationTarget );
      relation.setCategory( name );
      // add the relation to the CAS
      relation.addToIndexes();
      return relation;
   }


}
package org.apache.ctakes.core.util;


import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.neo4j.UriConstants;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/25/2016
 */
@Immutable
final public class RelationUtil {
   private RelationUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "RelationUtil" );

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
    * @param relations            relations of interest
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
    *
    * @param relations relations of interest
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
    * @param relations            relations of interest
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

   static private Map.Entry<BinaryTextRelation, IdentifiedAnnotation> getRelationEntry( final BinaryTextRelation relation,
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
         return (IdentifiedAnnotation) argument;
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

   static private Map.Entry<BinaryTextRelation, IdentifiedAnnotation> getRelationTargetEntry( final BinaryTextRelation relation,
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
    * @param relations            relations of interest
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
    * @param relations            relations of interest
    * @param annotation identified annotation of interest
    * @return all relations in the given relations where the second argument is the given annotation
    */
   static public <T extends BinaryTextRelation> Collection<T> getRelationsAsSecond(
         final Collection<T> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
            .filter( r -> r.getArg2().getArgument().equals( annotation ) )
            .collect( Collectors.toList());
   }



   /**
    * Candidates for primary annotations of a possible relation are preceding candidates in the same paragraph,
    * or if none then nearest following candidate in the same sentence
    *
    * @param jcas            ye olde ...
    * @param sources      all candidate owners of a type in a paragraph.  Should be the things BEFORE
    * @param targets all candidate attributes of a type that has a relation in a paragraph.  Should be the things AFTER
    * @param onlyOneTarget true if the attribute owner should only have one of the provided attributes
    * @return a map of owners and their attributes
    */
   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> createSourceTargetMap(
         final JCas jcas,
         final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
         final List<IdentifiedAnnotation> sources,
         final List<IdentifiedAnnotation> targets,
         final boolean onlyOneTarget ) {
      if ( sources.isEmpty() || targets.isEmpty() ) {
         return Collections.emptyMap();
      }
      final boolean haveOnlyOne = sources.size() == 1 && targets.size() == 1;
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargets
            = new HashMap<>( sources.size() );

      final Collection<Sentence> targetSentences = new ArrayList<>();
      for ( IdentifiedAnnotation target : targets ) {
         targetSentences.clear();
         IdentifiedAnnotation bestSource = null;
         for ( IdentifiedAnnotation source : sources ) {
            if ( source.equals( target ) ) {
               continue;
            }
            if ( onlyOneTarget && sourceTargets.containsKey( source ) ) {
               continue;
            }
            if ( source.getBegin() > target.getEnd() ) {
               if ( haveOnlyOne ) {
                  break;
               }
               if ( !onlyOneTarget ) {
                  if ( bestSource == null ) {
                     if ( targetSentences.isEmpty() ) {
                        targetSentences.addAll( coveringSentences.get( target ) );
                     }
                     final Collection<Sentence> sourceSentences = new ArrayList<>( coveringSentences.get( source ) );
                     sourceSentences.retainAll( targetSentences );
                     if ( !sourceSentences.isEmpty() ) {
                        bestSource = source;
                     }
                  }
                  break;
               }
               if ( bestSource != null ) {
                  break;
               }
            }
            bestSource = source;
         }
         if ( bestSource != null ) {
            sourceTargets.computeIfAbsent( bestSource, a -> new ArrayList<>() ).add( target );
         }
      }
      return sourceTargets;
   }

   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> createSourceTargetMap(
           final JCas jcas,
           final List<IdentifiedAnnotation> sources,
           final List<IdentifiedAnnotation> targets,
           final boolean onlyOneTarget ) {
      if ( sources.isEmpty() || targets.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences
              = JCasUtil.indexCovering( jcas, IdentifiedAnnotation.class, Sentence.class );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> sourceTargets
              = new HashMap<>( sources.size() );

      for ( IdentifiedAnnotation target : targets ) {
         IdentifiedAnnotation bestSource = null;
         for ( IdentifiedAnnotation source : sources ) {
            final Collection<Sentence> targetSentences = new ArrayList<>( coveringSentences.get( target ) );
            if ( source.equals( target ) ) {
               continue;
            }
            if ( onlyOneTarget && sourceTargets.containsKey( source ) ) {
               continue;
            }
            if ( source.getBegin() > target.getEnd() ) {
               if ( !onlyOneTarget ) {
                  if ( bestSource == null ) {
                     targetSentences.retainAll( coveringSentences.get( source ) );
                     if ( !targetSentences.isEmpty() ) {
                        bestSource = source;
                     }
                  }
                  break;
               }
               if ( bestSource != null ) {
                  break;
               }
            }
            bestSource = source;
         }
         if ( bestSource != null ) {
            sourceTargets.computeIfAbsent( bestSource, a -> new ArrayList<>() ).add( target );
         }
      }
      return sourceTargets;
   }

   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> createReverseAttributeMap(
         final JCas jcas,
         final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
         final List<IdentifiedAnnotation> attributeOwners,
         final List<IdentifiedAnnotation> attributes,
         final boolean onlyOneOwner ) {
      if ( attributeOwners.isEmpty() || attributes.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Collection<IdentifiedAnnotation> modifiers
            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jcas, UriConstants.BODY_MODIFIER );
      attributeOwners.removeAll( modifiers );
      if ( attributeOwners.isEmpty() || attributes.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> ownerMap = new HashMap<>( attributeOwners
            .size() );
      for ( IdentifiedAnnotation owner : attributeOwners ) {
         if ( onlyOneOwner && ownerMap.containsKey( owner ) ) {
            continue;
         }
         final Collection<Sentence> ownerSentences = coveringSentences.get( owner );

         final Collection<IdentifiedAnnotation> sentenceAttributes = new ArrayList<>();
         for ( IdentifiedAnnotation attribute : attributes ) {
            if ( owner.equals( attribute ) ) {
               continue;
            }
            final Collection<Sentence> attributeSentences = coveringSentences.get( attribute );
            attributeSentences.retainAll( ownerSentences );
            if ( !attributeSentences.isEmpty() ) {
               sentenceAttributes.add( attribute );
            }
         }
         final Collection<IdentifiedAnnotation> assignedAttributes = new ArrayList<>( attributes.size() );
         IdentifiedAnnotation bestAttribute = null;
         for ( IdentifiedAnnotation attribute : sentenceAttributes ) {
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
         }
      }
      return ownerMap;
   }



   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> getBestMatches(
         final JCas jcas,
         final List<IdentifiedAnnotation> matchables,
         final List<IdentifiedAnnotation> toMatches,
         final boolean onlyOneMatch,
         final boolean sentenceFirst ) {
      if ( matchables.isEmpty() || toMatches.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences
            = JCasUtil.indexCovering( jcas, IdentifiedAnnotation.class, Sentence.class );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> matchMap = new HashMap<>( matchables
            .size() );
      // for each attribute, get the best owner
      for ( IdentifiedAnnotation toMatch : toMatches ) {
         IdentifiedAnnotation bestMatchable = null;
         final Collection<Sentence> allToMatchSentences = new ArrayList<>( coveringSentences.get( toMatch ) );
         // if same-sentence owner-attribute relations are preferred, check the sentence first.
         if ( sentenceFirst ) {
            bestMatchable = getBestSameSentenceMatch( coveringSentences,
                  allToMatchSentences, toMatch, onlyOneMatch, matchMap.keySet() );
            if ( bestMatchable != null ) {
               LOGGER.warn( "Best Matchable " + bestMatchable.getCoveredText() + " to Match " + toMatch.getCoveredText() );
            }
         }
         // if there was no same-sentence best owner, get the nearest preceding owner
         if ( bestMatchable == null ) {
            for ( IdentifiedAnnotation matchable : matchables ) {
               if ( matchable.equals( toMatch ) ) {
                  continue;
               }
               if ( onlyOneMatch && matchMap.containsKey( matchable ) ) {
                  continue;
               }
               // owner is after attribute.  Only consider valid if in the same sentence.
               if ( matchable.getBegin() > toMatch.getEnd() ) {
                  if ( !onlyOneMatch ) {
                     if ( bestMatchable == null && !sentenceFirst ) {
                        final Collection<Sentence> toMatchSentences = new ArrayList<>( allToMatchSentences );
                        toMatchSentences.retainAll( coveringSentences.get( matchable ) );
                        if ( !toMatchSentences.isEmpty() ) {
                           bestMatchable = matchable;
                        }
                     }
                     break;
                  }
                  if ( bestMatchable != null ) {
                     break;
                  }
               }
               bestMatchable = matchable;
            }
         }

         if ( bestMatchable != null ) {
            LOGGER.error(
                  "   Assigning Best Matchable " + bestMatchable.getCoveredText() + " " + bestMatchable.getBegin() + "," + bestMatchable.getEnd()
                  + " to Match " + toMatch.getCoveredText() + " " + toMatch.getBegin() + "," + toMatch.getEnd() );
            matchMap.computeIfAbsent( bestMatchable, a -> new ArrayList<>() ).add( toMatch );
         }
      }
      return matchMap;
   }


   static private IdentifiedAnnotation getBestSameSentenceMatch( final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences,
                                                                 final Collection<Sentence> allToMatchSentences,
                                                                 final IdentifiedAnnotation toMatch,
                                                                 final boolean onlyOneMatch,
                                                                 final Collection<IdentifiedAnnotation> alreadyMatched
                                                                 ) {
      final Collection<IdentifiedAnnotation> sentenceMatchables
            = coveringSentences.entrySet().stream()
                               .filter( e -> e.getValue().equals( allToMatchSentences ) )
                               .map( Map.Entry::getKey )
                               .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                               .collect( Collectors.toList() );
      if ( sentenceMatchables.isEmpty() ) {
         return null;
      }
      int bestDistance = Integer.MAX_VALUE;
      IdentifiedAnnotation bestMatch = null;
      for ( IdentifiedAnnotation matchable : sentenceMatchables ) {
         if ( matchable.equals( toMatch ) ) {
            continue;
         }
         if ( onlyOneMatch && alreadyMatched.contains( matchable ) ) {
            continue;
         }
         final int abToOb = Math.abs( toMatch.getBegin() - matchable.getBegin() );
         final int aeToOb = Math.abs( toMatch.getEnd() - matchable.getBegin() );
         final int abToOe = Math.abs( toMatch.getBegin() - matchable.getEnd() );
         final int aeToOe = Math.abs( toMatch.getEnd() - matchable.getEnd() );
         final int distance = Math.min( Math.min( abToOb, aeToOb ), Math.min( abToOe, aeToOe ) );
         if ( distance < bestDistance ) {
            bestMatch = matchable;
         }
      }
      return bestMatch;
   }


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
      if ( name.equals( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE ) ) {
         return createRelation( jCas, new LocationOfTextRelation( jCas ), argument, target, name );
      }
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

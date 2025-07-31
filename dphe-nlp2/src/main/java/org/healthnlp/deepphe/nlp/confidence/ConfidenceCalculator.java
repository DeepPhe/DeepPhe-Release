package org.healthnlp.deepphe.nlp.confidence;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/29/2023}
 */
final public class ConfidenceCalculator {

   // 3/24/2024
   //  Using the formula : sum( value ) / ( sum( value/100 ) + 10 )
   // If you want to know what it looks like, graph it.

   // 3/26/2024  TODO  - maybe decrease mention scores in concept based upon the mention uri's
   //  distance from the main concept uri.  e.g. carcinoma, neoplasm : carcinoma scores *100%,
   //  neoplasm scores discounted as neoplasm n levels above carcinoma.

   static private final double DEFAULT_LIMITER = 100;
   static private final double DEFAULT_CURVER = 10;
   // decided upon 100 just from experimentation.
   static private final long CURVER_DIVIDER = 500;

//   static private final double DEFAULT_PRIMARY_MULTIPLIER = 1.05;
   static private final double DEFAULT_SECONDARY_MULTIPLIER = 0.9;

   private enum UriRank {
      PRIMARY,
      SECONDARY
   }


   private ConfidenceCalculator() {}



   static public List<UriConcept> rankConfidentConcepts( final Collection<UriConcept> concepts, final long mentionCount ) {
      final Map<Double,List<UriConcept>> confidenceMap
            = concepts.stream()
                      .collect( Collectors.groupingBy( c -> getConceptConfidence( c, mentionCount ) ) );
      final List<Double> sorted = new ArrayList<>( confidenceMap.keySet() );
      Collections.sort( sorted );
      final List<UriConcept> conceptList = new ArrayList<>( concepts.size() );
      for ( int i=sorted.size()-1; i==0; i-- ) {
         final List<UriConcept> confident = confidenceMap.getOrDefault( sorted.get( i ), Collections.emptyList() );
         if ( !confident.isEmpty() ) {
            confident.sort( Comparator.comparing( UriConcept::getUri ) );
            conceptList.addAll( confident );
         }
      }
      return conceptList;
   }


   static public double getConceptConfidence( final UriConcept concept, final long mentionCount ) {
      return getOwnersConfidence( concept.getMentions(), mentionCount );
   }


   static public long getMentionCount( final Collection<UriConceptRelation> relations ) {
      return relations.stream()
                      .map( UriConceptRelation::getTarget )
                      .map( UriConcept::getMentions )
                      .flatMap( Collection::stream )
                      .distinct()
                      .count();
   }



   static public double getAttributeConfidence( final Collection<UriConceptRelation> relations, final long mentionCount ) {
      final List<Double> relationScores = new ArrayList<>();
      final List<Double> conceptScores = new ArrayList<>();
      for ( UriConceptRelation relation : relations ) {
         relationScores.addAll( getScores( relation.getMentionRelations()) );
         conceptScores.addAll( getScores( relation.getTarget().getMentions() ) );
      }
      final double rConfidence = getScoresConfidence( relationScores, mentionCount );
      final double mConfidence = getScoresConfidence( conceptScores, mentionCount );
      LogFileWriter.add( "Harmonic Mean of Relations: " + rConfidence + " Concepts: " + mConfidence );
      return getHarmonicMean( rConfidence, mConfidence );
   }


   static public <O extends org.healthnlp.deepphe.neo4j.node.xn.ConfidenceOwner> double getOwnersConfidence( final Collection<O> owners, final long mentionCount ) {
      return getScoresConfidence( getScores( owners ), mentionCount );
   }


   static public <O extends org.healthnlp.deepphe.neo4j.node.xn.ConfidenceOwner> List<Double> getSecondaryScores( final Collection<O> owners, final long mentionCount ) {
      return owners.stream()
                   .map( org.healthnlp.deepphe.neo4j.node.xn.ConfidenceOwner::getdConfidence )
                   .map( d -> Math.max( 1, d * DEFAULT_SECONDARY_MULTIPLIER ) )
                   .collect( Collectors.toList() );
   }


   static public <O extends org.healthnlp.deepphe.neo4j.node.xn.ConfidenceOwner> List<Double> getScores( final Collection<O> owners ) {
      return owners.stream()
                   .map( org.healthnlp.deepphe.neo4j.node.xn.ConfidenceOwner::getdConfidence )
                   .collect( Collectors.toList() );
   }

   // 3/24/2024
   //  Using the formula : sum( value ) / ( sum( value/100 ) + curveAdjustment )
   // If you want to know what it looks like, graph it.
   // The sum/100 makes the max value 100.  The curve adjustment adjusts the curve so that the confidence curve
   // works for the number of mentions (therefore concepts, notes, etc.) of the patient.
   // We want the curve to stay 'low' so that a larger sum doesn't just pop confidence to 100.
   // The sum will depend upon overall mentions (concepts, notes, etc.), so we should adjust for that.
   static public double getScoresConfidence( final List<Double> scores, final long mentionCount ) {
      if ( scores.isEmpty() ) {
         // Don't really want 1, but helps prevent possible later div by 0.
         return 1;
      }
      final double sum = scores.stream().mapToDouble( d -> d ).sum();
      final long curver = Math.max( 1, mentionCount / CURVER_DIVIDER );
      final double den = sum / DEFAULT_LIMITER + curver;
      LogFileWriter.add( "ConfidenceCalculator scores = " + scores.size() + " mentionCount = " + mentionCount
            + " curver = " + curver
            + " sum = " + sum + " den = " + den + " conf = " + Math.max( 1, Math.min( 100, sum / den ) ) );
      return Math.max( 1, Math.min( 100, sum / den ) );
   }


   static public double getHarmonicMean( final double p, final double r ) {
      return 2 * p * r / ( p + r );
   }

}

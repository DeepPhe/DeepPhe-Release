package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.ConceptBuilder;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.uri.UriAnnotationFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.ctakes.typesystem.type.constants.CONST.NE_DISCOVERY_TECH_EXPLICIT_AE;


/**
 * @author SPF , chip-nlp
 * @since {12/17/2021}
 */
final public class NormalizeUtil {

   static public final String NORMALIZED_CODE = "NORMALIZED_CODE";
   static public final String HAS_NORMAL_VALUE = "HasNormalValue";

   static private final Normal[] ALL_NORMALS;
   static {
      final List<Normal> normals = new ArrayList<>();
//      normals.addAll( Arrays.asList( Affirmation.values() ) );
//      normals.addAll( Arrays.asList( Certainty.values() ) );
//      normals.addAll( Arrays.asList( Presence.values() ) );
//      normals.addAll( Arrays.asList( Level.values() ) );
//      normals.addAll( Arrays.asList( Laterality.values() ) );
//      normals.addAll( Arrays.asList( Involvement.values() ) );
//      normals.addAll( Arrays.asList( Invasive.values() ) );
      normals.addAll( Arrays.asList( Unit.values() ) );
      ALL_NORMALS = normals.toArray( new Normal[ 0 ] );
   }
   static public Normal[] getAllNormals() {
      return ALL_NORMALS;
   }

   static private final Pattern NUMBER_PATTERN = Pattern.compile( "[0-9]+(?:.[0-9]+)?" );
   static public final Pair<Integer> SPAN_NOT_FOUND = new Pair<>( -1, -1 );
   static public final Integer INT_NOT_FOUND = Integer.MIN_VALUE;
   static public final Float FLOAT_NOT_FOUND = Float.MIN_VALUE;
   static public final Number NUMBER_NOT_FOUND = INT_NOT_FOUND;


   static public String getNormalizedText( final String text ) {
      if ( text == null || text.isEmpty() ) {
         return "";
      }
      return Normal.replaceText( text, ALL_NORMALS );
   }

   static public Map<Pair<Integer>, Collection<Normal>> getSpanMap( final String text ) {
      if ( text == null || text.isEmpty() ) {
         return Collections.emptyMap();
      }
      return Normal.getSpanMap( text, ALL_NORMALS );
   }

   static List<Normal> getNormals( final String text ) {
      if ( text == null || text.isEmpty() ) {
         return Collections.emptyList();
      }
      return Normal.getNormals( text, ALL_NORMALS );
   }

   static public IdentifiedAnnotation createNormal( final JCas jCas,
                                           final int spanOffset,
                                           final Matcher matcher,
                                           final String groupName ) {
            final Pair<Integer> span = RegexUtil.getGroupSpan( matcher, groupName );
      if ( !RegexUtil.isValidSpan( span ) ) {
         return null;
      }
      // Semantic Type is T081 : Quantitative Concept, which is in Semantic Group Lab Modifier
      return new IdentifiedAnnotationBuilder().type( SemanticTui.T081 )
                                              .span( spanOffset + span.getValue1(),
                                                     spanOffset + span.getValue2() )
                                              .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
                                              .build( jCas );
   }

   static public IdentifiedAnnotation createNormal( final JCas jCas,
                                                                                final int begin,
                                                                                final int end,
                                                    final String schema,
                                                    final String code,
                                                                                final String cui,
                                                                                final String prefText,
                                                                                final SemanticTui semanticTui,
                                                                                final Segment section,
                                                                                final float confidence ) {
      return createNormal( jCas, begin, end, schema, code, cui, prefText, semanticTui, section,
                                           UriAnnotationFactory.getSentenceId( jCas, begin, end ), confidence );
   }

//   DPHE_CODING_SCHEME isn't available is general ctakes.
   static public IdentifiedAnnotation createNormal( final JCas jCas,
                                                                                final int begin,
                                                                                final int end,
                                                                                final String schema,
                                                                                final String code,
                                                                                final String cui,
                                                                                final String prefText,
                                                                                final SemanticTui semanticTui,
                                                                                final Segment section,
                                                                                final String sentenceId,
                                                                                final float confidence ) {
//      LOGGER.info( "Creating Normalizable " + groupName + " " + (spanOffset + span.getValue1()) + "-" + (spanOffset + span.getValue2()) );
      final OntologyConcept concept = new ConceptBuilder().cui( cui )
                                                          .type( semanticTui )
                                                          .schema( schema )
                                                          .code( code )
                                                          .preferredText( prefText )
                                                          .build( jCas );
      final IdentifiedAnnotation annotation
            = new IdentifiedAnnotationBuilder().span( begin, end )
                                                .type( semanticTui )
                                                .concept( concept )
                                               .discoveredBy( NE_DISCOVERY_TECH_EXPLICIT_AE )
                                               .confidence( confidence )
                                               .build( jCas );
      annotation.setSegmentID( section.getId() );
      annotation.setSentenceID( sentenceId );
      return annotation;
   }


//   static public DegreeOfTextRelation normalize( final JCas jCas,
//                                                 final Annotation annotation,
//                                                 final Annotation normalization ) {
//      if ( annotation.equals( normalization ) ) {
//         throw new IllegalArgumentException( "Cannot normalize " + annotation.getCoveredText() + " as itself." );
//      }
//      return new RelationBuilder<DegreeOfTextRelation>().creator( DegreeOfTextRelation::new )
//                                                         .annotation( annotation )
//                                                         .hasRelated( normalization )
//                                                         .build( jCas );
//  }


//   static public NormalizableAnnotation createNormalizable( final JCas jCas,
//                                                             final int spanOffset,
//                                                             final Matcher matcher,
//                                                             final String groupName ) {
//      final Pair<Integer> span = RegexUtil.getGroupSpan( matcher, groupName );
//      if ( !RegexUtil.isValidSpan( span ) ) {
//         return null;
//      }
////      LOGGER.info( "Creating Normalizable " + groupName + " " + (spanOffset + span.getValue1()) + "-" + (spanOffset + span.getValue2()) );
//      final NormalizableAnnotation name = new NormalizableAnnotation( jCas,
//                                                                      spanOffset + span.getValue1(),
//                                                                      spanOffset + span.getValue2() );
//      name.addToIndexes();
//      return name;
//   }
//
//   static public NormalizableAnnotation createNormalizable( final JCas jCas,
//                                                            final int begin,
//                                                            final int end ) {
////      LOGGER.info( "Creating Normalizable " + groupName + " " + (spanOffset + span.getValue1()) + "-" + (spanOffset + span.getValue2()) );
//      final NormalizableAnnotation name = new NormalizableAnnotation( jCas, begin, end );
//      name.addToIndexes();
//      return name;
//   }
//
//   static public NormalizableIdentifiedAnnotation createIdentifiedNormalizable( final JCas jCas,
//                                                            final int begin,
//                                                            final int end ) {
////      LOGGER.info( "Creating Normalizable " + groupName + " " + (spanOffset + span.getValue1()) + "-" + (spanOffset + span.getValue2()) );
//      final NormalizableIdentifiedAnnotation name = new NormalizableIdentifiedAnnotation( jCas, begin, end );
//      name.addToIndexes();
//      return name;
//   }
//
//   static public NormalizableIdentifiedAnnotation createIdentifiedNormalizable( final JCas jCas,
//                                                                                final int begin,
//                                                                                final int end,
//                                                                                final String uri,
//                                                                                final String cui,
//                                                                                final String prefText,
//                                                                                final SemanticTui semanticTui,
//                                                                                final Segment section,
//                                                                                final float confidence ) {
//      return createIdentifiedNormalizable( jCas, begin, end, uri, cui, prefText, semanticTui, section,
//                                           UriAnnotationFactory.getSentenceId( jCas, begin, end ), confidence );
//   }
//
//   static public NormalizableIdentifiedAnnotation createIdentifiedNormalizable( final JCas jCas,
//                                                                                final int begin,
//                                                                                final int end,
//                                                                                final String uri,
//                                                                                final String cui,
//                                                                                final String prefText,
//                                                                                final SemanticTui semanticTui,
//                                                                                final Segment section,
//                                                                                final String sentenceId,
//                                                                                final float confidence ) {
////      LOGGER.info( "Creating Normalizable " + groupName + " " + (spanOffset + span.getValue1()) + "-" + (spanOffset + span.getValue2()) );
//      final NormalizableIdentifiedAnnotation annotation = new NormalizableIdentifiedAnnotation( jCas, begin, end );
//      annotation.setNormalizedValue( uri );
//      annotation.setSegmentID( section.getId() );
//      annotation.setSentenceID( sentenceId );
//      annotation.setTypeID( semanticTui.getGroupCode() );
//      annotation.setDiscoveryTechnique( NE_DISCOVERY_TECH_EXPLICIT_AE );
//      annotation.setConfidence( confidence );
//      final OntologyConcept concept
//            = new ConceptBuilder().cui( cui )
//                                  .type( semanticTui )
//                                  .preferredText( prefText )
//                                  .schema( DPHE_CODING_SCHEME )
//                                  .code( uri )
//                                  .build( jCas );
////      final UmlsConcept umlsConcept = UriAnnotationFactory.createUmlsConcept( jCas,
////                                                                              cui,
////                                                                              semanticTui.name(),
////                                                                              prefText,
////                                                                              uri );
//      final FSArray conceptArray = new FSArray( jCas, 1 );
//      conceptArray.set( 0, concept );
//      annotation.setOntologyConceptArr( conceptArray );
//      annotation.addToIndexes( jCas );
//      return annotation;
//   }
//


   //  While I would really like to (re)use existing and covered ctakes Types,
   //  from experience, previous annotators are not as accurate in annotating full text as
   //  this can be when analyzing a short snippet of text in the FormattedListEntry value.
   //  In addition, there are not enough Types in the system to cover everything that we want to
   //  normalize, they are of different parent types, and would themselves still require normalization.


   static public Number getNormalizedNumber( final IdentifiedAnnotation annotation ) {
      final String text = annotation.getCoveredText();
      if ( annotation.getCoveredText() == null ) {
         return NUMBER_NOT_FOUND;
      }
      return getNormalizedNumber( text.trim().toLowerCase() );
   }

   static public Number getNormalizedNumber( final String text ) {
      final String lowerText = text.trim().toLowerCase();
      final int intValue = getIntValue( lowerText );
      if ( intValue != INT_NOT_FOUND) {
         return intValue;
      }
      final Float floatValue = getFloatValue( lowerText );
      if ( !floatValue.equals( FLOAT_NOT_FOUND ) ) {
         return floatValue;
      }
      return NUMBER_NOT_FOUND;
   }

   static public int getNormalizedInteger( final String text ) {
      return getIntValue( text.trim().toLowerCase() );
   }

   static public float getNormalizedFloat( final String text ) {
      return getFloatValue( text.trim().toLowerCase() );
   }

   static private boolean spanFits( final String text, final Pair<Integer> span ) {
      return span.getValue1() >= 0 && span.getValue2() <= text.length();
   }

   static private boolean offsetFits( final String text, final int offset ) {
      return offset >= 0 && offset < text.length();
   }

   static public Pair<Integer> getNumberSpan( final String text ) {
      final Matcher matcher = NUMBER_PATTERN.matcher( text );
      if ( matcher.find() ) {
         return new Pair<>( matcher.start(), matcher.end() );
      }
      return SPAN_NOT_FOUND;
   }

   static public String getNumberText( final String text ) {
      final Pair<Integer> span = getNumberSpan( text );
      if ( span.equals( SPAN_NOT_FOUND ) ) {
         return "";
      }
      return text.substring( span.getValue1(), span.getValue2() );
   }

   static public Number getNumberValue( final String text ) {
      if ( text.indexOf( '.' ) >= 0 ) {
         return getFloatValue( text );
      }
      return getIntValue( text );
   }

   static private int getIntValue( final String text ) {
      try {
         return Integer.parseInt( text );
      } catch ( NumberFormatException nfE ) {
         return INT_NOT_FOUND;
      }
   }

   static private float getFloatValue( final String text ) {
      try {
         return Float.parseFloat( text );
      } catch ( NumberFormatException nfE ) {
         return FLOAT_NOT_FOUND;
      }
   }

   static public String getNormalizedText( final IdentifiedAnnotation annotation ) {
      return getNormalizedText( annotation.getCoveredText() );
   }


   static private Pair<Integer> getAnySpan( final String fullText,
                                            final int subjectOffset,
                                            final String... prefixes ) {
      if ( subjectOffset < 0 ) {
         return SPAN_NOT_FOUND;
      }
      for ( String prefix : prefixes ) {
         final Pair<Integer> span = getSpan( fullText, subjectOffset, prefix );
         if ( span.getValue1() >= 0 ) {
            return span;
         }
      }
      return SPAN_NOT_FOUND;
   }

   static private Pair<Integer> getSpan( final String fullText,
                                         final int subjectOffset,
                                         final String prefix ) {
      if ( subjectOffset < prefix.length() ) {
         return SPAN_NOT_FOUND;
      }
      if ( subjectOffset == prefix.length()+1
           && fullText.startsWith( prefix + " " ) ) {
         return new Pair<>( 0, subjectOffset );
      } else if ( subjectOffset > prefix.length()+1
                  && fullText.startsWith( " " + prefix + " ", subjectOffset-prefix.length()-2 ) ) {
         return new Pair<>( subjectOffset-1-prefix.length(), subjectOffset-1 );
      }
      return SPAN_NOT_FOUND;
   }

   static Pair<Integer> getAnyNumberableSpan( final String fullText,
                                              final int subjectOffset,
                                              final String... prefixes ) {
      for ( String prefix : prefixes ) {
         final Pair<Integer> span = getNumberableSpan( fullText, subjectOffset, prefix );
         if ( span.getValue1() >= 0 ) {
            return span;
         }
      }
      return SPAN_NOT_FOUND;
   }

   static private Pair<Integer> getNumberableSpan( final String fullText,
                                                   final int subjectOffset,
                                                   final String prefix ) {
      if ( subjectOffset < prefix.length()
           || Character.isLetter( fullText.charAt( subjectOffset-1 ) )
           || !fullText.startsWith( prefix, subjectOffset-prefix.length()-1 ) ) {
         return SPAN_NOT_FOUND;
      }
      if ( subjectOffset == prefix.length()+1 ) {
         return new Pair<>( 0, prefix.length() );
      } else if ( subjectOffset > prefix.length()+1
                  && !Character.isLetter( fullText.charAt( subjectOffset-prefix.length()-2 ) ) ) {
         return new Pair<>( subjectOffset-1-prefix.length(), subjectOffset-1 );
      }
      return SPAN_NOT_FOUND;
   }


   /////  These could be added later, depending upon whether we need such things.
//   static private final String[] GAUGE = { "gauge" };
//   static private final String[] INTERNATIONAL = { "international", "intl", "iu" };
//   static private final String[] UNITED_STATES_PHARMACOPEIA = { "usp" };
//   static private final String[] WET_WEIGHT = { "ww", "%ww" };
//   static private final String[] VOIDED_VOLUME = { "vv", "%vv" };
//   static private final String[] WEIGHT_IN_VOLUME = { "wv", "%wv" };






   // For quick testing
   public static void main( String[] args ) {
      final String[] texts = {
            "Mitotic Rate Score: 2 (intermediate proliferative rate)",
            "% of Tumor Cells with Nuclear Positivity: 90",
            "Average Intensity of Tumor Cell Nuclei Staining - Strong (3+)",
            "Equivocal (Score 2+)"
      };

      for ( String text : texts ) {
         System.out.println( text );
         System.out.println( "Number: " + getNumberText( text ) );
         final long start = System.nanoTime();
         System.out.println( Normal.replaceText( text, ALL_NORMALS ) );
         final long stop = System.nanoTime();
         System.out.println( "Time " + (stop-start) + " " + ((stop-start)/1000000) );
//         System.out.println( Normal.getSpanMap( text, ALL_NORMALS )
//                                   .entrySet()
//                                   .stream()
//                                   .map( e -> e.getValue().stream()
//                                               .map( Normal::getNormal )
//                                               .collect( Collectors.joining( "," ) )
//                                              + " "
//                                              + e.getKey().getValue1() + "," + e.getKey().getValue2() )
//                                   .collect( Collectors.joining( "\n" ) ) );
      }
   }

}

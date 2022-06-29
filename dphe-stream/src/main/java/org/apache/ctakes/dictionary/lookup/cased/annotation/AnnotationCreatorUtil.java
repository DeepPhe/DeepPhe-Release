package org.apache.ctakes.dictionary.lookup.cased.annotation;

import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.annotation.ConceptBuilder;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.dictionary.lookup.cased.detailer.Details;
import org.apache.ctakes.dictionary.lookup.cased.lookup.DiscoveredTerm;
import org.apache.ctakes.dictionary.lookup.cased.util.textspan.ContiguousTextSpan;
import org.apache.ctakes.dictionary.lookup.cased.util.textspan.MagicTextSpan;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/26/2020
 */
@Immutable
final public class AnnotationCreatorUtil {

   static private final Logger LOGGER = Logger.getLogger( "AnnotationCreatorUtil" );

   static private final Pair<String> EMPTY_SCHEMA_CODE = new Pair<>( "", "" );

   private AnnotationCreatorUtil() {
   }


   static public Map<DiscoveredTerm, Collection<MagicTextSpan>> mapTermSpans(
         final Map<Pair<Integer>, Collection<DiscoveredTerm>> allDiscoveredTermsMap ) {
      final Map<DiscoveredTerm, Collection<MagicTextSpan>> termSpanMap = new HashMap<>();
      for ( Map.Entry<Pair<Integer>, Collection<DiscoveredTerm>> spanTerms : allDiscoveredTermsMap.entrySet() ) {
         final MagicTextSpan textSpan = new ContiguousTextSpan( spanTerms.getKey() );
         spanTerms.getValue().forEach( t -> termSpanMap.computeIfAbsent( t, s -> new HashSet<>() ).add( textSpan ) );
      }
      return termSpanMap;
   }


   static public void createAnnotations( final JCas jcas,
                                         final Pair<Integer> textSpan,
                                         final Collection<DiscoveredTerm> discoveredTerms,
                                         final Map<DiscoveredTerm, Collection<Details>> detailsMap,
                                         final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      discoveredTerms.forEach( t
            -> createAnnotation( jcas, textSpan, t, detailsMap.get( t ), reassignSemantics ) );
   }

   static private void createAnnotation( final JCas jcas,
                                         final Pair<Integer> textSpan,
                                         final DiscoveredTerm discoveredTerm,
                                         final Collection<Details> allDetails,
                                         final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final Collection<SemanticTui> tuis = getTuis( allDetails );
      final SemanticGroup bestGroup = getSemanticGroup( tuis, reassignSemantics );
      final IdentifiedAnnotationBuilder annotationBuilder
            = new IdentifiedAnnotationBuilder().group( bestGroup )
                                               .span( textSpan )
                                               .discoveredBy( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP )
                                               .confidence( discoveredTerm.getConfidence() );
      final String cui = discoveredTerm.getCui();
      final String prefText = getPreferredText( allDetails );
      final Collection<Pair<String>> schemaCodes = getSchemaCodes( discoveredTerm.getVocabCodes() );
      for ( SemanticTui tui : tuis ) {
         schemaCodes.stream()
               .map( c -> createUmlsConcept( jcas, cui, tui, prefText, c ) )
               .forEach( annotationBuilder::concept );
      }
      annotationBuilder.build( jcas );
   }

   static private Collection<Pair<String>> getSchemaCodes( final String[] vocabCodes ) {
      final Collection<Pair<String>> schemaCodes = new HashSet<>();
      if ( vocabCodes == null ) {
         schemaCodes.add( EMPTY_SCHEMA_CODE );
         return schemaCodes;
      }
      for ( String vocabCode : vocabCodes ) {
         String[] schemaCode = StringUtil.fastSplit( vocabCode, '|' );
         if ( schemaCode.length == 2 ) {
            schemaCodes.add( new Pair<>( schemaCode[ 0 ], schemaCode[ 1 ] ) );
         }
      }
      if ( schemaCodes.isEmpty() ) {
            schemaCodes.add( EMPTY_SCHEMA_CODE );
      }
      return schemaCodes;
   }

//   static private String getPreferredText( final Collection<TermEncoding> termEncodings ) {
//      return termEncodings.stream()
//                          .filter( CodeSchema.PREFERRED_TEXT::isSchema )
//                          .map( TermEncoding::getSchemaCode )
//                          .map( Object::toString )
//                          .distinct()
//                          .collect( Collectors.joining( ";" ) );
//   }

   static private String getPreferredText( final Collection<Details> allDetails ) {
      return allDetails.stream()
                       .max( Comparator.comparing( Details::getRank ) )
                       .orElse( Details.EMPTY_DETAILS )
                       .getPreferredText();
   }


//   static private final Predicate<TermEncoding> isPrefTextEncoding
//         = CodeSchema.PREFERRED_TEXT::isSchema;


//   static private String getTui( final Collection<TermEncoding> termEncodings ) {
//      return termEncodings.stream()
//                          .filter( CodeSchema.TUI::isSchema )
//                          .map( TermEncoding::getSchemaCode )
//                          .map( AnnotationCreatorUtil::parseTuiValue )
//                          .map( TuiCodeUtil::getAsTui )
//                          .distinct()
//                          .collect( Collectors.joining( ";" ) );
//   }

   static private Collection<SemanticTui> getTuis( final Collection<Details> details ) {
      return details.stream()
                    .max( Comparator.comparing( Details::getRank ) )
                    .map( Details::getTuis )
                    .orElse( Collections.singletonList( SemanticTui.UNKNOWN ) );
   }

//   static private final Predicate<TermEncoding> isTuiEncoding = CodeSchema.TUI::isSchema;

//   static private OntologyConcept createUmlsConcept( final JCas jcas,
//                                                 final String cui,
//                                                 final String tui,
//                                                 final String preferredText ) {
//      return createUmlsConcept( jcas, cui, tui, preferredText, EMPTY_SCHEMA_CODE );
//   }

//   static private OntologyConcept createUmlsConcept( final JCas jcas,
//                                                         final String cui,
//                                                         final String tui,
//                                                         final String preferredText,
//                                                         final TermEncoding termEncoding ) {
//      return new ConceptBuilder().cui( cui )
//                                 .tui( tui )
//                                 .preferredText( preferredText )
//                                 .schema( termEncoding.getSchema() )
//                                 .code( termEncoding.getSchemaCode().toString() )
//                                 .build( jcas );
//   }

   static private OntologyConcept createUmlsConcept( final JCas jcas,
                                                     final String cui,
                                                     final SemanticTui tui,
                                                     final String preferredText,
                                                     final Pair<String> schemaCode ) {
      return new ConceptBuilder().cui( cui )
                                 .tui( tui.name() )
                                 .preferredText( preferredText )
                                 .schema( schemaCode.getValue1() )
                                 .code( schemaCode.getValue2() )
                                 .build( jcas );
   }


   static public Map<SemanticGroup, Collection<DiscoveredTerm>> mapSemanticTerms(
         final Map<DiscoveredTerm, Collection<Details>> detailsMap,
         final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final Map<SemanticGroup, Collection<DiscoveredTerm>> semanticTermMap = new EnumMap<>( SemanticGroup.class );
      for ( Map.Entry<DiscoveredTerm, Collection<Details>> discoveredDetails : detailsMap.entrySet() ) {
         getAllSemanticGroups( discoveredDetails.getValue(), reassignSemantics )
               .forEach( g -> semanticTermMap.computeIfAbsent( g, s -> new HashSet<>() )
                                             .add( discoveredDetails.getKey() ) );
      }
      return semanticTermMap;
   }


   static private Collection<SemanticGroup> getAllSemanticGroups(
         final Collection<Details> details,
         final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final Collection<SemanticGroup> groups
            = details.stream()
                     .map( Details::getTuis )
                     .map( s -> getSemanticGroup( s, reassignSemantics ) )
                     .collect( Collectors.toSet() );
      if ( groups.isEmpty() ) {
         return Collections.singletonList( SemanticGroup.UNKNOWN );
      }
      return groups;
   }

   static private SemanticGroup getSemanticGroup( final Collection<SemanticTui> tuis,
                                                  final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final Collection<SemanticGroup> groups = EnumSet.noneOf( SemanticGroup.class );
      for ( SemanticTui tui : tuis ) {
         if ( !reassignSemantics.isEmpty() ) {
            final SemanticGroup reassignGroup = reassignSemantics.get( tui );
            if ( reassignGroup != null ) {
               groups.add( reassignGroup );
               continue;
            }
         }
         groups.add( tui.getGroup() );
      }
      return SemanticGroup.getBestGroup( groups );
   }


//   static private int parseTuiValue( final Object object ) {
//      try {
//         return Integer.parseInt( object.toString() );
//      } catch ( NumberFormatException nfE ) {
//         return SemanticTui.UNKNOWN.getCode();
//      }
//   }


//   static private Map<DiscoveredTerm, Collection<SemanticGroup>> mapTermSemantics(
//         final Map<DiscoveredTerm, Collection<TermEncoding>> termEncodingMap,
//         final Map<SemanticTui,SemanticGroup> reassignSemantics ) {
//      final Map<DiscoveredTerm, Collection<SemanticGroup>> termSemanticsMap = new HashMap<>( termEncodingMap.size() );
//      termEncodingMap.forEach( (k,v) -> termSemanticsMap.put( k, getSemanticGroups( v, reassignSemantics) ) );
//      return termSemanticsMap;
//   }


//   static private Map<TermEncoding,SemanticGroup> mapEncodingSemantics( final Collection<TermEncoding> termEncodings,
//                                                                        final Map<SemanticTui,SemanticGroup> reassignSemantics ) {
//      return termEncodings.stream()
//                          .collect( Collectors.toMap( Function.identity(),
//                                e -> getSemanticGroup( e, reassignSemantics ) ) );
//   }


}

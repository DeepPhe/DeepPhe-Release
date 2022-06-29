package org.apache.ctakes.dictionary.lookup.cased.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.dictionary.lookup.cased.annotation.AlikeSubsumingAnnotationCreator;
import org.apache.ctakes.dictionary.lookup.cased.annotation.AnnotationCreator;
import org.apache.ctakes.dictionary.lookup.cased.annotation.NonSubsumingAnnotationCreator;
import org.apache.ctakes.dictionary.lookup.cased.annotation.SemanticSubsumingAnnotationCreator;
import org.apache.ctakes.dictionary.lookup.cased.detailer.*;
import org.apache.ctakes.dictionary.lookup.cased.dictionary.*;
import org.apache.ctakes.dictionary.lookup.cased.lookup.ContiguousLookupEngine;
import org.apache.ctakes.dictionary.lookup.cased.lookup.DiscoveredTerm;
import org.apache.ctakes.dictionary.lookup.cased.lookup.LookupEngine;
import org.apache.ctakes.dictionary.lookup.cased.lookup.LookupToken;
import org.apache.ctakes.dictionary.lookup2.ae.JCasTermAnnotator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/12/2020
 */
@PipeBitInfo(
      name = "CasedAnnotationFinder",
      description = "Finds all-uppercase or normal terms in text.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { BASE_TOKEN, SENTENCE },
      products = IDENTIFIED_ANNOTATION
)
final public class CasedAnnotationFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "CasedAnnotationFinder" );

   static public final String DICTIONARY_TYPE = "_type";
   static public final String DETAILER_TYPE = "_type";


   // dictionaries accepts a comma-separated list
   @ConfigurationParameter( name = "dictionaries", mandatory = true,
         description = "Dictionaries to use for lookup." )
   private String[] _dictionaries;

   static private final String snomed_rxnorm_2020aa_type = "Jdbc";


   // https://www.eecis.udel.edu/~vijay/cis889/ie/pos-set.pdf

   static private final String[] VERB_POS = { "VB", "VBD", "VBG", "VBN", "VBP", "VBZ",
                                              "VV", "VVD", "VVG", "VVN", "VVP", "VVZ" };
   @ConfigurationParameter( name = "lookupVerbs", mandatory = false,
         description = "Use Verb parts of speech for lookup." )
   private String _lookupVerbs = "yes";

   static private final String[] NOUN_POS = { "NN", "NNS", "NP", "NPS", "NNP", "NNPS" };
   @ConfigurationParameter( name = "lookupNouns", mandatory = false,
         description = "Use Noun parts of speech for lookup." )
   private String _lookupNouns = "yes";

   static private final String[] ADJECTIVE_POS = { "JJ", "JJR", "JJS" };
   @ConfigurationParameter( name = "lookupAdjectives", mandatory = false,
         description = "Use Adjective parts of speech for lookup." )
   private String _lookupAdjectives = "yes";

   static private final String[] ADVERB_POS = { "RB", "RBR", "RBS" };
   @ConfigurationParameter( name = "lookupAdverbs", mandatory = false,
         description = "Use Adverb parts of speech for lookup." )
   private String _lookupAdverbs = "yes";

   @ConfigurationParameter( name = "otherLookups", mandatory = false,
         description = "List of other parts of speech for lookup." )
   private String[] _otherLookups = {};

   // minimum span required to accept a term
   @ConfigurationParameter( name = JCasTermAnnotator.PARAM_MIN_SPAN_KEY, mandatory = false,
         description = "Minimum number of characters for a term." )
   protected int _minLookupSpan = JCasTermAnnotator.DEFAULT_MINIMUM_SPAN;


   @ConfigurationParameter( name = "allowWordSkips", mandatory = false,
         description = "Terms may include words that do not match.  So-called loose matching." )
   protected String _allowSkips = "no";

   static private final String CONS_SKIP_PRP_KEY = "consecutiveSkips";
   @ConfigurationParameter( name = CONS_SKIP_PRP_KEY, mandatory = false,
         description = "Number of consecutive non-comma tokens that can be skipped." )
   private int _consecutiveSkipMax = 2;

   static private final String TOTAL_SKIP_PRP_KEY = "totalSkips";
   @ConfigurationParameter( name = TOTAL_SKIP_PRP_KEY, mandatory = false,
         description = "Number of total tokens that can be skipped." )
   private int _totalSkipMax = 4;


   @ConfigurationParameter( name = "subsume", mandatory = false,
         description = "Subsume contained terms of the same semantic group.", defaultValue = "yes" )
   private String _subsume = "yes";

   @ConfigurationParameter( name = "subsumeSemantics", mandatory = false,
         description = "Subsume contained terms of the same and certain other semantic groups.", defaultValue = "yes" )
   private String _subsumeSemantics = "yes";


   @ConfigurationParameter( name = "reassignSemantics", mandatory = false,
         description = "Reassign Semantic Types (TUIs) to non-default Semantic Groups." )
   private String[] _reassignSemanticList = {};


   // code lists accepts a comma-separated list
   @ConfigurationParameter( name = "detailers", mandatory = true,
         description = "Term Detailers with Term Details." )
   private String[] _detailers;


   private boolean _allowSkipping;

   private AnnotationCreator _annotationCreator;

   final private Collection<String> _lookupPos = new HashSet<>();

   final private Map<SemanticTui, SemanticGroup> _semanticReassignment = new HashMap<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      LOGGER.info( "Initializing Dictionary Lookup ..." );
      super.initialize( context );

      if ( isParameterTrue( _subsumeSemantics ) ) {
         _annotationCreator = new SemanticSubsumingAnnotationCreator();
      } else if ( isParameterTrue( _subsume ) ) {
         _annotationCreator = new AlikeSubsumingAnnotationCreator();
      } else {
         _annotationCreator = new NonSubsumingAnnotationCreator();
      }

      setupDictionaries( context );
      setupDetailers( context );
      setupPos();
      setupReassignSemantics();
   }


   static private boolean isParameterTrue( final String value ) {
      return value.equalsIgnoreCase( "yes" ) || value.equalsIgnoreCase( "true" );
   }

   private void setupDictionaries( final UimaContext context ) throws ResourceInitializationException {
      if ( _dictionaries.length == 0 ) {
         LOGGER.error( "Dictionary List is empty.  Consider using the default cTAKES Dictionary." +
                       "  If you are using a piper file, add the line \"load sno_rx_16ab_settings\"" );
         throw new ResourceInitializationException();
      }
      for ( String name : _dictionaries ) {
         final CasedDictionary dictionary = createDictionary( name, context );
         if ( dictionary == null ) {
            LOGGER.error( "Could not create Dictionary for " + name );
            throw new ResourceInitializationException();
         }
         DictionaryStore.getInstance().addDictionary( dictionary );
      }
   }


   private CasedDictionary createDictionary( final String name, final UimaContext context ) {
      final String type = EnvironmentVariable.getEnv( name + DICTIONARY_TYPE, context );
      if ( type == null || type.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         LOGGER.error(
               "No Dictionary Type specified for " + name + ".  Please set parameter " + name + DICTIONARY_TYPE );
         return null;
      }
      try {
         switch ( type.toUpperCase() ) {
            case JdbcDictionary
                  .DICTIONARY_TYPE:
               return new JdbcDictionary( name, context );
            case BsvDictionary
                  .DICTIONARY_TYPE:
               return new BsvDictionary( name, context );
            case BsvListDictionary
                  .DICTIONARY_TYPE:
               return new BsvListDictionary( name, context );
            default:
               LOGGER.error( "Unknown Dictionary type " + type + " specified for " + name );
         }
      } catch ( SQLException multE ) {
         LOGGER.error( multE.getMessage() );
      }
      return null;
   }


   private void setupDetailers( final UimaContext context ) throws ResourceInitializationException {
      if ( _detailers.length == 0 ) {
         LOGGER.error( "Term Detailer List is empty.  Consider using the default cTAKES Term Detailer." +
                       "  If you are using a piper file, add the line \"load sno_rx_2020aa_settings\"" );
         throw new ResourceInitializationException();
      }
      for ( String name : _detailers ) {
         final TermDetailer detailer = createDetailer( name, context );
         if ( detailer == null ) {
            LOGGER.error( "Could not create Term Detailer for " + name );
            throw new ResourceInitializationException();
         }
         DetailsStore.getInstance().addDetailer( detailer );
      }
   }


   private TermDetailer createDetailer( final String name, final UimaContext context ) {
      final String type = EnvironmentVariable.getEnv( name + DETAILER_TYPE, context );
      if ( type == null || type.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         LOGGER.error(
               "No Term Detailer Type specified for " + name + ".  Please set parameter " + name + DETAILER_TYPE );
         return null;
      }
      try {
         switch ( type.toUpperCase() ) {
            case JdbcDetailer
                  .DETAILER_TYPE:
               return new JdbcDetailer( name, context );
            case BsvDetailer
                  .DETAILER_TYPE:
               return new BsvDetailer( name, context );
            case BsvListDetailer
                  .DETAILER_TYPE:
               return new BsvListDetailer( name, context );
            default:
               LOGGER.error( "Unknown Term Detailer type " + type + " specified for " + name );
         }
      } catch ( SQLException multE ) {
         LOGGER.error( multE.getMessage() );
      }
      return null;
   }


   private void setupPos() throws ResourceInitializationException {
      if ( isTrue( _lookupVerbs ) ) {
         _lookupPos.addAll( Arrays.asList( VERB_POS ) );
      }
      if ( isTrue( _lookupNouns ) ) {
         _lookupPos.addAll( Arrays.asList( NOUN_POS ) );
      }
      if ( isTrue( _lookupAdjectives ) ) {
         _lookupPos.addAll( Arrays.asList( ADJECTIVE_POS ) );
      }
      if ( isTrue( _lookupAdverbs ) ) {
         _lookupPos.addAll( Arrays.asList( ADVERB_POS ) );
      }
      if ( _otherLookups.length != 0 ) {
         _lookupPos.addAll( Arrays.asList( _otherLookups ) );
      }
      if ( _lookupPos.isEmpty() ) {
         LOGGER.error( "No Parts of Speech indicated for Lookup.  At least one Part of Speech must be used." );
         throw new ResourceInitializationException();
      }
      LOGGER.info( "Using Parts of Speech " + String.join( ", ", _lookupPos ) );
   }

   private void setupReassignSemantics() {
      if ( _semanticReassignment == null || _reassignSemanticList.length == 0 ) {
         return;
      }
      for ( String keyValue : _reassignSemanticList ) {
         final String[] splits = StringUtil.fastSplit( keyValue, ':' );
         if ( splits.length != 2 ) {
            LOGGER.warn( "Improper Key : Value pair for Semantic Reassignment " + keyValue );
            continue;
         }
         final SemanticTui tui = SemanticTui.getTui( splits[ 0 ].trim() );
         final SemanticGroup group = SemanticGroup.getGroup( splits[ 1 ].trim() );
         _semanticReassignment.put( tui, group );
      }
      LOGGER.info( "Reassigned Semantics: "
                   + _semanticReassignment.entrySet()
                                          .stream()
//                                          .map( e -> e.getKey().getSemanticType() + " : " + e.getValue().getLongName() )
                                          .map( e -> e.getKey().getSemanticType() + " : " + e.getValue().getName() )
                                          .collect( Collectors.joining( ", " ) ) );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Named Entities ..." );

      // Get all BaseTokens, grouped by Sentence.
      final Map<Sentence, Collection<BaseToken>> sentenceBaseTokens
            = JCasUtil.indexCovered( jCas, Sentence.class, BaseToken.class );

      // Discover Terms in text, grouped by text span.
      final Map<Pair<Integer>, Collection<DiscoveredTerm>> allDiscoveredTermsMap = new HashMap<>();
      try {
         // Using foreach loop because try/catch in a stream is terrible.
         for ( Collection<BaseToken> baseTokens : sentenceBaseTokens.values() ) {
            allDiscoveredTermsMap.putAll( getDiscoveredTerms( baseTokens ) );
         }
      } catch ( ArrayIndexOutOfBoundsException iobE ) {
         // JCasHashMap will throw this every once in a while.  Assume the windows are done and move on.
         LOGGER.warn( iobE.getMessage() );
      }


      // Get all details for the discovered terms.
      final Map<DiscoveredTerm, Collection<Details>> termDetailsMap
            = allDiscoveredTermsMap.values()
                                   .stream()
                                   .flatMap( Collection::parallelStream )
                                   .collect( Collectors.toMap( Function.identity(), this::getDetails ) );
      createAnnotations( jCas, allDiscoveredTermsMap, termDetailsMap );
   }


   private void createAnnotations( final JCas jCas,
                                   final Map<Pair<Integer>, Collection<DiscoveredTerm>> allDiscoveredTermsMap,
                                   final Map<DiscoveredTerm, Collection<Details>> termDetailsMap ) {
      _annotationCreator.createAnnotations( jCas, allDiscoveredTermsMap, termDetailsMap, _semanticReassignment );
   }


   private Collection<Details> getDetails( final DiscoveredTerm discoveredTerm ) {
      return DetailsStore.getInstance()
                         .getDetailers()
                         .stream()
                         .map( e -> e.getDetails( discoveredTerm ) )
                         .filter( Objects::nonNull )
                         .collect( Collectors.toSet() );
   }


   public Map<Pair<Integer>, Collection<DiscoveredTerm>> getDiscoveredTerms( final Collection<BaseToken> baseTokens ) {
      final Map<CasedDictionary, Map<Pair<Integer>, Collection<DiscoveredTerm>>> discoveredTermsMap
            = findTerms( baseTokens );
      return discoveredTermsMap.values()
                               .stream()
                               .map( Map::entrySet )
                               .flatMap( Collection::stream )
                               .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
   }


   /**
    * Given a set of dictionaries, tokens, and lookup token indices, populate a terms map with discovered terms
    *
    * @param baseTokens -
    * @return dictionaries to map of text spans to terms discovered at those text spans.
    */
   private Map<CasedDictionary, Map<Pair<Integer>, Collection<DiscoveredTerm>>> findTerms(
         final Collection<BaseToken> baseTokens ) {
      final Collection<CasedDictionary> dictionaries = DictionaryStore.getInstance().getDictionaries();
      final Map<CasedDictionary, Map<Pair<Integer>, Collection<DiscoveredTerm>>> dictionaryTermsMap
            = new HashMap<>( dictionaries.size() );
      final List<LookupToken> windowTokens = baseTokens.stream()
                                                          .filter( isWantedToken )
                                                          .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                                          .map( toLookupToken )
                                                          .collect( Collectors.toList() );
      final LookupEngine engine = getLookupEngine();
      dictionaries.forEach( d -> dictionaryTermsMap.put( d,
            engine.findTerms( d, windowTokens, _consecutiveSkipMax, _totalSkipMax ) ) );
      return dictionaryTermsMap;
   }

   static private final Predicate<BaseToken> isWantedToken = t -> !(t instanceof NewlineToken);

   private final Function<BaseToken, LookupToken> toLookupToken = b -> new LookupToken( b, isValidLookup( b ) );


   private boolean isValidLookup( final BaseToken baseToken ) {
      // We are only interested in tokens that are -words- of a certain length.
      if ( !(baseToken instanceof WordToken)
           || (baseToken.getEnd() - baseToken.getBegin() < _minLookupSpan) ) {
         return false;
      }
      // We are only interested in tokens that are -words- of the wanted part of speech.
      final String partOfSpeech = baseToken.getPartOfSpeech();
      return partOfSpeech == null || _lookupPos.contains( partOfSpeech );
   }


   private LookupEngine getLookupEngine() {
      return new ContiguousLookupEngine();
   }


   static private int parseInt( final Object value, final String name, final int defaultValue ) {
      if ( value instanceof Integer ) {
         return (Integer)value;
      } else if ( value instanceof String ) {
         try {
            return Integer.parseInt( (String)value );
         } catch ( NumberFormatException nfE ) {
            LOGGER.warn( "Could not parse " + name + " " + value + " as an integer" );
         }
      } else {
         LOGGER.warn( "Could not parse " + name + " " + value + " as an integer" );
      }
      return defaultValue;
   }


   static private boolean isTrue( final String text ) {
      return text.equalsIgnoreCase( "yes" ) || text.equalsIgnoreCase( "true" );
   }


}

package org.apache.ctakes.ner.ae;

import org.apache.ctakes.ner.creator.AnnotationCreator;
import org.apache.ctakes.ner.creator.DpheAnnotationCreator;
import org.apache.ctakes.ner.detail.Details;
import org.apache.ctakes.ner.detail.jdbc.JdbcDetailer;
import org.apache.ctakes.ner.filter.PassFilter;
import org.apache.ctakes.ner.filter.HierarchyTermFilter;
import org.apache.ctakes.ner.filter.TermFilter;
import org.apache.ctakes.ner.group.GroupHierarchy;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.ner.group.dphe.DpheGroupAccessor;
import org.apache.ctakes.ner.term.DetailedTerm;
import org.apache.ctakes.ner.term.DiscoveredTerm;
import org.apache.ctakes.ner.dictionary.bsv.BsvDictionary;
import org.apache.ctakes.ner.dictionary.DictionaryStore;
import org.apache.ctakes.ner.dictionary.Dictionary;
import org.apache.ctakes.ner.dictionary.jdbc.JdbcDictionary;
import org.apache.ctakes.ner.dictionary.DictionaryChecker;
import org.apache.ctakes.ner.term.LookupToken;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.ner.detail.Detailer;
import org.apache.ctakes.ner.detail.DetailerStore;
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
      name = "NamedEntityRecognizer",
      description = "Finds all-uppercase or normal terms in text.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { BASE_TOKEN, SENTENCE },
      products = IDENTIFIED_ANNOTATION
)
public class NamedEntityFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "NamedEntityFinder" );

   static public final String DICTIONARY_TYPE = "_type";
   static public final String ENCODER_TYPE = "_type";

   static private final Collection<String> AVAILABLE_TYPES = Arrays.asList( "BSV", "JDBC" );


   // dictionaries accepts a comma-separated list
   @ConfigurationParameter( name = "Dictionaries", mandatory = true,
         description = "Dictionaries to use for lookup." )
   private String[] _dictionaries;

//   static private final String snomed_rxnorm_2020aa_type = "Jdbc";


   // https://www.eecis.udel.edu/~vijay/cis889/ie/pos-set.pdf

   static private final String[] VERB_POS = { "VB", "VBD", "VBG", "VBN", "VBP", "VBZ",
                                              "VV", "VVD", "VVG", "VVN", "VVP", "VVZ" };
   @ConfigurationParameter( name = "LookupVerbs", mandatory = false,
         description = "Use Verb parts of speech for lookup." )
   private String _lookupVerbs = "yes";

   static private final String[] NOUN_POS = { "NN", "NNS", "NP", "NPS", "NNP", "NNPS" };
   @ConfigurationParameter( name = "LookupNouns", mandatory = false,
         description = "Use Noun parts of speech for lookup." )
   private String _lookupNouns = "yes";

   static private final String[] ADJECTIVE_POS = { "JJ", "JJR", "JJS" };
   @ConfigurationParameter( name = "LookupAdjectives", mandatory = false,
         description = "Use Adjective parts of speech for lookup." )
   private String _lookupAdjectives = "yes";

   static private final String[] ADVERB_POS = { "RB", "RBR", "RBS" };
   @ConfigurationParameter( name = "LookupAdverbs", mandatory = false,
         description = "Use Adverb parts of speech for lookup." )
   private String _lookupAdverbs = "yes";

   @ConfigurationParameter( name = "OtherLookups", mandatory = false,
         description = "List of other parts of speech for lookup." )
   private String[] _otherLookups = {};

   @ConfigurationParameter( name = "UseAllPOS", mandatory = false,
                            description = "Use all parts of speech for lookup." )
   private String _useAllpos = "no";

   // minimum span required to accept a term
   @ConfigurationParameter( name = "MinimumSpan", mandatory = false,
         description = "Minimum number of characters for a term." )
   protected int _minLookupSpan = 3;


   @ConfigurationParameter( name = "AllowWordSkips", mandatory = false,
         description = "Terms may include words that do not match.  So-called loose matching." )
   protected String _allowSkips = "no";

   @ConfigurationParameter( name = "ConsecutiveSkips", mandatory = false,
         description = "Number of consecutive non-comma tokens that can be skipped." )
   private int _consecutiveSkipMax = 2;

   @ConfigurationParameter( name = "TotalSkips", mandatory = false,
         description = "Number of total tokens that can be skipped." )
   private int _totalSkipMax = 4;


   @ConfigurationParameter( name = "Subsume", mandatory = false,
         description = "Subsume contained terms of the same semantic group.", defaultValue = "yes" )
   private String _subsume = "yes";

   @ConfigurationParameter( name = "SubsumeSemantics", mandatory = false,
         description = "Subsume contained terms of the same and certain other semantic groups.", defaultValue = "yes" )
   private String _subsumeSemantics = "yes";


   @ConfigurationParameter( name = "ReassignSemantics", mandatory = false,
         description = "Reassign Semantic Types (TUIs) to non-default Semantic Groups." )
   private String[] _reassignSemanticList = {};


   // code lists accepts a comma-separated list
   @ConfigurationParameter( name = "Detailers", mandatory = true,
         description = "Term Detailers with cui, tui, uri and schema codes." )
   private String[] _detailers;


   @ConfigurationParameter( name = "TermGrouping", mandatory = false,
                            description = "Term Grouping type based upon coding such as semantic group." )
   private String _termGrouping;



   private boolean _allowSkipping;

   private TermFilter _termFilter;
   private final AnnotationCreator _annotationCreator = new DpheAnnotationCreator();

   final private Collection<String> _lookupPos = new HashSet<>();
   private boolean _lookupAllPos;

   final private Map<SemanticTui, SemanticGroup> _semanticReassignment = new HashMap<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      LOGGER.info( "Initializing Dictionary Lookup ..." );
      super.initialize( context );

      setupDictionaries( context );
      setupDetailers( context );
      setupTermFilter();
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
         final Dictionary dictionary = createDictionary( name, context );
         if ( dictionary == null ) {
            LOGGER.error( "Could not create Dictionary for " + name );
            throw new ResourceInitializationException();
         }
         DictionaryStore.getInstance().addDictionary( dictionary );
      }
   }


   private Dictionary createDictionary( final String name, final UimaContext context ) {
      final String type = EnvironmentVariable.getEnv( name + DICTIONARY_TYPE, context );
      if ( type == null || type.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         LOGGER.error(
               "No Dictionary Type specified for " + name + ".  Please set parameter " + name + DICTIONARY_TYPE );
         LOGGER.info( "Available Types: " + String.join( " , ", AVAILABLE_TYPES ) );
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
//            case BsvListDictionary
//                  .DICTIONARY_TYPE:
//               return new BsvListDictionary( name, context );
            default:
               LOGGER.error( "Unknown Dictionary type " + type + " specified for " + name );
               LOGGER.info( "Available Types: " + String.join( " , ", AVAILABLE_TYPES ) );
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
         final Detailer detailer = createDetailer( name, context );
         if ( detailer == null ) {
            LOGGER.error( "Could not create Term Encoder for " + name );
            throw new ResourceInitializationException();
         }
         DetailerStore.getInstance().addDetailer( detailer );
      }
   }


   private Detailer createDetailer( final String name, final UimaContext context ) {
      final String type = EnvironmentVariable.getEnv( name + ENCODER_TYPE, context );
      if ( type == null || type.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         LOGGER.error(
               "No Term Encoder Type specified for " + name + ".  Please set parameter " + name + ENCODER_TYPE );
         LOGGER.info( "Available Types: " + String.join( " , ", AVAILABLE_TYPES ) );
         return null;
      }
      try {
         switch ( type.toUpperCase() ) {
            case JdbcDetailer
                  .DETAILER_TYPE:
               return new JdbcDetailer( name, context );
//            case BsvEncoder
//                  .ENCODER_TYPE:
//               return new BsvEncoder( name, context );
//            case BsvListEncoder
//                  .ENCODER_TYPE:
//               return new BsvListEncoder( name, context );
            default:
               LOGGER.error( "Unknown Term Encoder type " + type + " specified for " + name );
         }
      } catch ( SQLException multE ) {
         LOGGER.error( multE.getMessage() );
      }
      return null;
   }

   private void setupTermFilter() {
      if ( isParameterTrue( _subsumeSemantics ) ) {
         _termFilter = createTermFilter( HierarchyTermFilter::new );
//         _annotationCreator = new SemanticSubsumingAnnotationCreator();
      } else if ( isParameterTrue( _subsume ) ) {
         _termFilter = createTermFilter( HierarchyTermFilter::new );
//         _annotationCreator = new AlikeSubsumingAnnotationCreator();
      } else {
         _termFilter = new PassFilter();
      }
   }

   private TermFilter createTermFilter( final Function<GroupHierarchy<?>,TermFilter> filterCreator ) {
      if ( _termGrouping == null || _termGrouping.isEmpty() ) {
         return filterCreator.apply( DpheGroupAccessor.getInstance() );
      }
      switch ( _termGrouping.toUpperCase() ) {
         case DpheGroup.DPHE_GROUP:
            return filterCreator.apply( DpheGroupAccessor.getInstance() );
//            case BsvEncoder
//                  .ENCODER_TYPE:
//               return new BsvEncoder( name, context );
//            case BsvListEncoder
//                  .ENCODER_TYPE:
//               return new BsvListEncoder( name, context );
         default:
            LOGGER.error( "Unknown Annotation Grouping type " + _termGrouping );
      }
      return filterCreator.apply( DpheGroupAccessor.getInstance() );
   }


   private void setupPos() throws ResourceInitializationException {
      if  ( isTrue( _useAllpos ) ) {
         _lookupAllPos = true;
         LOGGER.info( "Using All Parts of Speech" );
         return;
      }
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
                                          .map( e -> e.getKey().getSemanticType() + " : " + e.getValue().getLongName() )
                                          .collect( Collectors.joining( ", " ) ) );
   }


   //////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////
   //
   //                PROCESS
   //
   //////////////////////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////////////////////


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Named Entities ..." );
      // Get all BaseTokens, grouped by Sentence.
      final Map<Sentence, Collection<BaseToken>> sentenceBaseTokensMap
            = JCasUtil.indexCovered( jCas, Sentence.class, BaseToken.class );
      // Discover Terms in text, grouped by text span.
      final Map<Pair<Integer>, Collection<DiscoveredTerm>> discoveredTermsMap = new HashMap<>();
      try {
         // Using foreach loop because try/catch in a stream is terrible.
         for ( Collection<BaseToken> sentenceBaseTokens : sentenceBaseTokensMap.values() ) {
            discoveredTermsMap.putAll( getDiscoveredTerms( sentenceBaseTokens ) );
         }
      } catch ( ArrayIndexOutOfBoundsException iobE ) {
         // JCasHashMap will throw this every once in a while.  Assume the windows are done and move on.
         LOGGER.warn( iobE.getMessage() );
      }
      final Map<String,Collection<Details>> uriDetailsMap
            = discoveredTermsMap.values().stream()
                                .flatMap( Collection::stream )
                                .map( DiscoveredTerm::getUri )
                                .distinct()
                                .collect( Collectors.toMap( Function.identity(), this::getDetails ) );
      final Collection<DetailedTerm> detailedTerms = createDetailedTerms( discoveredTermsMap, uriDetailsMap );
      final Collection<DetailedTerm> filteredTerms = getFilteredTerms( detailedTerms );
      createAnnotations( jCas, filteredTerms );
   }

   /**
    * Sort the tokens, iterate over the stored dictionaries, discover terms in each dictionary.
    * @param baseTokens for a sentence.
    * @return map of discovered terms, keyed by their offsets in the sentence.
    */
   public Map<Pair<Integer>, Collection<DiscoveredTerm>> getDiscoveredTerms( final Collection<BaseToken> baseTokens ) {
      final Collection<Dictionary> dictionaries = DictionaryStore.getInstance().getDictionaries();
      final Map<Pair<Integer>, Collection<DiscoveredTerm>> discoveredTerms = new HashMap<>();
      final List<LookupToken> lookupTokens = baseTokens.stream()
                                                       .filter( isWantedToken )
                                                       .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                                       .map( toLookupToken )
                                                       .collect( Collectors.toList() );
      final DictionaryChecker engine = getDictionaryChecker();
      for ( Dictionary finder : dictionaries ) {
         final Map<Pair<Integer>, Collection<DiscoveredTerm>> terms
               = engine.findTerms( finder, lookupTokens, _consecutiveSkipMax, _totalSkipMax );
         terms.forEach( (k,v) -> discoveredTerms.computeIfAbsent( k,  t -> new HashSet<>() )
                                                .addAll( v ) );
      }
      return discoveredTerms;
   }

   static private final Predicate<BaseToken> isWantedToken = t -> !(t instanceof NewlineToken);

   private final Function<BaseToken, LookupToken> toLookupToken = b -> new LookupToken( b, isValidLookup( b ) );

   /**
    *
    * @param baseToken -
    * @return true if it is a word token of length > minimum lookup span and valid lookup part of speech.
    */
   private boolean isValidLookup( final BaseToken baseToken ) {
      // We are only interested in tokens that are -words- of a certain length.
      if ( !(baseToken instanceof WordToken)
           || (baseToken.getEnd() - baseToken.getBegin() < _minLookupSpan) ) {
         return false;
      }
      if ( _lookupAllPos ) {
         return true;
      }
      // We are only interested in tokens that are -words- of the wanted part of speech.
      final String partOfSpeech = baseToken.getPartOfSpeech();
      return partOfSpeech == null || _lookupPos.contains( partOfSpeech );
   }


   private DictionaryChecker getDictionaryChecker() {
      return new DictionaryChecker();
   }

//   private Collection<Details> getDetails( final DiscoveredTerm discoveredTerm ) {
//      return DetailerStore.getInstance()
//                          .getDetailers()
//                          .stream()
//                          .map( e -> e.getDetails( discoveredTerm ) )
//                          .filter( Objects::nonNull )
//                          .flatMap( Collection::stream )
//                          .collect( Collectors.toSet() );
//   }

   private Collection<Details> getDetails( final String uri ) {
      return DetailerStore.getInstance()
                          .getDetailers()
                          .stream()
                          .map( e -> e.getDetails( uri ) )
                          .filter( Objects::nonNull )
                          .flatMap( Collection::stream )
                          .collect( Collectors.toSet() );
   }

   private Collection<DetailedTerm> createDetailedTerms(
         final Map<Pair<Integer>,Collection<DiscoveredTerm>> discoveredTermsMap,
         final Map<String,Collection<Details>> uriDetailsMap ) {
      final Collection<DetailedTerm> detailedTerms = new ArrayList<>();
      for ( Map.Entry<Pair<Integer>,Collection<DiscoveredTerm>> termEntry : discoveredTermsMap.entrySet() ) {
         final Pair<Integer> textSpan = termEntry.getKey();
         termEntry.getValue()
                  .stream()
                  .map( d -> new DetailedTerm( textSpan, d,
                        uriDetailsMap.getOrDefault( d.getUri(), Collections.emptyList() ) ) )
                  .forEach( detailedTerms::add );
      }
      return detailedTerms;
   }

   private Collection<DetailedTerm> getFilteredTerms( final Collection<DetailedTerm> detailedTerms ) {
      return _termFilter.getFilteredTerms( detailedTerms );
   }

   // TODO - redo creation of IdentifiedAnnotation from detailedTerm
   private void createAnnotations( final JCas jCas,
                                   final Collection<DetailedTerm> detailedTerms ) {
      _annotationCreator.createAnnotations( jCas, detailedTerms );
   }


   static protected int parseInt( final Object value, final String name, final int defaultValue ) {
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

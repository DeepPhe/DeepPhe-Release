package org.apache.ctakes.ner.dictionary.bsv;


import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.ner.dictionary.Dictionary;
import org.apache.ctakes.ner.dictionary.InMemoryDictionary;
import org.apache.ctakes.ner.term.TermCandidate;
import org.apache.ctakes.ner.tokenizer.CasedTokenizer;
import org.apache.ctakes.ner.tokenizer.RareWordUtil;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
final public class BsvDictionary implements org.apache.ctakes.ner.dictionary.Dictionary {

   static public final String DICTIONARY_TYPE = "BSV";

   static private final Logger LOGGER = Logger.getLogger( "BsvTermFinder" );

   final private Dictionary _delegateTermFinder;

   /**
    * @param name        unique name for dictionary
    * @param uimaContext -
    */
   public BsvDictionary( final String name, final UimaContext uimaContext ) {
      this( name, EnvironmentVariable.getEnv( name + "_file", uimaContext ) );
   }

   /**
    * @param name    unique name for dictionary
    * @param bsvPath path to bsv file containing synonyms and cuis
    */
   public BsvDictionary( final String name, final String bsvPath ) {
      _delegateTermFinder = new InMemoryDictionary( getName(), parseBsvTerms( bsvPath) );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateTermFinder.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<TermCandidate> getTermCandidates( final String lookupText,
                                                       final int indexInSentence,
                                                       final int sentenceLength ) {
      return _delegateTermFinder.getTermCandidates( lookupText, indexInSentence, sentenceLength );
   }

   /**
    *
    * @param bsvFilePath path to file containing term rows and bsv columns
    * @return collection of all valid terms read from the bsv file
    */
   static private Map<String, Collection<TermCandidate>> parseBsvTerms( final String bsvFilePath ) {
      final Collection<String> lines = new ArrayList<>();
      try ( final BufferedReader reader
                  = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( bsvFilePath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            line = line.trim();
            if ( line.isEmpty() || line.startsWith( "//" ) || line.startsWith( "#" ) ) {
               line = reader.readLine();
               continue;
            }
            lines.add( line );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return Collections.emptyMap();
      }
      final Map<String, Collection<String[]>> uriTokens = createUriTokens( lines, bsvFilePath );
      final Collection<String[]> tokensCollection = uriTokens.values()
                                                             .stream()
                                                             .flatMap( Collection::stream )
                                                             .collect( Collectors.toList() );
      final Map<String, Long> tokenCounts = RareWordUtil.getTokenCounts( tokensCollection );
      final Map<String[], Integer> rareIndices = getRareIndices( tokensCollection, tokenCounts );
      return createRareTokenTerms( uriTokens, rareIndices );
   }

   static private Map<String, Collection<String[]>> createUriTokens( final Collection<String> lines,
                                                                     final String bsvFilePath ) {
      final Map<String, Collection<String[]>> uriTokens = new HashMap<>();
      for ( String line : lines ) {
         final String[] splits = StringUtil.fastSplit( line, '|' );
         if ( splits.length != 2 ) {
            LOGGER.warn( "Invalid Line \"" + line + "\" in " + bsvFilePath );
            continue;
         }
         final String uri = splits[ 0 ].trim();
         if ( uri.isEmpty() ) {
            LOGGER.warn( "Invalid Line \"" + line + "\" in " + bsvFilePath );
            continue;
         }
         final String text = splits[ 1 ].trim();
         if ( text.isEmpty() ) {
            LOGGER.warn( "Invalid Line \"" + line + "\" in " + bsvFilePath );
            continue;
         }
         final String[] tokens = CasedTokenizer.getTokens( text );
         uriTokens.computeIfAbsent( uri, u -> new HashSet<>() ).add( tokens );
      }
      return uriTokens;
   }

   static private Map<String[], Integer> getRareIndices( final Collection<String[]> tokensCollection,
                                                         final Map<String, Long> tokenCounts ) {
      final Map<String[], Integer> rareIndices = new HashMap<>( tokensCollection.size() );
      for ( String[] tokens : tokensCollection ) {
         int rareIndex = RareWordUtil.getRareTokenIndex( tokens, tokenCounts );
         if ( rareIndex < 0 ) {
            rareIndex = getLousyIndex( tokens );
         }
         rareIndices.put( tokens, rareIndex );
      }
      return rareIndices;
   }

   static private int getLousyIndex( final String[] tokens ) {
      int index = 0;
      int bestLength = 0;
      for ( int i = 0; i < tokens.length; i++ ) {
         if ( tokens[ i ].length() > bestLength ) {
            bestLength = tokens[ i ].length();
            index = i;
         }
      }
      return index;
   }

   static private Map<String, Collection<TermCandidate>> createRareTokenTerms(
         final Map<String, Collection<String[]>> uriTokensMap,
         final Map<String[], Integer> rareIndices ) {
      final Map<String, Collection<TermCandidate>> rareTokenTerms = new HashMap<>( uriTokensMap.size() );
      for ( Map.Entry<String,Collection<String[]>> uriTokens : uriTokensMap.entrySet() ) {
         final String uri = uriTokens.getKey();
         for ( String[] tokens : uriTokens.getValue() ) {
            final int rareIndex = rareIndices.getOrDefault( tokens, 0 );
            final TermCandidate term = new TermCandidate( uri, rareIndex, tokens );
            rareTokenTerms.computeIfAbsent( tokens[rareIndex].toLowerCase(), u -> new HashSet<>() ).add( term );
         }
      }
      return rareTokenTerms;
   }
}


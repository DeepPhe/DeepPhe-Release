package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {10/20/2021}
 */
@PipeBitInfo(
      name = "Paragraph Finder",
      description = "Annotates Paragraphs by detecting them using Regular Expressions provided in an input File or by empty text lines.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.PARAGRAPH }
)
final public class ParagraphFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "ParagraphFinder" );


   static public final String PARAGRAPH_TYPES_PATH = "PARAGRAPH_TYPES_PATH";
   static public final String PARAGRAPH_TYPES_DESC
         = "path to a file containing a list of regular expressions and corresponding paragraph types.";


   @ConfigurationParameter(
         name = PARAGRAPH_TYPES_PATH,
         description = PARAGRAPH_TYPES_DESC,
         mandatory = false
   )
   private String _paragraphTypesPath;

   // Allows spaces or tabs within the double-eol paragraph separator.
//   static private final String DEFAULT_PARAGRAPH = "Default Paragraph||(?:(?:[\\t ]*\\r?\\n){2,})";
   static private final String DEFAULT_SEPARATOR = "(?:^[\\t ]*\\r?\\n)+";

   /**
    * Holder for section type as defined in the user's specification bsv file
    */
   static private final class ParagraphType {
      private final String __name;
      private final Pattern __separatorPattern;

      private ParagraphType( final String name, final Pattern separatorPattern ) {
         __name = name;
         __separatorPattern = separatorPattern;
      }
   }

   private final Collection<ParagraphType> _paragraphTypes = new HashSet<>();


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      if ( _paragraphTypesPath == null ) {
         LOGGER.info( "No " + PARAGRAPH_TYPES_DESC + ", Using default paragraph separator: two newlines" );
         _paragraphTypes.add( new ParagraphType( "Default",
                                                 Pattern.compile( DEFAULT_SEPARATOR, Pattern.MULTILINE ) ) );
         return;
      }
      try {
         final List<RegexUtil.RegexItemInfo> itemInfos
               = RegexUtil.parseBsvFile( _paragraphTypesPath,
                                         1,
                                         "Paragraph Separator Type || Separator regular expression" );
         for ( RegexUtil.RegexItemInfo itemInfo : itemInfos ) {
            _paragraphTypes.add( new ParagraphType( itemInfo.getName(),
                                                    itemInfo.getPatternList().get( 0 ) ) );
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
//      LOGGER.info( "Finished Parsing" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      if ( _paragraphTypes.isEmpty() ) {
//         LOGGER.info( "Finished processing, no paragraph types defined" );
         return;
      }
      LOGGER.info( "Annotating Paragraphs ..." );
      createParagraphs( jcas );
   }


   private Collection<Pair<Integer>> findSeparators( final String docText ) {
      final Collection<Pair<Integer>> separators = new HashSet<>();
      for ( ParagraphType paragraphType : _paragraphTypes ) {
         if ( paragraphType.__separatorPattern == null ) {
            continue;
         }
         separators.addAll( findSeparators( docText, paragraphType.__separatorPattern ) );
      }
      return separators;
   }

   // package protected for unit tests
   static Collection<Pair<Integer>> findSeparators( final String docText,
                                                    final Pattern pattern ) {
      // the start tag of this tag is the start of the current match
      // the end tag of this tag is the end of the current match, exclusive
      try ( RegexSpanFinder finder = new RegexSpanFinder( pattern ) ) {
         return finder.findSpans( docText );
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.error( iaE.getMessage() );
      }
      return Collections.emptyList();
   }


   /**
    * All tags are treated equally as segment bounds, whether header or footer
    *
    * @param jcas -
    */
   private void createParagraphs( final JCas jcas ) {
      final Collection<Segment> sections = JCasUtil.select( jcas, Segment.class );
      for ( Segment section : sections ) {
         final int offset = section.getBegin();
         final String text = section.getCoveredText();
         final Collection<Pair<Integer>> separators = findSeparators( text );
         if ( separators.isEmpty() ) {
            // whole text is simple paragraph
            final Paragraph paragraph = new Paragraph( jcas, offset, section.getEnd() );
            paragraph.addToIndexes();
            continue;
         }
         final List<Pair<Integer>> boundsList = separators.stream()
                                                          .sorted( Comparator.comparingInt( Pair::getValue1 ) )
                                                          .collect( Collectors.toList() );
         Pair<Integer> leftBounds = boundsList.get( 0 );
         int paragraphEnd;
         if ( leftBounds.getValue1() > 0 ) {
            // Add unspecified generic first paragraph
            paragraphEnd = leftBounds.getValue1();
            if ( offset < 0 || offset + paragraphEnd < 0 ) {
               LOGGER.error( "First Paragraph out of bounds " + offset + "," + (offset + paragraphEnd) );
            } else {
               final Paragraph paragraph = new Paragraph( jcas, offset, offset + paragraphEnd );
               paragraph.addToIndexes();
            }
            // will start the next paragraph with bounds at 0
         }
         final int length = boundsList.size();
         // add paragraphs 1 -> n
         for ( int i = 0; i < length; i++ ) {
            leftBounds = boundsList.get( i );
            final int paragraphBegin = leftBounds.getValue2();
            if ( i + 1 < length ) {
               paragraphEnd = boundsList.get( i + 1 ).getValue2();
            } else {
               // the last paragraph
               paragraphEnd = text.length();
            }
            if ( paragraphBegin == paragraphEnd ) {
               // TODO
               continue;
            }
            if ( offset + paragraphBegin < 0 || offset + paragraphEnd < 0 ) {
               LOGGER.error( "Paragraph out of bounds " + (offset + paragraphBegin) + "," + (offset + paragraphEnd) );
            } else {
               final Paragraph paragraph = new Paragraph( jcas, offset + paragraphBegin, offset + paragraphEnd );
               paragraph.addToIndexes();
            }
         }
      }
   }


}

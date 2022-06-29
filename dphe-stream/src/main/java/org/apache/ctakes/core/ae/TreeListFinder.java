package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.core.util.regex.TimeoutMatcher;
import org.apache.ctakes.core.util.window.InWindowFinder;
import org.apache.ctakes.core.util.window.InWindowFinderUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.FormattedList;
import org.apache.ctakes.typesystem.type.textspan.FormattedListEntry;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {10/12/2021}
 */
@PipeBitInfo(
      name = "Tree List Finder",
      description = "Annotates formatted List Sections by detecting them using Regular Expressions provided in an input File.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.LIST }
)
final public class TreeListFinder extends JCasAnnotator_ImplBase implements InWindowFinder<FormattedList> {
// TODO Replace ctakes ListAnnotator in core.ae with this.

   static private final Logger LOGGER = Logger.getLogger( "TreeListFinder" );


   static public final String LIST_TYPES_PATH = "TreeListRegexBsv";
   static private final String LIST_TYPES_DESC
         = "path to a file containing a list of regular expressions and corresponding list types.";


   @ConfigurationParameter(
         name = LIST_TYPES_PATH,
         description = LIST_TYPES_DESC,
         defaultValue = "org/apache/ctakes/core/treelist/DefaultTreeListRegex.bsv"
   )
   private String _listRegexPath;

// The new format is:
// List Name || Full List Regex || Single List Line Regex
// Where Single Line Regex may contain the named groups <Index> <Name> <Value> <Details>

   static private final String HEADING_GROUP = "Heading";
   static private final String REFINEMENT_GROUP = "Refinement";
   static private final String LIST_GROUP = "List";


   /**
    * Holder for list type as defined in the user's specification bsv file
    */
   static private final class TreeListType {
      private final String __name;
      private final Pattern __fullPattern;
      private final Pattern __entryPattern;

      private TreeListType( final String name,
                        final Pattern fullPattern,
                        final Pattern entryPattern ) {
         __name = name;
         __fullPattern = fullPattern;
         __entryPattern = entryPattern;
      }
   }

   private final List<TreeListType> _treeListTypes = new ArrayList<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      try {
         final List<RegexUtil.RegexItemInfo> itemInfos
               = RegexUtil.parseBsvFile( _listRegexPath,
                                         2,
                                         "TreeList Name/Type "
                                         + "|| Full Regex; Heading, {Refinement}, List "
                                         + "|| List Entry Regex; Name, Value" );
         for ( RegexUtil.RegexItemInfo itemInfo : itemInfos ) {
            final List<Pattern> patternList = itemInfo.getPatternList();
            final TreeListType treeListType = new TreeListType( itemInfo.getName(),
                                                                patternList.get( 0 ),
                                                                patternList.get( 1 ) );
            _treeListTypes.add( treeListType );
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      if ( _treeListTypes.isEmpty() ) {
         LOGGER.warn( "No TreeList types defined.  TreeList Finding will be skipped." );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      if ( _treeListTypes.isEmpty() ) {
         return;
      }
      InWindowFinderUtil.findInWindows( jcas, LOGGER, "Finding TreeLists", this );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<FormattedList> addFound( final JCas jcas, final Integer windowOffset, final String windowText,
                                   final List<FormattedList> foundItems ) {
      return findTreeLists( jcas, windowOffset, windowText, foundItems );
   }


   private List<FormattedList> findTreeLists( final JCas jCas,
                                         final int windowOffset,
                                         final String windowText,
                                         final List<FormattedList> treeLists ) {
      if ( windowText.trim().length() <= 3 ) {
         return treeLists;
      }
      final int windowLength = windowText.length();
      final Collection<Pair<Integer>> windowSpans
            = new HashSet<>( TextSpanUtil.getAvailableSpans( windowOffset, windowLength, treeLists ) );
      for ( TreeListType treeListType : _treeListTypes ) {
         final List<FormattedList> newLists
               = findTreeLists( jCas, windowOffset, windowSpans, windowText, treeListType );
         if ( newLists.isEmpty() ) {
            continue;
         }
         treeLists.addAll( newLists );
         windowSpans.clear();
         windowSpans.addAll( TextSpanUtil.getAvailableSpans( windowOffset, windowLength, treeLists ) );
         if ( windowSpans.isEmpty() ) {
            break;
         }
      }
      return treeLists;
   }

   static private List<FormattedList> findTreeLists( final JCas jCas,
                                                final int windowOffset,
                                                final Collection<Pair<Integer>> windowSpans,
                                                final String windowText,
                                                final TreeListType treeListType ) {
      final List<FormattedList> treeLists = new ArrayList<>();
      for ( final Pair<Integer> span : windowSpans ) {
         treeLists.addAll(
               findTreeLists( jCas, windowOffset, span.getValue1(), span.getValue2(), windowText, treeListType) );
      }
      return treeLists;
   }


   static private List<FormattedList> findTreeLists( final JCas jCas,
                                                final int windowOffset,
                                                final int begin,
                                                final int end,
                                                final String windowText,
                                                final TreeListType treeListType ) {
      final String spanText = windowText.substring( begin, end );
      if ( spanText.trim().length() <= 3 ) {
         return Collections.emptyList();
      }
      final List<FormattedList> treeLists = new ArrayList<>();
      try ( TimeoutMatcher finder = new TimeoutMatcher( treeListType.__fullPattern, spanText ) ) {
         Matcher matcher = finder.nextMatch();
         while ( matcher != null ) {
            final FormattedList treeList
                  = createTreeList( jCas, windowOffset + begin, spanText, matcher, treeListType );
            treeLists.add( treeList );
            matcher = finder.nextMatch();
         }
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.error( iaE.getMessage() );
      }
      return treeLists;
   }


   static private FormattedList createTreeList( final JCas jCas,
                                                final int spanOffset,
                                                final String spanText,
                                                final Matcher matcher,
                                                final TreeListType treeListType ) {
      final FormattedList list = new FormattedList( jCas,
                                                    spanOffset + matcher.start(),
                                                    spanOffset + matcher.end() );
      list.setListType( treeListType.__name );
      final IdentifiedAnnotation heading = createHeading( jCas, spanOffset, matcher );
      if ( heading != null ) {
         list.setHeading( heading );
      }
      final Pair<Integer> listSpan = RegexUtil.getGroupSpan( matcher, LIST_GROUP );
      if ( RegexUtil.isValidSpan( listSpan ) ) {
         final String listText = spanText.substring( listSpan.getValue1(), listSpan.getValue2() );
         final List<FormattedListEntry> listEntries
               = ListEntryFinder.createListEntries( jCas, spanOffset + listSpan.getValue1(), listText,
                                                    treeListType.__entryPattern );
         final List<FormattedListEntry> entryList =
               listEntries.stream()
                          .sorted( Comparator.comparingInt( FormattedListEntry::getBegin ) )
                          .collect( Collectors.toList() );
         final FSArray entryArray = new FSArray( jCas, listEntries.size() );
         for ( int i=0; i < entryList.size(); i++ ) {
            entryArray.set( i, entryList.get( i ) );
         }
         entryArray.addToIndexes();
         list.setListEntries( entryArray );
      }
      list.addToIndexes();
      return list;
   }

   static private IdentifiedAnnotation createHeading( final JCas jCas,
                                                      final int spanOffset,
                                                      final Matcher matcher ) {
      final Pair<Integer> span = RegexUtil.getGroupSpan( matcher, HEADING_GROUP );
      if ( !RegexUtil.isValidSpan( span ) ) {
         return null;
      }
      final IdentifiedAnnotation header
            = new IdentifiedAnnotationBuilder().span( spanOffset + span.getValue1(),
                                              spanOffset + span.getValue2() )
                                               .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
                                               .generic()
                                               .build( jCas );
      final String details = RegexUtil.getGroupText( matcher, REFINEMENT_GROUP );
      if ( !details.isEmpty() ) {
//         header.setDetails( details );
         // TODO Utilize Details in list header
         LOGGER.warn( "TODO: Utilize Details in list header" );
      }
      return header;
   }


   static public AnalysisEngineDescription createEngineDescription( final String listTypesPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( TreeListFinder.class,
                                                            LIST_TYPES_PATH, listTypesPath );
   }


}

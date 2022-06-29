package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.typesystem.type.textspan.FormattedList;
import org.apache.ctakes.typesystem.type.textspan.FormattedListEntry;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {10/12/2021}
 */
@PipeBitInfo(
      name = "List Finder",
      description = "Annotates formatted List Sections by detecting them using Regular Expressions provided in an input File.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.LIST }
)
final public class ListFinder extends JCasAnnotator_ImplBase {
   // TODO Replace ctakes ListAnnotator in core.ae with this.
   // TODO Deprecate the old ListAnnotator stuff.

   static private final Logger LOGGER = Logger.getLogger( "ListFinder" );


   static public final String LIST_TYPES_PATH = "LIST_TYPES_PATH";
   static private final String LIST_TYPES_DESC
         = "path to a file containing a list of regular expressions and corresponding list types.";

   /**
    * classic ctakes default segment id
    */
   static private final String DEFAULT_LIST_ID = "SIMPLE_LIST";

   @ConfigurationParameter(
         name = LIST_TYPES_PATH,
         description = LIST_TYPES_DESC,
         defaultValue = "org/apache/ctakes/core/list/DefaultListRegex.bsv"
   )
   private String _listTypesPath;

// The new format is:
// List Name || Full List Regex || Single List Line Regex
// Where Single Line Regex may contain the named groups <Index> <Name> <Value> <Details>

//   static private final String INDEX_GROUP = "Index";
//   static private final String NAME_GROUP = "Name";
//   static private final String VALUE_GROUP = "Value";
//   static private final String DETAILS_GROUP = "Details";

   /**
    * Holder for list type as defined in the user's specification bsv file
    */
   static private final class ListType {
      private final String __name;
      private final Pattern __listPattern;
      private final Pattern __entryPattern;

      private ListType( final String name,
                        final Pattern fullPattern,
                        final Pattern entryPattern ) {
         __name = name;
         __listPattern = fullPattern;
         __entryPattern = entryPattern;
      }
   }

   private final Collection<ListType> _listTypes = new HashSet<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      try {
         final List<RegexUtil.RegexItemInfo> itemInfos
               = RegexUtil.parseBsvFile( _listTypesPath,
                                         2,
                                         "List Name/Type "
                                         + "|| Full Regex "
                                         + "|| List Entry Regex; Index, Name, Value, Details" );
         for ( RegexUtil.RegexItemInfo itemInfo : itemInfos ) {
            final List<Pattern> patternList = itemInfo.getPatternList();
            final ListType listType = new ListType( itemInfo.getName(), patternList.get( 0 ), patternList.get( 1 ) );
            _listTypes.add( listType );
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      if ( _listTypes.isEmpty() ) {
         LOGGER.info( "No List types defined." );
         return;
      }
      LOGGER.info( "Annotating Lists ..." );
      final Collection<Paragraph> paragraphs = JCasUtil.select( jcas, Paragraph.class );
      if ( paragraphs != null && !paragraphs.isEmpty() ) {
         for ( Paragraph paragraph : paragraphs ) {
            try {
               final Map<Pair<Integer>, ListType> spannedListTypes = findListTypes( paragraph.getCoveredText() );
//               final Map<Pair<Integer>, ListType> uniqueListTypes = getUniqueListTypes( listTypes );
//               createLists( jcas, uniqueListTypes, paragraph.getCoveredText(), paragraph.getBegin() );
               createLists( jcas, spannedListTypes, paragraph.getCoveredText(), paragraph.getBegin() );
            } catch ( StringIndexOutOfBoundsException oobE ) {
               // I'm not sure how this ever happens.  Paragraph bounds from the ParagraphAnnotator are always valid.
               // I have run ~1000 notes without problem, but one note in Seer causes problems.  Ignore.
            }
         }
      } else {
         for ( Segment section : JCasUtil.select( jcas, Segment.class ) ) {
            final Map<Pair<Integer>, ListType> spannedListTypes = findListTypes( section.getCoveredText() );
//            final Map<Pair<Integer>, ListType> uniqueListTypes = getUniqueListTypes( listTypes );
//            createLists( jcas, uniqueListTypes, section.getCoveredText(), section.getBegin() );
            createLists( jcas, spannedListTypes, section.getCoveredText(), section.getBegin() );
         }
      }
//      LOGGER.info( "Finished processing" );
   }


   private Map<Pair<Integer>, ListType> findListTypes( final String windowText ) {
      if ( windowText.length() < 10 ) {
         return Collections.emptyMap();
      }
      int count = 0;
      int lineLength = 0;
      for ( char c : windowText.toCharArray() ) {
         lineLength++;
         if ( c == '\n' ) {
            if ( lineLength > 4 ) {
               count++;
               if ( count > 1 ) {
                  break;
               }
            }
            lineLength = 0;
         }
      }
      if ( count < 2 ) {
         return Collections.emptyMap();
      }
      final Map<Pair<Integer>, ListType> spannedListTypes = new HashMap<>();
      for ( ListType listType : _listTypes ) {
         if ( listType.__listPattern == null ) {
            continue;
         }
         final Collection<Pair<Integer>> currentListSpans = new HashSet<>();
         try ( RegexSpanFinder finder = new RegexSpanFinder( listType.__listPattern ) ) {
            final List<Pair<Integer>> spans = finder.findSpans( windowText );
            currentListSpans.addAll( spans );
         }
         if ( currentListSpans.isEmpty() ) {
            continue;
         }
         for ( Pair<Integer> span : currentListSpans ) {
            if ( span.getValue1() >= 0 && span.getValue2() <= windowText.length() ) {
               spannedListTypes.put( span, listType );
            }
         }
      }
      return spannedListTypes;
   }

   /**
    * @param jcas      ye olde ...
    * @param spannedListTypes discovered list spans and corresponding list types
    * @param windowText      full text for the window (Section or Paragraph)
    * @param windowOffset    offset of the given window within the whole document
    */
   static private void createLists( final JCas jcas,
                                    final Map<Pair<Integer>, ListType> spannedListTypes,
                                    final String windowText,
                                    final int windowOffset ) {
      if ( spannedListTypes == null || spannedListTypes.isEmpty() ) {
         return;
      }
      for ( Map.Entry<Pair<Integer>, ListType> spannedListType : spannedListTypes.entrySet() ) {
         final Pair<Integer> span = spannedListType.getKey();
         final String listText = windowText.substring( span.getValue1(), span.getValue2() );
         final ListType listType = spannedListType.getValue();
         createList( jcas,
                     windowOffset + span.getValue1(),
                     listText,
                     listType.__name,
                     listType.__entryPattern );
      }
   }

   static public FormattedList createList( final JCas jCas,
                                            final int listOffset,
                                            final String listText,
                                            final String listName,
                                            final Pattern entryPattern ) {
      final FormattedList list = new FormattedList( jCas, listOffset, listOffset + listText.length() );
      final List<FormattedListEntry> listEntries
            = ListEntryFinder.createListEntries( jCas, listOffset, listText, entryPattern );
      final List<FormattedListEntry> entryList =
            listEntries.stream()
                       .sorted( Comparator.comparingInt( FormattedListEntry::getBegin ) )
                       .collect( Collectors.toList() );
      final FSArray entryArray = new FSArray( jCas, listEntries.size() );
      for ( int i=0; i < entryList.size(); i++ ) {
         entryArray.set( i, entryList.get( i ) );
      }
      entryArray.addToIndexes();
      list.setListType( listName );
      list.setListEntries( entryArray );
      list.addToIndexes();
      return list;
   }




   static public AnalysisEngineDescription createEngineDescription( final String listTypesPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ListFinder.class,
                                                            LIST_TYPES_PATH, listTypesPath );
   }








   //   static private final Function<Pair<Integer>,Integer> sizePair = p -> p.getValue2() - p.getValue1();

   // TODO - Do we actually want to get rid of overlaps?  It seems like two overlapping lists may each have
   // valuable Index, Name, Value, Details information.
//   /**
//    * Get rid of list overlaps
//    *
//    * @param listTypes -
//    * @return list types that don't overlap
//    */
//   static private Map<Pair<Integer>, ListType> getUniqueListTypes( final Map<Pair<Integer>, ListType> listTypes ) {
//      if ( listTypes == null || listTypes.size() <= 1 ) {
//         return listTypes;
//      }
//      final Collection<Pair<Integer>> notSubsumed = subsumeSpans( listTypes.keySet() );
//      listTypes.keySet().retainAll( notSubsumed );
//
//
//      final Collection<Pair<Integer>> removalTypeBounds = new HashSet<>();
//      final Map<Pair<Integer>, Pair<Integer>> newTypeBounds = new HashMap<>();
//      while ( true ) {
//         final List<Pair<Integer>> sortedSpans
//               = listTypes.keySet().stream()
//                          .sorted( Comparator.comparingInt( Pair::getValue1 ) )
//                          .collect( Collectors.toList() );
//         final List<Pair<Integer>> sortedSizes
//               = listTypes.keySet().stream()
//                          .sorted( Comparator.comparing( sizePair ).reversed() )
//                          .collect( Collectors.toList() );
//         for ( int i = 0; i < sortedSpans.size() - 1; i++ ) {
//            final Pair<Integer> spanI = sortedSpans.get( i );
//            for ( int j = i + 1; j < sortedSpans.size(); j++ ) {
//               final Pair<Integer> spanJ = sortedSpans.get( j );
//               if ( spanI.getValue1() >= spanJ.getValue2() ) {
//                  break;
//               }
//               if ( spanI.getValue1() <= spanJ.getValue1() && spanI.getValue2() < spanJ.getValue2() ) {
//                  removalTypeBounds.remove(  )
//               }
//
//
//
//               if ( spanI.getValue1() <= boundsJ.getValue1() && boundsJ.getValue1() <= spanI.getValue2() ) {
//                  removalTypeBounds.add( boundsJ );
////                  if ( boundsJ.getValue2() > boundsI.getValue2() ) {
////                      Add J as a second list
////                     newTypeBounds.put( new Pair<>( boundsI.getValue2(), boundsJ.getValue2() ), boundsJ );
////                  }
//               } else if
//               } else if ( boundsJ.getValue2() >= boundsI.getValue1() && boundsJ.getValue2() <= boundsI.getValue2() ) {
//                  removalTypeBounds.add( boundsJ );
//                  if ( boundsJ.getValue1() < boundsI.getValue1() ) {
//                     // Add J as a second list
//                     newTypeBounds.put( new Pair<>( boundsJ.getValue1(), boundsI.getValue1() ), boundsJ );
//                  }
//               }
//            }
//         }
//         if ( removalTypeBounds.isEmpty() ) {
//            return listTypes;
//         }
//         for ( Map.Entry<Pair<Integer>, Pair<Integer>> pairEntry : newTypeBounds.entrySet() ) {
//            listTypes.put( pairEntry.getKey(), listTypes.get( pairEntry.getValue() ) );
//         }
//         removalTypeBounds.addAll( newTypeBounds.values() );
//         listTypes.keySet().removeAll( removalTypeBounds );
//         if ( listTypes.size() == 1 ) {
//            return listTypes;
//         }
//         newTypeBounds.clear();
//         removalTypeBounds.clear();
//      }
//   }
//
//
//   static private Collection<Pair<Integer>> subsumeSpans( final Collection<Pair<Integer>> spans ) {
//      final List<Pair<Integer>> sortedSizes
//            = spans.stream()
//                   .sorted( Comparator.comparing( sizePair ).reversed() )
//                   .collect( Collectors.toList() );
//      final Collection<Pair<Integer>> removals = new HashSet<>();
//      for ( int i = 0; i < sortedSizes.size() - 1; i++ ) {
//         final Pair<Integer> spanI = sortedSizes.get( i );
//         for ( int j = i + 1; j < sortedSizes.size(); j++ ) {
//            final Pair<Integer> spanJ = sortedSizes.get( j );
//            if ( !removals.contains( spanJ )
//                 && spanI.getValue1() <= spanJ.getValue1()
//                 && spanJ.getValue2() <= spanI.getValue2() ) {
//               removals.add( spanI );
//            }
//         }
//      }
//      sortedSizes.removeAll( removals );
//      return sortedSizes;
//   }
//
//
//   static private Map<Pair<Integer>,Pair<Integer>> replaceOverlaps( final Collection<Pair<Integer>> spans ) {
//
//   }


}

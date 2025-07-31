package org.apache.ctakes.core.util.regex;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.Pair;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {10/26/2021}
 */
public final class RegexUtil {

   static private final Logger LOGGER = Logger.getLogger( "RegexUtil" );

   private RegexUtil() {}

   static public List<RegexItemInfo> parseBsvFile( final String filePath ) throws IOException {
      return parseBsvFile( filePath, 1, "" );
   }

   static public List<RegexItemInfo> parseBsvFile( final String filePath, final int regexMin,
                                                   final String columnFormat ) throws IOException {
      if ( filePath == null || filePath.isEmpty() ) {
         throw new IOException( "No File Path to Regex BSV." );
      }
      final List<RegexItemInfo> regexItemInfos = new ArrayList<>();
//      LOGGER.info( "Parsing " + filePath );
      try ( BufferedReader reader
                  = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( filePath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            final RegexItemInfo regexItemInfo = parseBsvLine( line, regexMin, columnFormat );
            if ( !regexItemInfo.equals( COMMENT_INFO ) ) {
               regexItemInfos.add( regexItemInfo );
            }
            line = reader.readLine();
         }
      }
      return regexItemInfos;
   }

   static public RegexItemInfo parseBsvLine( final String line, final int regexMin, final String columnFormat ) {
      if ( line.isEmpty() || line.startsWith( "#" ) || line.startsWith( "//" ) ) {
         // comment
         return COMMENT_INFO;
      }
      final String[] splits = line.split( "\\|\\|" );
      if ( splits.length < 1 + regexMin ) {
         LOGGER.warn( "Bad Regex definition: " + line + " ; There must be at least a name plus "
                      + regexMin + " regular expressions." );
         if ( !columnFormat.isEmpty() ) {
            LOGGER.warn( columnFormat );
         }
         return COMMENT_INFO;
      }
      return new RegexItemInfo( splits );
   }


   static public final Pair<Integer> INVALID_SPAN = new Pair<>( -1, -1 );

   static public String getGroupText( final Matcher matcher, final String groupName ) {
      try {
         final String value = matcher.group( groupName );
         return value == null ? "" : value;
      } catch ( IllegalArgumentException iaE ) {
         return "";
      }
   }


   static public Pair<Integer> getGroupSpan( final Matcher matcher, final String groupName ) {
      try {
         final int start = matcher.start( groupName );
         final int end = matcher.end( groupName );
         return new Pair<>( start, end );
      } catch ( IllegalArgumentException | IndexOutOfBoundsException multE ) {
         return INVALID_SPAN;
      }
   }

   static public boolean isValidSpan( final Pair<Integer> span ) {
      return !INVALID_SPAN.equals( span );
   }



   static private final RegexItemInfo COMMENT_INFO = new RegexItemInfo( new String[]{ "COMMENT" } );

   static public final class RegexItemInfo {
      private final String _name;
      private final List<String> _regexList = new ArrayList<>();
      private RegexItemInfo( final String[] splits ) {
         _name = splits[ 0 ].trim();
         for ( int i=1; i<splits.length; i++ ) {
            _regexList.add( splits[ i ].trim() );
         }
      }
      public String getName() {
         return _name;
      }
      public List<String> getRegexList() {
         return _regexList;
      }
      public List<Pattern> getPatternList() {
         return _regexList.stream()
                          .map( r -> Pattern.compile( r, Pattern.MULTILINE ) )
                          .collect( Collectors.toList() );
      }
   }


}

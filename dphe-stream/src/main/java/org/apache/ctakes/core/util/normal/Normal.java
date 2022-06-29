package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
public interface Normal {

   String getNormalName();

//   default boolean hasValue( final String text ) {
//      return !getSpan( text ).equals( NormalizeUtil.SPAN_NOT_FOUND );
//   }
//
//   default boolean isValue( final String text ) {
//      final Pair<Integer> span = getSpan( text );
//      return span.getValue1() == 0 && span.getValue2() == text.length();
//   }

   default Pair<Integer> getSpan( final String text ) {
      return getSpan( text, 0 );
   }

   Pair<Integer> getSpan( String text, int startIndex );

   default List<Pair<Integer>> getSpans( final String text ) {
      final List<Pair<Integer>> spans = new ArrayList<>();
      Pair<Integer> span = getSpan( text );
      if ( span.getValue1() < 0 ) {
         return Collections.emptyList();
      }
      int previousEnd = 0;
      while ( span.getValue1() >= 0 ) {
         spans.add( span );
         previousEnd = span.getValue2();
         span = getSpan( text, previousEnd );
      }
      return spans;
   }

   // Mostly for Debugging
   default String replaceText( final String text ) {
      final List<Pair<Integer>> spans = getSpans( text );
      if ( spans.isEmpty() ) {
         return text;
      }
      final StringBuilder sb = new StringBuilder();
      int previousEnd = 0;
      for ( Pair<Integer> span : spans ) {
         if ( span.getValue1() > 0 ) {
            sb.append( text, previousEnd, span.getValue1() )
              .append( " " );
         }
         sb.append( getNormalName() );
         if ( span.getValue2() < text.length() ) {
            sb.append( " " );
         }
         previousEnd = span.getValue2();
      }
      if ( previousEnd < text.length() ) {
         sb.append( text, previousEnd, text.length() );
      }
      return sb.toString();
   }

   @SafeVarargs
   static <T extends Normal> Map<Pair<Integer>, Collection<T>> getSpanMap( final String text,
                                                                           final T... normals ) {
      final Map<Pair<Integer>, Collection<T>> spanMap = new HashMap<>();
      for ( T normal : normals ) {
         normal.getSpans( text )
               .forEach( s -> spanMap.computeIfAbsent( s, l -> new HashSet<>() )
                                     .add( normal ) );
      }
      return spanMap;
   }

   @SafeVarargs
   static <T extends Normal> Map<Pair<Integer>, Collection<T>> getSingleSpanMap( final String text,
                                                                                 final T... normals ) {
      final Map<Pair<Integer>, Collection<T>> spanMap = new HashMap<>();
      for ( T normal : normals ) {
         normal.getSpans( text )
               .forEach( s -> spanMap.computeIfAbsent( s, l -> new HashSet<>() )
                                     .add( normal ) );
         if ( !spanMap.isEmpty() ) {
            break;
         }
      }
      return spanMap;
   }

   @SafeVarargs
   static <T extends Normal> List<T> getNormals( final String text,
                                                 final T... normals ) {
      final Map<Pair<Integer>, Collection<T>> spanMap = getSpanMap( text, normals );
      return spanMap.keySet()
                    .stream()
                    .sorted( Comparator.comparing( Pair<Integer>::getValue1 )
                                       .thenComparingInt( Pair::getValue2 ) )
                    .map( spanMap::get )
                    .flatMap( Collection::stream )
                    .collect( Collectors.toList() );
   }

   static String replaceText( final String text,
                              final Normal... normals ) {
      String newText = text;
      for ( Normal normal : normals ) {
         newText = normal.replaceText( newText );
      }
      return newText;
   }

}

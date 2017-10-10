package org.apache.ctakes.cancer.phenotype.receptor;

import org.apache.ctakes.cancer.util.SpanOffsetComparator;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/29/2015
 */
final public class StatusFinder {

   private StatusFinder() {
   }

   static private final Logger LOGGER = Logger.getLogger( "StatusFinder" );


   //   // TODO add negative lookahead for "antibody" and "immunostain"
   // TODO - - switch to (and/or(ER|PR|HER2)){0,2}
   static private final String TYPE_REGEX
         = "Triple|(?:Estrogen(?:\\s+and\\s+Progesterone)?)|(?:ER(?:\\s*(?:/|and|or)\\s*(?:PR|(?:HER-? ?2(\\s*/?\\s*neu)?)))?)|(?:Progesterone|PR)|(?:HER-? ?2(\\s*/?\\s*neu)?(?:\\s*\\(?ERBB2\\)?)?)";
   static private final String INTERIM_EX
         = "(?:(?:-?\\s*?Receptors?\\s*-?)?\\s*(?:(?:status|expression|test)\\s*)?(?:(?:is|are|was)\\s*)?\\s*:?)|(?:\\s+PROTEIN\\s+EXPRESSION\\s+\\(0-3\\+\\):)";
   static private final String STRENGTH_EX = "(?:strong|weak)(?:ly)?";
   static private final String LONG_VALUE = "(?:\\+?pos(?:itive)?)|(?:-?neg(?:ative)?)"
         + "|(?:N/?A\\b)|unknown|indeterminate|borderline|equivocal|(?:not\\s+assessed)";
   static private final String SHORT_VALUE = "\\b[0-3]\\+|[0-3]{0}\\+(?:pos)?|-(?:neg)?";

   // Order is very important
   static private final String FULL_REGEX = "\\b(?<TYPE>" + TYPE_REGEX + ")\\s*"
         + "(?:" + INTERIM_EX + ")?\\s*"
         + "(?:" + STRENGTH_EX + ")?\\s*"
         + "(?<VALUE>(?:" + LONG_VALUE + ")|(?:" + SHORT_VALUE + "))";
   static private final Pattern FULL_PATTERN = Pattern.compile( FULL_REGEX, Pattern.CASE_INSENSITIVE );

   static private final String AND_OR = "\\s*,?\\s*(?:/|and|or)?\\s+";
   static private final String ER_PR_HER2 = "(?:" + StatusType.ER.getRegex()
         + ")|(?:" + StatusType.PR.getRegex() + ")|(?:" + StatusType.HER2.getRegex() + ")";


   static private final String VALUE_TYPE_REGEX = "\\b(?:" + STRENGTH_EX + ")?\\s*"
         + "(?:" + LONG_VALUE + ")" +
         "\\s+for\\s+" +
         "(?:" + ER_PR_HER2 + ")" + "(?:" + AND_OR + "(?:" + ER_PR_HER2 + ")){0,2}";

   static private final Pattern VALUE_TYPE_PATTERN = Pattern.compile( VALUE_TYPE_REGEX, Pattern.CASE_INSENSITIVE );


   static public void addReceptorStatuses( final JCas jcas, final AnnotationFS lookupWindow ) {
      final List<Status> statuses = getReceptorStatuses( lookupWindow.getCoveredText() );
      final Collection<Status> statuses2 = getReceptorStatuses2( lookupWindow.getCoveredText() );
      statuses.addAll( statuses2 );
      if ( statuses.isEmpty() ) {
         return;
      }
      statuses.sort( SpanOffsetComparator.getInstance() );
      final int windowStartOffset = lookupWindow.getBegin();
      for ( Status status : statuses ) {
         StatusPhenotypeFactory.getInstance()
               .createPhenotype( jcas, windowStartOffset, status );
      }
   }

   static List<Status> getReceptorStatuses( final String lookupWindow ) {
      if ( lookupWindow.length() < 3 ) {
         return Collections.emptyList();
      }
      final List<Status> statuses = new ArrayList<>();
      final Matcher fullMatcher = FULL_PATTERN.matcher( lookupWindow );
      while ( fullMatcher.find() ) {
         final String typeWindow = fullMatcher.group( "TYPE" );
         final int typeWindowStart = fullMatcher.start( "TYPE" );
         final String valueWindow = fullMatcher.group( "VALUE" );
         final int valueWindowStart = fullMatcher.start( "VALUE" );
         SpannedStatusType spannedType = null;
         SpannedStatusValue spannedValue = null;
         for ( StatusType type : StatusType.values() ) {
            final Matcher typeMatcher = type.getMatcher( typeWindow );
            while ( typeMatcher.find() ) {
               final int typeStart = typeWindowStart + typeMatcher.start();
               final int typeEnd = typeWindowStart + typeMatcher.end();
               spannedType = new SpannedStatusType( type, typeStart, typeEnd );
               spannedValue = null;
               for ( StatusValue value : StatusValue.values() ) {
                  final Matcher valueMatcher = value.getMatcher( valueWindow );
                  if ( valueMatcher.matches() ) {
                     if ( valueWindowStart + valueMatcher.end() < lookupWindow.length()
                           && lookupWindow.charAt( valueWindowStart + valueMatcher.end() ) == ':' ) {
                        // Kludge because negative lookahead doesn't appear to be working.
                        continue;
                     }
                     spannedValue = new SpannedStatusValue( value,
                           valueWindowStart + valueMatcher.start(),
                           valueWindowStart + valueMatcher.end() );
                     break;
                  }
               }
               if ( spannedValue != null ) {
                  statuses.add( new Status( spannedType, spannedValue ) );
               }
            }
         }
      }
      return statuses;
   }

   static List<Status> getReceptorStatuses2( final String lookupWindow ) {
      if ( lookupWindow.length() < 3 ) {
         return Collections.emptyList();
      }
      final List<Status> statuses = new ArrayList<>();
      final Matcher fullMatcher = VALUE_TYPE_PATTERN.matcher( lookupWindow );
      while ( fullMatcher.find() ) {
         final String matchWindow = lookupWindow.substring( fullMatcher.start(), fullMatcher.end() );
         SpannedStatusType spannedType = null;
         SpannedStatusValue spannedValue = null;
         for ( StatusValue value : StatusValue.values() ) {
            final Matcher valueMatcher = value.getMatcher( matchWindow );
            if ( valueMatcher.find() ) {
               final int valueStart = fullMatcher.start() + valueMatcher.start();
               final int valueEnd = fullMatcher.start() + valueMatcher.end();
               spannedValue = new SpannedStatusValue( value, valueStart, valueEnd );
               final String typeLookupWindow = matchWindow.substring( valueMatcher.end() );
               for ( StatusType type : StatusType.values() ) {
                  final Matcher typeMatcher = type.getMatcher( typeLookupWindow );
                  if ( typeMatcher.find() ) {
                     spannedType = new SpannedStatusType( type,
                           valueEnd + typeMatcher.start(),
                           valueEnd + typeMatcher.end() );
                     statuses.add( new Status( spannedType, spannedValue ) );
                  }
               }
            }
         }
      }
      return statuses;
   }

}

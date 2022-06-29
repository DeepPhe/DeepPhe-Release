package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.util.normal.NormalizeUtil;
import org.apache.ctakes.typesystem.type.textspan.FormattedListEntry;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author SPF , chip-nlp
 * @since {10/26/2021}
 */
final public class ListEntryFinder {
   // TODO Replace ctakes ListAnnotator in core.ae with this and the rest of the formatted list stuff.
   // TODO Deprecate the old ListAnnotator stuff.

   static private final Logger LOGGER = Logger.getLogger( "ListEntryFinder" );

   static private final String INDEX_GROUP = "Index";
   static private final String NAME_GROUP = "Name";
   static private final String VALUE_GROUP = "Value";
   static private final String DETAILS_GROUP = "Details";

   static public List<FormattedListEntry> createListEntries( final JCas jCas,
                                                              final int listOffset,
                                                              final String listText,
                                                              final Pattern entryPattern ) {
      final List<FormattedListEntry> entries = new ArrayList<>();
      final Matcher matcher = entryPattern.matcher( listText );
      while ( matcher.find() ) {
         final FormattedListEntry entry = new FormattedListEntry( jCas,
                                                                  listOffset + matcher.start(),
                                                                  listOffset + matcher.end() );
         entry.setIndex( NormalizeUtil.createNormal( jCas, listOffset, matcher, INDEX_GROUP ) );
         entry.setName( NormalizeUtil.createNormal( jCas, listOffset, matcher, NAME_GROUP ) );
         entry.setValue( NormalizeUtil.createNormal( jCas, listOffset, matcher, VALUE_GROUP ) );
         entry.setDetails( NormalizeUtil.createNormal( jCas, listOffset, matcher, DETAILS_GROUP ) );
         entry.addToIndexes();
         entries.add( entry );
      }
      return entries;
   }

//   static private NormalizableAnnotation createNormalizable( final JCas jCas,
//                                                             final int spanOffset,
//                                                             final Matcher matcher,
//                                                             final String groupName ) {
//      final Pair<Integer> span = RegexUtil.getGroupSpan( matcher, groupName );
//      if ( !RegexUtil.isValidSpan( span ) ) {
//         return null;
//      }
////      LOGGER.info( "Creating Normalizable " + groupName + " " + (spanOffset + span.getValue1()) + "-" + (spanOffset + span.getValue2()) );
//      final NormalizableAnnotation name = new NormalizableAnnotation( jCas,
//                                                                        spanOffset + span.getValue1(),
//                                                                        spanOffset + span.getValue2() );
//      name.addToIndexes();
//      return name;
//   }
//
//
//   //  While I would really like to (re)use existing and covered ctakes Types,
//   //  from experience, previous annotators are not as accurate in annotating full text as
//   //  this can be when analyzing a short snippet of text in the FormattedListEntry value.
//   //  In addition, there are not enough Types in the system to cover everything that we want to
//   //  normalize, they are of different parent types, and would themselves still require normalization.
//
//
//   static private String getNormalizedText( final NormalizableAnnotation annotation ) {
//      final String text = annotation.getCoveredText();
//      if ( annotation.getCoveredText() == null ) {
//         return "";
//      }
//      return getNormalizedText( text.trim().toLowerCase() );
//   }
//
//   static private Number getNormalizedNumber( final NormalizableAnnotation annotation ) {
//      final String text = annotation.getCoveredText();
//      if ( annotation.getCoveredText() == null ) {
//         return Integer.MIN_VALUE;
//      }
//      return getNormalizedNumber( text.trim().toLowerCase() );
//   }
//
//   static private String getNormalizedText( final NormalizableAnnotation annotation ) {
//      final String text = annotation.getCoveredText();
//      if ( annotation.getCoveredText() == null ) {
//         return "";
//      }
//      return getNormalizedText( text.trim().toLowerCase() );
//   }
//
//   static private String getNormalizedText( final String text ) {
//      String value = parseInt( text );
//      if ( !value.isEmpty() ) {
//         return value;
//      }
//      value = parseFloat( text );
//      if ( !value.isEmpty() ) {
//         return value;
//      }
//      return null;
//   }
//
//   static private Number getNormalizedNumber( final String text ) {
//      String value = parseInt( text );
//      if ( !value.isEmpty() ) {
//         return value;
//      }
//      value = parseFloat( text );
//      if ( !value.isEmpty() ) {
//         return value;
//      }
//      return "";
//   }
//
//   static private String parseInt( final String value ) {
//      try {
//         final int number = Integer.parseInt( value );
//         return value;
//      } catch ( NumberFormatException nfE ) {
//         return "";
//      }
//   }
//
//   static private String parseFloat( final String value ) {
//      try {
//         final float number = Float.parseFloat( value );
//         return value;
//      } catch ( NumberFormatException nfE ) {
//         return "";
//      }
//   }
//
//
//   private enum PRESENCE {
//      PRESENT( "present", "positive", "affirmed", "affirmative", "true", "yes" ),
//      ABSENT( "absent", "not present", "negated", "negative", "false", "no", "not" ),
//      NOT_IDENTIFIED("unclassifiable", "unclassified",
//                     "not classified", "cannot be classified",
//                     "not assessable", "not assessed", "cannot be assessed",
//                     "not identified", "cannot be identified",
//                     "not definitely identified",
//                     "not determined", "cannot be determined", "undetermined",
//                     "indeterminate",
//                     "not given" );
//      final String[] _texts;
//      PRESENCE( final String... texts ) {
//         _texts = texts;
//      }
//   }
//
//   private enum LEVEL {
//      LOW( "low", "weak", "weakly" ),
//      MEDIUM( "medium", "intermediate", "moderate", "moderately", "average", "not amplified" ),
//      HIGH( "high", "strong", "strongly", "amplified" );
//      final String[] _texts;
//      LEVEL( final String... texts ) {
//         _texts = texts;
//      }
//   }
//
////   static private final String[] NOT_IDENTIFIED = { "unclassifiable", "unclassified",
////                                                    "not classified", "cannot be classified",
////                                                    "not assessable", "not assessed", "cannot be assessed",
////                                                    "not identified", "cannot be identified",
////                                                    "not definitely identified",
////                                                    "not determined", "cannot be determined", "undetermined",
////                                                    "indeterminate",
////                                                    "not given" };
////
////   static private final String[] ABSENT = { "absent", "not present", "negated", "negative", "false", "no", "not" };
////   static private final String[] PRESENT = { "present", "positive", "affirmed", "affirmative", "true", "yes" };
//
////   static private final String[] LOW = { "low", "weak", "weakly" };
////   static private final String[] MEDIUM = { "medium", "intermediate", "moderate", "moderately",
////                                            "average", "not amplified" };
////   static private final String[] HIGH = { "high", "strong", "strongly", "amplified" };
//
////   static private String getPresence( final String valueText ) {
////      final String lowerText = valueText.toLowerCase();
////      for ( String low : LOW ) {
////         final int index = getIndex( lowerText, low );
////         if ( index >= 0 ) {
////            return markText( valueText, "Green", index, low.length() );
////         }
////      }
////      for ( String medium : MEDIUM ) {
////         final int index = getIndex( lowerText, medium );
////         if ( index >= 0 ) {
////            return markText( valueText, "Yellow", index, medium.length() );
////         }
////      }
////      for ( String high : HIGH ) {
////         final int index = getIndex( lowerText, high );
////         if ( index >= 0 ) {
////            return markText( valueText, "Orange", index, high.length() );
////         }
////      }
////      for ( String not : NOT_IDENTIFIED ) {
////         final int index = getIndex( lowerText, not );
////         if ( index >= 0 ) {
////            return markText( valueText, "DodgerBlue", index, not.length() );
////         }
////      }
////      for ( String absent : ABSENT ) {
////         final int index = getIndex( lowerText, absent );
////         if ( index >= 0 ) {
////            return markText( valueText, "Tomato", index, absent.length() );
////         }
////      }
////      for ( String present : PRESENT ) {
////         final int index = getIndex( lowerText, present );
////         if ( index >= 0 ) {
////            return markText( valueText, "MediumSeaGreen", index, present.length() );
////         }
////      }
////
////      return valueText;
////   }
//
//   // is lf low flow?  low frequency?
////   static private final String[] UNIT_ARRAY = { "gr", "gm",
////                                                "mgs", "mg", "milligrams", "milligram", "kg",
////                                                "micrograms", "microgram",
////                                                "grams", "gram", "mcg", "ug",
////                                                "millicurie", "mic", "oz",
////                                                "lf", "ml", "milliliter", "liter",
////                                                "milliequivalent", "meq",
////                                                "usp", "titradose",
////                                                "units", "unit", "unt", "iu", "mmu",
////                                                "mm2", "cm2", "mm", "cm", "cc",
////                                                "gauge", "intl", "bau", "mci", "ud", "au",
////                                                "ww", "vv", "wv",
////                                                "percent", "%ww", "%vv", "%wv", "%",
////                                                "actuation", "actuat", "vial", "vil", "packet", "pkt",
////                                                "l", "g", "u", };
//
//   private enum UNITS {
//      PERCENT( "percent", "%" ),
//      MILLIGRAM( "milligram", "mg", "mgs", "milligrams" ),
//      MICROGRAM( "microgram", "mcg", "ug", "micrograms" ),
//      KILOOGRAM( "kilogram", "kg", "kilograms" ),
//      GRAM( "gram", "g", "gm", "gr", "grams" ),
//      OUNCE( "ounce", "oz", "ounces" ),
//      MILLICURIE( "millicurie", "mic", "mci", "millicuries" ),
//      MILLILITER( "milliliter", "ml", "millilitre", "milliliters", "millilitres" ),
//      MICROLITER( "microliter", "ul", "microlitre", "microliters", "microlitres" ),
//      LITER( "liter", "l", "litre", "liters", "litres" ),
//      SQUARE_MILLIMETER( "square millimeter", "mm2", "square millimeters" ),
//      MILLIMETER( "millimeter", "mm", "millimeters" ),
//      SQUARE_CENTIMETER( "square centimeter", "cm2", "square centimeters" ),
//      CUBIC_CENTIMETER( "cubic centimeter", "cc", "c3", "cubic centimeters" ),
//      CENTIMETER( "centimeter", "cm", "centimeters" ),
//      MILLIEQUIVALENT( "milliequivalent", "meq" ),
//      MEDICAL_MAINTENANCE_UNIT( "mmu" ),
//      BLOOD_ACTIVATOR_UNIT( "bau" ),
//      UNIT( "unit", "unt", "ud", "au", "units" ), // unit dose (ud), arbitrary unit (au)
//      TITRADOSE( "titradose" ),
//      VIAL( "vial", "vil", "vials" ),
//      PACKET( "packet", "pkt", "packets" );
//
//      final String[] _texts;
//      UNITS( final String... texts ) {
//         _texts = texts;
//      }
//
//      public Pair<Integer> getUnitSpan( final String fullText ) {
//         return getUnitSpan( fullText, 0 );
//      }
//
//      public Pair<Integer> getUnitSpan( final String fullText, final int startIndex ) {
//         for ( String text : _texts ) {
//            int index = fullText.indexOf( text, startIndex );
//            if ( index < 0 ) {
//               continue;
//            }
//            if ( index == 0 ) {
//               if ( fullText.length() == text.length() ) {
//                  return new Pair<>( 0, text.length() );
//               } else if ( !Character.isLetter( fullText.charAt( text.length() + 1 ) ) ) {
//                  return new Pair<>( 0, text.length() );
//               }
//            } else if ( !Character.isLetter( fullText.charAt( index - 1 ) ) ) {
//               if ( fullText.length() == index + text.length() ) {
//                  return new Pair<>( index, index + text.length() );
//               } else if ( !Character.isLetter( fullText.charAt( index + text.length() ) ) ) {
//                  return new Pair<>( index, index + text.length() );
//               }
//            }
//         }
//         return NOT_FOUND;
//      }
//
//      public Pair<Integer> getSquareSpan( final String fullText, final Pair<Integer> unitSpan ) {
//         return getAnySpan( fullText, unitSpan.getValue1()-1, "sq", "sq.", "square" );
//      }
//
//      public Pair<Integer> getCubicSpan( final String fullText, final Pair<Integer> unitSpan ) {
//         return getAnySpan( fullText, unitSpan.getValue1()-1, "cu", "cu.", "cubic" );
//      }
//
//      public Pair<Integer> getPerSpan( final String fullText, final Pair<Integer> unitSpan ) {
//         return getAnySpan( fullText, unitSpan.getValue1()-1, "/", "per" );
//      }
//
//      public String replaceUnits( final String fullText ) {
//         Pair<Integer> unitSpan = getUnitSpan( fullText );
//         if ( unitSpan.getValue1() < 0 ) {
//            return fullText;
//         }
//         final StringBuilder sb = new StringBuilder();
//         int previousEnd = 0;
//         while ( unitSpan.getValue1() >= 0 ) {
//            Pair<Integer> replacementSpan = unitSpan;
//            String replacement = name();
//            final Pair<Integer> squareSpan = getSquareSpan( fullText, unitSpan );
//            if ( squareSpan.getValue1() >= 0 ) {
//               replacementSpan = new Pair<>( squareSpan.getValue1(), replacementSpan.getValue2() );
//               replacement = "SQUARE " + replacement;
//            } else {
//               final Pair<Integer> cubicSpan = getCubicSpan( fullText, unitSpan );
//               if ( cubicSpan.getValue1() >= 0 ) {
//                  replacementSpan = new Pair<>( cubicSpan.getValue1(), replacementSpan.getValue2() );
//                  replacement = "CUBIC " + replacement;
//               }
//            }
//            final Pair<Integer> perSpan = getPerSpan( fullText, replacementSpan );
//            if ( perSpan.getValue1() >= 0 ) {
//               replacementSpan = new Pair<>( perSpan.getValue1(), replacementSpan.getValue2() );
//               replacement = "PER " + replacement;
//            }
//            sb.append( fullText, previousEnd, replacementSpan.getValue1() )
//              .append( " " )
//              .append( replacement )
//              .append( " " );
//            previousEnd = replacementSpan.getValue2();
//            unitSpan = getUnitSpan( fullText, previousEnd );
//         }
//         sb.append( fullText, previousEnd, fullText.length() );
//         return sb.toString();
//      }
//
//      static public String replaceAllUnits( final String fullText ) {
//         String text = fullText.trim().toLowerCase();
//         for ( UNITS units : UNITS.values() ) {
//            text = units.replaceUnits( text );
//         }
//         return text.trim();
//      }
//   }
//
//   static private final Pair<Integer> NOT_FOUND = new Pair<>( -1, -1 );
//
//   static private Pair<Integer> getAnySpan( final String fullText, final int subjectOffset, final String... prefixes ) {
//      if ( subjectOffset < 0 ) {
//         return NOT_FOUND;
//      }
//      for ( String prefix : prefixes ) {
//         final Pair<Integer> span = getSpan( fullText, subjectOffset, prefix );
//         if ( span.getValue1() >= 0 ) {
//            return span;
//         }
//      }
//      return NOT_FOUND;
//   }
//
//   static private Pair<Integer> getSpan( final String fullText, final int subjectOffset, final String prefix ) {
//      if ( subjectOffset < prefix.length() ) {
//         return NOT_FOUND;
//      }
//      if ( subjectOffset == prefix.length()+1
//           && fullText.startsWith( prefix + " " ) ) {
//         return new Pair<>( 0, subjectOffset );
//      } else if ( subjectOffset > prefix.length()+1
//                  && fullText.startsWith( " " + prefix + " ", subjectOffset-prefix.length()-2 ) ) {
//         return new Pair<>( subjectOffset-prefix.length()-2, subjectOffset );
//      }
//      return NOT_FOUND;
//   }
//
//   static private Pair<Integer> getAnyNumberableSpan( final String fullText, final int subjectOffset,
//                                             final String... prefixes ) {
//      for ( String prefix : prefixes ) {
//         final Pair<Integer> span = getNumberableSpan( fullText, subjectOffset, prefix );
//         if ( span.getValue1() >= 0 ) {
//            return span;
//         }
//      }
//      return NOT_FOUND;
//   }
//
//   static private Pair<Integer> getNumberableSpan( final String fullText, final int subjectOffset,
//                                                  final String prefix ) {
//      if ( subjectOffset < prefix.length() ) {
//         return NOT_FOUND;
//      }
//      if ( subjectOffset == prefix.length()+1
//           && fullText.startsWith( prefix + " " ) ) {
//         return new Pair<>( 0, subjectOffset );
//      } else if ( subjectOffset > prefix.length()+1
//                  && !Character.isLetter( fullText.charAt( subjectOffset-prefix.length()-2 ) )
//                  && fullText.charAt( subjectOffset-1 ) == ' ' ) {
//         return new Pair<>( subjectOffset-prefix.length()-2, subjectOffset );
//      }
//      return NOT_FOUND;
//   }
//
//   static private final String[] GAUGE = { "gauge" };
//   static private final String[] INTERNATIONAL = { "international", "intl", "iu" };
//   static private final String[] UNITED_STATES_PHARMACOPEIA = { "usp" };
//   static private final String[] WET_WEIGHT = { "ww", "%ww" };
//   static private final String[] VOIDED_VOLUME = { "vv", "%vv" };
//   static private final String[] WEIGHT_IN_VOLUME = { "wv", "%wv" };
//
//
////   static private final String[] SQUARE = { "square", "sq", "sq." };
////   static private final String[] CUBIC = { "cubic" };
////   static private final String[] PER = { "per", "/" };
//
//
//   static private final Pattern NUMBER_PATTERN = Pattern.compile( "[0-9]+(?:.[0-9]+)?" );
//   // This will be huge, but oh well.
////   static private final Collection<Pattern> LONE_UNIT_PATTERNS = new ArrayList<>();
////   static private final Collection<Pattern> PER_UNIT_PATTERNS = new ArrayList<>();
//
//
////   static {
////      for ( String unit : UNIT_ARRAY ) {
////         LONE_UNIT_PATTERNS.add(
////               Pattern.compile( "(?:\\/|(?:per\\s))?(?:sq.?\\s)?\\s*"
////                                + unit ) );
//////                                + "\\s+" ) );
//////               Pattern.compile( "\\/?\\s*"
//////                                + unit
//////                                + "\\s+" ) );
////         for ( String nom : UNIT_ARRAY ) {
////            PER_UNIT_PATTERNS.add( Pattern.compile( nom
////                                                    + "(?:\\/|(?:per\\s))(?:sq.?\\s)?\\s*"
////                                                    + unit ) );
//////                                                    + "\\s+" ) );
////         }
////      }
////   }
//
//
//
//
//
//
////   static private String markUnits( final String valueText ) {
////      return UNITS.replaceAllUnits( valueText );
////      for ( Pattern perUnit : PER_UNIT_PATTERNS ) {
////         final Matcher matcher = perUnit.matcher( valueText );
////         while ( matcher.find() ) {
////            if ( matcher.start() == 0 || !Character.isLetter( valueText.charAt( matcher.start() - 1 ) ) ) {
////               return valueText.substring( 0, matcher.start() )
////                      + "<I>" + valueText.substring( matcher.start(), matcher.end() ) + "</I>"
////                      + valueText.substring( matcher.end() );
////            }
////         }
////      }
////      for ( Pattern perUnit : LONE_UNIT_PATTERNS ) {
////         final Matcher matcher = perUnit.matcher( valueText );
////         while ( matcher.find() ) {
////            if ( ( matcher.start() == 0
////                   || !Character.isLetter( valueText.charAt( matcher.start() - 1 ) ) )
////                 && ( matcher.end() == valueText.length()
////                      || !Character.isLetter( valueText.charAt( matcher.end() ) ) ) ) {
////               return valueText.substring( 0, matcher.start() )
////                      + "<I>" + valueText.substring( matcher.start(), matcher.end() ) + "</I>"
////                      + valueText.substring( matcher.end() );
////            }
////         }
////      }
////      return valueText;
////   }
////

}


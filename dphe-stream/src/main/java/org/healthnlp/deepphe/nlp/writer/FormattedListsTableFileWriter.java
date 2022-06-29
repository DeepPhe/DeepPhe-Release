package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textspan.FormattedList;
import org.apache.ctakes.typesystem.type.textspan.FormattedListEntry;
import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {4/26/2021}
 */
@PipeBitInfo(
      name = "Formatted List Table Writer",
      description = "Writes the discovered Lists to files in a Table format.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = PipeBitInfo.TypeProduct.LIST
)
public class FormattedListsTableFileWriter extends AbstractFileWriter<List<List<String>>> {
//public class ListsTableFileWriter extends AbstractTableFileWriter {
// TODO 10/12/2021   For some reason this keeps claiming that AbstractTableFileWriter does not exist in ctakes trunk.
//  Why?


   // TODO  Everything below is a copy of AbstractTableFileWriter in ctakes core.cc

   static private final Logger LOGGER = Logger.getLogger( "FormattedListsTableFileWriter" );


   protected enum TableType {
      BSV,
      CSV,
      HTML,
      TAB
   }

   /**
    * Name of configuration parameter that must be set to the path of a directory into which the
    * output files will be written.
    */
   @ConfigurationParameter(
         name = "TableType",
         description = "Type of Table to write to File. Possible values are: BSV, CSV, HTML, TAB",
         mandatory = false
   )
   private String _tableType;


   private final List<String> _headerRow = new ArrayList<>();
   private final List<List<String>> _dataRows = new ArrayList<>();
   private final List<String> _footerRow = new ArrayList<>();

   /**
    * @param jCas ye olde ...
    * @return A list of ordered rows, each being a list of ordered cells.
    */
//   abstract protected List<List<String>> createDataRows( JCas jCas );
   protected List<List<String>> createDataRows( final JCas jCas ) {
      final Collection<FormattedList> lists =
            JCasUtil.select( jCas, FormattedList.class ).stream()
                    .sorted( Comparator.comparingInt( FormattedList::getBegin ) )
                    .collect( Collectors.toList() );
      final List<ListEntryRow> rows = new ArrayList<>();
      for ( FormattedList list : lists ) {
         rows.addAll( createListEntries( list ) );
      }
      return rows.stream().map( ListEntryRow::getColumns ).collect( Collectors.toList() );
   }

   static private List<ListEntryRow> createListEntries( final FormattedList list ) {
      FSArray entries = list.getListEntries();
      final int size = entries.size();
      final List<ListEntryRow> rows = new ArrayList<>( size );
      for ( int i=0; i<size; i++ ) {
         rows.add( new ListEntryRow( getAnnotationText( list.getHeading() ),
                                                        (FormattedListEntry)entries.get( i ) ) );
      }
      return rows;
   }


   /**
    * A Table Header indicates the type of content in each table cell.  Though not necessary, it is nice to have.
    *
    * @param jCas ye olde ...
    * @return an empty List.  This is the default.  Please override if necessary.
    */
   protected List<String> createHeaderRow( final JCas jCas ) {
//      return Collections.emptyList();
      return Arrays.asList(
            " Header ",
            " Index ",
            " Entry Name ",
            " Entry Value ",
            " Details ");
   }

   /**
    * @return the header list of ordered cells.
    */
   final protected List<String> getHeaderRow() {
      return _headerRow;
   }

   /**
    * A Table Footer is usually some type of "Summary" line.  For instance a column of numbers may have a "Total".
    *
    * @param jCas ye olde ...
    * @return an empty List.  This is the default.  Please override if necessary.
    */
   protected List<String> createFooterRow( final JCas jCas ) {
      return Collections.emptyList();
   }

   /**
    * @return the footer list of ordered cells.
    */
   final protected List<String> getFooterRow() {
      return _footerRow;
   }


   /**
    * @param jCas the jcas passed to the process( jcas ) method.
    */
   @Override
   protected void createData( final JCas jCas ) {
      _headerRow.clear();
      _dataRows.clear();
      _footerRow.clear();
      final List<String> header = createHeaderRow( jCas );
      if ( header != null ) {
         _headerRow.addAll( header );
      }
      final List<List<String>> rows = createDataRows( jCas );
      if ( rows != null ) {
         _dataRows.addAll( rows );
      }
      final List<String> footer = createFooterRow( jCas );
      if ( footer != null ) {
         _footerRow.addAll( footer );
      }
   }

   /**
    * @return completed patient JCases
    */
   @Override
   protected List<List<String>> getData() {
      return _dataRows;
   }

   /**
    * called after writing is complete
    *
    * @param data -
    */
   @Override
   protected void writeComplete( final List<List<String>> data ) {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final List<List<String>> dataRows,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final TableType tableType = Arrays.stream( TableType.values() )
                                        .filter( s -> s.name()
                                                       .equalsIgnoreCase( _tableType ) )
                                        .findFirst()
                                        .orElse( TableType.BSV );
      final File file = new File( outputDir, documentId + "_table." + tableType.name() );
      LOGGER.info( "Writing " + tableType.name() + " Table to " + file.getPath() + " ..." );
      final String header = createTableHeader( tableType, getHeaderRow() );

      final String footer = createTableFooter( tableType, getFooterRow() );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( header );

         for ( List<String> dataRow : dataRows ) {
            final String row = createTableRow( tableType, dataRow );
            writer.write( row );
         }

         writer.write( footer );
      }
   }


   ////////////////////////////////////////////////////////////////////////
   //
   //    The following default implementations should be fine as-is.
   //
   ////////////////////////////////////////////////////////////////////////


   protected String createHtmlStyle() {
      return "table {\n"
             + "  margin-left: auto;\n"
             + "  margin-right: auto;\n"
             + "}\n";
   }


   protected String createTableHeader( final TableType tableType, final List<String> headerRow ) {
      switch ( tableType ) {
         case BSV:
            return String.join( "|", headerRow ) + "\n";
         case CSV:
            return String.join( ",", headerRow ) + "\n";
         case TAB:
            return String.join( "\t", headerRow ) + "\n";
         case HTML:
            return createHtmlHeader( headerRow );
      }
      return String.join( "|", headerRow ) + "\n";
   }

   protected String createHtmlHeader( final List<String> headerRow ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "<!DOCTYPE html>\n"
                 + "<html>\n"
                 + "<head>\n"
                 + "<style>\n" );
      sb.append( createHtmlStyle() );
      sb.append( "</style>\n"
                 + "</head>\n"
                 + "<body>\n"
                 + "\n"
                 + "<table>\n"
                 + " <thead>\n"
                 + "  <tr>\n" );
      for ( String cell : headerRow ) {
         sb.append( "    <th>" )
           .append( cell )
           .append( "</th>\n" );
      }
      sb.append( "  </tr>\n"
                 + " </thead>\n" );
      return sb.toString();
   }


   protected String createTableRow( final TableType tableType, final List<String> dataRow ) {
      switch ( tableType ) {
         case BSV:
            return String.join( "|", dataRow ) + "\n";
         case CSV:
            return String.join( ",", dataRow ) + "\n";
         case TAB:
            return String.join( "\t", dataRow ) + "\n";
         case HTML:
            return createHtmlRow( dataRow );
      }
      return String.join( "|", dataRow ) + "\n";
   }

   protected String createHtmlRow( final List<String> dataRow ) {
      final StringBuilder sb = new StringBuilder();
      for ( String cell : dataRow ) {
         sb.append( "    <td>" )
           .append( cell )
           .append( "</td>\n" );
      }
      return "  <tr>\n" + sb.toString() + "  </tr>\n";
   }


   protected String createTableFooter( final TableType tableType, final List<String> footerRow ) {
      switch ( tableType ) {
         case BSV:
            return String.join( "|", footerRow ) + "\n";
         case CSV:
            return String.join( ",", footerRow ) + "\n";
         case TAB:
            return String.join( "\t", footerRow ) + "\n";
         case HTML:
            return createHtmlFooter( footerRow );
      }
      return String.join( "|", footerRow ) + "\n";
   }

   protected String createHtmlFooter( final List<String> footerRow ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( " <tfoot>\n"
                 + "  <tf>\n" );
      for ( String cell : footerRow ) {
         sb.append( "    <td>" )
           .append( cell )
           .append( "</td>\n" );
      }
      sb.append( "  </tf>\n"
                 + " <tfoot>\n"
                 + "</table>\n"
                 + "</body>\n"
                 + "</html>\n" );
      return sb.toString();
   }


   /**
    * Simple container for annotation information.
    */
   static private class ListEntryRow {
      private final String _heading;
      private final String _index;
      private final String _name;
      private final String _value;
      private final String _details;

      private ListEntryRow( final String heading, final FormattedListEntry listEntry ) {
         _heading = heading;
//         _entryText = listEntry.getCoveredText();
         _index = getAnnotationText( listEntry.getIndex() );
         _name = getAnnotationText( listEntry.getName() );
         _details = getAnnotationText( listEntry.getDetails() );
         String value = getAnnotationText( listEntry.getValue() );
         if ( !value.isEmpty() ) {
            value = markPresence( value );
            value = markUnits( value );
            value = markNumbers( value );
         }
         _value = value;
      }

      public List<String> getColumns() {
         return Arrays.asList( _heading, _index, _name, _value, _details );
      }

   }


   static private final String getAnnotationText( final Annotation possibleNull ) {
      if ( possibleNull == null ) {
         return "";
      }
      return possibleNull.getCoveredText().trim();
   }

   //////////////////////////////////////////////////////////////////////////////////////////
   //
   //                          Numbers and Units
   //
   //////////////////////////////////////////////////////////////////////////////////////////

   //////////////////////////////////////////////////////////////////////////////
   //
   //                   Old code that I started to copy over.
   //
   //////////////////////////////////////////////////////////////////////////////


   static private final String[] UNIT_ARRAY = { "gr", "gm",
                                                "mgs", "mg", "milligrams", "milligram", "kg",
                                                "micrograms", "microgram",
                                                "grams", "gram", "mcg", "ug",
                                                "millicurie", "mic", "oz",
                                                "lf", "ml", "milliliter", "liter",
                                                "milliequivalent", "meq",
                                                "usp", "titradose",
                                                "units", "unit", "unt", "iu", "mmu",
                                                "mm2", "cm2", "mm", "cm", "cc",
                                                "gauge", "intl", "bau", "mci", "ud", "au",
                                                "ww", "vv", "wv",
                                                "percent", "%ww", "%vv", "%wv", "%",
                                                "actuation", "actuat", "vial", "vil", "packet", "pkt",
                                                "l", "g", "u", };

   static private final Pattern NUMBER_PATTERN = Pattern.compile( "[0-9]+(?:.[0-9]+)?" );
   // This will be huge, but oh well.
   static private final Collection<Pattern> LONE_UNIT_PATTERNS = new ArrayList<>();
   static private final Collection<Pattern> PER_UNIT_PATTERNS = new ArrayList<>();

   static {
      for ( String unit : UNIT_ARRAY ) {
         LONE_UNIT_PATTERNS.add(
               Pattern.compile( "(?:\\/|(?:per\\s))?(?:sq.?\\s)?\\s*"
                                + unit ) );
//                                + "\\s+" ) );
//               Pattern.compile( "\\/?\\s*"
//                                + unit
//                                + "\\s+" ) );
         for ( String nom : UNIT_ARRAY ) {
            PER_UNIT_PATTERNS.add( Pattern.compile( nom
                                                    + "(?:\\/|(?:per\\s))(?:sq.?\\s)?\\s*"
                                                    + unit ) );
//                                                    + "\\s+" ) );
         }
      }
   }

   static private String markNumbers( final String valueText ) {
      final StringBuilder sb = new StringBuilder();
      int previousEnd = 0;
      final Matcher matcher = NUMBER_PATTERN.matcher( valueText );
      while ( matcher.find() ) {
         if ( matcher.start() == 0 || !Character.isLetter( valueText.charAt( matcher.start() - 1 ) ) ) {
            sb.append( valueText, previousEnd, matcher.start() )
              .append( "<B>" )
              .append( valueText, matcher.start(), matcher.end() )
              .append( "</B>" );
            previousEnd = matcher.end();
         }
      }
      sb.append( valueText.substring( previousEnd ) );
      return sb.toString();
   }

   static private String markUnits( final String valueText ) {
      for ( Pattern perUnit : PER_UNIT_PATTERNS ) {
         final Matcher matcher = perUnit.matcher( valueText );
         while ( matcher.find() ) {
            if ( matcher.start() == 0 || !Character.isLetter( valueText.charAt( matcher.start() - 1 ) ) ) {
               return valueText.substring( 0, matcher.start() )
                      + "<I>" + valueText.substring( matcher.start(), matcher.end() ) + "</I>"
                      + valueText.substring( matcher.end() );
            }
         }
      }
      for ( Pattern perUnit : LONE_UNIT_PATTERNS ) {
         final Matcher matcher = perUnit.matcher( valueText );
         while ( matcher.find() ) {
            if ( ( matcher.start() == 0
                   || !Character.isLetter( valueText.charAt( matcher.start() - 1 ) ) )
                 && ( matcher.end() == valueText.length()
                      || !Character.isLetter( valueText.charAt( matcher.end() ) ) ) ) {
               return valueText.substring( 0, matcher.start() )
                      + "<I>" + valueText.substring( matcher.start(), matcher.end() ) + "</I>"
                      + valueText.substring( matcher.end() );
            }
         }
      }
      return valueText;
   }


   static private final String[] NOT_IDENTIFIED = { "unclassifiable", "unclassified",
                                                    "not classified", "cannot be classified",
                                                    "not assessable", "not assessed", "cannot be assessed",
                                                    "not identified", "cannot be identified",
                                                    "not definitely identified",
                                                    "not determined", "cannot be determined", "undetermined",
                                                    "indeterminate",
                                                    "not given" };

   static private final String[] ABSENT = { "absent", "not present", "negated", "negative", "false", "no", "not" };
   static private final String[] PRESENT = { "present", "positive", "affirmed", "affirmative", "true", "yes" };

   static private final String[] LOW = { "low", "weak", "weakly" };
   static private final String[] MEDIUM = { "medium", "intermediate", "moderate", "moderately",
                                            "average", "not amplified" };
   static private final String[] HIGH = { "high", "strong", "strongly", "amplified" };

   static private int getIndex( final String lowerText, final String searchText ) {
      final int index = lowerText.indexOf( searchText );
      if ( index >= 0
               && ( lowerText.length() == searchText.length()
                 || ( index == 0
                      && Character.isWhitespace( lowerText.charAt( searchText.length() ) ) )
                 || ( index == lowerText.length() - searchText.length()
                      && Character.isWhitespace( lowerText.charAt( index - 1 ) ) ) ) ) {
         return index;
      }
      return -1;
   }

   static private String markText( final String valueText, final String color,
                                   final int index, final int length ) {
      return valueText.substring( 0, index ) + "<span style=\"color:" + color +";\">"
             + valueText.substring( index, index + length ) + "</span>"
             + valueText.substring( index + length );
   }

   static private String markPresence( final String valueText ) {
      final String lowerText = valueText.toLowerCase();
      for ( String low : LOW ) {
         final int index = getIndex( lowerText, low );
         if ( index >= 0 ) {
            return markText( valueText, "Green", index, low.length() );
         }
      }
      for ( String medium : MEDIUM ) {
         final int index = getIndex( lowerText, medium );
         if ( index >= 0 ) {
            return markText( valueText, "Yellow", index, medium.length() );
         }
      }
      for ( String high : HIGH ) {
         final int index = getIndex( lowerText, high );
         if ( index >= 0 ) {
            return markText( valueText, "Orange", index, high.length() );
         }
      }
      for ( String not : NOT_IDENTIFIED ) {
         final int index = getIndex( lowerText, not );
         if ( index >= 0 ) {
            return markText( valueText, "DodgerBlue", index, not.length() );
         }
      }
      for ( String absent : ABSENT ) {
         final int index = getIndex( lowerText, absent );
         if ( index >= 0 ) {
            return markText( valueText, "Tomato", index, absent.length() );
         }
      }
      for ( String present : PRESENT ) {
         final int index = getIndex( lowerText, present );
         if ( index >= 0 ) {
            return markText( valueText, "MediumSeaGreen", index, present.length() );
         }
      }

      return valueText;
   }

}
package org.apache.ctakes.ner.detail.jdbc;

import java.util.Arrays;

/**
 * @author SPF , chip-nlp
 * @since {2/1/2022}
 */
public enum DpheDetailColumn {
   PREF_TEXT( "VARCHAR(256)", 256 ),
   GROUPING( "VARCHAR(64)", 64 ),
   CUI( "VARCHAR(128)", 128 ),
   TUI( "SMALLINT", 0 ),
   URI( "VARCHAR(256)", 256 ),
   CODES( "VARCHAR(2048)", 2048 );

   // In the future codes could go in the synonym table so that we know which unique snomed etc. go with synonyms.

   private final String _type;
   private final int _width;

   DpheDetailColumn( final String type, final int width ) {
      _type = type;
      _width = width;
   }

   String getType() {
      return _type;
   }

   public int getWidth() {
      return  _width;
   }

   int getColumnIndex() {
      return ordinal() + 1;
   }

   static public String[] declareColumns() {
      return Arrays.stream( values() )
                   .map( s -> s.name() + " " + s.getType() )
                   .toArray( String[]::new );
   }

}

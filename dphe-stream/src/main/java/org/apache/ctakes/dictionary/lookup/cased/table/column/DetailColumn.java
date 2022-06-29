package org.apache.ctakes.dictionary.lookup.cased.table.column;

import java.util.Arrays;

/**
 * @author SPF , chip-nlp
 * @since {2/1/2022}
 */
public enum DetailColumn {
   CUI( "VARCHAR(16)", 16 ),
   TUI( "VARCHAR(24)", 24 ),
   PREFERRED_TEXT( "VARCHAR(128)", 128 ),
   URI( "VARCHAR(128)", 128 ),
   RANK( "SMALLINT", 0 );

   private final String _type;
   private final int _width;

   DetailColumn( final String type, final int width ) {
      _type = type;
      _width = width;
   }

   String getType() {
      return _type;
   }

   public int getWidth() {
      return  _width;
   }

   public int getColumnIndex() {
      return ordinal() + 1;
   }

   static public String[] declareColumns() {
      return Arrays.stream( values() )
                   .map( s -> s.name() + " " + s.getType() )
                   .toArray( String[]::new );
   }

}

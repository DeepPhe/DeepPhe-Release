package org.apache.ctakes.dictionary.lookup.cased.table.column;

import java.util.Arrays;

/**
 * @author SPF , chip-nlp
 * @since {3/2/2022}
 */
public enum SynonymColumn {
   CUI( "VARCHAR(16)", 16 ),
   TOKENS( "VARCHAR(48) ARRAY", 96 ),
   LOOKUP_TOKEN( "VARCHAR(48)", 48 ),
   LOOKUP_INDEX( "SMALLINT", 0 ),

   TOP_RANK( "SMALLINT", 0 ),
   ENTRY_COUNTS( "SMALLINT", 0 ),
   SECOND_RANK( "SMALLINT", 0 ),
   SECOND_COUNTS( "SMALLINT", 0 ),
   VARIANT_TOP_RANK( "SMALLINT", 0 ),
   VARIANT_ENTRY_COUNTS( "SMALLINT", 0 ),
   OTHER_CUIS( "SMALLINT", 0 ),
   VOCAB_CODES( "VARCHAR(64) ARRAY", 256 );


   private final String _type;
   private final int _width;

   SynonymColumn( final String type, final int width ) {
      _type = type;
      _width = width;
   }

   String getType() {
      return _type;
   }

   int getWidth() {
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

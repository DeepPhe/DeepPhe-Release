package org.apache.ctakes.ner.dictionary.jdbc;

import java.util.Arrays;

/**
 * @author SPF , chip-nlp
 * @since {2/1/2022}
 */
public enum DpheSynonymColumn {
   PREF_TEXT( "VARCHAR(256)", 256 ),
   INDEX_TOKEN( "VARCHAR(128)", 128 ),
   TOKEN_OFFSET( "SMALLINT", 0 ),
   SYNONYM( "VARCHAR(1024)", 1024 );

   // To look up from viz tool/neo4j plugin, use:
   // SELECT DISTINCT TOKENS FROM URI_SYNONYMS WHERE LOWER(SYNONYM) LIKE lowercaseSearchText
   // https://alvinalexander.com/sql/sql-select-case-insensitive-query-queries-upper-lower/

   private final String _type;
   private final int _width;

   DpheSynonymColumn( final String type, final int width ) {
      _type = type;
      _width = width;
   }

   String getType() {
      return _type;
   }

   int getWidth() {
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

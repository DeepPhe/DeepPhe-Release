package org.healthnlp.deepphe.nlp.util;

/**
 * @author SPF , chip-nlp
 * @since {9/21/2023}
 */
public enum DebugLogger {
   DEFAULT,
   LOGGER_1,
   LOGGER_2,
   LOGGER_3,
   LOGGER_4,
   LOGGER_5;


   private final StringBuilder _sb = new StringBuilder();

   private boolean _debug = true;

   public void doDebug( final boolean debug ) {
      _debug = debug;
   }

   public void addDebug( final String text ) {
      if ( _debug ) {
         _sb.append( text ).append( "\n" );
      }
   }

   public String getDebug() {
      return _sb.toString();
   }

   public void resetDebug() {
      _sb.setLength( 0 );
      _debug = true;
   }

   // Facilities to use the Default

   static public void add( final String text ) {
      DEFAULT.addDebug( text );
   }

   static public String get() {
      return DEFAULT.getDebug();
   }

   static public void reset() {
      DEFAULT.resetDebug();
   }

}

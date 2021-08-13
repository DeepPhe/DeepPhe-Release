package org.healthnlp.deepphe.nlp.cr.naaccr;


import org.apache.ctakes.core.util.doc.JCasBuilder;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
final public class NaaccrDocument extends AbstractNaaccrItem<NaaccrSection> {

   // yyyyMMddhhmmss to yyyy-MM-dd hh:mm:ss
   static private final DateFormat FROM_DATE_FORMAT = new SimpleDateFormat( "yyyyMMddhhmmss" );
   static private final DateFormat TO_DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );

   private String _docTime;
   private boolean _isFirst = true;

   public void setDocDate( final String docTime ) {
      try {
         final Date date = FROM_DATE_FORMAT.parse( docTime );
         _docTime = TO_DATE_FORMAT.format( date );
      } catch ( ParseException pE ) {
         _docTime = docTime;
      }
   }


   public JCasBuilder addToBuilder( final JCasBuilder builder ) {
//      builder.setEncounterId( getId() );
      builder.setDocId( getId() );
      if ( _docTime != null && !_docTime.isEmpty() ) {
         builder.setDocTime( _docTime );
      }
      return super.addToBuilder( builder );
   }

   public boolean hasNextSection() {
      return _isFirst || hasNext();
   }

   public NaaccrSection nextSection() {
      if ( _isFirst ) {
         _isFirst = false;
         return get();
      }
      return next();
   }


}

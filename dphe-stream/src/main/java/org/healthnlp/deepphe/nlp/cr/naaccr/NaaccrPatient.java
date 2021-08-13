package org.healthnlp.deepphe.nlp.cr.naaccr;


import org.apache.ctakes.core.util.doc.JCasBuilder;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
final public class NaaccrPatient extends AbstractNaaccrItem<NaaccrTumor> {

   public JCasBuilder addToBuilder( final JCasBuilder builder ) {
      builder.setPatientId( getId() );
      builder.setDocIdPrefix( getId() );
      return super.addToBuilder( builder );
   }

   public boolean hasNextSection() {
      if ( get().hasNextSection() ) {
         return true;
      }
      return hasNext();
   }

   public NaaccrSection nextSection() {
      if ( get().hasNextSection() ) {
         return get().nextSection();
      }
      if ( hasNext() ) {
         next();
         return nextSection();
      }
      return NaaccrSection.NULL_SECTION;
   }


}

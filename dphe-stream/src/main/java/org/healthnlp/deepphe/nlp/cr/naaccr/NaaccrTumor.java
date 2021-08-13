package org.healthnlp.deepphe.nlp.cr.naaccr;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/14/2020
 */
final public class NaaccrTumor extends AbstractNaaccrItem<NaaccrDocument> {

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

package org.healthnlp.deepphe.nlp.cr.naaccr;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/15/2020
 */
public interface NaaccrSectionGetter {

   boolean hasNextSection();

   NaaccrSection nextSection();

   int getSectionCount();


}

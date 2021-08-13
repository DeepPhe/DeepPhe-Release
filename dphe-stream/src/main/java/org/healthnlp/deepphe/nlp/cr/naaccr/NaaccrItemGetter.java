package org.healthnlp.deepphe.nlp.cr.naaccr;

import java.util.Iterator;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/15/2020
 */
public interface NaaccrItemGetter<T extends NaaccrItem> extends Iterator<T> {

   T get();

}

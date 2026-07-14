package org.healthnlp.deepphe.nlp.util;

import org.apache.ctakes.core.util.IdCounter;

/**
 * @author SPF , chip-nlp
 * @since {12/29/2023}
 */
final public class IdCreator {

    private IdCreator() {}

    static public String createId( final String rootId,
                                   final String seedTime,
                                   final String type,
                                   final IdCounter idCounter ) {
        return rootId + "_"
              + seedTime  + "_"
              + type.replace( ' ', '_' ) + "_"
              + idCounter.incrementAndGet();
    }

}

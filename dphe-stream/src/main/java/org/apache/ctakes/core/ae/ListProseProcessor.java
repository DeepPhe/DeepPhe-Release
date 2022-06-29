package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.util.prose.ProseProcessor;
import org.apache.ctakes.core.util.treelist.ListProcessor;

/**
 * @author SPF , chip-nlp
 * @since {12/9/2021}
 */
public interface ListProseProcessor {

   ProseProcessor getProseProcessor();

   ListProcessor getListProcessor();

}

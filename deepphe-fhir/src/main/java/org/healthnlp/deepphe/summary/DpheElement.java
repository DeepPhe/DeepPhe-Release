package org.healthnlp.deepphe.summary;

import java.io.Serializable;


/**
 * DeepPhe model interface
 *
 * @author tseytlin
 */
public interface DpheElement extends Serializable {

   String getDisplayText();

   String getResourceIdentifier();

   String getSummaryText();

   String getAnnotationType();

}

package org.healthnlp.deepphe.summary.attribute.biomarker;

import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;

import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {4/9/2021}
 */
final public class BiomarkerCodeInfoStore implements CodeInfoStore {


   public void init( final UriInfoStore uriInfoStore, final Map<String,String> dependencies ) {
   }

   public String getBestCode() {
      return "Get Best Code from BiomarkerInfoStore";
   }


}

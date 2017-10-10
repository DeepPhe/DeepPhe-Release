package org.apache.ctakes.cancer.ae.relation;

import org.apache.ctakes.relationextractor.ae.LocationOfRelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Created by tmill on 2/21/17.
 */
public class DeepPheRelationExtractor extends LocationOfRelationExtractorAnnotator {

   @Override
   protected Class<? extends Annotation> getCoveringClass() {
      return Paragraph.class;
   }
}

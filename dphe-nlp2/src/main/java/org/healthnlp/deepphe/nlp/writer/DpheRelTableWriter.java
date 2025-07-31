package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.cc.AbstractTableFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @since {11/17/2023}
 */
@PipeBitInfo(
      name = "Dphe Relation Table Writer",
      description = "Writes a table of Relation information to file, grouped by Dphe Relation Type.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, IDENTIFIED_ANNOTATION, GENERIC_RELATION },
      usables = { DOCUMENT_ID_PREFIX }
)
public class DpheRelTableWriter extends AbstractTableFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "DpheRelTableWriter" );

   @Override
   public void writeFile( final List<List<String>> dataRows,
                          final String outputDir,
                          final String documentId,
                          final String fileName) throws IOException {
      // kludge to add "_rel" to filename.  filename is not used by abstractTableFileWriter, which should be fixed.
      super.writeFile( dataRows, outputDir, documentId+"_rel", fileName );
   }

      /**
       * {@inheritDoc}
       */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Arrays.asList(
            " Source ",
            " Relation ",
            " Target ",
            " Confidence " );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      return JCasUtil.select( jCas, BinaryTextRelation.class )
                                               .stream()
                                               .sorted( Comparator.comparing( BinaryTextRelation::getCategory )
                                                                  .thenComparing( b -> b.getArg1().getArgument().getCoveredText() ) )
                                               .map( RelationInfo::new )
                                               .map( RelationInfo::getColumns )
                                               .collect( Collectors.toList() );
   }


   /**
    * Simple container for annotation information.
    */
   static private class RelationInfo {
      private final String _category;
      private final String _source;
      private final String _target;
      private final double _confidence;

      private RelationInfo( final BinaryTextRelation relation ) {
         _category = relation.getCategory();
         _source = relation.getArg1().getArgument().getCoveredText();
         _target = relation.getArg2().getArgument().getCoveredText();
         _confidence = relation.getConfidence();
      }

      public List<String> getColumns() {
         return Arrays.asList(
               getSource(),
               getCategory(),
               getTarget(),
               getConfidence() + "" );
      }

      public String getCategory() {
         return _category;
      }

      public String getSource() {
         return _source;
      }

      public String getTarget() {
         return _target;
      }

      public double getConfidence() {
         return _confidence;
      }

   }


}

package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.cc.AbstractTableFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
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
@PipeBitInfo (
      name = "Gene Span Writer",
      description = "Writes a column of Gene information to file.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, IDENTIFIED_ANNOTATION },
      usables = { DOCUMENT_ID_PREFIX }
)
public class SpanTableWriter extends AbstractTableFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "DpheTableWriter" );

   static private final Comparator<Pair<Integer>> TextSpanComparator =
         Comparator.comparingInt( ( Pair<Integer> p ) -> p.getValue1() ).thenComparingInt( Pair::getValue2 );


   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      final Collection<Pair<Integer>> spans = new ArrayList<>();
      for ( IdentifiedAnnotation annotation : JCasUtil.select( jCas, IdentifiedAnnotation.class ) ) {
         spans.add( new Pair<>( annotation.getBegin(), annotation.getEnd() ) );
      }
      return spans.stream()
                  .distinct()
                  .sorted( TextSpanComparator )
                  .map( p -> p.getValue1() + "," + p.getValue2() )
                  .map( Collections::singletonList )
                  .collect( Collectors.toList() );
   }

   // TODO make a protected gatTableType() method in AbstractTableWriter
   @Override
   public void writeFile( final List<List<String>> dataRows,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      super.writeFile( dataRows, outputDir, documentId + "_span", fileName );
   }


}

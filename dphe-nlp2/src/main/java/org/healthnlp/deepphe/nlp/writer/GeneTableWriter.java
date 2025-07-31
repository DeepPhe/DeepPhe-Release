package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.cc.AbstractTableFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @since {11/17/2023}
 */
@PipeBitInfo (
      name = "Gene Table Writer",
      description = "Writes a table of Gene information to file, grouped by Dphe Type.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, IDENTIFIED_ANNOTATION },
      usables = { DOCUMENT_ID_PREFIX }
)
public class GeneTableWriter extends AbstractTableFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "DpheTableWriter" );


   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Arrays.asList(
            " Span ",
            " Dphe Group ",
            " URI ",
            " Confidence ",
            " Annotation Text ",
            " Window " );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      final Collection<AnnotationInfo> infos = new ArrayList<>();

      final Map<IdentifiedAnnotation, Collection<Segment>> annotationSectionMap
            = JCasUtil.indexCovering( jCas, IdentifiedAnnotation.class, Segment.class );
      for ( IdentifiedAnnotation annotation : annotationSectionMap.keySet() ) {
         final String uri = IdentifiedAnnotationUtil.getCodes( annotation, Neo4jConstants.DPHE_CODING_SCHEME )
                                                    .stream().findFirst().orElse( "" );
         DpheGroup group = UriInfoCache.getInstance().getDpheGroup( uri );
         if ( group == DpheGroup.UNKNOWN ) {
            LogFileWriter.add( "DpheTableWriter no group for " + uri );
            group = DpheGroup.MOLECULAR_SEQUENCE_VARIATION;
         }
         final String docText = jCas.getDocumentText();
         final int wBegin = Math.max( 0, annotation.getBegin() - 25 );
         final int wEnd = Math.min( annotation.getEnd() + 25, docText.length() );
         final String window = docText.substring( wBegin, wEnd ).replaceAll( "\\s+", " " );
//         if ( group != DpheGroup.UNKNOWN ) {
         infos.add( new AnnotationInfo( annotation, uri, group, window ) );
//         }
      }
      return infos.stream()
//                  .sorted( Comparator.comparing( AnnotationInfo::getGroup )
                  .sorted( Comparator.comparingInt( AnnotationInfo::getBegin )
                                     .thenComparingInt( AnnotationInfo::getEnd )
                                     .thenComparing( AnnotationInfo::getGroup )
                                     .thenComparing( AnnotationInfo::getConfidence )
                                     .thenComparing( AnnotationInfo::getUri )
                                     .thenComparing( AnnotationInfo::getDocText ) )
                  .map( AnnotationInfo::getColumns )
                  .collect( Collectors.toList() );
   }


   /**
    * Simple container for annotation information.
    */
   static private class AnnotationInfo {

      private final DpheGroup _group;
      private final int _begin;
      private final int _end;
      private final String _uri;
      private final float _confidence;
      private final String _docText;
      private final String _window;

      private AnnotationInfo( final IdentifiedAnnotation annotation,
                              final String uri,
                              final DpheGroup group,
                              final String window ) {
         _group = group;
         _begin = annotation.getBegin();
         _end = annotation.getEnd();
         _uri = uri;
         _confidence = IdentifiedAnnotationUtil.getConfidence( annotation );
         _docText = annotation.getCoveredText();
         _window = window;
      }

      public List<String> getColumns() {
         return Arrays.asList(
               getBegin() + "," + getEnd(),
               getGroup(),
               getUri(),
               getConfidence() + "",
               getDocText(),
               getWindow() );
      }

      public String getGroup() {
         return _group.getName();
      }

      public int getBegin() {
         return _begin;
      }

      public int getEnd() {
         return _end;
      }

      public String getUri() {
         return _uri;
      }

      public float getConfidence() {
         return _confidence;
      }

      public String getDocText() {
         return _docText;
      }

      public String getWindow() {
         return _window;
      }

   }


}

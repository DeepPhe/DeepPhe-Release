package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.cc.AbstractTableFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.ner.group.dphe.DpheGroupAccessor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @since {11/17/2023}
 */
@PipeBitInfo(
      name = "Dphe Table Writer",
      description = "Writes a table of Annotation information to file, grouped by Dphe Type.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, IDENTIFIED_ANNOTATION },
      usables = { DOCUMENT_ID_PREFIX }
)
public class DpheTableWriter extends AbstractTableFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "DpheTableWriter" );


   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Arrays.asList(
            " Dphe Group ",
            " Section ",
            " Span ",
            " Negated ",
            " Uncertain ",
            " Generic ",
            " URI ",
            " Confidence ",
            " Document Text " );
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
         if ( uri.isEmpty() ) {
            LogFileWriter.add( "DpheTableWriter no uri for text: " + annotation.getCoveredText() );
            continue;
         }
         final DpheGroup group = UriInfoCache.getInstance().getDpheGroup( uri );
//         if ( group == DpheGroup.UNKNOWN ) {
//            LogFileWriter.add( "DpheTableWriter no group for " + uri + " , " + annotation.getCoveredText() );
//         }
         if ( group != DpheGroup.UNKNOWN ) {
            infos.add( new AnnotationInfo( annotationSectionMap.get( annotation ), annotation, uri, group ) );
         }
      }
      return infos.stream()
//                  .sorted( Comparator.comparing( AnnotationInfo::getGroup )
                  .sorted( Comparator.comparingInt( AnnotationInfo::getBegin )
                                     .thenComparingInt( AnnotationInfo::getEnd )
                                     .thenComparing( AnnotationInfo::getGroup )
                                     .thenComparing( AnnotationInfo::getSection )
                                     .thenComparing( AnnotationInfo::isNegated )
                                     .thenComparing( AnnotationInfo::isUncertain )
                                     .thenComparing( AnnotationInfo::isGeneric )
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
      private final String _section;
      private final String _uri;
      private final boolean _negated;
      private final boolean _uncertain;
      private final boolean _generic;
      private final float _confidence;
      private final String _docText;

      private AnnotationInfo( final Collection<Segment> section,
                              final IdentifiedAnnotation annotation,
                              final String uri,
                              final DpheGroup group ) {
         _group = group;
         _begin = annotation.getBegin();
         _end = annotation.getEnd();
         final String sectionText = ( section == null || section.isEmpty() )
                                    ? "NULL"
                                    : new ArrayList<>( section ).get( 0 ).getPreferredText();
         _section = sectionText == null
                    ? "NULL"
                    : sectionText;
//         _uri = Neo4jOntologyConceptUtil.getUri( annotation );
         _uri = uri;
         _negated = IdentifiedAnnotationUtil.isNegated( annotation );
         _uncertain = IdentifiedAnnotationUtil.isUncertain( annotation );
         _generic = IdentifiedAnnotationUtil.isGeneric( annotation );
         _confidence = IdentifiedAnnotationUtil.getConfidence( annotation );
         _docText = annotation.getCoveredText();
      }

      public List<String> getColumns() {
         return Arrays.asList(
               getGroup(),
               getSection(),
               getBegin() + "," + getEnd(),
               isNegated() + "",
               isUncertain() + "",
               isGeneric() + "",
               getUri(),
               getConfidence() + "",
               getDocText() );
      }

      public String getGroup() {
         return _group.getName();
      }

      public String getSection() {
         return _section;
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

      public boolean isNegated() {
         return _negated;
      }

      public boolean isUncertain() {
         return _uncertain;
      }

      public boolean isGeneric() {
         return _generic;
      }

      public float getConfidence() {
         return _confidence;
      }

      public String getDocText() {
         return _docText;
      }

   }


}

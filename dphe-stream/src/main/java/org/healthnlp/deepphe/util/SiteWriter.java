package org.healthnlp.deepphe.util;

import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public final class SiteWriter extends JCasAnnotator_ImplBase {

   private final ToIntFunction<IdentifiedAnnotation> annotationLength = a -> a.getCoveredText().length();

   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      int total = 0;
      int unknown = 0;
      final Collection<String> unknowns = new HashSet<>();

      final Map<String,String> unknownsMap = new HashMap<>();
      unknownsMap.put( "CONNECTIVE & SOFT TISSUE", UriConstants.BODY_TISSUE );
      unknownsMap.put( "OROPHARNYX", "Pharynx" );
      unknownsMap.put( "RETICULO-ENDOTHELIAL", "Reticuloendothelial_System" );
      unknownsMap.put( "UNSPECIFIED DIGEST. ORGANS", "Entire_Digestive_Organ" );
      unknownsMap.put( "RESPIRATORY, NOS", "Respiratory_System" );
      unknownsMap.put( "PLACENTA", "Placenta_Part" );
      unknownsMap.put( "OTHER NERVOUS SYSTEM", "Central_Nervous_System" );
      unknownsMap.put( "ILL-DEFINED", UriConstants.BODY_REGION );
      unknownsMap.put( "OTHER URINARY ORGANS", "Genitourinary_System" );
      unknownsMap.put( "UNKNOWN", UriConstants.BODY_REGION );

      final String SITE_PATH = "C:\\Spiffy\\docs\\dphe\\icdo_info\\sites.bsv";
      try ( Writer writer = new FileWriter( SITE_PATH ) ) {
         final List<Sentence> sentences = JCasUtil.select( jCas, Sentence.class )
                                                  .stream()
                                                  .sorted( Comparator.comparingInt( Sentence::getBegin ) )
                                                  .collect( Collectors.toList() );
         System.out.println( "Sentences: " + sentences.size() );
         for ( Sentence sentence : sentences ) {
            final List<String> sites
                  = JCasUtil.selectCovered( jCas, AnatomicalSiteMention.class, sentence )
                            .stream()
                            .sorted( Comparator.comparingInt( annotationLength ) )
                            .map( Neo4jOntologyConceptUtil::getUri )
                            .collect( Collectors.toList() );
            if ( sites.isEmpty() ) {
               writer.write( sentence.getCoveredText().trim() + "|" + unknownsMap.get( sentence.getCoveredText().trim() ) + "\n" );
               unknowns.add( sentence.getCoveredText().trim() );
               unknown++;
            } else {
               writer.write( sentence.getCoveredText().trim() + "|" + sites.get( sites.size() - 1 ) + "\n" );
            }
            total++;
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
      unknowns.forEach( System.out::println );
      System.out.println( "Total Sites: " + total + " unknown: " + unknown );
   }

}

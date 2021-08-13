package org.healthnlp.deepphe.util;

import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public final class HistoWriter extends JCasAnnotator_ImplBase {

   private final ToIntFunction<IdentifiedAnnotation> annotationLength = a -> a.getCoveredText().length();

   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      int total = 0;
      int unknown = 0;
      final Collection<String> unknowns = new HashSet<>();
      final Map<String,String> unknownsMap = new HashMap<>();
      unknownsMap.put( "ML, SMALL B-CELL LYMPHOCYTIC", "B_Cell_Lymphomas" );
      unknownsMap.put( "ADENOCA IN FAMIL POLYP COLI", "Familial_Adenomatous_Polyposis" );
      unknownsMap.put( "PAPILLARY & FOLLICULAR ADENOCA.", "Papillary_And_Follicular_Adenocarcinoma" );
      unknownsMap.put( "SEBACEOUS/ECCRINE ADENOCA.", "Eccrine_Adenocarcinoma" );
      unknownsMap.put( "ADENOCA. WITH METAPLASIA", "Adenocarcinoma_With_Metaplasia" );
      unknownsMap.put( "PRECURS. CELL LYMPHOBLASTIC LYMPH.", "Lymphoblastic_Lymphoma" );
      unknownsMap.put( "BRONCHIOLO-ALVEOLAR ADENOCA.", "Bronchioloalveolar_Adenocarcinoma" );
      unknownsMap.put( "THERAPY RELATED AC. MYEL. LEUK.", "Therapy_Related_Leukemia" );
      unknownsMap.put( "ADENOID CYSTIC & CRIBRIFORM CA.", "Cribriform_Carcinoma" );
      unknownsMap.put( "FOLLIC. & MARGINAL LYMPH, NOS", "Follicular_Adenoma" );
      unknownsMap.put( "MUCINOUS CYSTADENOCARC., NOS", "Mucinous_Cystadenocarcinoma" );
      unknownsMap.put( "PAPILLARY CYSTADENOCA., NOS", "Papillary_Cystadenocarcinoma" );
      unknownsMap.put( "ADENOCA. IN ADENOMA. POLYP", "Adenocarcinoma_In_Adenomatous_Polyp" );
      unknownsMap.put( "COMB HEPATOCEL CA. & CHOLANG", "Intrahepatic_Cholangiocarcinoma" );
      unknownsMap.put( "CHRONIC MYELOPROLIFERATIVE DIS.", "Myeloproliferative_Neoplasm" );
      unknownsMap.put( "ENDOCRINOMAS", "Endocrine_Gland_Neoplasms" );
      unknownsMap.put( "ML, LARGE B-CELL, DIFFUSE", "Diffuse_Large_B_Cell_Lymphoma" );
      unknownsMap.put( "NONENCAPSUL. SCLEROSING CA.", "Nonencapsulated_Sclerosing_Carcinoma" );
      unknownsMap.put( "LOBULAR AND OTHER DUCTAL CA.", "Intraductal_And_Lobular_Carcinoma" );
      unknownsMap.put( "EXTRA-ADRENAL PARAGANG., MAL", "Malignant_Extra_Adrenal_Paraganglioma" );
      unknownsMap.put( "PAPILLARY SEROUS CYSTADENOCA", "Papillary_Serous_Cystadenocarcinoma" );

      final String HISTO_PATH = "C:\\Spiffy\\docs\\dphe\\icdo_info\\histo.bsv";
      try ( Writer writer = new FileWriter( HISTO_PATH ) ) {
         final List<Sentence> sentences = JCasUtil.select( jCas, Sentence.class )
                                                  .stream()
                                                  .sorted( Comparator.comparingInt( Sentence::getBegin ) )
                                                  .collect( Collectors.toList() );
         for ( Sentence sentence : sentences ) {
            final List<String> sites
                  = JCasUtil.selectCovered( jCas, DiseaseDisorderMention.class, sentence )
                            .stream()
                            .sorted( Comparator.comparingInt( annotationLength ) )
                            .map( Neo4jOntologyConceptUtil::getUri )
                            .collect( Collectors.toList() );
            if ( sites.isEmpty() ) {
               writer.write( sentence.getCoveredText().trim() + "|" + unknownsMap.get( sentence.getCoveredText().trim() ) + "\n" );
               unknowns.add( sentence.getCoveredText().trim() );
               unknown++;
            } else {
               writer.write( sentence.getCoveredText().trim()  + "|" + sites.get( sites.size() - 1 ) + "\n" );
            }
            total++;
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
      unknowns.forEach( System.out::println );
      System.out.println( "Total Histologies: " + total + " unknown: " + unknown );
   }

}

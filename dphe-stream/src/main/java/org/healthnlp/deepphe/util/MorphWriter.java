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

public final class MorphWriter extends JCasAnnotator_ImplBase {

   private final ToIntFunction<IdentifiedAnnotation> annotationLength = a -> a.getCoveredText().length();

   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      int total = 0;
      int unknown = 0;
      final Collection<String> unknowns = new HashSet<>();

      final Map<String,String> unknownsMap = new HashMap<>();
      unknownsMap.put( "Acute promyelocytic leuk.,t(15;17)(q22;q11-12)", "Acute_Promyelocytic_Leukemia" );
      unknownsMap.put( "Sertoli-Leydig cl tum., p.d. w heterologous elements", "Sertoli_Leydig_Cell_Tumor" );
      unknownsMap.put( "Primary cutan. CD30+ T-cell lymphoprolif. disorder", "C_ALCL" );
      unknownsMap.put( "ML, large B-cell, diffuse, immunoblastic, NOS", "Immunoblastic_Lymphoma" );
      unknownsMap.put( "Atypical chronic myeloid leuk., BCR/ABL negative", "Subacute_Myeloid_Leukemia" );
      unknownsMap.put( "Acute myeloid leuk. with multilineage dysplasia", "AML_With_Multilineage_Dysplasia" );
      unknownsMap.put( "Olfactory neurcytoma", "Olfactory_Neurocytoma" );
      unknownsMap.put( "ML, mixed sm. and lg. cell, diffuse", "Diffuse_Malignant_Lymphoma" );
      unknownsMap.put( "ML, lymphoplasmacytic", "Lymphoplasmacytic_Lymphoma" );
      unknownsMap.put( "Ac. myelomonocytic leuk. w abn. mar. eosinophils", "CMML_With_Eosinophilia" );
      unknownsMap.put( "Sq. cell carc. in situ with question. stromal invas.", "Stage_0_Squamous_Cell_Carcinoma" );
      unknownsMap.put( "Infiltrating lobular mixed with other types of carc.", "Invasive_Lobular_Breast_Carcinoma" );
      unknownsMap.put( "Hodgkin lymph., lymphocyt. deplet., diffuse fibrosis", "Hodgkin_Lymphoma__Lymphocyte_Depletion" );
      unknownsMap.put( "Refract. anemia with excess blasts in transformation", "Refractory_Anemia_With_Excess_Blasts" );
      unknownsMap.put( "ML, large B-cell, diffuse", "Diffuse_Large_B_Cell_Lymphoma" );
      unknownsMap.put( "Refractory cytopenia with multilineage dysplasia", "Refractory_Cytopenia_Of_Childhood" );
      unknownsMap.put( "Erdhiem-Chester Disease", "Erdheim_Chester_Disease" );
      unknownsMap.put( "Immunoproliferative small intestinal disease", "Immunoproliferative_Disorder" );  // Not very good
      unknownsMap.put( "Spongioneuroblastoma", "Polar_Spongioblastoma" );
      unknownsMap.put( "ML, small B lymphocytic, NOS", "Small_Lymphocytic_Lymphoma" );
      unknownsMap.put( "Hodgkin lymph., nodular lymphocyte predom.", "Childhood_NLPHD" );

      final String MORPH_PATH = "C:\\Spiffy\\docs\\dphe\\icdo_info\\morph.bsv";
      try ( Writer writer = new FileWriter( MORPH_PATH ) ) {
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
      System.out.println( "Total Morphologies: " + total + " unknown: " + unknown );
   }

}

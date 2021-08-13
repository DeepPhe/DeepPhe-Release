package org.healthnlp.deepphe.util;

import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.core.ae.Neo4jEmbeddedConnectAe;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * You can use this to create a dphe-compatible site-histology validation file.
 */
final public class TextToClassXlater {

   private TextToClassXlater() {}


   public static void main( String[] args ) {
      final String SITE_PATH = "C:\\Spiffy\\docs\\dphe\\icdo_info\\sites.bsv";
      final String HISTO_PATH = "C:\\Spiffy\\docs\\dphe\\icdo_info\\histo.bsv";
      final String MORPH_PATH = "C:\\Spiffy\\docs\\dphe\\icdo_info\\morph.bsv";
      final Map<String,String> siteMap = new HashMap<>();
      final Map<String,String> histoMap = new HashMap<>();
      final Map<String,String> morphMap = new HashMap<>();

      try ( BufferedReader reader = new BufferedReader( new FileReader( SITE_PATH ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] splits = StringUtil.fastSplit( line, '|' );
            siteMap.put( splits[ 0 ], splits[ 1 ] );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.out.println( ioE.getMessage() );
      }

      try ( BufferedReader reader = new BufferedReader( new FileReader( HISTO_PATH ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] splits = StringUtil.fastSplit( line, '|' );
            histoMap.put( splits[ 0 ], splits[ 1 ] );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.out.println( ioE.getMessage() );
      }

      try ( BufferedReader reader = new BufferedReader( new FileReader( MORPH_PATH ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] splits = StringUtil.fastSplit( line, '|' );
            morphMap.put( splits[ 0 ], splits[ 1 ] );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.out.println( ioE.getMessage() );
      }

      final String topoMorphValidatorPath = "C:\\Spiffy\\docs\\dphe\\icdo_info\\histology_site_validation.bsv";
      final String topoMorphValidatorPathOut = "C:\\Spiffy\\docs\\dphe\\icdo_info\\DpheHistologySites.bsv";

      try ( BufferedReader reader = new BufferedReader( new FileReader( topoMorphValidatorPath ) );
            BufferedWriter writer = new BufferedWriter( new FileWriter( topoMorphValidatorPathOut ) ) ) {
         int i = 0;
         String line = reader.readLine();
         while ( line != null ) {
            i++;
            if ( i == 1 || line.isEmpty() ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length < 6 ) {
               line = reader.readLine();
               continue;
            }
            writer.write( splits[ 0 ] + "|"
                          + siteMap.get( splits[ 1 ] ) + "|"
                          + splits[ 2 ]  + "|"
                          + histoMap.get( splits[ 3 ] ) + "|"
                          + splits[ 4 ]  + "|"
                          + morphMap.get( splits[ 5 ] ) + "\n" );
//            siteTexts.add( splits[ 1 ] );
//            histoTexts.add( splits[ 3 ] );
//            morphTexts.add( splits[ 5 ] );
            line = reader.readLine();
         }
         System.out.println( "Lines: " + i );
      } catch ( IOException ioE ) {
         System.out.println( ioE.getMessage() );
      }

//      final TextBySentenceBuilder siteBuilder = new TextBySentenceBuilder();
//      siteTexts.forEach( siteBuilder::addSentence );
//      try {
//         final JCas jCas = siteBuilder.build();
//         System.out.println( "Doc Length : " + jCas.getDocumentText().length() );
//         final AnalysisEngineDescription desc = createSitePipeline().getAnalysisEngineDesc();
//         SimplePipeline.runPipeline( jCas, desc );
//
//      } catch ( UIMAException | IOException uimaE ) {
//         System.err.println( uimaE.getMessage() );
//      }
//
//      final TextBySentenceBuilder histoBuilder = new TextBySentenceBuilder();
//      histoTexts.forEach( histoBuilder::addSentence );
//      try {
//         final JCas jCas = histoBuilder.build();
//         final AnalysisEngineDescription desc = createHistoPipeline().getAnalysisEngineDesc();
//         SimplePipeline.runPipeline( jCas, desc );
//      } catch ( UIMAException | IOException uimaE ) {
//         System.err.println( uimaE.getMessage() );
//      }
//
//      final TextBySentenceBuilder morphBuilder = new TextBySentenceBuilder();
//      morphTexts.forEach( morphBuilder::addSentence );
//      try {
//         final JCas jCas = morphBuilder.build();
//         final AnalysisEngineDescription desc = createMorphPipeline().getAnalysisEngineDesc();
//         SimplePipeline.runPipeline( jCas, desc );
//      } catch ( UIMAException | IOException uimaE ) {
//         System.err.println( uimaE.getMessage() );
//      }

   }


   static private PipelineBuilder createSitePipeline() {
      final PipelineBuilder builder = new PipelineBuilder();
      try {
         builder.add( Neo4jEmbeddedConnectAe.class )
                .add( TokenizerAnnotatorPTB.class )
                .addDescription( POSTagger.createAnnotatorDescription() )
                .set( "exclusionTags", "" )
                .set( "LookupXml","org/apache/ctakes/dictionary/lookup/fast/ncit_plus_16ab.xml")
                .add( DefaultJCasTermAnnotator.class )
                .add( SiteWriter.class );
      } catch ( ResourceInitializationException riE ) {
         System.err.println( riE.getMessage() );
      }
      return builder;
   }

   static private PipelineBuilder createHistoPipeline() {
      final PipelineBuilder builder = new PipelineBuilder();
      try {
         builder.add( Neo4jEmbeddedConnectAe.class )
                .add( TokenizerAnnotatorPTB.class )
                .addDescription( POSTagger.createAnnotatorDescription() )
                .set( "exclusionTags", "" )
                .set( "LookupXml","org/apache/ctakes/dictionary/lookup/fast/ncit_plus_16ab.xml")
                .add( DefaultJCasTermAnnotator.class )
                .add( HistoWriter.class );
      } catch ( ResourceInitializationException riE ) {
         System.err.println( riE.getMessage() );
      }
      return builder;
   }

   static private PipelineBuilder createMorphPipeline() {
      final PipelineBuilder builder = new PipelineBuilder();
      try {
         builder.add( Neo4jEmbeddedConnectAe.class )
                .add( TokenizerAnnotatorPTB.class )
                .addDescription( POSTagger.createAnnotatorDescription() )
                .set( "exclusionTags", "" )
                .set( "LookupXml","org/apache/ctakes/dictionary/lookup/fast/ncit_plus_16ab.xml")
                .add( DefaultJCasTermAnnotator.class )
                .add( MorphWriter.class );
      } catch ( ResourceInitializationException riE ) {
         System.err.println( riE.getMessage() );
      }
      return builder;
   }


}


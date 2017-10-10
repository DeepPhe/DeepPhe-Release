package org.apache.ctakes.cancer.ae.section;

import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

/**
 * Grabs the document time from the pittsburgh header
 */
final public class PittHeaderAnnotator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PittHeaderAnnotator" );

   static private final Pattern DATE_PATTERN = Pattern.compile( ".*Principal Date\\D+(\\d+) (\\d+).*", DOTALL );


   /**
    * Grabs the document time from the header
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Parsing the Document Header ..." );
      // TODO -- use a document creation time type?
      final Collection<Segment> sections = JCasUtil.select( jcas, Segment.class );
      for ( Segment section : sections ) {
         if ( !section.getPreferredText().equals( PittSectionizer.PITTSBURGH_HEADER ) ) {
            continue;
         }
         final String text = section.getCoveredText();
         final Matcher dateMatcher = DATE_PATTERN.matcher( text );
         if ( dateMatcher.matches() ) {
            final TimeMention docTime = new TimeMention( jcas );
            docTime.setBegin( dateMatcher.start( 1 ) );
            docTime.setEnd( dateMatcher.end( 2 ) );
            docTime.setId( 0 );
            docTime.addToIndexes();
         }
         break;
      }
      LOGGER.info( "Finished Processing" );
   }


}

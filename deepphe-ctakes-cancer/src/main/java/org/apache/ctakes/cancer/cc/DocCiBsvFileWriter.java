package org.apache.ctakes.cancer.cc;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;


/**
 * Instead of attempting to use hard coded columns, make dynamic output using the available relation types.
 * When uris and / or relation names are changed there is no propagation / refactoring / problem.
 * Choice of which columns to use is up to the eval tool.
 * This also allows a run once, analyze as many times / ways as wanted process.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/4/2018
 */
public class DocCiBsvFileWriter extends AbstractJCasFileWriter {

   static private final String PARAM_OUTPUT_URIS = "OutputUris";

   static private final Logger LOGGER = Logger.getLogger( "DocCiBsvFileWriter" );

   @ConfigurationParameter( name = PARAM_OUTPUT_URIS, mandatory = false,
         description = "Set of URIs to output" )
   private String _outputUriParam;

   private Collection<String> _outputUris;
   private boolean _useDefaults = false;

   private CiBsvFileWriter _delegate;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      if ( _outputUriParam != null && !_outputUriParam.isEmpty() ) {
         _outputUris = Arrays.asList( StringUtil.fastSplit( _outputUriParam, ',' ) );
      } else {
         _useDefaults = true;
      }
      _delegate = new CiBsvFileWriter();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final Collection<ConceptInstance> conceptInstances
            = ConceptInstanceFactory.createUriConceptInstanceMap( jCas ).values().stream()
                                    .flatMap( Collection::stream )
                                    .sorted( CI_SORTER )
                                    .collect( Collectors.toList() );
      if ( _useDefaults ) {
         writeFile( documentId, outputDir, conceptInstances, UriConstants.getCancerUris(), "Cancer" );
         writeFile( documentId, outputDir, conceptInstances, UriConstants.getTumorUris(), "Tumor" );
         writeFile( documentId, outputDir, conceptInstances, UriConstants.getPrimaryUris(), "Primary" );
         writeFile( documentId, outputDir, conceptInstances, UriConstants.getMetastasisUris(), "Metastasis" );
         writeFile( documentId, outputDir, conceptInstances, UriConstants.getGenericTumorUris(), "UnknownTumor" );
         final File allFile = new File( outputDir, documentId + "_Everything.bsv" );
         _delegate.writeForwardBranch( conceptInstances, allFile );
      } else {
         for ( String outputUri : _outputUris ) {
            final String shortUri = UriUtil.getExtension( outputUri );
            final File file = new File( outputDir, documentId + "_" + shortUri + ".bsv" );
            _delegate.writeFile( outputUri, conceptInstances, file );
         }
      }
   }

   private void writeFile( final String documentId,
                           final String outputDir,
                           final Collection<ConceptInstance> conceptInstances,
                           final Collection<String> uris,
                           final String type ) throws IOException {
      final File file = new File( outputDir, documentId + "_" + type + ".bsv" );
      final Collection<ConceptInstance> branch
            = conceptInstances.stream()
                              .filter( ci -> uris.contains( ci.getUri() ) )
                              .sorted( CI_SORTER )
                              .collect( Collectors.toList() );
      _delegate.writeForwardBranch( branch, file );
   }


   static public final CiSorter CI_SORTER = new CiSorter();

   static private class CiSorter implements Comparator<ConceptInstance> {
      public int compare( final ConceptInstance ci1, final ConceptInstance ci2 ) {
         final String uri1 = ci1.getUri();
         final String uri2 = ci2.getUri();
         final int uriDiff = uri1.compareTo( uri2 );
         if ( uriDiff != 0 ) {
            return uriDiff;
         }
         int span1 = ci1.getAnnotations().stream()
                        .map( a -> a.getEnd() * 10 + a.getBegin() )
                        .mapToInt( Integer::valueOf ).sum();
         int span2 = ci2.getAnnotations().stream()
                        .map( a -> a.getEnd() * 10 + a.getBegin() )
                        .mapToInt( Integer::valueOf ).sum();
         int diff = span1 - span2;
         if ( diff != 0 ) {
            return diff;
         }
         final String text1 = ci1.toText();
         final String text2 = ci2.toText();
         int textDiff = text1.compareTo( text2 );
         if ( textDiff != 0 ) {
            return textDiff;
         }
         return ci1.getCoveredText().compareTo( ci2.getCoveredText() );
      }
   }

}

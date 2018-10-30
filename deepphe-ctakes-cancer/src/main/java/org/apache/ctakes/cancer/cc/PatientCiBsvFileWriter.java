package org.apache.ctakes.cancer.cc;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.core.patient.AbstractPatientFileWriter;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
public class PatientCiBsvFileWriter extends AbstractPatientFileWriter {

   static private final String PARAM_OUTPUT_URIS = "OutputUris";

   static private final Logger LOGGER = Logger.getLogger( "PatientCiBsvFileWriter" );

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
   public void writeFile( final Collection<JCas> patients,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      for ( JCas patient : patients ) {
         final String patientId = SourceMetadataUtil.getPatientIdentifier( patient );
         final Collection<JCas> docs = PatientViewUtil.getAllViews( patient );
         final Collection<ConceptInstance> conceptInstances = new ArrayList<>();
         for ( JCas doc : docs ) {
            for ( Collection<ConceptInstance> uriInstances
                  : ConceptInstanceFactory.createUriConceptInstanceMap( doc ).values() ) {
               conceptInstances.addAll( uriInstances );
            }
         }
         if ( _useDefaults ) {
            final Collection<String> cancerUris = UriConstants.getCancerUris();
            final File cancerFile = new File( outputDir, patientId + "_Cancer.bsv" );
            final Collection<ConceptInstance> cancerBranch = conceptInstances.stream()
                                                                             .filter( ci -> cancerUris
                                                                                   .contains( ci.getUri() ) )
                                                                             .collect( Collectors.toList() );
            _delegate.writeForwardBranch( cancerBranch, cancerFile );
            final Collection<String> tumorUris = UriConstants.getTumorUris();
            final File tumorFile = new File( outputDir, patientId + "_Tumor.bsv" );
            final Collection<ConceptInstance> tumorBranch = conceptInstances.stream()
                                                                            .filter( ci -> tumorUris
                                                                                  .contains( ci.getUri() ) )
                                                                            .collect( Collectors.toList() );
            _delegate.writeForwardBranch( tumorBranch, tumorFile );
         } else {
            for ( String outputUri : _outputUris ) {
               final String shortUri = UriUtil.getExtension( outputUri );
               final File file = new File( outputDir, patientId + "_" + shortUri + ".bsv" );
               _delegate.writeFile( outputUri, conceptInstances, file );
            }
         }
      }
   }


}

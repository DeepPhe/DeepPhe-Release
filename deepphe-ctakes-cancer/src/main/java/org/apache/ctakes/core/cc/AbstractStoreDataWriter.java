package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.store.DataStore;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/11/2019
 */
abstract public class AbstractStoreDataWriter<S extends DataStore<D>,D> extends JCasAnnotator_ImplBase implements DataWriter<D>, NamedEngine {

   public static final String PARAMETER_CLEAR_STORE = "ClearStore";
   public static final String PARAMETER_ENGINE_NAME = "EngineName";
   @ConfigurationParameter(
         name = PARAMETER_CLEAR_STORE,
         description = "The Writer should clear the Concept Instance data store when the pipeline is finished.",
         mandatory = false,
         defaultValue = {"true"}
   )
   private boolean _clear;

   @ConfigurationParameter(
         name = PARAMETER_ENGINE_NAME,
         description = "The Name to use for this File Writer.  Must be unique in the pipeline.",
         mandatory = false
   )
   private String _engineName;


   private D _data;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( context );
      getDataStore().registerEngine( getEngineName() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final D data = createAndGetData( jCas );
      write( data );
      writeComplete( data );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      D data = createAndGetData( null );
      while ( data != null ) {
         write( data );
         writeComplete( data );
         data = createAndGetData( null );
      }
      if ( _clear && getDataStore().isDataEmpty() ) {
         getDataStore().clearData();
         getDataStore().clearEngines();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getEngineName() {
      return _engineName != null && !_engineName.isEmpty() ? _engineName : getClass().getSimpleName();
   }

   abstract protected S getDataStore();

   public void createData( final JCas jCas ) {
      _data = getDataStore().pop( getEngineName() );
   }

   public D getData() { return _data; }

   public void writeComplete( final D data ) {}

}

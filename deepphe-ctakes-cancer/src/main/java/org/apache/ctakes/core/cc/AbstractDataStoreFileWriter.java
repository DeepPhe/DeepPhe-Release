package org.apache.ctakes.core.cc;


import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.store.DataStore;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/12/2019
 */
abstract public class AbstractDataStoreFileWriter<S extends DataStore<T>,T>
      extends AbstractFileWriter<T> implements NamedEngine {

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


   private T _data;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( context );
      getDataStore().registerEngine( getEngineName() );
   }


   abstract protected S getDataStore();

   /**
    * {@inheritDoc}
    */
   @Override
   public String getEngineName() {
      return _engineName != null && !_engineName.isEmpty() ? _engineName : getClass().getSimpleName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void createData( final JCas jCas ) {
      _data = getDataStore().pop( getEngineName() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public T getData() { return _data; }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeComplete( final T data ) {}

}

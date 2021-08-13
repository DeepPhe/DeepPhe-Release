package org.healthnlp.deepphe.nlp.reader.file;

import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.ProgressManager;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.impl.CollectionReaderDescription_impl;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterDeclarations_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This is an abstract implementation of the standard uimafit reader.
 * It centers around the {@link Doc} as the object being populated by the reader.
 * This makes it easier to abstract readers that create Docs via plaintext files,
 * DB entries, rest service entry, embedded xml texts and so forth.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2020
 */
abstract public class DocReader<T extends DocStore> extends JCasCollectionReader_ImplBase {


   private T _docStore;
   private boolean _initialized;
   private int _index = -1;


   public DocReader() {
      setMetaData( createMetaData() );
   }


   /**
    * This is the key to the whole reader.
    * The DocStore is responsible for holding Doc objects, be they created by that store itself or put there from elsewhere.
    *
    * @return Some DocStore that contains Doc objects.
    */
   abstract protected T createDocStore();


   protected boolean isInitialized() {
      return _initialized;
   }

   /**
    * This needs to be called.  If it is not called explicitly then the first hasNext() call will do so.
    *
    * @throws IOException for any initialization problem.
    */
   protected void initializeReader() throws IOException {
      final T docStore = getDocStore();
      if ( docStore == null ) {
         throw new IOException( "Document Store not initialized" );
      }
      docStore.initialize();
      initializeProgress();
      _initialized = true;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _docStore = createDocStore();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public boolean hasNext() {
      if ( !isInitialized() ) {
         try {
            initializeReader();
         } catch ( IOException ioE ) {
            Logger.getLogger( DocReader.class ).error( "Could not initialize Reader", ioE );
            return false;
         }
      }
      if ( getDocStore().isDynamic() ) {
         return true;
      }
      final boolean hasNext = getDocIndex() < getDocStore().getDocCount();
      if ( !hasNext ) {
         completeProgress();
      }
      return hasNext;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public void getNext( final JCas jcas ) throws IOException, CollectionException {
      setDocIndex( getDocIndex() + 1 );
      final int index = getDocIndex();
      final Doc doc = getDocStore().getDoc( index );
      updateProgress();
      doc.populateCas( jcas );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public Progress[] getProgress() {
      return new Progress[] {
            new ProgressImpl( getDocIndex(),
                  getDocStore().isDynamic() ? Integer.MAX_VALUE : getDocStore().getDocCount(),
                  Progress.ENTITIES )
      };
   }


   /**
    * @return the DocStore.  This may be null, so plan accordingly.
    */
   protected T getDocStore() {
      return _docStore;
   }

   /**
    * @return the index of the document currently being used.
    */
   public int getDocIndex() {
      return _index;
   }

   /**
    * @param index new index.  Usually current index + 1 for simple increment.
    */
   protected void setDocIndex( final int index ) {
      _index = index;
   }

   /**
    * Works with the ctakes ProgressManager.  Also initializes patient counts.
    */
   protected void initializeProgress() {
      final DocStore docStore = getDocStore();
      final int max = docStore.isDynamic() ? Integer.MAX_VALUE : docStore.getDocCount();
      ProgressManager.getInstance().initializeProgress( getClass().getSimpleName(), max );

      final Map<String, List<Doc>> patientDocMap
            = StreamSupport.stream( docStore.spliterator(), false )
                           .collect( Collectors.groupingBy( Doc::getPatientId ) );
      patientDocMap.forEach( ( k, v ) -> PatientNoteStore.getInstance().setWantedDocCount( k, v.size() ) );
   }

   /**
    * Works with the ctakes ProgressManager.
    */
   protected void updateProgress() {
      final int index = getDocIndex();
      ProgressManager.getInstance().updateProgress( index + 1 );
   }

   /**
    * Works with the ctakes ProgressManager.
    */
   protected void completeProgress() {
      if ( getDocStore().isDynamic() ) {
         // The store was dynamic, but completion should reset the progress max to the doc count.
         ProgressManager.getInstance().initializeProgress( getClass().getSimpleName(), getDocStore().getDocCount() );
      }
      ProgressManager.getInstance().updateProgress( getDocStore().getDocCount() );
   }


   ////////////////////////////////////////////////////////////////////////////////////
   //
   //             Sometimes UIMA has problems if the ResourceMetaData isn't explicit
   //
   ////////////////////////////////////////////////////////////////////////////////////


   /**
    * @return Resource metadata for an abstract reader.  This exists to make uima automation factories happy.
    */
   static private ResourceMetaData createMetaData() {
      final ReaderMetadata metadata = new ReaderMetadata();
      metadata.setUUID( "AFTR" );
      metadata.setName( "DocReader" );
      metadata.setVersion( "1" );
      metadata.setDescription( "Document Reader" );
      metadata.setVendor( "Apache cTAKES" );
      metadata.setCopyright( "2020" );
      return metadata;
   }

   /**
    * The following is required to prevent errors by automated Descriptor creation.
    */
   static private final class ReaderMetadata extends CollectionReaderDescription_impl implements ResourceMetaData {
      static final long serialVersionUID = 3408359518094534817L;
      private String mUUID;
      private String mName;
      private String mDescription;
      private String mVersion;
      private String mVendor;
      private String mCopyright;
      private ConfigurationParameterDeclarations mConfigurationParameterDeclarations
            = new ConfigurationParameterDeclarations_impl();
      private ConfigurationParameterSettings mConfigurationParameterSettings
            = new ConfigurationParameterSettings_impl();
      private static final XmlizationInfo XMLIZATION_INFO
            = new XmlizationInfo( "resourceMetaData", new PropertyXmlInfo[] { new PropertyXmlInfo( "name", false ),
                                                                              new PropertyXmlInfo( "description" ),
                                                                              new PropertyXmlInfo( "version" ),
                                                                              new PropertyXmlInfo( "vendor" ),
                                                                              new PropertyXmlInfo( "copyright" ),
                                                                              new PropertyXmlInfo( "configurationParameterDeclarations", (String)null ),
                                                                              new PropertyXmlInfo( "configurationParameterSettings", (String)null ) } );

      public void resolveImports() throws InvalidXMLException {
      }

      public void resolveImports( ResourceManager aResourceManager ) throws InvalidXMLException {
      }

      public String getUUID() {
         return this.mUUID;
      }

      public void setUUID( String aUUID ) {
         this.mUUID = aUUID;
      }

      public String getName() {
         return this.mName;
      }

      public void setName( String aName ) {
         this.mName = aName;
      }

      public String getVersion() {
         return this.mVersion;
      }

      public void setVersion( String aVersion ) {
         this.mVersion = aVersion;
      }

      public String getDescription() {
         return this.mDescription;
      }

      public void setDescription( String aDescription ) {
         this.mDescription = aDescription;
      }

      public String getVendor() {
         return this.mVendor;
      }

      public void setVendor( String aVendor ) {
         this.mVendor = aVendor;
      }

      public String getCopyright() {
         return this.mCopyright;
      }

      public void setCopyright( String aCopyright ) {
         this.mCopyright = aCopyright;
      }

      public ConfigurationParameterSettings getConfigurationParameterSettings() {
         return this.mConfigurationParameterSettings;
      }

      public void setConfigurationParameterSettings( ConfigurationParameterSettings aSettings ) {
         this.mConfigurationParameterSettings = aSettings;
      }

      public ConfigurationParameterDeclarations getConfigurationParameterDeclarations() {
         return this.mConfigurationParameterDeclarations;
      }

      public void setConfigurationParameterDeclarations( ConfigurationParameterDeclarations aDeclarations ) {
         this.mConfigurationParameterDeclarations = aDeclarations;
      }

      public void validateConfigurationParameterSettings() throws ResourceConfigurationException {
         ConfigurationParameterDeclarations cfgParamDecls = this.getConfigurationParameterDeclarations();
         ConfigurationParameterSettings cfgParamSettings = this.getConfigurationParameterSettings();
         NameValuePair[] nvps = cfgParamSettings.getParameterSettings();
         if ( nvps.length > 0 ) {
            this.validateConfigurationParameterSettings( nvps, (String)null, cfgParamDecls );
         } else {
            Map settingsForGroups = cfgParamSettings.getSettingsForGroups();
            Set entrySet = settingsForGroups.entrySet();
            Iterator it = entrySet.iterator();

            while ( it.hasNext() ) {
               Map.Entry entry = (Map.Entry)it.next();
               String groupName = (String)entry.getKey();
               nvps = (NameValuePair[])entry.getValue();
               if ( nvps != null ) {
                  this.validateConfigurationParameterSettings( nvps, groupName, cfgParamDecls );
               }
            }
         }

      }

      protected void validateConfigurationParameterSettings( NameValuePair[] aNVPs, String aGroupName,
                                                             ConfigurationParameterDeclarations aParamDecls )
            throws ResourceConfigurationException {
         for ( int i = 0; i < aNVPs.length; ++i ) {
            String name = aNVPs[ i ].getName();
            org.apache.uima.resource.metadata.ConfigurationParameter param
                  = aParamDecls.getConfigurationParameter( aGroupName, name );
            if ( param == null ) {
               if ( aGroupName == null ) {
                  throw new ResourceConfigurationException( "nonexistent_parameter", new Object[] { name,
                                                                                                    this.getName() } );
               }

               throw new ResourceConfigurationException( "nonexistent_parameter_in_group", new Object[] { name,
                                                                                                          aGroupName,
                                                                                                          this.getName() } );
            }

            this.validateConfigurationParameterDataTypeMatch( param, aNVPs[ i ] );
         }

      }

      protected void validateConfigurationParameterDataTypeMatch(
            org.apache.uima.resource.metadata.ConfigurationParameter aParam, NameValuePair aNVP )
            throws ResourceConfigurationException {
         String paramName = aParam.getName();
         String paramType = aParam.getType();
         Class valClass = aNVP.getValue().getClass();
         if ( aParam.isMultiValued() ) {
            if ( !valClass.isArray() ) {
               throw new ResourceConfigurationException( "array_required", new Object[] { paramName, this.getName() } );
            }

            valClass = valClass.getComponentType();
            if ( Array.getLength( aNVP.getValue() ) == 0 && valClass.equals( Object.class ) ) {
               aNVP.setValue( Array.newInstance( this.getClassForParameterType( paramType ), 0 ) );
               return;
            }
         }

         if ( valClass != this.getClassForParameterType( paramType ) ) {
            throw new ResourceConfigurationException( "parameter_type_mismatch", new Object[] { this.getName(),
                                                                                                valClass.getName(),
                                                                                                paramName,
                                                                                                paramType } );
         }
      }

      protected Class getClassForParameterType( String paramType ) {
         return "String".equals( paramType ) ? String.class : ("Boolean".equals( paramType ) ? Boolean.class : (
               "Integer".equals( paramType ) ? Integer.class : ("Float".equals( paramType ) ? Float.class : null)));
      }
   }


}

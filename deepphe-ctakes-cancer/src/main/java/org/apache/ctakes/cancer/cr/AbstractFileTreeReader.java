package org.apache.ctakes.cancer.cr;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.TextNumberComparator;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.structured.DocumentIdPrefix;
import org.apache.ctakes.typesystem.type.structured.DocumentPath;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.impl.CollectionReaderDescription_impl;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;


/**
 * Abstract to read files in a tree starting in a root directory.
 * By default, filenames are sorted with {@link TextNumberComparator}.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2017
 */
abstract public class AbstractFileTreeReader extends JCasCollectionReader_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "AbstractFileTreeReader" );

   /**
    * Name of configuration parameter that must be set to the path of
    * a directory containing input files.
    */
   @ConfigurationParameter(
         name = ConfigParameterConstants.PARAM_INPUTDIR,
         description = ConfigParameterConstants.DESC_INPUTDIR
   )
   private String _rootDirPath;

   /**
    * Name of configuration parameter that contains the character encoding used
    * by the input files.  If not specified, the default system encoding will
    * be used.
    */
   static public final String PARAM_ENCODING = "Encoding";
   @ConfigurationParameter(
         name = PARAM_ENCODING,
         description = "The character encoding used by the input files.",
         defaultValue = "unicode",
         mandatory = false
   )
   private String _encoding;

   /**
    * Name of optional configuration parameter that specifies the extensions
    * of the files that the collection reader will read.  Values for this
    * parameter should not begin with a dot <code>'.'</code>.
    */
   static public final String PARAM_EXTENSIONS = "Extensions";
   @ConfigurationParameter(
         name = PARAM_EXTENSIONS,
         description = "The extensions of the files that the collection reader will read.",
         defaultValue = "*",
         mandatory = false
   )
   private String[] _explicitExtensions;

   private File _rootDir;
   private Collection<String> _validExtensions;
   private List<File> _files;
   private int _currentIndex;
   private Comparator<File> _fileComparator;


   public AbstractFileTreeReader() {
      setMetaData( createMetaData() );
   }

   /**
    * @param jCas unpopulated jcas
    * @param file file to be read
    * @throws IOException should anything bad happen
    */
   abstract protected void readFile( final JCas jCas, final File file ) throws IOException;

   /**
    * @return Comparator to sort Files and Directories.  The default Comparator sorts by filename with {@link TextNumberComparator}.
    */
   protected Comparator<File> createFileComparator() {
      return new FileComparator();
   }

   /**
    * Gets the total number of documents that will be returned by this
    * collection reader.
    *
    * @return the number of documents in the collection
    */
   public int getNoteCount() {
      if ( _files == null ) {
         LOGGER.error( "Not yet initialized" );
         return 0;
      }
      return _files.size();
   }

   protected File getRootDir() {
      if ( _rootDir == null ) {
         LOGGER.error( "Not yet initialized" );
         return null;
      }
      return _rootDir;
   }

   protected String getRootPath() {
      final File rootDir = getRootDir();
      if ( rootDir == null ) {
         LOGGER.error( "Not yet initialized" );
         return "Unknown";
      }
      return rootDir.getAbsolutePath();
   }

   final protected String getValidEncoding() {
      if ( _rootDir == null ) {
         LOGGER.error( "Not yet initialized" );
         return "Unknown";
      }
      if ( _encoding == null || _encoding.isEmpty() ) {
         return "Unicode";
      }
      return _encoding;
   }

   protected Collection<String> getValidExtensions() {
      if ( _validExtensions == null ) {
         LOGGER.error( "Not yet initialized" );
         return Collections.emptyList();
      }
      return _validExtensions;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      try {
         _rootDir = FileLocator.getFile( _rootDirPath );
      } catch ( FileNotFoundException fnfE ) {
         throw new ResourceInitializationException( fnfE );
      }
      _validExtensions = createValidExtensions( _explicitExtensions );

      _currentIndex = 0;
      _files = getDescendentFiles( getRootDir(), getValidExtensions() );
   }

   /**
    * @param explicitExtensions array of file extensions as specified in the uima parameters
    * @return a collection of dot-prefixed extensions or none if {@code explicitExtensions} is null or empty
    */
   static private Collection<String> createValidExtensions( final String... explicitExtensions ) {
      if ( explicitExtensions == null || explicitExtensions.length == 0 ) {
         return Collections.emptyList();
      }
      if ( explicitExtensions.length == 1
            && (explicitExtensions[ 0 ].equals( "*" ) || explicitExtensions[ 0 ].equals( ".*" )) ) {
         return Collections.emptyList();
      }
      final Collection<String> validExtensions = new ArrayList<>( explicitExtensions.length );
      for ( String extension : explicitExtensions ) {
         if ( extension.startsWith( "." ) ) {
            validExtensions.add( extension );
         } else {
            validExtensions.add( '.' + extension );
         }
      }
      return validExtensions;
   }

   /**
    * @param parentDir       -
    * @param validExtensions collection of valid extensions or empty collection if all extensions are valid
    * @return List of files descending from the parent directory
    */
   protected List<File> getDescendentFiles( final File parentDir, final Collection<String> validExtensions ) {
      if ( _fileComparator == null ) {
         _fileComparator = createFileComparator();
      }
      final File[] children = parentDir.listFiles();
      if ( children == null || children.length == 0 ) {
         return Collections.emptyList();
      }
      final List<File> childDirs = new ArrayList<>();
      final List<File> descendentFiles = new ArrayList<>();
      for ( File child : children ) {
         if ( child.isDirectory() ) {
            childDirs.add( child );
            continue;
         }
         if ( isExtensionValid( child, validExtensions ) && !child.isHidden() ) {
            descendentFiles.add( child );
         }
      }
      descendentFiles.sort( _fileComparator );
      childDirs.sort( _fileComparator );
      for ( File childDir : childDirs ) {
         descendentFiles.addAll( getDescendentFiles( childDir, validExtensions ) );
      }
      return descendentFiles;
   }

   /**
    * @param file            -
    * @param validExtensions -
    * @return true if validExtensions is empty or contains an extension belonging to the given file
    */
   static private boolean isExtensionValid( final File file, final Collection<String> validExtensions ) {
      if ( validExtensions.isEmpty() ) {
         return true;
      }
      final String fileName = file.getName();
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) ) {
            if ( fileName.equals( extension ) ) {
               LOGGER.warn( "File " + file.getPath()
                     + " name exactly matches extension " + extension + " so it will not be read." );
               return false;
            }
            return true;
         }
      }
      return false;
   }

   /**
    * @param file            -
    * @param validExtensions -
    * @return the file name with the longest valid extension removed
    */
   protected String createDocumentID( final File file, final Collection<String> validExtensions ) {
      final String fileName = file.getName();
      String maxExtension = "";
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) && extension.length() > maxExtension.length() ) {
            maxExtension = extension;
         }
      }
      int lastDot = fileName.lastIndexOf( '.' );
      if ( !maxExtension.isEmpty() ) {
         lastDot = fileName.length() - maxExtension.length();
      }
      if ( lastDot < 0 ) {
         return fileName;
      }
      return fileName.substring( 0, lastDot );
   }

   /**
    * @param file    -
    * @param rootDir -
    * @return the subdirectory path between the root directory and the file
    */
   protected String createDocumentIdPrefix( final File file, final File rootDir ) {
      final String parentPath = file.getParent();
      final String rootPath = rootDir.getPath();
      if ( parentPath.equals( rootPath ) || !parentPath.startsWith( rootPath ) ) {
         return "";
      }
      return parentPath.substring( rootPath.length() + 1 );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean hasNext() {
      return _currentIndex < _files.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void getNext( final JCas jcas ) throws IOException, CollectionException {
      final File file = _files.get( _currentIndex );
      _currentIndex++;
      final String id = createDocumentID( file, getValidExtensions() );
      LOGGER.info( "Reading " + id + " : " + file.getPath() );
      readFile( jcas, file );
      // Add document metadata based upon file path
      final DocumentID documentId = new DocumentID( jcas );
      documentId.setDocumentID( id );
      documentId.addToIndexes();
      final DocumentIdPrefix documentIdPrefix = new DocumentIdPrefix( jcas );
      final String idPrefix = createDocumentIdPrefix( file, getRootDir() );
      documentIdPrefix.setDocumentIdPrefix( idPrefix );
      documentIdPrefix.addToIndexes();
      final DocumentPath documentPath = new DocumentPath( jcas );
      documentPath.setDocumentPath( file.getAbsolutePath() );
      documentPath.addToIndexes();
      LOGGER.info( "Finished Reading." );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Progress[] getProgress() {
      return new Progress[]{
            new ProgressImpl( _currentIndex, _files.size(), Progress.ENTITIES )
      };
   }


   static private ResourceMetaData createMetaData() {
      final ReaderMetadata metadata = new ReaderMetadata();
      metadata.setUUID( "AFTR" );
      metadata.setName( "AbstractFileTreeReader" );
      metadata.setVersion( "1" );
      metadata.setDescription( "Abstract for reader of files in a directory tree" );
      metadata.setVendor( "ctakes" );
      metadata.setCopyright( "2017" );
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
      private ConfigurationParameterDeclarations mConfigurationParameterDeclarations = new ConfigurationParameterDeclarations_impl();
      private ConfigurationParameterSettings mConfigurationParameterSettings = new ConfigurationParameterSettings_impl();
      private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo( "resourceMetaData", new PropertyXmlInfo[]{ new PropertyXmlInfo( "name", false ), new PropertyXmlInfo( "description" ), new PropertyXmlInfo( "version" ), new PropertyXmlInfo( "vendor" ), new PropertyXmlInfo( "copyright" ), new PropertyXmlInfo( "configurationParameterDeclarations", (String) null ), new PropertyXmlInfo( "configurationParameterSettings", (String) null ) } );

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
            this.validateConfigurationParameterSettings( nvps, (String) null, cfgParamDecls );
         } else {
            Map settingsForGroups = cfgParamSettings.getSettingsForGroups();
            Set entrySet = settingsForGroups.entrySet();
            Iterator it = entrySet.iterator();

            while ( it.hasNext() ) {
               Map.Entry entry = (Map.Entry) it.next();
               String groupName = (String) entry.getKey();
               nvps = (NameValuePair[]) entry.getValue();
               if ( nvps != null ) {
                  this.validateConfigurationParameterSettings( nvps, groupName, cfgParamDecls );
               }
            }
         }

      }

      protected void validateConfigurationParameterSettings( NameValuePair[] aNVPs, String aGroupName, ConfigurationParameterDeclarations aParamDecls ) throws ResourceConfigurationException {
         for ( int i = 0; i < aNVPs.length; ++i ) {
            String name = aNVPs[ i ].getName();
            org.apache.uima.resource.metadata.ConfigurationParameter param = aParamDecls.getConfigurationParameter( aGroupName, name );
            if ( param == null ) {
               if ( aGroupName == null ) {
                  throw new ResourceConfigurationException( "nonexistent_parameter", new Object[]{ name, this.getName() } );
               }

               throw new ResourceConfigurationException( "nonexistent_parameter_in_group", new Object[]{ name, aGroupName, this.getName() } );
            }

            this.validateConfigurationParameterDataTypeMatch( param, aNVPs[ i ] );
         }

      }

      protected void validateConfigurationParameterDataTypeMatch( org.apache.uima.resource.metadata.ConfigurationParameter aParam, NameValuePair aNVP ) throws ResourceConfigurationException {
         String paramName = aParam.getName();
         String paramType = aParam.getType();
         Class valClass = aNVP.getValue().getClass();
         if ( aParam.isMultiValued() ) {
            if ( !valClass.isArray() ) {
               throw new ResourceConfigurationException( "array_required", new Object[]{ paramName, this.getName() } );
            }

            valClass = valClass.getComponentType();
            if ( Array.getLength( aNVP.getValue() ) == 0 && valClass.equals( Object.class ) ) {
               aNVP.setValue( Array.newInstance( this.getClassForParameterType( paramType ), 0 ) );
               return;
            }
         }

         if ( valClass != this.getClassForParameterType( paramType ) ) {
            throw new ResourceConfigurationException( "parameter_type_mismatch", new Object[]{ this.getName(), valClass.getName(), paramName, paramType } );
         }
      }

      protected Class getClassForParameterType( String paramType ) {
         return "String".equals( paramType ) ? String.class : ("Boolean".equals( paramType ) ? Boolean.class : ("Integer".equals( paramType ) ? Integer.class : ("Float".equals( paramType ) ? Float.class : null)));
      }
   }

   static private class FileComparator implements Comparator<File> {
      private final Comparator<String> __delegate = new TextNumberComparator();

      public int compare( final File file1, final File file2 ) {
         return __delegate.compare( file1.getName(), file2.getName() );
      }
   }


}

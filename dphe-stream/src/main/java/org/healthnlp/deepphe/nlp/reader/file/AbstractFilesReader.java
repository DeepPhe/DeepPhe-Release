package org.healthnlp.deepphe.nlp.reader.file;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.NumberedSuffixComparator;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileNotFoundException;


/**
 * Abstract to read files in a tree starting in a root directory.
 * By default, filenames are sorted with {@link NumberedSuffixComparator}.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2017
 */
abstract public class AbstractFilesReader extends DocReader<DirHandler> {

   static private final Logger LOGGER = Logger.getLogger( "AbstractFilesReader" );

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
   static public final String UNICODE = "unicode";
   @ConfigurationParameter(
         name = PARAM_ENCODING,
         description = "The character encoding used by the input files.",
//         defaultValue = UNICODE,
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

   /**
    * Name of configuration parameter that must be set to false to remove windows \r characters
    */
   public static final String PARAM_KEEP_CR = "KeepCR";
   @ConfigurationParameter(
         name = PARAM_KEEP_CR,
         description = "Keep windows-format carriage return characters at line endings." +
                       "  This will only keep existing characters, it will not add them.",
         mandatory = false
   )
   private boolean _keepCrChar = true;

   /**
    * Name of configuration parameter that must be set to true to replace windows "\r\n" sequnces with "\n ".
    * Useful if windows Carriage Return characters wreak havoc upon trained models but text offsets must be preserved.
    * This may not play well with components that utilize double-space sequences.
    */
   public static final String CR_TO_SPACE = "CRtoSpace";
   @ConfigurationParameter(
         name = CR_TO_SPACE,
         description = "Change windows-format CR + LF character sequences to LF + <Space>.",
         mandatory = false
   )
   private boolean _crToSpace = false;


   static protected final String UNKNOWN = "Unknown";
   static protected final String NOT_INITIALIZED = "Not yet initialized";

   private File _rootDir;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      try {
         _rootDir = FileLocator.getFile( _rootDirPath );
      } catch ( FileNotFoundException fnfE ) {
         LOGGER.error( "No Directory found at " + _rootDirPath );
         throw new ResourceInitializationException( fnfE );
      }
      getDocStore().setRootDir( _rootDir );
      final FileHandler fileHandler = getDocStore().getFileHandler();
      fileHandler.createValidExtensions( _explicitExtensions );
      fileHandler.setValidEncoding( _encoding );
      fileHandler.setKeepCrChar( _keepCrChar );
   }


   /**
    * @return the root input directory as a File.
    */
   protected File getRootDir() {
      if ( _rootDir == null ) {
         LOGGER.error( NOT_INITIALIZED );
         return null;
      }
      return _rootDir;
   }


}

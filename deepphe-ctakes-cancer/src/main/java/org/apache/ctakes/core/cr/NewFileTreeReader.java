package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.note.NoteSpecs;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.structured.DocumentIdPrefix;
import org.apache.ctakes.typesystem.type.structured.DocumentPath;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Recursively reads a directory tree of files, sorted by level (root first),
 * creating the DocumentID from the file name and the DocumentIdPrefix by the subdirectory path between
 * the root and the leaf file
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/10/2016
 */
@PipeBitInfo(
      name = "Files in Dir Tree Reader",
      description = "Reads document texts from text files in a directory tree.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.DOCUMENT_ID_PREFIX }
)
final public class NewFileTreeReader extends JCasCollectionReader_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "NewFileTreeReader" );

   private static final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyyMMddhhmm" );

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
   public static final String PARAM_ENCODING = "Encoding";
   @ConfigurationParameter(
         name = PARAM_ENCODING,
         description = "The character encoding used by the input files.",
         mandatory = false
   )
   private String _encoding;

   /**
    * Name of optional configuration parameter that specifies the extensions
    * of the files that the collection reader will read.  Values for this
    * parameter should not begin with a dot <code>'.'</code>.
    */
   public static final String PARAM_EXTENSIONS = "Extensions";
   @ConfigurationParameter(
         name = PARAM_EXTENSIONS,
         description = "The extensions of the files that the collection reader will read." +
                       "  Values for this parameter should not begin with a dot.",
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


   /**
    * The patient id for each note is set using a directory name.
    * By default this is the directory directly under the root directory (PatientLevel=1).
    * This is appropriate for files such as in rootDir=data/, file in data/patientA/Text1.txt
    * It can be set to use directory names at any level below.
    * For instance, using PatientLevel=2 for rootDir=data/, file in data/hospitalX/patientA/Text1.txt
    * In this manner the notes for the same patient from several sites can be properly collated.
    */
   public static final String PATIENT_LEVEL = "PatientLevel";
   @ConfigurationParameter(
         name = PATIENT_LEVEL,
         description = "The level in the directory hierarchy at which patient identifiers exist."
                       + "Default value is 1; directly under root input directory.",
         mandatory = false
   )
   private int _patientLevel = 1;

   static private final Pattern CR_LF = Pattern.compile( "\\r\\n" );

   private File _rootDir;
   private Collection<String> _validExtensions;
   private List<File> _files;
   private Map<File, String> _filePatients;
   private int _currentIndex;
   private Map<String, Integer> _patientDocCounts = new HashMap<>();

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
      if ( _rootDir.isFile() ) {
         // does not check for valid extensions.  With one file just trust the user.
         final String patient = _rootDir.getParentFile().getName();
         _files = Collections.singletonList( _rootDir );
         _filePatients = Collections.singletonMap( _rootDir, patient );
         PatientNoteStore.getInstance().setWantedDocCount( patient, 1 );
      } else {
         // gather all of the files and set the document counts per patient.
         final File[] children = _rootDir.listFiles();
         if ( children == null || children.length == 0 ) {
            _filePatients = Collections.emptyMap();
            _files = Collections.emptyList();
            return;
         }
         if ( Arrays.stream( children ).noneMatch( File::isDirectory ) ) {
            _patientLevel = 0;
         }
         _filePatients = new HashMap<>();
         _files = getDescendentFiles( _rootDir, _validExtensions, 0 );
         _patientDocCounts.forEach( ( k, v ) -> PatientNoteStore.getInstance().setWantedDocCount( k, v ) );
      }
   }

   /**
    * @param explicitExtensions array of file extensions as specified in the uima parameters
    * @return a collection of dot-prefixed extensions or none if {@code explicitExtensions} is null or empty
    */
   static Collection<String> createValidExtensions( final String... explicitExtensions ) {
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
    * @param level           directory level beneath the root directory
    * @return List of files descending from the parent directory
    */
   private List<File> getDescendentFiles( final File parentDir,
                                          final Collection<String> validExtensions,
                                          final int level ) {
      final File[] children = parentDir.listFiles();
      if ( children == null || children.length == 0 ) {
         return Collections.emptyList();
      }
      final Collection<File> childDirs = new ArrayList<>();
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
      for ( File childDir : childDirs ) {
         descendentFiles.addAll( getDescendentFiles( childDir, validExtensions, level + 1 ) );
      }
      if ( level == _patientLevel ) {
         final String patientId = parentDir.getName();
         final int count = _patientDocCounts.getOrDefault( patientId, 0 );
         _patientDocCounts.put( patientId, count + descendentFiles.size() );
         descendentFiles.forEach( f -> _filePatients.put( f, patientId ) );
      }
      return descendentFiles;
   }

   /**
    * @param file            -
    * @param validExtensions -
    * @return true if validExtensions is empty or contains an extension belonging to the given file
    */
   static boolean isExtensionValid( final File file, final Collection<String> validExtensions ) {
      if ( validExtensions.isEmpty() ) {
         return true;
      }
      final String fileName = file.getName();
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) ) {
            if ( fileName.equals( extension ) ) {
               LOGGER.warn( "File " + file.getPath() + " is named as extension " + extension + " ; discarded" );
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
   static String createDocumentID( final File file, final Collection<String> validExtensions ) {
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
   static private String createDocumentIdPrefix( final File file, final File rootDir ) {
      final String parentPath = file.getParent();
      final String rootPath = rootDir.getPath();
      if ( parentPath.equals( rootPath ) || !parentPath.startsWith( rootPath ) ) {
         return "";
      }
      return parentPath.substring( rootPath.length() + 1 );
   }

   /**
    * @param documentId -
    * @return the file name with the longest valid extension removed
    */
   static private String createDocumentType( final String documentId ) {
      final int lastScore = documentId.lastIndexOf( '_' );
      if ( lastScore < 0 || lastScore == documentId.length() - 1 ) {
         return NoteSpecs.ID_NAME_CLINICAL_NOTE;
      }
      return documentId.substring( lastScore + 1 );
   }

   /**
    * @param file -
    * @return the file's last modification date as a string : {@link #DATE_FORMAT}
    */
   static private String createDocumentTime( final File file ) {
      final long millis = file.lastModified();
      return DATE_FORMAT.format( millis );
   }

   /**
    * Gets the total number of documents that will be returned by this
    * collection reader.  This is not part of the general collection reader
    * interface.
    *
    * @return the number of documents in the collection
    */
   public int getNumberOfDocuments() {
      return _files.size();
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
      String docText = readFile( file );
      if ( _crToSpace && !docText.isEmpty() && docText.contains( "\r" ) ) {
         LOGGER.info( "Performing EOL translation ..." );
         docText = CR_LF.matcher( docText ).replaceAll( "\n" );
      }
      if ( !docText.isEmpty() && !docText.endsWith( "\n" ) ) {
         // Make sure that we end with a newline
         docText += "\n";
      }
      jcas.setDocumentText( docText );
      final DocumentID documentId = new DocumentID( jcas );
      final String id = createDocumentID( file, _validExtensions );
      documentId.setDocumentID( id );
      documentId.addToIndexes();
      final DocumentIdPrefix documentIdPrefix = new DocumentIdPrefix( jcas );
      final String idPrefix = createDocumentIdPrefix( file, _rootDir );
      documentIdPrefix.setDocumentIdPrefix( idPrefix );
      documentIdPrefix.addToIndexes();
      final SourceData sourceData = SourceMetadataUtil.getOrCreateSourceData( jcas );
      final String docType = createDocumentType( id );
      sourceData.setNoteTypeCode( docType );
      final String docTime = createDocumentTime( file );
      sourceData.setSourceRevisionDate( docTime );
      final String patientId = _filePatients.get( file );
      SourceMetadataUtil.setPatientIdentifier( jcas, patientId );
      final DocumentPath documentPath = new DocumentPath( jcas );
      documentPath.setDocumentPath( file.getAbsolutePath() );
      documentPath.addToIndexes();
   }

   /**
    * Reads file using a Path and stream.  Failing that it calls {@link #readByBuffer(File)}
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readFile( final File file ) throws IOException {
      LOGGER.info( "Reading " + file.getPath() );
      if ( !_keepCrChar ) {
         try {
            return readByPath( file );
         } catch ( IOException ioE ) {
            // This is a pretty bad way to handle a MalformedInputException, but that can be thrown by the collector
            // in the stream, and java streams and exceptions do not go well together
            LOGGER.warn( "Bad characters in " + file.getPath() );
         }
      }
      return readByBuffer( file );
   }

   /**
    * Reads file using a Path and stream.
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readByPath( final File file ) throws IOException {
      if ( _encoding != null && !_encoding.isEmpty() ) {
         final Charset charset = Charset.forName( _encoding );
         try ( Stream<String> stream = Files.lines( file.toPath(), charset ) ) {
            return stream.collect( Collectors.joining( "\n" ) );
         }
      } else {
         return safeReadByPath( file );
      }
   }

   static private String safeReadByPath( final File file ) throws IOException {
      final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput( CodingErrorAction.IGNORE );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( Files
            .newInputStream( file.toPath() ), decoder ) ) ) {
         return reader.lines().collect( Collectors.joining( "\n" ) );
      }
   }

   /**
    * Reads file using buffered input stream
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readByBuffer( final File file ) throws IOException {
      // Use 8KB as the default buffer size
      byte[] buffer = new byte[ 8192 ];
      final StringBuilder sb = new StringBuilder();
      try ( final InputStream inputStream = new BufferedInputStream( new FileInputStream( file ), buffer.length ) ) {
         while ( true ) {
            final int length = inputStream.read( buffer );
            if ( length < 0 ) {
               break;
            }
            if ( _encoding != null ) {
               sb.append( new String( buffer, 0, length, _encoding ) );
            } else {
               sb.append( new String( buffer, 0, length ) );
            }
         }
      } catch ( FileNotFoundException fnfE ) {
         throw new IOException( fnfE );
      }
      return sb.toString();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void close() throws IOException {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Progress[] getProgress() {
      return new Progress[] {
            new ProgressImpl( _currentIndex, _files.size(), Progress.ENTITIES )
      };
   }


   /**
    * Convenience method to create a reader with an input directory
    *
    * @param inputDirectory -
    * @return new reader
    * @throws ResourceInitializationException -
    */
   public static CollectionReader createReader( final String inputDirectory ) throws ResourceInitializationException {
      return CollectionReaderFactory.createReader( FileTreeReader.class,
            ConfigParameterConstants.PARAM_INPUTDIR,
            inputDirectory );
   }

   @Test
   public void testCrLfMatcher() {
      final String CrLfText
            = "Hello,\r\n\tit is nice to meet you!\r\n  I hope that we can become good friends.  \r\nBye\r\n\r\n";
      final String LfText = "Hello,\n\tit is nice to meet you!\n  I hope that we can become good friends.  \nBye\n\n";
      Assert.assertEquals( CR_LF.matcher( CrLfText ).replaceAll( "\n" ), LfText );
   }

}

package org.healthnlp.deepphe.nlp.reader.file;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/19/2020
 */
abstract public class AbstractFileHandler implements FileHandler {


   final private List<File> _files = new ArrayList<>();

   final private MutableDocStore _docStore = new MutableDocStore();

   private String _encoding;
   private Collection<String> _validExtensions;
   private boolean _keepCrChar = true;


   protected boolean isWantedFile( final File file ) {
      return isExtensionValid( file ) && file.canRead() && !file.isHidden();
   }

   /**
    * @param file -
    * @return true if validExtensions is empty or contains an extension belonging to the given file
    */
   final protected boolean isExtensionValid( final File file ) {
      final Collection<String> validExtensions = getValidExtensions();
      if ( validExtensions.isEmpty() ) {
         return true;
      }
      final String fileName = file.getName();
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) ) {
            return !fileName.equals( extension );
         }
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean addFile( final File file ) throws IOException {
      if ( !isWantedFile( file ) ) {
         return false;
      }
      _files.add( file );
      _docStore.addDocs( createDocs( file ) );
      return true;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getFileCount() {
      return _files.size();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void setValidEncoding( final String encoding ) {
      _encoding = encoding;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getValidEncoding() {
      if ( _encoding == null || _encoding.isEmpty() ) {
         return UNKNOWN_ENCODING;
      }
      return _encoding;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getValidExtensions() {
      if ( _validExtensions == null ) {
         return Collections.emptyList();
      }
      return _validExtensions;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> createValidExtensions( final String... explicitExtensions ) {
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
      _validExtensions = validExtensions;
      return _validExtensions;
   }


   final public void setKeepCrChar( final boolean keepCrChar ) {
      _keepCrChar = keepCrChar;
   }

   final public boolean isKeepCrChar() {
      return _keepCrChar;
   }

   ///////////////////  DocStore  ///////////////////

   /**
    * {@inheritDoc}
    */
   @Override
   public Doc getDoc( final int index ) {
      return _docStore.getDoc( index );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getDocCount() {
      return _docStore.getDocCount();
   }


}

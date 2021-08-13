package org.healthnlp.deepphe.summary.attribute.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public enum DebugHelper {
   INSTANCE;

   static public DebugHelper getInstance() {
      return INSTANCE;
   }

   private File _outputFile = new File( "C:/Spiffy/output/dphe_output/dphe_5/DebugHelper.txt" );
   private Map<String,List<AttributeDebugObject>> _debugObjects = new HashMap<>();


   public File getOutputFile() {
      return _outputFile;
   }
   public void setOutputFile( final File outputFile ) {
      _outputFile = outputFile;
   }
   public void addDebugObject( final String patientId, final AttributeDebugObject attributeDebugObject ) {
      _debugObjects.computeIfAbsent( patientId, p -> new ArrayList<>() )
                   .add( attributeDebugObject );
   }
   public void clear() {
      _debugObjects.clear();
   }
   public void saveIncorrectAttributes() {
      try ( Writer writer = new BufferedWriter( new FileWriter( _outputFile ) ) ) {
         writer.write( _debugObjects.entrySet()
                      .stream()
                      .map( e -> getIncorrectText( e.getKey(), e.getValue() ) )
                      .collect( Collectors.joining() ) );
      } catch ( IOException ioE ) {
         Logger.getLogger( this.getClass() ).error( ioE.getMessage() );
      }
   }
   private String getIncorrectText( final String patientId, final List<AttributeDebugObject> attributeDebugObjects ) {
      final boolean isCorrect = attributeDebugObjects.stream().allMatch( AttributeDebugObject::isCorrect );
      if ( isCorrect ) {
         return "";
      }
      return patientId + "\n" + attributeDebugObjects.stream()
                                                     .map( AttributeDebugObject::getIncorrectText )
                                                     .filter( t -> !t.isEmpty() )
                                                     .collect( Collectors.joining( "\n" ) ) + "\n";
   }

   public String toString() {
      return _debugObjects.entrySet()
                          .stream()
                          .map( d -> d.getKey() + " " + d.getValue()
                                                         .stream()
                                                         .map( AttributeDebugObject::toString )
                                                         .collect( Collectors.joining() ) )
                          .collect( Collectors.joining() );
   }


   static public class AttributeDebugObject {
      final private String _name;
      final private String _systemCode;
      final private String _systemUri;
      final private String _goldCode;
      final private List<UriDebugObject> _uriDebugObjects = new ArrayList<>();
      public AttributeDebugObject( final String name, final String systemCode, final String systemUri, final String goldCode ) {
         _name = name;
         _systemCode = systemCode;
         _systemUri = systemUri;
         _goldCode = goldCode;
      }
      public boolean isCorrect() {
         return _systemCode.equalsIgnoreCase( _goldCode );
      }
      public boolean hasGoldCode() {
         return isCorrect()
                || _uriDebugObjects.stream()
                                   .map( u -> u._codes )
                                   .flatMap( Collection::stream )
                                   .anyMatch( c -> c.equalsIgnoreCase( _goldCode ) );
      }
      public void addUriDebugObject( final UriDebugObject uriDebugObject ) {
         _uriDebugObjects.add( uriDebugObject );
      }
      static private String line( final String line ) {
         return "   " + line + "\n";
      }
      static private String lines( final Collection<String> lines ) {
         return String.join("   \n   ", lines ) + "\n";
      }
      private String getIncorrectText() {
         return isCorrect() ? "" : toString();
      }
      public String toString() {
         return line( _name + "  " + isCorrect()
                      + "  System: " + _systemCode + "  " + _systemUri
                      + "  Gold: " + _goldCode + "  " + hasGoldCode() )
                + lines( _uriDebugObjects.stream()
                             .map( UriDebugObject::toString )
                             .collect( Collectors.toList() ) );
      }
    }


   static public class UriDebugObject {
      final private String _uri;
      final private int _strength;
      final private List<String> _texts = new ArrayList<>();
      final private List<String> _codes = new ArrayList<>();
      public UriDebugObject( final String uri, final int strength ) {
         _uri = uri;
         _strength = strength;
      }
      public void addText( final String text ) {
         _texts.add( text );
      }
      public void addCode( final String code ) {
         _codes.add( code );
      }
      static private String line( final String line ) {
         return "      " + line + "\n";
      }
      static private String lines( final Collection<String> lines ) {
         return "      " + String.join("\n      ", lines ) + "\n";
      }
      public String toString() {
         return line( _uri + " = " + _strength )
                + lines( _texts )
                + lines( _codes );
      }
   }


}

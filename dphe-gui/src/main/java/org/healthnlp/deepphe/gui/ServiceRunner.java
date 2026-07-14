package org.healthnlp.deepphe.gui;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The long-lived, stoppable analog of {@link org.apache.ctakes.core.util.external.SystemUtil.CommandRunner}.
 * <p>
 * {@code CommandRunner} runs a command to completion (or fire-and-forget) and never hands back the
 * {@link Process}, so it cannot drive a Startup / Shutdown toggle.  A {@code ServiceRunner} keeps the
 * process, drains its (merged) output to a log file, optionally stops it on JVM exit, and exposes
 * {@link #start()}, {@link #isAlive()} and {@link #stop(long)}.  It mirrors {@code CommandRunner}'s
 * configuration idiom: {@link #setDirectory}, {@link #setLogFile}, {@link #addEnvVar},
 * {@link #stopOnExit} and {@link #setSetJavaHome}.
 * <p>
 * Two deliberate differences from {@code CommandRunner}: a {@code ServiceRunner} is launched from an
 * argument list rather than a shell command line, so executable paths that contain spaces need no
 * quoting; and the Java environment ({@code JAVA_HOME} / {@code CTAKES_HOME} / {@code CLASSPATH}) is
 * added only when {@link #setSetJavaHome} is enabled, so non-Java services (Node, native binaries)
 * are not polluted with it.
 *
 * @author chip-nlp
 */
final class ServiceRunner {

   static private final Logger LOGGER = Logger.getLogger( "ServiceRunner" );
   static private final long FORCE_STOP_SECONDS = 5L;

   private final String _name;
   private final List<String> _command;
   private final Map<String, String> _environment = new LinkedHashMap<>();
   private File _directory = null;
   private File _logFile = null;
   private Runnable _onExit = null;
   private boolean _setJavaHome = false;
   private boolean _stopOnExit = false;
   private volatile Process _process = null;
   private Thread _shutdownHook = null;

   /**
    * @param name    human-readable service name, used in log messages.
    * @param command the executable followed by its arguments.
    */
   ServiceRunner( final String name, final List<String> command ) {
      _name = name;
      _command = new ArrayList<>( command );
   }

   void setDirectory( final File directory ) {
      _directory = directory;
   }

   void setLogFile( final File logFile ) {
      _logFile = logFile;
   }

   void addEnvVar( final String key, final String value ) {
      _environment.put( key, value );
   }

   /** When enabled, {@code JAVA_HOME} / {@code CTAKES_HOME} / {@code CLASSPATH} are added.  Off by default. */
   void setSetJavaHome( final boolean setJavaHome ) {
      _setJavaHome = setJavaHome;
   }

   /** When enabled, a JVM shutdown hook stops the process on exit.  Off by default. */
   void stopOnExit( final boolean stopOnExit ) {
      _stopOnExit = stopOnExit;
   }

   /** {@code onExit} is run once the process ends and its output is drained.  It runs off the EDT. */
   void setOnExit( final Runnable onExit ) {
      _onExit = onExit;
   }

   /** Starts the process.  A no-op if it is already running. */
   synchronized void start() throws IOException {
      if ( isAlive() ) {
         return;
      }
      final ProcessBuilder processBuilder = new ProcessBuilder( _command );
      if ( _directory != null ) {
         processBuilder.directory( _directory );
      }
      processBuilder.redirectErrorStream( true );
      applyEnvironment( processBuilder.environment() );
      prepareLogFile();
      LOGGER.info( "Starting " + _name + " ..." );
      LOGGER.info( String.join( " ", _command ) );
      final Process process = processBuilder.start();
      _process = process;
      if ( _stopOnExit ) {
         registerShutdownHook();
      }
      streamOutput( process );
   }

   synchronized boolean isAlive() {
      final Process process = _process;
      return process != null && process.isAlive();
   }

   /** Stops the process gracefully, forcing it after {@code timeoutSeconds}, and clears the JVM-exit hook. */
   synchronized void stop( final long timeoutSeconds ) {
      final Process process = _process;
      _process = null;
      removeShutdownHook();
      if ( process == null || !process.isAlive() ) {
         return;
      }
      LOGGER.info( "Stopping " + _name + " ..." );
      process.destroy();
      try {
         if ( !process.waitFor( timeoutSeconds, TimeUnit.SECONDS ) ) {
            LOGGER.warn( _name + " did not stop cleanly; forcing shutdown." );
            process.destroyForcibly();
            process.waitFor( FORCE_STOP_SECONDS, TimeUnit.SECONDS );
         }
      } catch ( InterruptedException intE ) {
         Thread.currentThread().interrupt();
         LOGGER.error( "Interrupted while stopping " + _name + ".", intE );
      }
   }

   private void applyEnvironment( final Map<String, String> environment ) {
      if ( _setJavaHome ) {
         final String javaHome = System.getProperty( "java.home" );
         if ( javaHome != null && !javaHome.isEmpty() ) {
            environment.put( "JAVA_HOME", javaHome );
         }
         if ( !environment.containsKey( "CTAKES_HOME" ) ) {
            String ctakesHome = System.getenv( "CTAKES_HOME" );
            if ( ctakesHome == null || ctakesHome.isEmpty() ) {
               ctakesHome = System.getProperty( "user.dir" );
            }
            environment.put( "CTAKES_HOME", ctakesHome );
         }
         if ( !environment.containsKey( "CLASSPATH" ) ) {
            final String classPath = System.getProperty( "java.class.path" );
            if ( classPath != null && !classPath.isEmpty() ) {
               environment.put( "CLASSPATH", classPath );
            }
         }
      }
      for ( String propertyName : System.getProperties().stringPropertyNames() ) {
         if ( propertyName.startsWith( "ctakes.env." ) ) {
            environment.put( propertyName.substring( "ctakes.env.".length() ),
                             System.getProperty( propertyName ) );
         }
      }
      environment.putAll( _environment );
   }

   private void prepareLogFile() {
      if ( _logFile == null ) {
         return;
      }
      final File parent = _logFile.getParentFile();
      if ( parent != null ) {
         parent.mkdirs();
      }
      LOGGER.info( "Writing " + _name + " output to " + _logFile.getPath() );
   }

   private void streamOutput( final Process process ) {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( () -> {
         try {
            drainOutput( process );
            final int exitCode = process.waitFor();
            LOGGER.info( _name + " stopped with exit code " + exitCode + "." );
         } catch ( IOException ioE ) {
            LOGGER.error( "Could not read " + _name + " output.", ioE );
         } catch ( InterruptedException intE ) {
            Thread.currentThread().interrupt();
            LOGGER.error( "Interrupted while waiting for " + _name + ".", intE );
         } finally {
            executor.shutdown();
            final Runnable onExit = _onExit;
            if ( onExit != null ) {
               onExit.run();
            }
         }
      } );
   }

   private void drainOutput( final Process process ) throws IOException {
      try ( BufferedReader reader = new BufferedReader(
            new InputStreamReader( process.getInputStream(), StandardCharsets.UTF_8 ) ) ) {
         if ( _logFile == null ) {
            logLines( reader, null );
            return;
         }
         try ( BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(
               new FileOutputStream( _logFile, false ), StandardCharsets.UTF_8 ) ) ) {
            logLines( reader, writer );
         }
      }
   }

   private void logLines( final BufferedReader reader, final BufferedWriter writer ) throws IOException {
      String line = reader.readLine();
      while ( line != null ) {
         if ( writer != null ) {
            writer.write( line );
            writer.newLine();
            writer.flush();
         }
         LOGGER.info( "[" + _name + "] " + line );
         line = reader.readLine();
      }
   }

   private void registerShutdownHook() {
      removeShutdownHook();
      final Thread hook = new Thread( () -> stop( FORCE_STOP_SECONDS ) );
      Runtime.getRuntime().addShutdownHook( hook );
      _shutdownHook = hook;
   }

   private void removeShutdownHook() {
      final Thread hook = _shutdownHook;
      _shutdownHook = null;
      if ( hook == null ) {
         return;
      }
      try {
         Runtime.getRuntime().removeShutdownHook( hook );
      } catch ( IllegalStateException isE ) {
         // The JVM is already shutting down; the hook is running or has run.
      }
   }

}

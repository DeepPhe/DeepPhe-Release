package org.healthnlp.deepphe.gui;

import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;
import org.neo4j.kernel.impl.store.kvstore.RotationTimeoutException;
import org.neo4j.kernel.lifecycle.LifecycleException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author SPF , chip-nlp
 * @since {10/12/2022}
 */
public class DesktopMainPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "DeepPhe Desktop" );

   static private final String DPHE_NAME = "Patient Phenotype Summarizer";
   static private final String VIZ_NAME = "DeepPhe Visualizer";
   static private final String NO_VALUE = "NO_PARAMETER_VALUE_PROVIDED";
   private final Map<String,String> _parameterMap = new HashMap<>();
   private String _parameterFile;
   private JButton _dpheButton;
   private JButton _vizButton;
   private boolean _stop;

   DesktopMainPanel() {
      super( new BorderLayout() );
      add( createToolBar(), BorderLayout.NORTH );
      add( LoggerPanel.createLoggerPanel(), BorderLayout.CENTER );
      SwingUtilities.invokeLater( new ButtonIconLoader() );
   }

   public void readParameterFile( final String... args ) {
      if ( args.length != 1 ) {
         logBadArgs( args );
         return;
      }
      _parameterFile = args[ 0 ];
      final File parmFile = new File( _parameterFile );
      if ( !parmFile.canRead() ) {
         LOGGER.error( "Cannot read parameter file." );
         LOGGER.error( "Please exit the application and correct your parameter file: " + _parameterFile );
         return;
      }
      try ( BufferedReader reader = new BufferedReader( new FileReader( args[0] ) ) ) {
         String line = "";
         while ( line != null ) {
            if ( line.isEmpty() || line.startsWith( "//" ) ) {
               line = reader.readLine();
               continue;
            }
            final int equals = line.indexOf( '=' );
            if ( equals <= 0 ) {
               LOGGER.warn( "Invalid line: " + line );
               line = reader.readLine();
               continue;
            }
            _parameterMap.put( line.substring( 0, equals ).trim().toUpperCase(), line.substring( equals+1 ).trim() );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      // Register Shutdown hooks first so that an exit by any subsequent bad parameter causes the server to stop.
      registerShutdownHook( "Neo4j", getParameter( "StopNeo4j" ), getParameter( "Neo4jDir" ) );
      if ( _stop ) {
         return;
      }
      registerShutdownHook( "DeepPhe Viz", getParameter( "StopViz", "StopVis" ), getParameter( "VizDir", "VisDir" ) );
      if ( _stop ) {
         return;
      }
      startNeo4j( getParameter( "StartNeo4j" ), getParameter( "Neo4jDir" ) );
      if ( _stop ) {
         return;
      }
      _dpheButton.addActionListener( new StartAction( DPHE_NAME,
                                                      getParameter( "StartDphe", "StartDeepPhe" ),
                                                      getParameter( "DpheDir", "DeepPheDir" ) ) );
      if ( _stop ) {
         return;
      }
      _vizButton.addActionListener( new StartAction( VIZ_NAME,
                                                     getParameter( "StartViz", "StartVis" ),
                                                     getParameter( "VizDir", "VisDir" ) ) );
   }

   private String getParameter( final String... names ) {
      final String value = Arrays.stream( names )
                                 .map( String::toUpperCase )
                                 .map( _parameterMap::get )
                                 .filter( Objects::nonNull ).findAny()
                                 .orElse( NO_VALUE );
      if ( value.equals( NO_VALUE ) ) {
         LOGGER.error( "No Parameter Value specified for " + String.join( ", ", names ) );
         LOGGER.error( "Please exit the application and correct your parameter file: " + _parameterFile );
         _stop = true;
      }
      return value;
   }

   static private void logBadArgs( final String... args ) {
      if ( args.length > 1 ) {
         LOGGER.error( "There are too many arguments in " + String.join( " ", args ) );
      }
      LOGGER.error( "A single argument pointing to a File containing run parameters is required." );
      LOGGER.info( "" );
      LOGGER.info( "Each line in the file should have the format:" );
      LOGGER.info( "Name=Value" );
      LOGGER.info( "" );
      LOGGER.info( "The required values are:" );
      LOGGER.info( "StartNeo4j" );
      LOGGER.info( "Neo4jDir" );
      LOGGER.info( "StopNeo4j" );
      LOGGER.info( "DpheDir or DeepPheDir" );
      LOGGER.info( "StartDphe or StartDeepPhe" );
//      LOGGER.info( "StopDphe or StopDeepPhe" );
      LOGGER.info( "VizDir or VisDir" );
      LOGGER.info( "StartViz or StartVis" );
//      LOGGER.info( "StopViz or StopVis" );
      LOGGER.info( "" );
      LOGGER.error( "Please restart the Application with a single argument pointing to a parameter file." );
   }


   private JToolBar createToolBar() {
      final JToolBar toolBar = new JToolBar();
      toolBar.setFloatable( false );
      toolBar.setRollover( true );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      _dpheButton = addButton( toolBar, DPHE_NAME );
      _dpheButton.setEnabled( false );
      toolBar.addSeparator( new Dimension( 50, 0 ) );
      _vizButton = addButton( toolBar, VIZ_NAME );
      _vizButton.setEnabled( false );

//      toolBar.addSeparator( new Dimension( 50, 0 ) );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      return toolBar;
   }

   static private JButton addButton( final JToolBar toolBar, final String toolTip ) {
      final JButton button = new JButton();
      button.setFocusPainted( false );
      // prevents first button from having a painted border
      button.setFocusable( false );
      button.setToolTipText( toolTip );
      toolBar.add( button );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      return button;
   }

   private void startNeo4j( final String command, final String dir ) {
      if ( _stop ) {
         return;
      }
      final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( command );
//      final StoppableCommandRunner runner = new StoppableCommandRunner( command );
      runner.setLogger( LOGGER );
//      runner.setStopOnExit( true );
      if ( dir != null && !dir.isEmpty() ) {
         runner.setDirectory( dir );
      }
      LOGGER.info( "Starting Neo4j Server  ..." );
      try {
         SystemUtil.run( runner );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }


   private final class StartAction implements ActionListener {
      private final String _name;
      private final String _command;
      private final String _dir;

      private StartAction( final String name, final String command, final String dir ) {
         _name = name;
         _command = command;
         _dir = dir;
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _dpheButton == null ) {
            return;
         }
         final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( _command );
         runner.setLogger( LOGGER );
         if ( _dir != null && !_dir.isEmpty() ) {
            runner.setDirectory( _dir );
         }
         LOGGER.info( "Starting " + _name + " ..." );
         try {
            SystemUtil.run( runner );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
      }
   }


   /**
    * Simple Startable that loads an icon
    *
    * Some icons <a href="https://www.freepik.com/free-vector/no-entry-hand-sign-isolated-white_10601278.htm#query=stop%20hand&position=1&from_view=keyword">Image by macrovector</a> on Freepik
    */
   private final class ButtonIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/healthnlp/deepphe/desktop/icon/";
         final String dphePng = "StartDphe360.png";
         final String vizPng = "StartVizSmall.png";

         final Icon dpheIcon = IconLoader.loadIcon( dir + dphePng );
         final Icon vizIcon = IconLoader.loadIcon( dir + vizPng );
         _dpheButton.setIcon( dpheIcon );
         _dpheButton.setEnabled( true );
         _vizButton.setIcon( vizIcon );
         _vizButton.setEnabled( true );
      }
   }



   /**
    * Registers a shutdown hook for the Neo4j instance so that it shuts down nicely when the VM exits.
    * This includes kill signals and user actions like "Ctrl-C".
    */
   private void registerShutdownHook( final String name, final String command, final String dir ) {
      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
         try {
            final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( command );
            runner.setLogger( LOGGER );
            //runner.wait( true );
            if ( dir != null && !dir.isEmpty() ) {
               runner.setDirectory( dir );
            }
            LOGGER.info( "Stopping " + name + " Server  ..." );
            try {
               SystemUtil.run( runner );
            } catch ( IOException ioE ) {
               LOGGER.error( ioE.getMessage() );
            }
         } catch ( LifecycleException | RotationTimeoutException multE ) {
            LOGGER.error( "Could not stop " + name + " Server.", multE );
         }
      } ) );

   }





//   public static class StoppableCommandRunner extends SystemUtil.CommandRunner {
//      private String _command;
//      private String _dir;
//      private String _outLog;
//      private String _errLog;
//      private Logger _logger;
//      private boolean _wait;
//      private boolean _stopOnExit;
//
//      public StoppableCommandRunner(String command) {
//         super( command );
//         _command = command;
//      }
//
//      public void setStopOnExit( final boolean stopOnExit ) {
//         _stopOnExit = stopOnExit;
//      }
//
//      public void setDirectory(String directory) {
//         super.setDirectory( directory );
//         this._dir = directory;
//      }
//
//      public void setLogger(Logger logger) {
//         super.setLogger( logger );
//         this._logger = logger;
//      }
//
//      public void setLogFiles(String outLog, String errLog) {
//         super.setLogFiles( outLog, errLog );
//         this._outLog = outLog;
//         this._errLog = errLog;
//      }
//
//      public void wait(boolean wait) {
//         super.wait(wait);
//         this._wait = wait;
//      }
//
//      private String getDefaultLogFile() {
//         Random randomizer = new Random();
//         int spaceIndex = this._command.indexOf(32);
//         return spaceIndex < 0 ? this._command + ".ctakes.log." + randomizer.nextLong() : this._command.substring(0, spaceIndex) + ".ctakes.log." + randomizer.nextLong();
//      }
//
//      private static void ensureEnvironment(ProcessBuilder processBuilder) {
//         Map<String, String> env = processBuilder.environment();
//         System.getProperties().stringPropertyNames().stream().filter((n) -> {
//            return n.startsWith("ctakes.env.");
//         }).forEach((n) -> {
//            String var10000 = (String)env.put(n.substring("ctakes.env.".length()), System.getProperty(n));
//         });
//         if (!env.containsKey("JAVA_HOME")) {
//            env.put("JAVA_HOME", System.getProperty("java.home"));
//         }
//
//         String classpath;
//         if (!env.containsKey("CTAKES_HOME")) {
//            classpath = System.getenv("CTAKES_HOME");
//            if (classpath == null || classpath.isEmpty()) {
//               classpath = System.getProperty("user.dir");
//            }
//
//            env.put("CTAKES_HOME", classpath);
//         }
//
//         if (!env.containsKey("CLASSPATH")) {
//            classpath = System.getProperty("java.class.path");
//            if (classpath != null && !classpath.isEmpty()) {
//               env.put("CLASSPATH", classpath);
//            }
//         }
//
//      }
//
//      public Boolean call() throws IOException, InterruptedException {
//         String command = this._command;
//         if (this._logger == null) {
//            if (this._outLog != null && !this._outLog.isEmpty()) {
//               command = command + " > " + this._outLog + " 2>&1";
//            } else {
//               command = command + " > " + this.getDefaultLogFile() + " 2>&1";
//            }
//         }
//
//         String cmd = "cmd.exe";
//         String cmdOpt = "/c";
//         String os = System.getProperty("os.name");
//         if (os.toLowerCase().contains("windows")) {
//            command = command.replace('/', '\\');
//         } else {
//            cmd = "bash";
//            cmdOpt = "-c";
//         }
//
//         ProcessBuilder processBuilder = new ProcessBuilder( cmd, cmdOpt, command );
////         ProcessBuilder processBuilder = new ProcessBuilder( command );
//         if (this._dir != null && !this._dir.isEmpty()) {
//            File dir = new File(this._dir);
//            if (!dir.exists()) {
//               dir.mkdirs();
//            }
//
//            processBuilder.directory(dir);
//         }
//
//         ensureEnvironment(processBuilder);
//         Process process = processBuilder.start();
//         if ( _stopOnExit ) {
//            registerShutdownHook( process );
//         }
//         if (this._logger != null) {
//            ExecutorService executors = Executors.newFixedThreadPool( 2 );
//            executors.submit(new OutputLogger(process, this._logger));
//            executors.submit(new ErrorLogger(process, this._logger));
//         }
//
//         return !this._wait || process.waitFor() == 0;
//      }
//
//      /**
//       * Registers a shutdown hook for the Neo4j instance so that it shuts down nicely when the VM exits.
//       * This includes kill signals and user actions like "Ctrl-C".
//       */
//      private void registerShutdownHook( final Process process ) {
//         Runtime.getRuntime().addShutdownHook( new Thread( () -> {
//            try {
//               process.destroy();
//               process.waitFor();
//             } catch ( InterruptedException multE ) {
//               LOGGER.error( "Could not stop process.", multE );
//            }
//         } ) );
//      }
//   }
//
//   private static class ErrorLogger implements Runnable {
//      private final InputStream _error;
//      private final Logger _logger;
//
//      private ErrorLogger(Process process, Logger logger) {
//         this._error = process.getErrorStream();
//         this._logger = logger;
//      }
//
//      public void run() {
//         try {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(this._error));
//            Throwable var2 = null;
//
//            try {
//               Stream var10000 = reader.lines();
//               Logger var10001 = this._logger;
//               var10000.forEach(var10001::error);
//            } catch (Throwable var12) {
//               var2 = var12;
//               throw var12;
//            } finally {
//               if (reader != null) {
//                  if (var2 != null) {
//                     try {
//                        reader.close();
//                     } catch (Throwable var11) {
//                        var2.addSuppressed(var11);
//                     }
//                  } else {
//                     reader.close();
//                  }
//               }
//
//            }
//         } catch (IOException var14) {
//            this._logger.error(var14.getMessage());
//         }
//
//      }
//   }
//
//   private static class OutputLogger implements Runnable {
//      private final InputStream _output;
//      private final Logger _logger;
//
//      private OutputLogger(Process process, Logger logger) {
//         this._output = process.getInputStream();
//         this._logger = logger;
//      }
//
//      public void run() {
//         try {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(this._output));
//            Throwable var2 = null;
//
//            try {
//               Stream var10000 = reader.lines();
//               Logger var10001 = this._logger;
//               var10000.forEach(var10001::info);
//            } catch (Throwable var12) {
//               var2 = var12;
//               throw var12;
//            } finally {
//               if (reader != null) {
//                  if (var2 != null) {
//                     try {
//                        reader.close();
//                     } catch (Throwable var11) {
//                        var2.addSuppressed(var11);
//                     }
//                  } else {
//                     reader.close();
//                  }
//               }
//
//            }
//         } catch (IOException var14) {
//            this._logger.error(var14.getMessage());
//         }
//
//      }
//   }



}


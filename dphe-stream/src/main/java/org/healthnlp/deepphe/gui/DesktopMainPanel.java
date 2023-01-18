package org.healthnlp.deepphe.gui;

import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;
import org.neo4j.kernel.impl.store.kvstore.RotationTimeoutException;
import org.neo4j.kernel.lifecycle.LifecycleException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

/**
 * @author SPF , chip-nlp
 * @since {10/12/2022}
 */
public class DesktopMainPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "DeepPhe Desktop" );

   static private final String DPHE_NAME = "Patient Phenotype Summarizer";
   static private final String VIZ_NAME = "DeepPhe Visualization Tool";
   static private final String HELP_NAME = "DeepPhe Web Site (Help)";
   static private final String NO_VALUE = "NO_PARAMETER_VALUE_PROVIDED";
   private static final String HTTPS_DEEPPHE_GITHUB_IO = "https://deepphe.github.io/";
   private final Map<String,String> _parameterMap = new HashMap<>();
   private String _parameterFile;
   private JButton _dpheButton;
   private JButton _vizButton;
   private JButton _helpButton;
   private boolean _stop;


   DesktopMainPanel() {
      super( new BorderLayout() );
      add( createToolBar(), BorderLayout.NORTH );
      add( createLogPanel(), BorderLayout.CENTER );
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
      // We should do something with the summarizer.  If neo4j is stopped then a running summarizer will fail.
//      registerShutdownHook( "DeepPhe Summarizer", getParameter( "StopSum", "StopSum" ),
//                            getParameter( "SumDir", "SumDir" ) );
//      if ( _stop ) {
//         return;
//      }
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
      if ( _stop ) {
         return;
      }
      _helpButton.addActionListener( new HelpAction() );
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
      LOGGER.info( "VizDir or VisDir" );
      LOGGER.info( "StartViz or StartVis" );
      LOGGER.info( "" );
      LOGGER.error( "Please restart the Application with a single argument pointing to a parameter file." );
   }


   private JToolBar createToolBar() {
      final JToolBar toolBar = new JToolBar();
      toolBar.setFloatable( false );
      toolBar.setRollover( true );
      toolBar.addSeparator( new Dimension( 50, 0 ) );
      _dpheButton = addButton( toolBar, DPHE_NAME );
      toolBar.addSeparator( new Dimension( 100, 0 ) );
      _vizButton = addButton( toolBar, VIZ_NAME );
      toolBar.addSeparator( new Dimension( 100, 0 ) );
      _helpButton = addButton( toolBar, HELP_NAME );
      toolBar.addSeparator( new Dimension( 50, 0 ) );
      return toolBar;
   }

   static private JButton addButton( final JToolBar toolBar, final String toolTip ) {
      final JButton button = new JButton();
      button.setFocusPainted( false );
      // prevents first button from having a painted border
      button.setFocusable( false );
      button.setToolTipText( toolTip );
      button.setHorizontalTextPosition( SwingConstants.CENTER );
      button.setVerticalTextPosition( SwingConstants.BOTTOM );
      button.setFont( new Font(Font.SERIF, Font.BOLD, 18 ) );
      button.setText( toolTip );
      toolBar.add( button );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      return button;
   }

   private void startNeo4j( final String command, final String dir ) {
      if ( _stop ) {
         return;
      }
      final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( command );
      runner.setLogger( LOGGER );
      if ( dir != null && !dir.isEmpty() ) {
         runner.setDirectory( dir );
      }
      //runner.stopOnExit( true );
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
      private boolean _paused = false;

      private StartAction( final String name, final String command, final String dir ) {
         _name = name;
         _command = command;
         _dir = dir;
      }

      @Override
      synchronized public void actionPerformed( final ActionEvent event ) {
         if ( _dpheButton == null || _paused ) {
            return;
         }
         _paused = true;
         final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( _command );
         runner.setLogger( LOGGER );
         if ( _dir != null && !_dir.isEmpty() ) {
            runner.setDirectory( _dir );
         }
         LOGGER.info( "Starting " + _name + " ..." );
         LOGGER.info( "\n     Initializing may require several seconds.\n     Please Wait.\n" );
         try {
            SystemUtil.run( runner );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
         Executors.newSingleThreadScheduledExecutor()
                  .schedule( () -> { _paused = false; }, 10, TimeUnit.SECONDS );
      }
   }

   private final class HelpAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _helpButton == null ) {
            return;
         }
         SystemUtil.openWebPage( HTTPS_DEEPPHE_GITHUB_IO );
      }
   }


   /**
    * Simple Runnable that loads icons
    *
    */
   private final class ButtonIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/healthnlp/deepphe/desktop/icon/";
         final String dphePng = "StartPFS_144_4.png";
         final String vizPng = "StartViz_144_3.png";
         final String helpPng = "Info_144_3.png";
         final Icon dpheIcon = IconLoader.loadIcon( dir + dphePng );
         final Icon vizIcon = IconLoader.loadIcon( dir + vizPng );
         final Icon helpIcon = IconLoader.loadIcon( dir + helpPng );
         _dpheButton.setIcon( dpheIcon );
         _vizButton.setIcon( vizIcon );
         _helpButton.setIcon( helpIcon );
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
            if ( dir != null && !dir.isEmpty() ) {
               runner.setDirectory( dir );
            }
            LOGGER.info( "Stopping " + name + " ..." );
            try {
               SystemUtil.run( runner );
            } catch ( IOException ioE ) {
               LOGGER.error( ioE.getMessage() );
            }
         } catch ( LifecycleException | RotationTimeoutException multE ) {
            LOGGER.error( "Could not stop " + name + ".", multE );
         }
      } ) );
   }

   public void popHello() {
      JOptionPane.showMessageDialog( this,
                                     "Welcome to the DeepPhe Desktop.\n"
                             + "Use the buttons at the top to process data, display "
                             + "results, or get help.\n"
                             + "At this time the Neo4j Server is being started for "
                             + "use by DeepPhe.\n"
                             + "It will be ready when the log states:\n"
                             + "... Remote interface available ...",
                                     "Welcome to DeepPhe Desktop",
                                     INFORMATION_MESSAGE );
   }

   static private JComponent createLogPanel() {
      final JPanel panel = new JPanel( new BorderLayout() );
      panel.setBorder( new EmptyBorder( 20, 5, 5, 5 ) );
      final JLabel label = new JLabel( "Desktop Activity Log:" );
      label.setFont( new Font(Font.DIALOG, Font.PLAIN, 14 ) );
      label.setBorder( new EmptyBorder( 5, 20, 5, 5 ) );
      panel.add( label, BorderLayout.NORTH );
      panel.add( LoggerPanel.createLoggerPanel(), BorderLayout.CENTER );
      return panel;
   }

}


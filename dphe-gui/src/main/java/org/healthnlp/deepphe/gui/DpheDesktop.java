package org.healthnlp.deepphe.gui;

import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.util.ParameterHandler;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.healthnlp.deepphe.gui.DpheDesktop.ProjectParameter.*;


/**
 * @author SPF , chip-nlp
 * @since {10/12/2022}
 */
public class DpheDesktop {

    static private final Logger LOGGER = Logger.getLogger( "DeepPheDesktop" );

    static private final String CWD = System.getProperty( "user.dir" );
    static private final String ROOT = System.getenv( "DEEPPHE_ROOT" );
    static private final String ALT_ROOT = new File( CWD ).getParent();
    // Example corpus/output live in the user's writable DeepPheDocs directory,
    // not under the (possibly read-only) install root. The installer unpacks
    // DeepPhe-examples.zip there and points ExampleProject.txt at the same place.
    static private final String DOCS_DIR = System.getProperty( "user.home" ) + "/DeepPheDocs";
    static final String HTTPS_DEEPPHE_NLP_WIKI = "https://github.com/DeepPhe/DeepPhe-Release/wiki";
    static final String HTTPS_DEEPPHE_GITHUB_IO = "https://deepphe.github.io/";
    static private final String PROJECT_LIST_PATH = DirConfig.PROJECT.getFullDir() + "/ProjectList.txt";

    static private final Map<String,String> _desktopConfigMap = new HashMap<>();

    static private final ArrayList<String> _projectList = new ArrayList<>();
    static private final Map<String,String> _projectFileMap = new HashMap<>();
    static private final Map<String,String> _projectConfig = new HashMap<>();

    static private void initDesktopConfig( final String configFile ) {
        if ( configFile == null || configFile.trim().isEmpty() ) {
            LOGGER.info( "No Desktop Configuration file specified.  Using default configuration." );
            return;
        }
        ParameterHandler.readMapFromFile( configFile, _desktopConfigMap );
    }

    static String getRoot() {
        if ( ROOT != null ) {
            return ROOT;
        }
        return ALT_ROOT;
    }

    static ArrayList<String> getProjectList() {
        return _projectList;
    }

    static void readProjectList() {
        boolean listOk = ParameterHandler.readMapFromFile( PROJECT_LIST_PATH, _projectFileMap, false );
        listOk = listOk && !_projectFileMap.isEmpty();
        if ( listOk ) {
            ParameterHandler.readKeysFromFile( PROJECT_LIST_PATH, _projectList );
        } else {
            _projectList.add( PROJECT_NAME._defaultValue );
            _projectFileMap.put( PROJECT_NAME._defaultValue, CONFIG_FILE._defaultValue );
        }
        PROJECT_NAME.set( _projectList.get( 0 ) );
    }

    static void writeProjectList() {
        LOGGER.info( "Writing project list to " + PROJECT_LIST_PATH + " ..." );
        try ( Writer writer = new BufferedWriter( new FileWriter( PROJECT_LIST_PATH ) ) ) {
            for ( String project : _projectList ) {
                final String projectFile = getProjectFile( project );
                writer.write( project + "=" + projectFile + "\n" );
            }
        } catch ( IOException ioE ) {
            LOGGER.error( "Could not write project list to " + PROJECT_LIST_PATH );
            LOGGER.error( ioE.getMessage() );
        }
    }

    static private String getProjectFile( final String name ) {
        return _projectFileMap.computeIfAbsent( name, p -> DirConfig.PROJECT.getFullDir() + "/" + p + ".txt" );
    }

    static private void readProjectFile() {
        final String file = CONFIG_FILE.get();
        ParameterHandler.readMapFromFile( file, _projectConfig );
    }

    // TODO = extract a Parm interface with get, set.
    //  Extract method to parameterHandler that reads and writes given that interface.
    static void writeProjectFile() {
        final String file = CONFIG_FILE.get();
        LOGGER.info( "Writing current project parameters to " + file + " ..." );
        try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
            writer.write( "// Project file saved " + LocalDate.now() + "\n\n");
            for ( ProjectParameter parm : ProjectParameter.values() ) {
                writer.write( parm.name() + "=" + parm.get() + "\n" );
            }
         } catch ( IOException ioE ) {
            LOGGER.error( "Could not write " + file, ioE );
        }
    }


    enum ProjectParameter {
        PROJECT_NAME( "ExampleProject" ) {
            @Override
            void set( String value ) {
                if ( value.equals( get() ) ) {
                    return;
                }
                writeProjectFile();
                super.set( value );
                _projectList.remove( value );
                _projectList.add( 0, value );
                _projectFileMap.computeIfAbsent( value, p -> DirConfig.PROJECT.getFullDir() + "/" + p + ".txt" );
                readProjectFile();
            }
        },
        CONFIG_FILE( DirConfig.PROJECT.getFullDir() + "/" + PROJECT_NAME._defaultValue + ".txt" ) {
            String get() {
                return DirConfig.PROJECT.getFullDir() + "/" + PROJECT_NAME.get() + ".txt";
            }
        },
        PIPER_FILE( DirConfig.DPHE.getFullDir() + "/resources/pipeline/DefaultDeepPhe.piper" ),
        CORPUS_DIR( DOCS_DIR + "/example_corpus" ),
        OMOP_DB( DOCS_DIR + "/example_omop/patient_demographics.json" ),
        OUTPUT_DIR( DOCS_DIR + "/example_output" );
        private final String _defaultValue;
        ProjectParameter( final String defaultValue ) {
            _defaultValue = defaultValue;
        }
        String get() {
            return _projectConfig.getOrDefault( toString(), _defaultValue );
        }
        void set( final String value ) {
            _projectConfig.put( toString(), value.trim() );
        }
        static private void setDefaultProject() {
            for ( ProjectParameter projectParameter : values() ) {
                projectParameter.set( projectParameter._defaultValue );
            }
        }
    }

    enum DirConfig {
        DPHE( ".DeepPhe", "DpheDir", "DeepPheDir", "NlpDir", "SummarizerDir" ),
        ETL( ".DeepPhe/tools/dphe-pipeline",
             "DpheVizDbCreatorDir", "DpheVisDbCreatorDir", "VizDbCreatorDir", "VisDbCreatorDir" ),
        DATA_API( ".DeepPhe/tools/dphe-data-api", "DataApiDir", "DpheDataApiDir" ),
        VIZ( ".DeepPhe/tools/deepphe-visualizer-v2", "VizDir", "VisDir", "VizualizerDir", "VisualizerDir" ),
        LOG( ".DeepPhe/logs", "LogsDir", "LogDir" ),
        PROJECT( ".DeepPhe/resources/projects", "ProjectsDir", "ProjectDir" ),
        EXAMPLE( "examples", "ExamplesDir", "ExampleDir", "SamplesDir", "SampleDir" );
        private final String _defaultSubDir;
        private final String[] _configNames;
        DirConfig( final String subDir, final String ... configNames ) {
            _defaultSubDir = subDir;
            _configNames = configNames;
        }
        String getSubDir() {
            return ParameterHandler.getOrDefault( _desktopConfigMap, _defaultSubDir, _configNames );
        }
        String getFullDir() {
            return DpheDesktop.getRoot() + "/" + getSubDir();
        }
    }

    enum ToolCommandConfig {
        DPHE( "bin/runDeepPheGUI", "StartDphe", "StartDeepPhe", "StartNlp", "StartSummarizer" ),
        ETL( "runDbCreator", "StartDpheVizDbCreator", "StartDpheVisDbCreator", "StartVizDbCreator", "StartVisDbCreator" ),
        DATA_API( "dphe-data-api", "StartDataApi", "StartDpheDataApi" ),
        VIZ( "start-viz", "StartViz", "StartVis", "StartVizualizer", "StartVisualizer" );
        private final String _defaultCommand;
        private final String[] _configNames;
        ToolCommandConfig( final String command, final String ... configNames ) {
            _defaultCommand = command;
            _configNames = configNames;
        }
        String getCommand() {
            return ParameterHandler.getOrDefault( _desktopConfigMap, _defaultCommand, _configNames );
        }
    }

    enum ToolConfig {
        DPHE( DirConfig.DPHE, ToolCommandConfig.DPHE, "DeepPheGUI.log" ),
        ETL( DirConfig.ETL, ToolCommandConfig.ETL, "DeepPheDbCreator.log" ),
        DATA_API( DirConfig.DATA_API, ToolCommandConfig.DATA_API, "viz/DeepPheDataApi.log" ),
        VIZ( DirConfig.VIZ, ToolCommandConfig.VIZ, "viz/DeepPheViz.log" );
        private final DirConfig _DirConfig;
        private final ToolCommandConfig _toolCommandConfig;
        private final String _logFile;
        ToolConfig( final DirConfig dirConfig, final ToolCommandConfig toolCommandConfig, final String logFile ) {
            _DirConfig = dirConfig;
            _toolCommandConfig = toolCommandConfig;
            _logFile = logFile;
        }
        String getFullDir() {
            return _DirConfig.getFullDir();
        }
        String getFullCommand() {
            return getFullDir() + "/" + _toolCommandConfig.getCommand();
        }
        String getCommand() {
            return _toolCommandConfig.getCommand();
        }
        String getLogFile() {
            return _logFile;
        }
    }



    static private JFrame createFrame() {
        final JFrame frame = new JFrame( "DeepPhe Desktop" );
        frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
        // Use 1024 x 768 as the minimum required resolution (XGA)
        // iPhone 3 : 480 x 320 (3:2, HVGA)
        // iPhone 4 : 960 x 640  (3:2, unique to Apple)
        // iPhone 5 : 1136 x 640 (under 16:9, unique to Apple)
        // iPad 3&4 : 2048 x 1536 (4:3, QXGA)
        // iPad Mini: 1024 x 768 (4:3, XGA)
        final Dimension size = new Dimension( 1024, 600 );
        frame.setSize( size );
        frame.setMinimumSize( size );
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );
        return frame;
    }


    /**
     * Put the bundled JRE's {@code bin} directory first on the {@code PATH} of every
     * child process this app launches. install4j runs the desktop app under its
     * bundled JRE, so {@code java.home} points straight at it. The helper scripts
     * ({@code bin/runDeepPheGui.sh}, {@code runDeepPhe.sh}, the {@code .bat}
     * equivalents) invoke a bare {@code java}, which fails with "command not found"
     * on machines that have no system Java on the PATH. cTAKES {@code SystemUtil} and
     * our own runners copy every {@code ctakes.env.*} system property into the child
     * environment, so registering {@code PATH} here reaches all spawn paths without
     * touching the scripts.
     */
    static private void putBundledJreOnChildPath() {
        final String javaHome = System.getProperty( "java.home" );
        if ( javaHome == null || javaHome.isEmpty() ) {
            return;
        }
        final String javaBin = javaHome + File.separator + "bin";
        final String currentPath = System.getenv( "PATH" );
        final String newPath = ( currentPath == null || currentPath.isEmpty() )
                               ? javaBin
                               : javaBin + File.pathSeparator + currentPath;
        SystemUtil.addEnvironmentVariables( "PATH", newPath );
    }

    public static void main( final String... args ) {
        putBundledJreOnChildPath();
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
            UIManager.getDefaults()
                    .put( "SplitPane.border", BorderFactory.createEmptyBorder() );
            // Needed for MacOS, which sets gridlines to white by default
            UIManager.getDefaults()
                    .put( "Table.gridColor", Color.GRAY );
        } catch ( ClassNotFoundException | InstantiationException
                  | IllegalAccessException | UnsupportedLookAndFeelException multE ) {
            LOGGER.error( multE.getLocalizedMessage() );
        }
        final JFrame frame = createFrame();
        ProjectParameter.setDefaultProject();
        final DesktopMainPanel mainPanel = new DesktopMainPanel();
        frame.add( mainPanel );
        frame.pack();
        frame.setVisible( true );
        DisablerPane.getInstance().initialize( frame );
        if ( args.length == 1 ) {
            initDesktopConfig( args[0] );
        }
        mainPanel.initialize();
        mainPanel.popHello();
        LOGGER.info( "Welcome to the DeepPhe Desktop.\n"
              + "Enter your project settings at the top, then "
              + "use the buttons in the center to process data, create a database, "
              + "display results, or get help." );

    }

}

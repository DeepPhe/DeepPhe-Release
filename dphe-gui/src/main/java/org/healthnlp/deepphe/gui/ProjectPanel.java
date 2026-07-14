package org.healthnlp.deepphe.gui;

import org.apache.ctakes.gui.component.FileTableCellEditor;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static org.healthnlp.deepphe.gui.DpheDesktop.ProjectParameter.*;
import static org.healthnlp.deepphe.gui.DpheDesktop.ProjectParameter.PIPER_FILE;

/**
 * @author SPF , chip-nlp
 * @since {6/22/2026}
 */
public class ProjectPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "ProjectPanel" );


   static private final String PROJECT_LABEL = "Project:";

   private final ProjectTableModel _tableModel = new ProjectTableModel();

   public ProjectPanel() {
      super( new BorderLayout( 0, 5 ) );
      DpheDesktop.readProjectList();
      setBorder( new EmptyBorder( 5, 20, 5, 20 ) );
      final JPanel projectPanel = new JPanel( new BorderLayout( 10, 0 ) );
      final JLabel label = new JLabel( PROJECT_LABEL );
      label.setFont( new Font( Font.DIALOG, Font.BOLD, 14 ) );
      projectPanel.add( label, BorderLayout.WEST );
      JComboBox<String> projectCombo = new JComboBox<>( new ProjectComboModel() );
      projectCombo.setEditable( true );
      projectCombo.setFont( new Font( Font.SANS_SERIF, Font.PLAIN, 14 ) );
      projectPanel.add( projectCombo, BorderLayout.CENTER );
      add( projectPanel, BorderLayout.NORTH );
      add( createProjectTable(), BorderLayout.CENTER );
      add( new JSeparator( SwingConstants.HORIZONTAL ), BorderLayout.SOUTH );
      registerShutdownHook();
   }

   private JComponent createProjectTable() {
      final JTable table = new JTable( _tableModel ) {
         @Override
         public String getToolTipText( final MouseEvent event) {
            final Point p = event.getPoint();
            return _tableModel.getToolTip( rowAtPoint( p ), columnAtPoint( p ) );
         }
         @Override
         public TableCellEditor getCellEditor( final int row, final int column ) {
            if ( column == 2 ) {
               return _tableModel.getCellEditor( row, column );
            }
            return super.getCellEditor( row, column );
         }
         public TableCellRenderer getCellRenderer( final int row, final int column ) {
            if ( column == 2 ) {
               return _tableModel.getCellEditor( row, column );
            }
            return super.getCellRenderer( row, column );
         }
      };
      table.setFont( new Font( Font.SANS_SERIF, Font.PLAIN, 14 ) );
      table.setBorder( new BevelBorder( BevelBorder.LOWERED ) );
      table.putClientProperty( "terminateEditOnFocusLost", true );
      table.setRowHeight( 20 );
      table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      table.getColumnModel()
               .getColumn( 0 )
               .setPreferredWidth( 200 );
      table.getColumnModel()
               .getColumn( 0 )
               .setMaxWidth( 200 );
      table.getColumnModel()
               .getColumn( 2 )
               .setMaxWidth( 25 );
      table.setRowSelectionAllowed( true );
      table.setCellSelectionEnabled( true );
      ListSelectionModel selectionModel = table.getSelectionModel();
      selectionModel.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
      return table;
   }



   private class ProjectComboModel extends AbstractListModel<String> implements ComboBoxModel<String> {
      @Override
      public void setSelectedItem( final Object item ) {
         if ( item == null ) {
            setSelectedItem( getSelectedItem() );
            return;
         }
         final String project = item.toString().trim();
         if ( project.isEmpty() ) {
            setSelectedItem( getSelectedItem() );
            return;
         }
         if ( project.equals( getSelectedItem() ) ) {
            return;
         }
         PROJECT_NAME.set( project );
         _tableModel.reset();
         fireContentsChanged(this, -1, -1);
      }

      @Override
      public Object getSelectedItem() {
         return PROJECT_NAME.get();
      }

      @Override
      public int getSize() {
         return DpheDesktop.getProjectList().size();
      }

      @Override
      public String getElementAt( final int index ) {
         return DpheDesktop.getProjectList().get( index );
      }
   }

   private enum TableRow {
      PIPER_FILE( " Piper File ", "A Piper File defining a Pipeline.", "file",
            DpheDesktop.ProjectParameter.PIPER_FILE, FILES_ONLY, "Piper File", "piper" ),
      CORPUS_DIR( " Corpus Directory ",
            "A directory containing patient directories filled with document files.", "directory",
            DpheDesktop.ProjectParameter.CORPUS_DIR, DIRECTORIES_ONLY, null, null ),
      OMOP_DB( " OMOP Database ",
            "A JSON file with demographics in the required OMOP format.", "file",
            DpheDesktop.ProjectParameter.OMOP_DB, FILES_ONLY, "OMOP JSON", "json" ),
      OUTPUT_DIR( " Output Directory ",
            "A directory for output from the NLP Summarizer and Data Merge Tool.", "directory",
            DpheDesktop.ProjectParameter.OUTPUT_DIR, DIRECTORIES_ONLY, null, null );
      private final String _name;
      private final String _tip;
      private final String _type;
      private final Supplier<String> _getter;
      private final Consumer<String> _setter;
      private final FileTableCellEditor _chooser;
      TableRow( final String name, final String tip, final String type,
                final DpheDesktop.ProjectParameter parm,
                final int mode, final String filterName, final String filterExt ) {
         _name = name;
         _tip = tip;
         _type = type;
         _getter = parm::get;
         _setter = f -> parm.set( f.trim() );
         _chooser = new FileTableCellEditor();
         _chooser.getFileChooser().setFileSelectionMode( mode );
         if ( filterName != null && filterExt != null ) {
            _chooser.getFileChooser().setFileFilter( new FileNameExtensionFilter( filterName, filterExt ) );
         }
      }
      static private TableRow get( final int row ) {
         return values()[ row ];
      }
   }

   private final class ProjectTableModel implements TableModel {
      private final EventListenerList _listenerList = new EventListenerList();
      private final String[] COLUMN_NAMES = { "Name", "Value", "Explorer" };
      private final Class<?>[] COLUMN_CLASSES = { String.class, String.class, File.class };

      private void reset() {
         fireTableChanged( new TableModelEvent( this ) );
      }
      private String normalizePath( final String filepath ) {
         return Paths.get( filepath ).toAbsolutePath().normalize().toString();
      }
      private String getToolTip( final int row, final int column ) {
         switch ( column ) {
            case 0 : return TableRow.get( row )._tip;
            case 1 : return "Type or paste a " + TableRow.get( row )._type + " path, or drag and drop.";
            case 2 : return "Click to open a file explorer.";
         }
         return "";
      }
      private FileTableCellEditor getCellEditor( final int row, final int column ) {
         TableRow.get( row )._chooser.getFileChooser().setSelectedFile( (File)getValueAt( row, column ) );
         return TableRow.get( row )._chooser;
      }
      @Override
      public int getRowCount() {
         return TableRow.values().length;
      }
      @Override
      public int getColumnCount() {
         return COLUMN_NAMES.length;
      }
      @Override
      public String getColumnName( final int column ) {
         return COLUMN_NAMES[ column ];
      }
      @Override
      public Class<?> getColumnClass( final int column ) {
         return COLUMN_CLASSES[ column ];
      }
      @Override
      public Object getValueAt( final int row, final int column ) {
         if ( column == 0 ) {
            return TableRow.get( row )._name;
         } else if ( column == 1 ) {
            return normalizePath( TableRow.get( row )._getter.get() );

         } else if ( column == 2 ) {
            return new File( (String) getValueAt( row, 1 ) );
         }
         return "ERROR";
      }
      @Override
      public boolean isCellEditable( final int row, final int column ) {
         return column != 0;
      }
      @Override
      public void setValueAt( final Object aValue, final int row, final int column ) {
         if ( column == 1 ) {
            TableRow.get( row )._setter.accept( aValue.toString().trim() );
         } else if ( column == 2 && aValue instanceof File ) {
            TableRow.get( row )._setter.accept( ((File)aValue).getPath() );
         }
         fireTableChanged( new TableModelEvent( this, row, row, 1 ) );
      }
      @Override
      public void addTableModelListener( final TableModelListener listener ) {
         _listenerList.add( TableModelListener.class, listener );
      }
      @Override
      public void removeTableModelListener( final TableModelListener listener ) {
         _listenerList.remove( TableModelListener.class, listener );
      }
      private void fireTableChanged( final TableModelEvent event ) {
         // Guaranteed to return a non-null array
         Object[] listeners = _listenerList.getListenerList();
         // Process the listeners last to first, notifying
         // those that are interested in this event
         for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[ i ] == TableModelListener.class ) {
               ((TableModelListener)listeners[ i + 1 ]).tableChanged( event );
            }
         }
      }
   }


   public String getPiperGuiParms() {
      final File cliFile = new File( DpheDesktop.DirConfig.PROJECT.getFullDir(), PROJECT_NAME.get() + ".cli" );
      try ( Writer writer = new BufferedWriter( new FileWriter( cliFile ) ) ) {
         writer.write( "InputDirectory=" + CORPUS_DIR.get() + "\n" );
         writer.write( "OutputDirectory=" + OUTPUT_DIR.get() + "\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not write Piper CLI file with project parameters.", ioE );
      }
      return "-p " + PIPER_FILE.get() + " -c " + cliFile.getPath();
   }

   public String getEtlParms() {
      return OUTPUT_DIR.get() + " " + OMOP_DB.get() + " " + OUTPUT_DIR.get() + "/vizDb/" + PROJECT_NAME.get();
   }

   public String getVizParms() {
      return OUTPUT_DIR.get() + "/vizDb/" + PROJECT_NAME.get();
   }

   public File getVisualizerDatabaseFile() {
      return new File( getDocsDirectory(), "visualizer_database/deepphe.sqlite3" );
   }

   private File getDocsDirectory() {
      final File outputDir = new File( OUTPUT_DIR.get() ).getAbsoluteFile();
      if ( "json".equalsIgnoreCase( outputDir.getName() )
            && outputDir.getParentFile() != null
            && outputDir.getParentFile().getParentFile() != null ) {
         return outputDir.getParentFile().getParentFile();
      }
      if ( outputDir.getParentFile() != null ) {
         return outputDir.getParentFile();
      }
      return outputDir;
   }


   private void registerShutdownHook() {
      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
         DpheDesktop.writeProjectFile();
         DpheDesktop.writeProjectList();
      } ) );
   }

}

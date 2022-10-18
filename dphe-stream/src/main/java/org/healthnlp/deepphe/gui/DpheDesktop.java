package org.healthnlp.deepphe.gui;

import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @since {10/12/2022}
 */
public class DpheDesktop {

   static private final Logger LOGGER = Logger.getLogger( "DeepPheDesktop" );

   static private JFrame createFrame() {
      final JFrame frame = new JFrame( "DeepPhe Desktop" );
      frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
      // Use 1024 x 768 as the minimum required resolution (XGA)
      // iPhone 3 : 480 x 320 (3:2, HVGA)
      // iPhone 4 : 960 x 640  (3:2, unique to Apple)
      // iPhone 5 : 1136 x 640 (under 16:9, unique to Apple)
      // iPad 3&4 : 2048 x 1536 (4:3, QXGA)
      // iPad Mini: 1024 x 768 (4:3, XGA)
//      final Dimension size = new Dimension( 800, 600 );
//      final Dimension size = new Dimension( 1024, 768 );
      final Dimension size = new Dimension( 1024, 600 );
      frame.setSize( size );
      frame.setMinimumSize( size );
      System.setProperty( "apple.laf.useScreenMenuBar", "true" );
      return frame;
   }


   public static void main( final String... args ) {
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
      final DesktopMainPanel mainPanel = new DesktopMainPanel();
      frame.add( mainPanel );
      frame.pack();
      frame.setVisible( true );
      DisablerPane.getInstance().initialize( frame );
      mainPanel.readParameterFile( args );
      mainPanel.popHello();
   }

}

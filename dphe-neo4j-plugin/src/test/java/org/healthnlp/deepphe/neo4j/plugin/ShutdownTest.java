package org.healthnlp.deepphe.neo4j.plugin;

import org.healthnlp.deepphe.neo4j.embedded.ShutdownHook;
import org.healthnlp.deepphe.neo4j.embedded.TestServiceFactory;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.transaction.log.files.LogFiles;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/3/2020
 */
public class ShutdownTest {


   @Test
   public void recoveryNoTxLogs() {
      final File graphDir = new File( "resources/graph/neo4j/ontology.db" );
      final List<String> preDb1 = getFilePaths( graphDir );
      final List<String> preCounts1 = getCountPaths( graphDir );

      final GraphDatabaseService graphDb1 = TestServiceFactory.getInstance().getGraphDb();
      final List<String> inDb1 = getFilePaths( graphDir );

      final LogFiles logFiles1 = ((GraphDatabaseAPI) graphDb1).getDependencyResolver()
                                                              .resolveDependency( LogFiles.class );
      final File[] txLogFiles1 = logFiles1.logFiles();
      final List<String> dbLogs1 = getFilePaths( txLogFiles1 );
      final List<String> counts1 = getCountPaths( graphDir );
      //  junit does not execute shutdown hooks, but we can mimic the behavior.
      ShutdownHook.createTestHook( graphDb1, graphDir.getAbsolutePath() ).start();
      try {
         Thread.sleep( 1000 );
      } catch ( InterruptedException intE ) {
         System.err.println( intE.getMessage() );
      }

      final List<String> postDb1 = getFilePaths( graphDir );
      final List<String> postDbCounts1 = getCountPaths( graphDir );


      final GraphDatabaseService graphDb2 = TestServiceFactory.createService( TestServiceFactory.getGraphDir() );
      final LogFiles logFiles2 = ((GraphDatabaseAPI) graphDb2).getDependencyResolver()
                                                              .resolveDependency( LogFiles.class );
      final File[] txLogFiles2 = logFiles2.logFiles();
      final List<String> dbLogs2 = getFilePaths( txLogFiles2 );
      final List<String> counts2 = getCountPaths( graphDir );
      //  junit does not execute shutdown hooks, but we can mimic the behavior.
      ShutdownHook.createTestHook( graphDb2, graphDir.getAbsolutePath() ).start();
      try {
         Thread.sleep( 1000 );
      } catch ( InterruptedException intE ) {
         System.err.println( intE.getMessage() );
      }
      final List<String> postDb2 = getFilePaths( graphDir );
      final List<String> postCounts2 = getCountPaths( graphDir );

      System.out.println( "pre-Db 1 Files: " + preDb1.size() );
      System.out.println( "pre-Db 1 Counts: " + preCounts1.size() );
      System.out.println( "\nDb 1 Files: " + inDb1.size() );
      System.out.println( "Db 1 Logs: " + dbLogs1.size() );
      System.out.println( "Db 1 Counts: " + counts1.size() );
      System.out.println( "\nPost Db 1 Files: " + postDb1.size() );
      System.out.println( "Post Db 1 Counts: " + postDbCounts1.size() );
      System.out.println( "\nDb 2 Logs: " + dbLogs2.size() );
      System.out.println( "Db 2 Counts: " + counts2.size() );
      System.out.println( "\nPost Db 2 Files: " + postDb2.size() );
      System.out.println( "Post Db 2 Counts: " + postCounts2.size() );
   }

   static private List<String> getCountPaths( final File dir ) {
      return getFilePaths( dir ).stream()
                                .filter( p -> p.contains( ".counts.db" ) )
                                .collect( Collectors.toList() );
   }

   static private List<String> getFilePaths( final File dir ) {
      return getFilePaths( dir.listFiles() );
   }

   static private List<String> getFilePaths( final File[] files ) {
      return Arrays.stream( files )
                     .map( File::getAbsolutePath )
                     .sorted()
                     .collect( Collectors.toList() );
   }


}

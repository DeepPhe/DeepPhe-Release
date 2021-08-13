package org.healthnlp.deepphe.neo4j.driver;


import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.kernel.impl.store.kvstore.RotationTimeoutException;
import org.neo4j.kernel.lifecycle.LifecycleException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/23/2020
 */
final public class DriverFactory {


   private DriverFactory() {}

   /**
    *
    * @return Driver for the remote Neo4j Server using the given credentials.
    */
   static public Driver createDriver( final String uri, final String user, final String pass ) {
      final Driver driver = GraphDatabase.driver( uri, AuthTokens.basic( user, pass ) );
      registerShutdownHook( driver );
      return driver;
   }

   /**
    * Registers a shutdown hook for the Neo4j instance so that it shuts down nicely when the VM exits.
    * This includes kill signals and user actions like "Ctrl-C".
    * @param driver any new Driver.
    */
   static private void registerShutdownHook( final Driver driver ) {
      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
         try {
            driver.close();
         } catch ( LifecycleException | RotationTimeoutException multE ) {
            // ignore
         }
      } ) );
   }


}

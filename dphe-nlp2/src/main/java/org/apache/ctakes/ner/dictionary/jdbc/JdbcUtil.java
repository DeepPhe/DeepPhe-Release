package org.apache.ctakes.ner.dictionary.jdbc;


import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class JdbcUtil {

   static private final Logger LOGGER = Logger.getLogger( "JdbcUtil" );

   private JdbcUtil() {
   }

   static public final String HSQL_DRIVER = "org.hsqldb.jdbcDriver";
   static public final String HSQL_URL_PREFIX = "jdbc:hsqldb:file:";
   static public final String DEFAULT_USER = "sa";
   static public final String DEFAULT_PASS = "";


   static public void registerDriver() {
      registerDriver( HSQL_DRIVER );
   }

   static public void registerDriver( final String driverClass ) {
      try {
         Driver driver = (Driver)Class.forName( driverClass ).getDeclaredConstructor().newInstance();
         DriverManager.registerDriver( driver );
      } catch ( Exception e ) {
         // TODO At least four different exceptions are thrown here, and should be caught and handled individually
         LOGGER.error( "Could not register Driver " + driverClass );
         LOGGER.error( e.getMessage() );
         System.exit( 1 );
      }
   }

   static public Connection createDatabaseConnection( final String url, final String user, final String pass ) {
      return createDatabaseConnection( HSQL_DRIVER, url, user, pass );
   }

   static public Connection createDatabaseConnection( final String driverClass,
                                                      final String url,
                                                      final String user,
                                                      final String pass ) {
      registerDriver( driverClass );
      LOGGER.info( "Connecting to " + url + " as " + user );
      Connection connection = null;
      try {
         connection = DriverManager.getConnection( url, user, pass );
      } catch ( SQLException sqlE ) {
         // thrown by Connection.prepareStatement(..) and getTotalRowCount(..)
         LOGGER.error( "Could not establish connection to " + url + " as " + user );
         LOGGER.error( sqlE.getMessage() );
         System.exit( 1 );
      }
      registerShutdownHook( connection );
      return connection;
   }

   static public String getParameterValue( final String rootName,
                                           final String parameterName,
                                           final UimaContext uimaContext,
                                           final String defaultValue ) {
      final String value = EnvironmentVariable.getEnv( rootName + '_' + parameterName, uimaContext );
      if ( value != null && !value.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         return value;
      }
      return defaultValue;
   }


   static public PreparedStatement createPreparedStatement( final String name,
                                                            final String jdbcDriver,
                                                            final String jdbcUrl,
                                                            final String jdbcUser,
                                                            final String jdbcPass,
                                                            final String tableName,
                                                            final String indexName ) throws SQLException {
      if ( jdbcDriver == null || jdbcDriver.isEmpty() ) {
         throw new SQLException( "No JDBC Driver specified for " + name );
      }
      if ( jdbcUrl == null || jdbcUrl.isEmpty() ) {
         throw new SQLException( "No URL specified for " + name );
      }
      if ( tableName == null || tableName.isEmpty() ) {
         throw new SQLException( "No Table specified for " + name );
      }
      // DO NOT use try with resources here.  Try with resources uses a closable and closes it when exiting the try
      final Connection connection = createDatabaseConnection( jdbcDriver, jdbcUrl, jdbcUser, jdbcPass );
      if ( connection == null ) {
         throw new SQLException( "Could not connect to " + name );
      }
      return createSelectCall( connection, tableName, indexName );
   }


   /**
    * @return sql call to use for term lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   static private PreparedStatement createSelectCall( final Connection connection,
                                                      final String table,
                                                      final String index ) throws SQLException {
      final String lookupSql = "SELECT * FROM " + table + " WHERE " + index + " = ?";
      return connection.prepareStatement( lookupSql );
   }

   /**
    * @param statement sql call to use for lookup
    * @param text      of the text to use for lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   static public void fillSelectCall( final PreparedStatement statement, final String text ) throws SQLException {
      statement.clearParameters();
      statement.setString( 1, text );
   }

   /**
    * @param statement sql call to use for lookup
    * @param value     of the long to use for lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   static public void fillSelectCall( final PreparedStatement statement, final long value ) throws SQLException {
      statement.clearParameters();
      statement.setLong( 1, value );
   }

   /**
    * register shutdown hook that will shut down the database, removing temporary and lock files.
    *
    * @param connection -
    */
   static private void registerShutdownHook( final Connection connection ) {
      // Registers a shutdown hook for the Hsql instance so that it
      // shuts down nicely and any temporary or lock files are cleaned up.
      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
         try {
            final Statement shutdown = connection.createStatement();
            shutdown.execute( "SHUTDOWN" );
            shutdown.close();
            // The db is read-only, so there should be no need to roll back any transactions.
            connection.clearWarnings();
            connection.close();
         } catch ( SQLException sqlE ) {
            // ignore
         }
      } ) );
   }

}

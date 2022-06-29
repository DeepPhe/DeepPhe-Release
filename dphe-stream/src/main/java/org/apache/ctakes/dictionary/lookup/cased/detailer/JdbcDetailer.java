package org.apache.ctakes.dictionary.lookup.cased.detailer;


import org.apache.ctakes.dictionary.lookup.cased.table.column.DetailColumn;
import org.apache.ctakes.dictionary.lookup.cased.util.jdbc.JdbcUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.apache.ctakes.dictionary.lookup.cased.table.column.DetailColumn.*;
import static org.apache.ctakes.dictionary.lookup.cased.util.jdbc.JdbcUtil.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class JdbcDetailer implements TermDetailer {

   static public final String DETAILER_TYPE = "JDBC";

   static private final Logger LOGGER = Logger.getLogger( "JdbcDetailer" );

   private final String _name;
   private final PreparedStatement _selectDetailsStatement;


   public JdbcDetailer( final String name, final UimaContext uimaContext ) throws SQLException {
      this( name,
            getParameterValue( name, "driver", uimaContext, HSQL_DRIVER ),
            getParameterValue( name, "url", uimaContext, "" ),
            getParameterValue( name, "table", uimaContext, name.toUpperCase() ),
            getParameterValue( name, "user", uimaContext, DEFAULT_USER ),
            getParameterValue( name, "pass", uimaContext, DEFAULT_PASS ) );
   }

   /**
    * @param name       unique name for dictionary
    * @param jdbcDriver -
    * @param jdbcUrl    -
    * @param tableName  -
    * @param jdbcUser   -
    * @param jdbcPass   -
    */
   public JdbcDetailer( final String name,
                        final String jdbcDriver,
                        final String jdbcUrl,
                        final String tableName,
                        final String jdbcUser,
                        final String jdbcPass ) throws SQLException {
      _name = name;
      _selectDetailsStatement
            = JdbcUtil.createPreparedStatement( name,
                                                jdbcDriver,
                                                jdbcUrl,
                                                jdbcUser,
                                                jdbcPass,
                                                DETAILS_TABLE,
                                                DetailColumn.CUI.name() );
      LOGGER.info( "Connected to " + name + " table " + tableName );
   }

   public String getName() {
      return _name;
   }


   /**
    * {@inheritDoc}
    * @param cui
    * @return
    */
   @Override
   public Details getDetails( final String cui ) {
      try {
         JdbcUtil.fillSelectCall( _selectDetailsStatement, cui );
         final ResultSet resultSet = _selectDetailsStatement.executeQuery();
         final Details details = new Details(
               resultSet.getString( TUI.getColumnIndex() ),
               resultSet.getString( PREFERRED_TEXT.getColumnIndex() ),
               resultSet.getString( URI.getColumnIndex() ),
               resultSet.getShort( RANK.getColumnIndex() ) );
         resultSet.close();
         return details;
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return Details.EMPTY_DETAILS;
   }


}

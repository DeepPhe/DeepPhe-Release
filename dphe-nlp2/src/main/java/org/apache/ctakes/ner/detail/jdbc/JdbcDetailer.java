package org.apache.ctakes.ner.detail.jdbc;


import org.apache.ctakes.ner.detail.Detailer;
import org.apache.ctakes.ner.detail.Details;
import org.apache.ctakes.ner.dictionary.jdbc.JdbcUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.apache.ctakes.ner.detail.jdbc.DpheDetailColumn.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class JdbcDetailer implements Detailer {

   static public final String DETAILER_TYPE = "JDBC";

   static private final Logger LOGGER = Logger.getLogger( "JdbcDetailer" );

   static public final String DETAILS_TABLE = "URI_DETAILS";

   static private final String snomed_rxnorm_2020aa_url
         = "jdbc:hsqldb:file:resources/org/apache/ctakes/dictionary/lookup/cased/sno_rx_2020aa/sno_rx_2020aa";
   static private final String snomed_rxnorm_2020aa_driver = JdbcUtil.HSQL_DRIVER;
   static private final String snomed_rxnorm_2020aa_user = JdbcUtil.DEFAULT_USER;
   static private final String snomed_rxnorm_2020aa_pass = JdbcUtil.DEFAULT_PASS;

   private final String _name;
   private final PreparedStatement _selectDetails;



   public JdbcDetailer( final String name, final UimaContext uimaContext ) throws SQLException {
      this( name,
            JdbcUtil.getParameterValue( name, "driver", uimaContext, snomed_rxnorm_2020aa_driver ),
            JdbcUtil.getParameterValue( name, "url", uimaContext, snomed_rxnorm_2020aa_url ),
            JdbcUtil.getParameterValue( name, "details", uimaContext, DETAILS_TABLE ),
            JdbcUtil.getParameterValue( name, "user", uimaContext, snomed_rxnorm_2020aa_user ),
            JdbcUtil.getParameterValue( name, "pass", uimaContext, snomed_rxnorm_2020aa_pass ) );
   }

   /**
    * @param name       unique name for dictionary
    * @param jdbcDriver -
    * @param jdbcUrl    -
    * @param tableName  Name of table containing details
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
      _selectDetails = JdbcUtil.createPreparedStatement( name,
            jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, tableName, PREF_TEXT.name() );
      LOGGER.info( "Connected to " + name + " table " + tableName );
   }

   public String getName() {
      return _name;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Details> getDetails( final String preferredText ) {
      final List<Details> detailsList = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectDetails, preferredText );
         final ResultSet resultSet = _selectDetails.executeQuery();
         while ( resultSet.next() ) {
            final Details details = new Details(
                  resultSet.getString( PREF_TEXT.getColumnIndex() ),
                  resultSet.getString( GROUPING.getColumnIndex() ),
                  resultSet.getString( CUI.getColumnIndex() ),
                  resultSet.getInt( TUI.getColumnIndex() ),
                  resultSet.getString( URI.getColumnIndex() ),
                  resultSet.getString( CODES.getColumnIndex() ) );
            detailsList.add( details );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return detailsList;
   }

}

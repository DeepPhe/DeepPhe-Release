package org.apache.ctakes.dictionary.lookup.cased.dictionary;


import org.apache.ctakes.dictionary.lookup.cased.lookup.CandidateTerm;
import org.apache.ctakes.dictionary.lookup.cased.lookup.LookupToken;
import org.apache.ctakes.dictionary.lookup.cased.table.column.SynonymColumn;
import org.apache.ctakes.dictionary.lookup.cased.util.jdbc.JdbcUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;

import static org.apache.ctakes.dictionary.lookup.cased.table.column.SynonymColumn.*;
import static org.apache.ctakes.dictionary.lookup.cased.util.jdbc.JdbcUtil.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/14/2020
 */
final public class JdbcDictionary implements CasedDictionary {

   static public final String DICTIONARY_TYPE = "JDBC";

   static private final Logger LOGGER = Logger.getLogger( "JdbcDictionary" );

   static private final String dictionary_url
         = "jdbc:hsqldb:file:resources/org/healthnlp/deepphe/dictionary/ncit_plus_2022";
   static private final String dictionary_driver = "org.hsqldb.jdbcDriver";
   static private final String dictionary_user = "sa";
   static private final String dictionary_pass = "";

   private final String _name;


   private final PreparedStatement _selectSynonymsCall;


   /**
    * @param name        unique name for dictionary
    * @param uimaContext -
    */
   public JdbcDictionary( final String name, final UimaContext uimaContext ) throws SQLException {
      this( name,
            getParameterValue( name, "driver", uimaContext, HSQL_DRIVER ),
            getParameterValue( name, "url", uimaContext, "" ),
            getParameterValue( name, "synonyms", uimaContext, SYNONYMS_TABLE ),
            getParameterValue( name, "user", uimaContext, DEFAULT_USER ),
            getParameterValue( name, "pass", uimaContext, DEFAULT_PASS ) );
   }

   /**
    * @param name       unique name for dictionary
    * @param jdbcDriver -
    * @param jdbcUrl    -
    * @param synonymsName  Name of table containing uppercase-only terms
    * @param jdbcUser   -
    * @param jdbcPass   -
    */
   public JdbcDictionary( final String name,
                          final String jdbcDriver,
                          final String jdbcUrl,
                          final String synonymsName,
                          final String jdbcUser,
                          final String jdbcPass ) throws SQLException {
      _name = name;
      _selectSynonymsCall = JdbcUtil.createPreparedStatement( name,
                                                              jdbcDriver,
                                                              jdbcUrl,
                                                              jdbcUser,
                                                              jdbcPass,
                                                              synonymsName,
                                                              LOOKUP_TOKEN.name() );
      LOGGER.info( "Connected to " + name + " table " + synonymsName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<CandidateTerm> getCandidateTerms( final LookupToken lookupToken ) {
      final Collection<CandidateTerm> candidateTerms = new HashSet<>();
      try {
         JdbcUtil.fillSelectCall( _selectSynonymsCall, lookupToken.getLowerText() );
         final ResultSet resultSet = _selectSynonymsCall.executeQuery();
         while ( resultSet.next() ) {
            candidateTerms.add( new CandidateTerm(
                  resultSet.getString( CUI.getColumnIndex() ),
                  (String[])resultSet.getArray( SynonymColumn.TOKENS.getColumnIndex() ).getArray(),
                  resultSet.getShort( LOOKUP_INDEX.getColumnIndex() ),
                  resultSet.getShort( TOP_RANK.getColumnIndex() ),
                  resultSet.getShort( ENTRY_COUNTS.getColumnIndex() ),
                  resultSet.getShort( SECOND_RANK.getColumnIndex() ),
                  resultSet.getShort( SECOND_COUNTS.getColumnIndex() ),
                  resultSet.getShort( VARIANT_TOP_RANK.getColumnIndex() ),
                  resultSet.getShort( VARIANT_ENTRY_COUNTS.getColumnIndex() ),
                  resultSet.getShort( OTHER_CUIS.getColumnIndex() ),
                  (String[])resultSet.getArray( VOCAB_CODES.getColumnIndex() ).getArray()
            ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return candidateTerms;
   }



}

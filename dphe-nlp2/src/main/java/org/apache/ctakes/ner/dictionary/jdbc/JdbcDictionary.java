package org.apache.ctakes.ner.dictionary.jdbc;


import org.apache.ctakes.ner.term.TermCandidate;
import org.apache.ctakes.ner.dictionary.Dictionary;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/14/2020
 */
final public class JdbcDictionary implements Dictionary {

   static private final Logger LOGGER = Logger.getLogger( "JdbcDictionary" );

   static public final String DICTIONARY_TYPE = "JDBC";
   static public final String SYNONYMS_TABLE = "URI_SYNONYMS";

   static private final String snomed_rxnorm_2020aa_url
         = "jdbc:hsqldb:file:resources/org/apache/ctakes/dictionary/lookup/cased/sno_rx_2020aa/sno_rx_2020aa";
   static private final String snomed_rxnorm_2020aa_driver = JdbcUtil.HSQL_DRIVER;
   static private final String snomed_rxnorm_2020aa_user = JdbcUtil.DEFAULT_USER;
   static private final String snomed_rxnorm_2020aa_pass = JdbcUtil.DEFAULT_PASS;

   private final String _name;

   private final PreparedStatement _selectSynonyms;


   /**
    * @param name        unique name for dictionary
    * @param uimaContext -
    */
   public JdbcDictionary( final String name, final UimaContext uimaContext ) throws SQLException {
      this( name,
            JdbcUtil.getParameterValue( name, "driver", uimaContext, snomed_rxnorm_2020aa_driver ),
            JdbcUtil.getParameterValue( name, "url", uimaContext, snomed_rxnorm_2020aa_url ),
            JdbcUtil.getParameterValue( name, "synonyms", uimaContext, SYNONYMS_TABLE ),
            JdbcUtil.getParameterValue( name, "user", uimaContext, snomed_rxnorm_2020aa_user ),
            JdbcUtil.getParameterValue( name, "pass", uimaContext, snomed_rxnorm_2020aa_pass ) );
   }

   /**
    * @param name       unique name for dictionary
    * @param jdbcDriver -
    * @param jdbcUrl    -
    * @param tableName  Name of table containing synonyms
    * @param jdbcUser   -
    * @param jdbcPass   -
    */
   public JdbcDictionary( final String name,
                          final String jdbcDriver,
                          final String jdbcUrl,
                          final String tableName,
                          final String jdbcUser,
                          final String jdbcPass ) throws SQLException {
      _name = name;
      _selectSynonyms = JdbcUtil.createPreparedStatement( name,
            jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, tableName, DpheSynonymColumn.INDEX_TOKEN.name() );
      LOGGER.info( "Connected to " + name + " table " + tableName );
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
   public Collection<TermCandidate> getTermCandidates( final String lookupText,
                                                       final int indexInSentence,
                                                       final int sentenceLength ) {
      final List<TermCandidate> termCandidates = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectSynonyms, lookupText );
         final ResultSet resultSet = _selectSynonyms.executeQuery();
         while ( resultSet.next() ) {
            final TermCandidate termCandidate = new TermCandidate(
                  resultSet.getString( DpheSynonymColumn.PREF_TEXT.getColumnIndex() ),
                  resultSet.getInt( DpheSynonymColumn.TOKEN_OFFSET.getColumnIndex() ),
                  resultSet.getString( DpheSynonymColumn.SYNONYM.getColumnIndex() ) );
            if ( termCandidate.fitsInSentence( indexInSentence, sentenceLength ) ) {
               termCandidates.add( termCandidate );
            }
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return termCandidates;
   }


}

package org.healthnlp.deepphe.summary.attribute;

import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Map;

abstract public class AbstractAttributeHelper implements AttributeHelper {

   private final ConceptAggregate _neoplasm;
   private final Collection<ConceptAggregate> _allConcepts;

   private String _bestUri;
   private double _bestUriScore;

   private String _runnerUpUri;
   private double _runnerUpScore;

   private String _bestIcdoCode;
   private Collection<String> _possibleIcdoCodes;

   private Collection<ConceptAggregate> _bestNeoplasmConceptsForType;
   private Collection<ConceptAggregate> _allNeoplasmConceptsForType;
   private Collection<ConceptAggregate> _allPatientConceptsForType;

   private Map<String,Collection<String>> _allPatientUriRootsForType;



   public AbstractAttributeHelper( final ConceptAggregate neoplasm, final Collection<ConceptAggregate> allConcepts ) {
      _neoplasm = neoplasm;
      _allConcepts = allConcepts;
   }

   public ConceptAggregate getNeoplasm() {
      return _neoplasm;
   }

   public Collection<ConceptAggregate> getAllPatientConcepts() {
      return _allConcepts;
   }


   //////////////////////////////////////////////////////
   //
   //       Attribute Best Value, URI
   //

   public String getBestUriForType() {
      return _bestUri;
   }

   protected void setBestUriForType( final String uri ) {
      _bestUri = uri;
   }

   public double getBestUriScoreForType() {
      return _bestUriScore;
   }

   protected void setBestUriScoreForType( final double score ) {
      _bestUriScore = score;
   }

   public String getRunnerUpUriForType() {
      return _runnerUpUri;
   }

   protected void setRunnerUpUriForType( final String uri ) {
      _runnerUpUri = uri;
   }

   public double getRunnerUpUriScoreForType() {
      return _runnerUpScore;
   }

   protected void setRunnerUpUriScoreForType( final double score ) {
      _runnerUpScore = score;
   }


   public String getBestIcdoCode() {
      return _bestIcdoCode;
   }

   protected void setBestIcdoCode( final String code ) {
      _bestIcdoCode = code;
   }

   public Collection<String> getPossibleIcdoCodes() {
      return _possibleIcdoCodes;
   }

   public void setPossibleIcdoCodes( final Collection<String> codes ) {
      _possibleIcdoCodes = codes;
   }


   //////////////////////////////////////////////////////
   //
   //       Attribute Best Value, Concepts
   //

   public Collection<ConceptAggregate> getBestConceptsForType() {
      return _bestNeoplasmConceptsForType;
   }

   protected void setBestConceptsForType( final Collection<ConceptAggregate> concepts ) {
      _bestNeoplasmConceptsForType = concepts;
   }


   //////////////////////////////////////////////////////
   //
   //       Attribute, All Values in Neoplasm
   //


   public Collection<ConceptAggregate> getAllNeoplasmConceptsForType() {
      return _allNeoplasmConceptsForType;
   }

   protected void setAllNeoplasmConceptsForType( final Collection<ConceptAggregate> concepts ) {
      _allNeoplasmConceptsForType = concepts;
   }

   //////////////////////////////////////////////////////
   //
   //       Attribute, All Values in Patient
   //

   public Collection<ConceptAggregate> getAllPatientConceptsForType() {
      return _allPatientConceptsForType;
   }

   protected void setAllPatientConceptsForType( final Collection<ConceptAggregate> concepts ) {
      _allPatientConceptsForType = concepts;
   }

   public Map<String,Collection<String>> getAllPatientUriRootsForType() {
      if ( _allPatientUriRootsForType == null ) {
         setAllPatientUriRootsForType( UriUtil.mapUriRoots( getAllPatientUrisForType() ) );
      }
      return _allPatientUriRootsForType;
   }

   protected void setAllPatientUriRootsForType( final Map<String, Collection<String>> uriRootsMap ) {
      _allPatientUriRootsForType = uriRootsMap;
   }



}

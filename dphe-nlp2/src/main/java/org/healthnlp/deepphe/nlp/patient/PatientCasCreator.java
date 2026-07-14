package org.healthnlp.deepphe.nlp.patient;

import org.apache.ctakes.core.store.ObjectCreator;
import org.apache.ctakes.core.util.annotation.ConceptBuilder;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Element;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.relation.ElementRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.CasCopier;
import org.healthnlp.deepphe.neo4j.constant.UriConstants2;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.xn.ConceptRelation;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.*;
import java.util.function.Function;

/**
 * @author SPF , chip-nlp
 * @since {9/11/2023}
 */
final public class PatientCasCreator implements ObjectCreator<JCas> {

   static public final Logger LOGGER = Logger.getLogger( "PatientCasCreator" );

   public JCas create( final String patientId ) {
      try {
         return new JCasBuilder().setPatientId( patientId )
                                 .setBirthDay( "" )
                                 .setDeathday( "" )
                                 .setGender( "" )
                                 .setFirstName( "" )
                                 .setLastName( patientId )
                                 .build();
      } catch ( UIMAException uE ) {
         LOGGER.error( "Could not build JCas for patient " + patientId );
      }
      return null;
   }

   static public void addDocCas( final JCas patient, final JCas doc ) {
      try {
         final String docId = DocIdUtil.getDocumentID( doc );
         final JCas view = patient.createView( docId );
         // https://www.tabnine.com/code/java/methods/org.apache.uima.util.CasCopier/copyFs
         final CasCopier copier = new CasCopier( doc.getCas(), view.getCas() );
//         Feature sofaFeature = patient.getTypeSystem().getFeatureByFullName( CAS.FEATURE_FULL_NAME_SOFA);
         view.setDocumentText( doc.getDocumentText() );
         for ( TOP top : JCasUtil.select( doc, TOP.class ) ) {
            try {
               final TOP copy = (TOP)copier.copyFs( top );
//               copy.setFeatureValue( sofaFeature, view.getSofa() );
               copy.addToIndexes();
            } catch ( Exception e ) {
               LOGGER.error( "Exception: " + e.getMessage() );
            }
         }
      } catch ( UIMAException uE ) {
         LOGGER.error( "Could not create JCas View for document " + DocIdUtil.getDocumentID( doc ) );
      }
   }

   static public void fillPatientCas( final JCas patientCas,
                                       final Collection<UriConcept> concepts,
                                       final Collection<ConceptRelation> relations,
                                       final Map<Mention, IdentifiedAnnotation> mentionAnnotationMap ) {
//      final Map<UriConcept, Element> conceptElementMap = new HashMap<>( concepts.size() );
      final Map<String, Element> conceptIdElementMap = new HashMap<>( concepts.size() );
      for ( UriConcept concept : concepts ) {
         final DpheGroup group = concept.getDpheGroup();
         final Element element = createPatientElement( patientCas, concept, mentionAnnotationMap,
               group.getCreator() );
//         conceptElementMap.put( concept, element );
         conceptIdElementMap.put( concept.getId(), element );
      }

      for ( ConceptRelation relation : relations ) {
         createPatientRelation( patientCas, relation, conceptIdElementMap );
      }
//      for ( UriConcept concept : concepts ) {
//         createPatientRelations( patientCas, concept, conceptElementMap );
//      }
   }


   static private <E extends Element> E createPatientElement( final JCas patientCas,
                                                              final UriConcept concept,
                                                              final Map<Mention,IdentifiedAnnotation> mentionAnnotationMap,
                                                              final Function<JCas, E> creator ) {
      String cui = concept.getCodifications().getOrDefault( "CUI", Collections.emptyList() )
                          .stream().filter( Objects::nonNull ).findFirst().orElse( "" );
      if ( cui.isEmpty() ) {
         cui = concept.getCodifications().getOrDefault( "DeepPhe", Collections.emptyList() )
                      .stream().filter( Objects::nonNull ).findFirst().orElse( "" );
      }
      if ( UriInfoCache.getInstance().getPrefText( concept.getUri() ).isEmpty() ) {
         LogFileWriter.add( "PatientCasCreator empty prefText for " + concept.getUri() );
      }
      final ConceptBuilder builder = new ConceptBuilder()
            .preferredText( UriInfoCache.getInstance().getPrefText( concept.getUri() ) )
            .cui( cui )
            .code( concept.getUri() )
            .type( concept.getDpheGroup().getTui() )
            .score( concept.getConfidence() )
            .schema( UriConstants2.PATIENT_CONCEPT_SCHEMA );
      final OntologyConcept ontoConcept = builder.build( patientCas );
      final E event = creator.apply( patientCas );
      event.setId( concept.hashCode() );
      event.setOntologyConcept( ontoConcept );
      event.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE );
      event.setSubject( concept.getSubject() );
      event.setPolarity( concept.isNegated() ? CONST.NE_POLARITY_NEGATION_PRESENT : CONST.NE_POLARITY_NEGATION_ABSENT );
      event.setUncertainty( concept.isUncertain() ? CONST.NE_UNCERTAINTY_PRESENT : CONST.NE_UNCERTAINTY_ABSENT );
      event.setHistoryOf( concept.inPatientHistory() ? CONST.NE_HISTORY_OF_PRESENT : CONST.NE_HISTORY_OF_ABSENT );
      event.setConfidence( concept.getConfidence() );
      final List<Mention> mentions = new ArrayList<>( concept.getMentions() );
      final int mentionCount = mentions.size();
      final FSArray array = new FSArray( patientCas, mentionCount );
      for ( int i=0; i<mentionCount; i++ ) {
         final IdentifiedAnnotation annotation = mentionAnnotationMap.get( mentions.get( i ) );
         array.set( i, annotation );
      }
      event.setMentions( array );
      event.addToIndexes( patientCas );
      return event;
   }


   static private Collection<ElementRelation> createPatientRelations( final JCas patientCas,
                                                                      final UriConcept concept,
                                                                      final Map<UriConcept,Element> conceptElementMap ) {
      final Map<String,Collection<UriConceptRelation>> conceptRelationsMap = concept.getAllRelations();
      if ( conceptRelationsMap.isEmpty() ) {
         return Collections.emptyList();
      }
      final Element source = conceptElementMap.get( concept );
      final Collection<ElementRelation> patientRelations = new HashSet<>();
      for ( Map.Entry<String,Collection<UriConceptRelation>> conceptRelations : conceptRelationsMap.entrySet() ) {
         for ( UriConceptRelation conceptRelation : conceptRelations.getValue() ) {
            final Element target = conceptElementMap.get( conceptRelation.getTarget() );
            if ( target == null ) {
               continue;
            }
            final ElementRelation relation = new ElementRelation( patientCas );
            relation.setCategory( conceptRelations.getKey() );
            relation.setArg1( source );
            relation.setArg2( target );
            relation.setConfidence( conceptRelation.getConfidence() );
            // TODO add constant to ctakes
            relation.setDiscoveryTechnique( CONST.REL_DISCOVERY_TECH_GOLD_ANNOTATION + 1 );
            relation.addToIndexes( patientCas );
         }
      }
      return patientRelations;
   }

   static private ElementRelation createPatientRelation( final JCas patientCas,
                                                                      final ConceptRelation conceptRelation,
                                                                      final Map<String,Element> conceptIdElementMap ) {
      final Element source = conceptIdElementMap.get( conceptRelation.getSourceId() );
      final Element target = conceptIdElementMap.get( conceptRelation.getTargetId() );
      if ( source == null || target == null ) {
         return null;
      }
      final ElementRelation relation = new ElementRelation( patientCas );
      relation.setCategory( conceptRelation.getType() );
      relation.setArg1( source );
      relation.setArg2( target );
      relation.setConfidence( conceptRelation.getdConfidence() );
      // TODO add constant to ctakes
      relation.setDiscoveryTechnique( CONST.REL_DISCOVERY_TECH_GOLD_ANNOTATION + 1 );
      relation.addToIndexes( patientCas );
      return relation;
   }

}

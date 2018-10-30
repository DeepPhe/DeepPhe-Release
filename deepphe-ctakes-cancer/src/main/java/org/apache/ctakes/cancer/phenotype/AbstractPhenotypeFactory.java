package org.apache.ctakes.cancer.phenotype;

import org.apache.ctakes.cancer.phenotype.property.*;
import org.apache.ctakes.cancer.uri.UriAnnotationFactory;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.DegreeOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Modifier;
import org.apache.ctakes.typesystem.type.textsem.SeverityModifier;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.Collections;

import static org.apache.ctakes.typesystem.type.constants.CONST.MODIFIER_TYPE_ID_SEVERITY_CLASS;
import static org.healthnlp.deepphe.neo4j.Neo4jConstants.DPHE_CODING_SCHEME;

/**
 * Abstract for classes that should be used to create full neoplasm property instances.
 * An instance is defined as the collection of all property types and values associated with a single neoplasm.
 * Children of this factory exist for each property type, so full instance creation requires use of each.
 *
 * Use of any {@code createPhenotype()} method will create:
 * <ul>
 * appropriate property type annotations
 * neoplasm relations between the property type annotations and the nearest provided neoplasm in the text
 * property value annotations
 * degree-of relations between the property type annotations and the appropriate value annotations
 * test-for relations between property type annotations and the nearest provided test in the text
 * </ul>
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/17/2016
 */
@Deprecated
abstract public class AbstractPhenotypeFactory<T extends Type, V extends Value, E extends IdentifiedAnnotation> {

   // TODO add <..., P extends Test, ...>

   static private final Logger LOGGER = Logger.getLogger( "AbstractPhenotypeFactory" );


   public AbstractPhenotypeFactory( final String phenotypeName ) {

   }


   /**
    * Create the type as an event mention and add it to the cas,
    * create the value as a modifier and add it to the cas,
    * create the type to value tie as a DegreeOfTextRelation
    *
    * @param jcas              -
    * @param windowStartOffset character offset of window containing the receptor status
    * @param spannedProperty   -
    * @return the property as a event mention
    */
   final public E createPhenotype( final JCas jcas,
                                   final int windowStartOffset,
                                   final SpannedProperty<T, V> spannedProperty ) {
      return createPhenotype( jcas, windowStartOffset, spannedProperty, Collections.emptyList() );
   }

   /**
    * Create the type as an event mention and add it to the cas,
    * create the value as a modifier and add it to the cas,
    * create the type to value tie as a DegreeOfTextRelation
    * Tie the type as an event mention to the closest neoplasm
    *
    * @param jcas              -
    * @param windowStartOffset character offset of window containing the receptor status
    * @param spannedProperty   -
    * @param neoplasms         nearby neoplasms
    * @return the property as a event mention
    */
   final public E createPhenotype( final JCas jcas,
                                   final int windowStartOffset,
                                   final SpannedProperty<T, V> spannedProperty,
                                   final Iterable<IdentifiedAnnotation> neoplasms ) {
      return createPhenotype( jcas, windowStartOffset, spannedProperty, neoplasms, null );
   }

   /**
    * Create the type as an event mention and add it to the cas,
    * create the value as a modifier and add it to the cas,
    * create the type to value tie as a DegreeOfTextRelation
    * Tie the type as an event mention to the closest neoplasm
    * Tie the type to the closest diagnostic test
    *
    * @param jcas              -
    * @param windowStartOffset character offset of window containing the receptor status
    * @param spannedProperty   -
    * @param neoplasms         nearby neoplasms
    * @param diagnosticTests   nearby diagnostic tests
    * @return the property as a event mention
    */
   abstract public E createPhenotype( final JCas jcas,
                                      final int windowStartOffset,
                                      final SpannedProperty<T, V> spannedProperty,
                                      final Iterable<IdentifiedAnnotation> neoplasms,
                                      final Iterable<IdentifiedAnnotation> diagnosticTests );



   /**
    * Create an event mention based upon spanned property type and add it to the cas
    *
    * @param jcas              -
    * @param windowStartOffset character offset of window containing the property
    * @param spannedProperty   -
    * @return the property as a event mention
    */
   final protected E createTypeEventMention( final JCas jcas,
                                             final int windowStartOffset,
                                             final SpannedProperty<T, V> spannedProperty ) {
      final SpannedType<T> spannedType = spannedProperty.getSpannedType();
      final E typeMention = createSpanEventMention( jcas,
            windowStartOffset + spannedType.getStartOffset(),
            windowStartOffset + spannedType.getEndOffset() );
      final SpannedValue<V> spannedValue = spannedProperty.getSpannedValue();
      final V value = spannedValue.getValue();
      final T type = spannedType.getType();
      // Main Umls Concept
      final String cui = type.getCui( value );
      final String tui = type.getTui();
      final String title = type.getTitle();
      UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCui( cui == null ? "" : cui );
      umlsConcept.setTui( tui == null ? "" : tui );
      umlsConcept.setPreferredText( title == null ? "" : title );
      umlsConcept.setCodingScheme( UriConstants.DPHE_SCHEME );
      umlsConcept.setCode( type.getUri() );
      final FSArray ontologyConcepts = new FSArray( jcas, 1 );
      ontologyConcepts.set( 0, umlsConcept );
      typeMention.setOntologyConceptArr( ontologyConcepts );
      typeMention.addToIndexes();
      return typeMention;
   }

   /**
    * Create a sign/symptom and add it to the cas
    *
    * @param jcas              -
    * @param eventUri -
    * @param windowStartOffset -
    * @param spannedProperty   -
    * @return the property as a event mention
    */
   final protected E createPropertyEventMention( final JCas jcas, final String eventUri,
                                                 final int windowStartOffset,
                                                 final SpannedProperty<T, V> spannedProperty ) {

      final E eventMention = createSpanEventMention( jcas,
            windowStartOffset + spannedProperty.getStartOffset(),
            windowStartOffset + spannedProperty.getEndOffset() );
      // Main Umls Concept
      final UmlsConcept umlsConcept = UriAnnotationFactory.createUmlsConcept( jcas, eventUri );
      final FSArray ontologyConcepts = new FSArray( jcas, 1 );
      ontologyConcepts.set( 0, umlsConcept );
      eventMention.setOntologyConceptArr( ontologyConcepts );
      eventMention.addToIndexes();
      return eventMention;
   }

   /**
    * @param jcas        -
    * @param startOffset -
    * @param endOffset   -
    * @return EventMention of the proper type at the given text span
    */
   abstract protected E createSpanEventMention( final JCas jcas, final int startOffset, final int endOffset );

   /**
    * Create a modifier and add it to the cas
    *
    * @param jcas              -
    * @param windowStartOffset character offset of window containing the property
    * @param spannedProperty   -
    * @return the modifier representing the value of the property
    */
   final protected Modifier createValueModifier( final JCas jcas,
                                                 final int windowStartOffset,
                                                 final SpannedProperty<T, V> spannedProperty ) {
      final SpannedValue<V> spannedValue = spannedProperty.getSpannedValue();
      final SeverityModifier valueModifier = new SeverityModifier( jcas,
            windowStartOffset + spannedValue.getStartOffset(),
            windowStartOffset + spannedValue.getEndOffset() );
      valueModifier.setTypeID( MODIFIER_TYPE_ID_SEVERITY_CLASS );
      final V value = spannedValue.getValue();
      // Value uri concept
      final String cui = value.getCui();
      final String tui = value.getTui();
      final String title = value.getTitle();
      UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCui( cui == null ? "" : cui );
      umlsConcept.setTui( tui == null ? "" : tui );
      umlsConcept.setPreferredText( title == null ? "" : title );
      umlsConcept.setCodingScheme( DPHE_CODING_SCHEME );
      umlsConcept.setCode( value.getUri() );
      final FSArray ontologyConcepts = new FSArray( jcas, 1 );
      ontologyConcepts.set( 0, umlsConcept );
      valueModifier.setOntologyConceptArr( ontologyConcepts );
      valueModifier.addToIndexes();
      return valueModifier;
   }


   /**
    * Create the degree of relation and add is to the cas
    * http://blulab.chpc.utah.edu/ontologies/v2/ConText.owl#Severity
    * http://blulab.chpc.utah.edu/ontologies/v2/ConText.owl#Degree
    *
    * @param jCas         -
    * @param eventMention -
    * @param modifier     -
    */
   final public void createEventMentionDegree( final JCas jCas,
                                               final E eventMention,
                                               final Modifier modifier ) {
      final RelationArgument typeArgument = new RelationArgument( jCas );
      typeArgument.setArgument( eventMention );
      typeArgument.setRole( "Argument" );
      typeArgument.addToIndexes();
      final RelationArgument valueArgument = new RelationArgument( jCas );
      valueArgument.setArgument( modifier );
      valueArgument.setRole( "Related_to" );
      valueArgument.addToIndexes();
      final DegreeOfTextRelation degreeOf = new DegreeOfTextRelation( jCas );
      degreeOf.setArg1( typeArgument );
      degreeOf.setArg2( valueArgument );
      degreeOf.setCategory( "Degree_of" );
      degreeOf.addToIndexes();
   }


}

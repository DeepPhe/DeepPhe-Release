package org.healthnlp.deepphe.nlp.ae.attribute;

import org.apache.ctakes.core.util.annotation.ConceptBuilder;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.nlp.uri.UriInfoUtil;

import java.util.Collection;
import java.util.HashSet;

import static org.apache.ctakes.ner.group.dphe.DpheGroup.DPHE_GROUPING_SCHEME;
import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.DPHE_CODING_SCHEME;
import static org.healthnlp.deepphe.neo4j.constant.Neo4jConstants.DPHE_VALUE_SCHEME;

/**
 * @author SPF , chip-nlp
 * @since {11/16/2023}
 */
final public class AnnotationFactory {

   private AnnotationFactory() {}


   static public IdentifiedAnnotation createAnnotation( final JCas jCas,
                                                        final int begin, final int end,
                                                        final DpheGroup group,
                                                        final String uri,
                                                        final String cui,
                                                        final String prefText ) {
      final IdentifiedAnnotationBuilder builder = new IdentifiedAnnotationBuilder()
            .span( begin, end )
            .type( group.getTui() )
            .confidence( 80f )
            .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
            .subject( CONST.ATTR_SUBJECT_PATIENT );
      addConcepts( jCas, builder, group, uri, cui, prefText );
      UriInfoCache.getInstance().initBasics( uri, cui, group, prefText );
      return builder.build( jCas );
   }

   static public IdentifiedAnnotation createAnnotation( final JCas jCas,
                                                        final int begin, final int end,
                                                        final DpheGroup group,
                                                        final String uri,
                                                        final String cui,
                                                        final String prefText,
                                                        final String value ) {
      final IdentifiedAnnotationBuilder builder = new IdentifiedAnnotationBuilder()
            .span( begin, end )
            .type( group.getTui() )
            .confidence( 80f )
            .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE )
            .subject( CONST.ATTR_SUBJECT_PATIENT );
      addConcepts( jCas, builder, group, uri, cui, prefText );
      addValue( jCas, builder, group, cui, prefText, value );
      UriInfoCache.getInstance().initBasics( uri, cui, group, prefText );
      return builder.build( jCas );
   }

   static private void addConcepts( final JCas jCas,
                                    final IdentifiedAnnotationBuilder annotationBuilder,
                                    final DpheGroup group,
                                    final String uri,
                                    final String cui,
                                    final String prefText ) {
      final Collection<OntologyConcept> concepts = new HashSet<>();
      final ConceptBuilder builder = new ConceptBuilder()
            .cui( cui )
            .type( group.getTui() )
            .preferredText( prefText );
      concepts.add( builder.schema( DPHE_CODING_SCHEME )
                           .code( uri )
                           .build( jCas ) );
      concepts.add( builder.schema( DPHE_GROUPING_SCHEME )
                           .code( group.getName() )
                           .build( jCas ) );
      concepts.forEach( annotationBuilder::concept );
   }

   static private void addValue( final JCas jCas,
                                    final IdentifiedAnnotationBuilder annotationBuilder,
                                    final DpheGroup group,
                                    final String cui,
                                    final String prefText,
                                 final String value ) {
      final OntologyConcept concept = new ConceptBuilder()
            .cui( cui )
            .type( group.getTui() )
            .preferredText( prefText )
            .schema( DPHE_VALUE_SCHEME )
            .code( value.toLowerCase() )
            .build( jCas );
      annotationBuilder.concept( concept );
   }


}

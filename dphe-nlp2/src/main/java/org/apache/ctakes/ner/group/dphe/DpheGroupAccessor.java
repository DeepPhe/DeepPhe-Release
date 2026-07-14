package org.apache.ctakes.ner.group.dphe;

import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.ner.group.AnnotationGroupAccessor;
import org.apache.ctakes.ner.group.DetailedTermGroupAccessor;
import org.apache.ctakes.ner.group.GroupHierarchy;
import org.apache.ctakes.ner.term.DetailedTerm;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.ctakes.ner.group.dphe.DpheGroup.*;

/**
 * @author SPF , chip-nlp
 * @since {11/8/2023}
 */
public enum DpheGroupAccessor implements DetailedTermGroupAccessor<DpheGroup>,
      AnnotationGroupAccessor<DpheGroup>, GroupHierarchy<DpheGroup> {
   INSTANCE;

   static public DpheGroupAccessor getInstance() {
      return INSTANCE;
   }


   private final Trie<DpheGroup> _trie;

   DpheGroupAccessor() {
      _trie = new Trie<>();
      _trie.addRoot( BODY_PART );
      _trie.addChild( BODY_PART, ORGAN  );
      _trie.addChild( ORGAN, BODY_REGION );
      _trie.addChild( BODY_REGION, BODY_CAVITY );
      _trie.addChild( BODY_CAVITY, ORGAN_SYSTEM );
      // Don't want something like "Lymph Node" to subsume "Lymph"
//      _trie.addChild( ORGAN_SYSTEM, BODY_FLUID_OR_SUBSTANCE );
//      _trie.addRoot( TISSUE );
//      _trie.addChild( TISSUE, BODY_FLUID_OR_SUBSTANCE );
      _trie.addRoot( PHARMACOLOGIC_SUBSTANCE );
      _trie.addChild( PHARMACOLOGIC_SUBSTANCE, THERAPY_REGIMEN );
      _trie.addChild( PHARMACOLOGIC_SUBSTANCE, PROPERTY_OR_ATTRIBUTE );
      _trie.addRoot( CANCER );
      _trie.addChild( CANCER, DISEASE_OR_DISORDER );
      _trie.addRoot( MASS );
      _trie.addChild( MASS, DISEASE_OR_DISORDER );
//      _trie.addRoot( INTERVENTION_OR_PROCEDURE );
//      _trie.addChild( INTERVENTION_OR_PROCEDURE, GENE );
      _trie.addRoot( GENE_PRODUCT );
      _trie.addChild( GENE_PRODUCT, GENE );
      _trie.addChild( GENE, ABNORMAL_CELL );
      _trie.addChild( GENE, PROPERTY_OR_ATTRIBUTE );

//      _trie.addRoot( DpheGroup.SPATIAL_QUALIFIER );
//      _trie.addChild( DpheGroup.SPATIAL_QUALIFIER, DpheGroup.FINDING );
//      _trie.addChild( DpheGroup.SPATIAL_QUALIFIER, DpheGroup.PHARMACOLOGIC_SUBSTANCE );
//      _trie.addChild( DpheGroup.SPATIAL_QUALIFIER, DpheGroup.GENE );
//      _trie.addChild( DpheGroup.SPATIAL_QUALIFIER, DpheGroup.GENE_PRODUCT );
   }

   @Override
   public List<DpheGroup> getGroups() {
      return Arrays.asList( DpheGroup.values() );
   }

   @Override
   public Trie<DpheGroup> getTrie() {
      return _trie;
   }

   // Do not use Organ System.  It is too broad and subsumes too many -unrelated- body parts.
   static private final Collection<DpheGroup> ANATOMY = new HashSet<>( Arrays.asList(
//         ORGAN, ORGAN_SYSTEM, BODY_PART, BODY_CAVITY, BODY_REGION ) );
         ORGAN, BODY_PART, BODY_CAVITY, BODY_REGION ) );
   static private final Function<DpheGroup,DpheGroup> bestGroup = g -> ANATOMY.contains( g ) ? BODY_PART : g;

   @Override
   public Collection<DpheGroup> getAnnotationGroups( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getCodes( annotation, DpheGroup.DPHE_GROUPING_SCHEME )
                                .stream()
                                .map( this::getByName )
                                .filter( Objects::nonNull )
                                .map( bestGroup )
                                .collect( Collectors.toList() );
   }

   @Override
   public Collection<DpheGroup> getDetailedTermGroups( final DetailedTerm term ) {
      return term.getGroupNames()
                                .stream()
                                .map( this::getByName )
                                .filter( Objects::nonNull )
                                .collect( Collectors.toList() );
   }

   /**
    *
    * @return group to be used when a return value is null.
    */
   @Override
   public DpheGroup getNullGroup() {
      return DpheGroup.UNKNOWN;
   }

}

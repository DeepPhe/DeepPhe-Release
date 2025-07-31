package org.healthnlp.deepphe.nlp.attribute.topo_minor.brain;


import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.Collection;
import java.util.HashSet;

/**
 * https://training.seer.cancer.gov/brain/tumors/abstract-code-stage/topographic.html
 * @author SPF , chip-nlp
 * @since {4/5/2023}
 */
public enum BrainUriCollection {
   INSTANCE;

   static public BrainUriCollection getInstance() {
      return INSTANCE;
   }

   private Collection<String> _meningesUris;
   private Collection<String> _brainUris;
   private Collection<String> _nerveUris;

   // C70 is meninges
   private Collection<String> _cerebralMeninges;
   private Collection<String> _spinalMeninges;
   private Collection<String> _meningesNOS;
   // C71 is Brain
   private Collection<String> _brain_0;
   private Collection<String> _frontalLobe;
   private Collection<String> _temporalLobe;
   private Collection<String> _parietalLobe;
   private Collection<String> _occipitalLobe;
   private Collection<String> _ventricle;
   private Collection<String> _brain_6;
   private Collection<String> _brain_7;
   private Collection<String> _brain_8;
   private Collection<String> _brain_9;
   // C72 is Spinal Cord / Other CNS
   private Collection<String> _spinalCord;
   private Collection<String> _caudaEquina;
   private Collection<String> _olfactoryNerve;
   private Collection<String> _opticNerve;
   private Collection<String> _acousticNerve;
   private Collection<String> _cranialNerve;
//   private Collection<String> _overlapping;
   private Collection<String> _cnsNOS;

   BrainUriCollection() {
      initBrainUris();
   }

   private void initBrainUris() {
      _cerebralMeninges = getBranchUris( "CerebralMeninges" );
      _spinalMeninges = getBranchUris( "SpinalMeninges" );
      _meningesNOS = getBranchUris( "Meninges" );
      _meningesNOS.removeAll( _cerebralMeninges );
      _meningesNOS.removeAll( _spinalMeninges );

      _brain_0 = getBranchUris( "SupratentorialBrain" );
      _frontalLobe = getBranchUris( "Forehead" );
      _temporalLobe = getBranchUris( "TemporalLobe" );
      _temporalLobe.addAll( getBranchUris( "Uncus" ) );
      _parietalLobe = getBranchUris( "ParietalLobe" );
      _occipitalLobe = getBranchUris( "OccipitalLobe" );
      _ventricle = getBranchUris( "BrainVentricle" );
      final Collection<String> fourthVentricle = getBranchUris( "FourthVentricle" );
      _ventricle.removeAll( fourthVentricle );
      _brain_6 = getBranchUris( "Cerebellum" );
      _brain_7 = getBranchUris( "BrainStem" );
      _brain_8 = getBranchUris( "CorpusCallosum" );
      _brain_9 = getBranchUris( "Intracranial" );
//      _brain_9.addAll( getBranchUris( "Cranial_Fossa__NOS" ) );
      _brain_9.addAll( getBranchUris( "SuprasellarRegion" ) );
      _brain_7.addAll( fourthVentricle );

      _spinalCord = getBranchUris( "SpinalCord" );
      _caudaEquina = getBranchUris( "CaudaEquina" );
      _olfactoryNerve = getBranchUris( "OlfactoryNerve" );
      _opticNerve = getBranchUris( "OpticNerve" );
      _acousticNerve = getBranchUris( "AcousticNerve" );
      _cranialNerve = getBranchUris( "CranialNerve" );
      _cnsNOS = getBranchUris( "NervousSystemPart" );
      _cnsNOS.removeAll( _brain_9 );
      _cnsNOS.removeAll( _brain_0 );
      _cnsNOS.removeAll( _cranialNerve );

      _brain_9.removeAll( _brain_0 );
      _brain_9.removeAll( _ventricle );
      _brain_9.removeAll( _brain_6 );
      _brain_9.removeAll( _brain_7 );

      _brain_0.removeAll( _frontalLobe );
      _brain_0.removeAll( _temporalLobe );
      _brain_0.removeAll( _parietalLobe );
      _brain_0.removeAll( _occipitalLobe );
      _brain_0.removeAll( _brain_8 );
      _brain_0.removeAll( _brain_9 );

      _cranialNerve.removeAll( _olfactoryNerve );
      _cranialNerve.removeAll( _opticNerve );
      _cranialNerve.removeAll( _acousticNerve );
      _brain_0.removeAll( _olfactoryNerve );

      // C70 is meninges
      _meningesUris = new HashSet<>();
      _meningesUris.addAll( _cerebralMeninges );
      _meningesUris.addAll( _spinalMeninges );
      _meningesUris.addAll( _meningesNOS );
      // C71 is Brain
      _brainUris = new HashSet<>();
      _brainUris.addAll( _brain_0 );
      _brainUris.addAll( _frontalLobe );
      _brainUris.addAll( _temporalLobe );
      _brainUris.addAll( _parietalLobe );
      _brainUris.addAll( _occipitalLobe );
      _brainUris.addAll( _ventricle );
      _brainUris.addAll( _brain_6 );
      _brainUris.addAll( _brain_7 );
      _brainUris.addAll( _brain_8 );
      _brainUris.addAll( _brain_9 );
      // C72 is Spinal Cord
      _nerveUris = new HashSet<>();
      _nerveUris.addAll( _spinalCord );
      _nerveUris.addAll( _caudaEquina );
      _nerveUris.addAll( _olfactoryNerve );
      _nerveUris.addAll( _opticNerve );
      _nerveUris.addAll( _acousticNerve );
      _nerveUris.addAll( _cranialNerve );
//   _allBrainUris.addAll( _overlapping );
      _nerveUris.addAll( _cnsNOS );
   }


   public Collection<String> getMeningesUris() {
      return _meningesUris;
   }

   public Collection<String> getBrainUris() {
      return _brainUris;
   }

   public Collection<String> getNerveUris() {
      return _nerveUris;
   }

   public Collection<String> getCerebralMeninges() {
      return _cerebralMeninges;
   }

   public Collection<String> getSpinalMeninges() {
      return _spinalMeninges;
   }

   public Collection<String> getMeningesNOS() {
      return _meningesNOS;
   }

   public Collection<String> getBrain_0() {
      return _brain_0;
   }

   public Collection<String> getFrontalLobe() {
      return _frontalLobe;
   }

   public Collection<String> getTemporalLobe() {
      return _temporalLobe;
   }

   public Collection<String> getParietalLobe() {
      return _parietalLobe;
   }

   public Collection<String> getOccipitalLobe() {
      return _occipitalLobe;
   }

   public Collection<String> getVentricle() {
      return _ventricle;
   }

   public Collection<String> getBrain_6() {
      return _brain_6;
   }

   public Collection<String> getBrain_7() {
      return _brain_7;
   }

   public Collection<String> getBrain_8() {
      return _brain_8;
   }

   public Collection<String> getBrain_9() {
      return _brain_9;
   }

   public Collection<String> getSpinalCord() {
      return _spinalCord;
   }

   public Collection<String> getCaudaEquina() {
      return _caudaEquina;
   }

   public Collection<String> getOlfactoryNerve() {
      return _olfactoryNerve;
   }

   public Collection<String> getOpticNerve() {
      return _opticNerve;
   }

   public Collection<String> getAcousticNerve() {
      return _acousticNerve;
   }

   public Collection<String> getCranialNerve() {
      return _cranialNerve;
   }

   public Collection<String> getCnsNOS() {
      return _cnsNOS;
   }

   static private Collection<String> getBranchUris( final String root ) {
      return new HashSet<>( UriInfoCache.getInstance().getUriBranch(  root ) );
   }

}

package org.healthnlp.deepphe.uima.fhir;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.ConceptInstanceFactory;
import org.apache.ctakes.cancer.concept.instance.ConceptRelationUtil;
import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.Time;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.Relation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.fhir.AnatomicalSite;
import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRRegistry;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.healthnlp.deepphe.util.OntologyUtils;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Condition.ConditionEvidenceComponent;
import org.hl7.fhir.instance.model.DomainResource;
import org.hl7.fhir.instance.model.Extension;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class cTAKESUtils {

   static private final Logger LOGGER = Logger.getLogger( "cTAKESUtils" );

   /**
    * get FHIR date object from cTAKES time mention
    *
    * @param tm -
    * @return -
    */
   public static java.util.Date getDate( TimeMention tm ) {
      Time t = tm.getTime();
      org.apache.ctakes.typesystem.type.refsem.Date dt = tm.getDate();
      String yr = dt.getYear();
      String dy = dt.getDay();
      String mo = dt.getMonth();

      String time = "";
      if ( tm.getTime() != null )
         time = tm.getTime().getNormalizedForm();

      String dateTime = (mo + "/" + dy + "/" + yr + " " + time).trim();

      return TextTools.parseDate( dateTime );
   }

   // public static Extension createMentionExtension( final ConceptInstance
   // conceptInstance ) {
   // IdentifiedAnnotation mention = conceptInstance.getIdentifiedAnnotation();
   // StringBuffer b = new StringBuffer(mention.getCoveredText());
   // int st = mention.getBegin();
   // int en = mention.getEnd();
   // if(!equals(conceptInstance,FHIRConstants.QUANTITY_URI)){
   // for(ConceptInstance v :
   // ConceptInstanceUtil.getPropertyValues(conceptInstance)){
   // b.append(v.getIdentifiedAnnotation().getCoveredText());
   // en = v.getIdentifiedAnnotation().getEnd();
   // }
   // }
   // return FHIRUtils.createMentionExtension(b.toString(),st, en );
   // }

   //static private final Comparator<IdentifiedAnnotation> SpanSort = (a1, a2) -> Integer.compare(a1.getBegin(),	a2.getBegin());

   public static List<Extension> createMentionExtensions( final ConceptInstance conceptInstance ) {
      List<Extension> list = new ArrayList<Extension>();
      for ( IdentifiedAnnotation a : conceptInstance.getAnnotations() ) {
         list.add( createMentionExtension( a ) );
      }
      return list;
   }

   /**
    * add extention that is related to which section this mention came from
    *
    * @param conceptInstance
    * @return
    */

   public static List<Extension> createSectionExtensions( final ConceptInstance conceptInstance ) {
      List<Extension> list = new ArrayList<Extension>();
      for ( IdentifiedAnnotation a : conceptInstance.getAnnotations() ) {
         for ( Segment seg : JCasUtil.selectCovering( getJcas( a ), Segment.class, a ) ) {
            String sectionName = seg.getPreferredText();
            if ( sectionName != null ) {
               list.add( FHIRUtils.createExtension( FHIRUtils.SECTION_URL, sectionName ) );
            }
         }
      }
      return list;
   }


   public static List<ConceptInstance> getLocations( ConceptInstance conceptInstance ) {
      List<ConceptInstance> list = new ArrayList<ConceptInstance>();
      for ( ConceptInstance ci : ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.BODY_SITE_URI ) ) {
         // skip null uris
         if ( ci.getUri() == null || ci.getUri().trim().length() == 0 ) {
            LOGGER.error( "Null URI detected for " + ci + " for " + ci.getCoveredText() );
            continue;
         }
         Element site = cTAKESUtils.getResource( ci );
         if ( site != null && site instanceof AnatomicalSite ) {
            list.add( ci );
         }
      }
      return list;
   }

	
	
	
	/*
	public static Extension createMentionExtension( final ConceptInstance conceptInstance ) {
      Collection<IdentifiedAnnotation> mentions = conceptInstance.getAnnotations();
      String mentionText = mentions.stream()
            .sorted( SpanSort )
            .map( Annotation::getCoveredText )
            .collect( Collectors.joining() );
      int st = Integer.MAX_VALUE;
      int en = Integer.MIN_VALUE;
      for ( IdentifiedAnnotation mention : mentions ) {
         st = Math.min( st, mention.getBegin() );
         en = Math.max( en, mention.getEnd() );
      }
      if(!equals(conceptInstance,FHIRConstants.QUANTITY_URI)){
         final String valueText = ConceptInstanceUtil.getPropertyValues( conceptInstance ).stream()
               .map( ConceptInstance::getAnnotations )
               .flatMap( Collection::stream )
               .sorted( SpanSort )
               .map( Annotation::getCoveredText )
               .collect( Collectors.joining() );
         mentionText += valueText;
         final int valueEnd = ConceptInstanceUtil.getPropertyValues( conceptInstance ).stream()
               .map( ConceptInstance::getAnnotations )
               .flatMap( Collection::stream )
               .map( Annotation::getEnd )
               .mapToInt( i -> i )
               .max()
					.orElse( en );
			en = Math.max( en, valueEnd );
      }
      return FHIRUtils.createMentionExtension( mentionText, st, en );
   }
   */

   public static Extension createMentionExtension( final IdentifiedAnnotation mention ) {
      StringBuffer b = new StringBuffer( mention.getCoveredText() );
      int st = mention.getBegin();
      int en = mention.getEnd();
      return FHIRUtils.createMentionExtension( b.toString(), st, en );
   }

//	public static List<IdentifiedAnnotation> getBodySiteModifiers(IdentifiedAnnotation ia) {
//		List<IdentifiedAnnotation> list = new ArrayList<IdentifiedAnnotation>();
//		list.addAll(PhenotypeAnnotationUtil.getBodySides(ia));
//		list.addAll(PhenotypeAnnotationUtil.getQuadrants(ia));
//		list.addAll(PhenotypeAnnotationUtil.getClockwises(ia));
//		return list;
//	}


   public static Set<IdentifiedAnnotation> getBodySiteModifiers( ConceptInstance conceptInstance ) {
      Set<IdentifiedAnnotation> list = new LinkedHashSet<IdentifiedAnnotation>();
      for ( ConceptInstance neoplasm : ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.NEOPLASM_URIS ) ) {
         for ( IdentifiedAnnotation ia : cTAKESUtils.getBodySiteModifiers4Tumor( neoplasm ) ) {
            list.add( ia );
         }
      }
      return list;
   }


   private static List<IdentifiedAnnotation> getBodySiteModifiers4Tumor( final ConceptInstance conceptInstance ) {
      final List<IdentifiedAnnotation> list = new ArrayList<>();
//		list.addAll(getBodyModifiers(conceptInstance, PhenotypeAnnotationUtil::getBodySides));
//		list.addAll(getBodyModifiers(conceptInstance, PhenotypeAnnotationUtil::getQuadrants));
//		list.addAll(getBodyModifiers(conceptInstance, PhenotypeAnnotationUtil::getClockwises));
      ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.LATERALITY_URI )
            .forEach( ci -> list.addAll( ci.getAnnotations() ) );
      ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.QUADRANT_URI )
            .forEach( ci -> list.addAll( ci.getAnnotations() ) );
      ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.CLOCKFACE_POSITION_URI )
            .forEach( ci -> list.addAll( ci.getAnnotations() ) );
      return list;
   }


   static private final Predicate<IdentifiedAnnotation> neoplasmAnnotation = a -> DiseaseDisorderMention.class.isInstance( a ) || SignSymptomMention.class.isInstance( a );

   static private Collection<IdentifiedAnnotation> getBodyModifiers( final ConceptInstance conceptInstance,
                                                                     final Function<Collection<IdentifiedAnnotation>, Collection<IdentifiedAnnotation>> mapper ) {
      return conceptInstance.getAnnotations().stream().filter( neoplasmAnnotation )
            .map( Collections::singletonList ).map( mapper ).flatMap( Collection::stream ).collect( Collectors.toSet() );
/*		return conceptInstance.getAnnotations().stream().filter(AnatomicalSiteMention.class::isInstance)
				.map(Collections::singletonList).map(mapper).flatMap(Collection::stream).collect(Collectors.toSet());*/
   }

   public static boolean equals( ConceptInstance conceptInstance, URI u ) {
      return u.toString().equals( conceptInstance.getUri() );
   }

   // /**
   // * get codeblce concept form OntologyConcept annotation
   // * @param ia -
   // * @return -
   // */
   // public static CodeableConcept getCodeableConcept(IdentifiedAnnotation
   // ia){
   // return setCodeableConcept(new CodeableConcept(),ia);
   // }
   //
   //
   // /**
   // * get codeblce concept form OntologyConcept annotation
   // * @param ia -
   // * @return -
   // */
   // public static CodeableConcept getCodeableConcept( ConceptInstance ia ) {
   // //TODO: maybe make better
   // return setCodeableConcept( new CodeableConcept(),
   // ia.getIdentifiedAnnotation() );
   // }

   // /**
   // * get codeblce concept form OntologyConcept annotation
   // * @param cc -
   // * @param ia -
   // * @return -
   // */
   // public static CodeableConcept setCodeableConcept(CodeableConcept
   // cc,ConceptInstance ia){
   // return setCodeableConcept(cc, ia.getIdentifiedAnnotation());
   // }

   // /**
   // * get codeblce concept form OntologyConcept annotation
   // * @param cc -
   // * @param ia -
   // * @return -
   // */
   // public static CodeableConcept setCodeableConcept(CodeableConcept
   // cc,IdentifiedAnnotation ia){
   // cc.setText(ia.getCoveredText());
   //
   // // go over mapped concepts (merge them into multiple coding systems)
   // if(ia.getOntologyConceptArr() != null){
   // List<String> cuis = new ArrayList<String>();
   // for(int i=0;i<ia.getOntologyConceptArr().size();i++){
   // OntologyConcept c = ia.getOntologyConceptArr(i);
   //
   // // add coding for this concept
   // Coding ccc = cc.addCoding();
   // ccc.setCode(c.getCode());
   // ccc.setDisplay(getConceptName(ia));
   // ccc.setSystem(c.getCodingScheme());
   // cc.setText(ccc.getDisplay()); //TODO: decide if i want to use URI name or
   // string
   // cuis.add(c.getCode());
   //
   // // add codign for UMLS
   // if(c instanceof UmlsConcept){
   // String cui = ((UmlsConcept)c).getCui();
   // if(!cuis.contains(cui)){
   // Coding cccc = cc.addCoding();
   // cccc.setCode(cui);
   // cccc.setDisplay(((UmlsConcept)c).getPreferredText());
   // cccc.setSystem(FHIRUtils.SCHEMA_UMLS);
   // cuis.add(cui);
   // }
   // }
   // }
   // }
   // // set display text if unavialble
   // if(cc.getText() == null){
   // for(Coding ccc: cc.getCoding()){
   // if(ccc.getDisplay() != null){
   // cc.setText(ccc.getDisplay());
   // break;
   // }
   // }
   // }
   //
   //
   // return cc;
   // }

   /**
    * get codeblce concept form OntologyConcept annotation
    *
    * @param ia
    *            -
    * @return -
    *
   public static CodeableConcept getCodeableConcept(final IdentifiedAnnotation ia) {
   return setCodeableConcept(new CodeableConcept(), Collections.singletonList(ia));
   }
    */
   /**
    * get codeblce concept form OntologyConcept annotation
    *
    * @param conceptInstance -
    * @return -
    */
   public static CodeableConcept getCodeableConcept( final ConceptInstance conceptInstance ) {
      return FHIRUtils.getCodeableConcept( URI.create( conceptInstance.getUri() ) );

      // TODO: maybe make better
      //return setCodeableConcept(new CodeableConcept(), conceptInstance);
   }

   /**
    * get codeblce concept form OntologyConcept annotation
    *
    * @param cc              -
    * @param conceptInstance -
    * @return -
    */
   public static CodeableConcept setCodeableConcept( final CodeableConcept cc, final ConceptInstance conceptInstance ) {
      FHIRUtils.setCodeableConcept( cc, URI.create( conceptInstance.getUri() ) );
      return cc;
   }

   /**
    * get codeblce concept form OntologyConcept annotation
    *
    * @param cc
    * -
    * @param annotations
    * -
    * @return -
    * <p>
    * public static CodeableConcept setCodeableConcept(final CodeableConcept cc,final Collection<IdentifiedAnnotation> annotations) {
    * final String coveredText = annotations.stream().sorted(FirstSpan).map(IdentifiedAnnotation::getCoveredText).collect(Collectors.joining());
    * cc.setText(coveredText);
    * // go over mapped concepts (merge them into multiple coding systems)
    * for (IdentifiedAnnotation annotation : annotations) {
    * final String conceptName = getConceptName(annotation);
    * final Map<String, Collection<String>> schemeCodes = OntologyConceptUtil.getSchemeCodes(annotation);
    * List<String> cuis = new ArrayList<String>();
    * for (Map.Entry<String, Collection<String>> entry : schemeCodes.entrySet()) {
    * for (String code : entry.getValue()) {
    * Coding ccc = cc.addCoding();
    * ccc.setCode(code);
    * ccc.setDisplay(conceptName);
    * ccc.setSystem(entry.getKey());
    * cc.setText(ccc.getDisplay()); // TODO: decide if i want to
    * // use URI name or string
    * cuis.add(code);
    * }
    * }
    * // add coding for this concept
    * for (String cui : OntologyConceptUtil.getCuis(annotation)) {
    * if (cuis.contains(cui)) {
    * continue;
    * }
    * Coding cccc = cc.addCoding();
    * cccc.setCode(cui);
    * cccc.setDisplay(annotation.getCoveredText());
    * cccc.setSystem(FHIRUtils.SCHEMA_UMLS);
    * cuis.add(cui);
    * }
    * }
    * <p>
    * // set display text if unavialble
    * if (cc.getText() == null) {
    * for (Coding ccc : cc.getCoding()) {
    * if (ccc.getDisplay() != null) {
    * cc.setText(ccc.getDisplay());
    * break;
    * }
    * }
    * }
    * return cc;
    * }
    */
   static private final Comparator<IdentifiedAnnotation> FirstSpan = ( a1, a2 ) -> Integer.compare( a1.getBegin(),
         a2.getBegin() );

   static private final Pattern CUI_PATTERN = Pattern.compile( "CL?\\d{6,7}" );
   static private final java.util.function.Predicate<String> CUI_PREDICATE = CUI_PATTERN.asPredicate();

   /**
    * get concept class from a default ontology based on Concept
    *
    * @param ia -
    * @return -
    */
   public static String getConceptCode( IdentifiedAnnotation ia ) {
      // TODO - can use OntologyConceptUtil.getCuis( ia );
      return OntologyConceptUtil.getCuis( ia ).stream().filter( CUI_PREDICATE ).findFirst().get();
      // String cui = null;
      // for(int i=0;i<ia.getOntologyConceptArr().size();i++){
      // OntologyConcept c = ia.getOntologyConceptArr(i);
      // if(c instanceof UmlsConcept){
      // cui = ((UmlsConcept)c).getCui();
      // }else{
      // cui = c.getCode();
      // }
      // if(cui != null && cui.matches("CL?\\d{6,7}"))
      // break;
      // }
      // return cui;
   }

   /**
    * get concept class from a default ontology based on Concept
    *
    * @param ia -
    * @return -
    */
   public static String getConceptURI( ConceptInstance ia ) {
      return getConceptURI( ia.getAnnotations().iterator().next() );
   }

   /**
    * get concept class from a default ontology based on Concept
    *
    * @param ia -
    * @return -
    */
   public static String getConceptURI( IdentifiedAnnotation ia ) {
      // TODO - can use OwlOntologyConceptUtil.getUris( ia );
      return OwlOntologyConceptUtil.getUris( ia ).stream().findFirst().orElse( OwlConstants.UNKNOWN_URI );
      // String cui = null;
      // for(int i=0;i<ia.getOntologyConceptArr().size();i++){
      // OntologyConcept c = ia.getOntologyConceptArr(i);
      // cui = c.getCode();
      // if(cui != null && cui.startsWith("http://"))
      // break;
      // }
      // return cui;
   }

   /**
    * get concept class from a default ontology based on Concept
    *
    * @param ia -
    * @return -
    */
   public static String getConceptName( IdentifiedAnnotation ia ) {
      if ( ia == null )
         return null;
      String name = getConceptURI( ia );
      if ( name != null )
         name = FHIRUtils.getConceptName( URI.create( name ) );
      return name == null ? ia.getCoveredText() : name;
   }

   /**
    * get related item from cTAKES
    *
    * @param source   -
    * @param relation -
    * @return -
    */
   public static IdentifiedAnnotation getRelatedItem( IdentifiedAnnotation source, Relation relation ) {
      if ( relation != null ) {
         if ( relation instanceof BinaryTextRelation ) {
            BinaryTextRelation r = (BinaryTextRelation) relation;
            if ( r.getArg1().getArgument().equals( source ) )
               return (IdentifiedAnnotation) r.getArg2().getArgument();
            else
               return (IdentifiedAnnotation) r.getArg1().getArgument();
         }
      }
      return null;
   }

   /**
    * get document text for a given annotated JCas
    *
    * @param cas -
    * @return -
    */
   public static String getDocumentText( JCas cas ) {
      return cas.getDocumentText();
//		Iterator<Annotation> it = cas.getAnnotationIndex(DocumentAnnotation.type).iterator();
//		if (it.hasNext())
//			return it.next().getCoveredText();
//		return null;
   }

   /**
    * get concept class from a default ontology based on Concept
    *
    * @param ont -
    * @param m   -
    * @return -
    */
   public static IClass getConceptClass( IOntology ont, IdentifiedAnnotation m ) {
      // CancerSize doesn't have a CUI, but can be mapped
		/*
		 * if(m instanceof CancerSize){ return ont.getClass(TUMOR_SIZE); }
		 */

      String cui = getConceptURI( m );
      return cui != null ? ont.getClass( cui ) : null;
   }

   /**
    * get a set of concept by type from the annotated document
    *
    * @param cas  -
    * @param type -
    * @return -
    */
   public static List<IdentifiedAnnotation> getAnnotationsByType( JCas cas, int type ) {
      List<IdentifiedAnnotation> list = new ArrayList<>();
      Iterator<Annotation> it = cas.getAnnotationIndex( type ).iterator();
      while ( it.hasNext() ) {
         IdentifiedAnnotation ia = (IdentifiedAnnotation) it.next();
         // don't add stuff that doesn't have a Class or ontology array
         // if(getConceptClass(ia) != null)
         list.add( ia );
      }
      return filterAnnotations( list );
   }

   /**
    * get a set of concept by type from the annotated document
    *
    * @param cas  -
    * @param type -
    * @return -
    */
   public static List<ConceptInstance> getAnnotationsByType( JCas cas, URI type ) {
      // TODO is manipulation required?
      return new ArrayList<>( ConceptInstanceFactory.createBranchConceptInstances( cas, type.toString() ) );
      // List<IdentifiedAnnotation> annotations = new
      // ArrayList<IdentifiedAnnotation>();
      // for(IdentifiedAnnotation a:
      // OwlOntologyConceptUtil.getAnnotationsByUriBranch(cas,type.toString())){
      // annotations.add(a);
      // }
      // return annotations;
   }

   /**
    * get a set of concept by type from the annotated document
    *
    * @param cas  -
    * @param type -
    * @return -
    */
   public static List<Relation> getRelationsByType( JCas cas, Type type ) {
      List<Relation> list = new ArrayList<>();
      Iterator<FeatureStructure> it = cas.getFSIndexRepository().getAllIndexedFS( type );
      while ( it.hasNext() ) {
         list.add( (Relation) it.next() );
      }
      return list;
   }

   /**
    * get a set of concept by type from the annotated document
    *
    * @param an        -
    * @param classType -
    * @return -
    */
   public static List<Annotation> getRelatedAnnotationsByType( IdentifiedAnnotation an, Class classType ) {
      JCas cas = null;
      try {
         cas = an.getCAS().getJCas();
      } catch ( CASException e1 ) {
         e1.printStackTrace();
      }
      Type type = null;
      try {
         type = (Type) classType.getMethod( "getType" )
               .invoke( classType.getDeclaredConstructor( JCas.class ).newInstance( cas ) );
      } catch ( Exception e ) {
         e.printStackTrace();
      }
      List<Annotation> list = new ArrayList<Annotation>();
      Iterator<FeatureStructure> it = cas.getFSIndexRepository().getAllIndexedFS( type );
      while ( it.hasNext() ) {
         BinaryTextRelation br = (BinaryTextRelation) it.next();
         if ( br.getArg1().getArgument().getCoveredText().equals( an ) ) {
            list.add( br.getArg2().getArgument() );
         } else if ( br.getArg2().getArgument().equals( an ) ) {
            list.add( br.getArg1().getArgument() );
         }
      }
      return list;
   }

   /**
    * get anatomic location of an annotation // * @param an -
    *
    * @return -
    */
	/*
	 * public static AnatomicalSiteMention
	 * getAnatimicLocation(IdentifiedAnnotation an){ // JCas cas = null; // try
	 * { // cas = an.getCAS().getJCas(); // } catch (CASException e) { //
	 * e.printStackTrace(); // } // for(Relation r: getRelationsByType(cas,new
	 * LocationOfTextRelation(cas).getType())){ // LocationOfTextRelation lr =
	 * (LocationOfTextRelation) r; // if(equals(lr.getArg1(),an) &&
	 * lr.getArg2().getArgument() instanceof AnatomicalSiteMention){ // return
	 * (AnatomicalSiteMention) lr.getArg2().getArgument(); // } // } // return
	 * null; // TODO - can use InstanceUtil.getLocations( an ) - there may be
	 * occasions where there are more than 1: breast ; nipple final JCas jcas =
	 * getJcas( an ); if ( jcas == null ) { return null; } return
	 * (AnatomicalSiteMention)InstanceUtil.getLocations( jcas, an ).stream()
	 * .filter( AnatomicalSiteMention.class::isInstance ) .findFirst().get(); }
	 */
	/*
	 * is relation argument equals to identified annotation?
	 */
   private static boolean equals( RelationArgument a, Annotation b ) {
      if ( a.getArgument().equals( b ) )
         return true;
      return (a.getArgument().getCoveredText().equals( b.getCoveredText() )
            && a.getArgument().getBegin() == b.getBegin());
   }

//	/**
//	 * get anatomic location of an annotation
//	 *
//	 * @param an
//	 *            -
//	 * @return -
//	 */
//	public static IdentifiedAnnotation getDegreeOf(IdentifiedAnnotation an) {
//		final JCas jcas = getJcas(an);
//		if (jcas == null) {
//			return null;
//		}
//		if (PhenotypeAnnotationUtil.getPropertyValues(jcas, an).isEmpty())
//			return null;
//		return PhenotypeAnnotationUtil.getPropertyValues(jcas, an).stream().findFirst().get();
//	}

   private static List<IdentifiedAnnotation> filterAnnotations( List<IdentifiedAnnotation> list ) {
      if ( list.isEmpty() || list.size() == 1 )
         return list;
      for ( ListIterator<IdentifiedAnnotation> it = list.listIterator(); it.hasNext(); ) {
         IdentifiedAnnotation m = it.next();
         // keep annotation that might be part of relationship
         if ( !getRelatedAnnotationsByType( m, BinaryTextRelation.class ).isEmpty() )
            continue;

         // filter out if something more specific exists
         // if(hasMoreSpecific(m,list) || hasIdenticalSpan(m,list))
         // it.remove();
      }
      return list;
   }

//	/**
//	 * get size measurement of an identified annotation, if such exists
//	 *
//	 * @param dm
//	 *            -
//	 * @return -
//	 */
//	public static SizeMeasurement getSizeMeasurement(IdentifiedAnnotation dm) {
//		// TODO - can we safely use a MeasurementAnnotation ? Should we just
//		// stick to CancerSize for now?
//		// if cancer size, then use their value
//		if (dm instanceof CancerSize) {
//			// ob.setCode(FHIRUtils.getCodeableConcept(FHIRConstants.TUMOR_SIZE_URI));
//			FSArray arr = ((CancerSize) dm).getMeasurements();
//			if (arr != null) {
//				for (int i = 0; i < arr.size();) {
//					return (SizeMeasurement) arr.get(i);
//				}
//			}
//		}
//		return null;
//	}

   private static boolean hasIdenticalSpan( IdentifiedAnnotation m, List<IdentifiedAnnotation> list ) {
      for ( IdentifiedAnnotation mm : list ) {
         if ( !mm.equals( m ) && mm.getCoveredText().equals( m.getCoveredText() ) )
            return true;
      }
      return false;
   }

   // public static TnmClassification getTnmClassification(IdentifiedAnnotation
   // dm){
   // for(Annotation a:
   // cTAKESUtils.getRelatedAnnotationsByType(dm,NeoplasmRelation.class)){
   // if(a instanceof TnmClassification){
   // return (TnmClassification) a;
   // }
   // }
   // return null;
   // }
//	public static Collection<IdentifiedAnnotation> getTnmClassifications(final IdentifiedAnnotation neoplasm) {
//		final JCas jcas = getJcas(neoplasm);
//		if (jcas == null) {
//			return null;
//		}
//		return PhenotypeAnnotationUtil.getNeoplasmPropertiesBranch(jcas, neoplasm, TnmPropertyUtil.getParentUri());
//
//		/*
//		 * return ConceptInstanceUtil.getBranchConceptInstances(jcas,
//		 * TnmPropertyUtil.getParentUri()).stream().
//		 * map(ConceptInstanceUtil::getPropertyValues).collect(Collectors.toList
//		 * ());;
//		 *
//		 */
//	}

   // public static CancerStage getCancerStage(IdentifiedAnnotation dm){
   // for(Annotation a:
   // cTAKESUtils.getRelatedAnnotationsByType(dm,NeoplasmRelation.class)){
   // if(a instanceof CancerStage){
   // return (CancerStage) a;
   // }
   // }
   // return null;
   // }
//	public static Collection<IdentifiedAnnotation> getCancerStages(final IdentifiedAnnotation neoplasm) {
//		final JCas jcas = getJcas(neoplasm);
//		if (jcas == null) {
//			return null;
//		}
//		return PhenotypeAnnotationUtil.getNeoplasmPropertiesBranch(jcas, neoplasm, OwlConstants.CANCER_STAGE_URI);
//	}

   /**
    * @param annotation any type of annotation
    * @return jcas containing the annotation or null if there is none
    */
   static public JCas getJcas( final TOP annotation ) {
      try {
         return annotation.getCAS().getJCas();
      } catch ( CASException casE ) {
         LOGGER.error( casE.getMessage() );
      }
      return null;
   }

   private static boolean isEmpty( String s ) {
      return s == null || s.trim().length() == 0;
   }

   public static void addResource( Element e, ConceptInstance conceptInstance ) {
      // FHIRRegistry.getInstance().addElement( e,conceptInstance.getIdentifiedAnnotation() );
      //FHIRRegistry.getInstance().addElement(e, conceptInstance);
      if ( !conceptInstance.getAnnotations().isEmpty() )
         FHIRRegistry.getInstance().addElement( e, conceptInstance.getAnnotations().iterator().next() );
   }

   public static void addResource( Element e ) {
      FHIRRegistry.getInstance().addElement( e );
   }

   public static Element getResource( ConceptInstance ci ) {
      // return  FHIRRegistry.getInstance().getElement(ci.getIdentifiedAnnotation());
      //return FHIRRegistry.getInstance().getElement(ci);
      if ( !ci.getAnnotations().isEmpty() )
         return FHIRRegistry.getInstance().getElement( ci.getAnnotations().iterator().next() );
      return null;

   }

   public static void addLanguageContext( ConceptInstance conceptInstance, DomainResource dx ) {
      if ( !isEmpty( conceptInstance.getDocTimeRel() ) ) {
         dx.addExtension( FHIRUtils.createDocTimeRelExtension( conceptInstance.getDocTimeRel() ) );
      }
      if ( !isEmpty( conceptInstance.getModality() ) ) {
         dx.addExtension( FHIRUtils.createModalityExtension( conceptInstance.getModality() ) );
      }

      if ( !isEmpty( conceptInstance.getSubject() ) ) {
         dx.addExtension( FHIRUtils.createExtension( FHIRUtils.LANGUAGE_ASPECT_EXPERIENCER, conceptInstance.getSubject() ) );
      }

      if ( conceptInstance.isNegated() ) {
         dx.addExtension(
               FHIRUtils.createExtension( FHIRUtils.LANGUAGE_ASPECT_NEGATED_URL, "" + conceptInstance.isNegated() ) );
      }
      if ( conceptInstance.isUncertain() ) {
         dx.addExtension( FHIRUtils.createExtension( FHIRUtils.LANGUAGE_ASPECT_UNCERTAIN_URL,
               "" + conceptInstance.isUncertain() ) );
      }
      if ( conceptInstance.isConditional() ) {
         dx.addExtension( FHIRUtils.createExtension( FHIRUtils.LANGUAGE_ASPECT_CONDITIONAL_URL,
               "" + conceptInstance.isConditional() ) );
      }
      if ( conceptInstance.isIntermittent() ) {
         dx.addExtension( FHIRUtils.createExtension( FHIRUtils.LANGUAGE_ASPECT_INTERMITTENT_URL,
               "" + conceptInstance.isIntermittent() ) );
      }
      if ( conceptInstance.isGeneric() ) {
         dx.addExtension( FHIRUtils.createExtension( FHIRUtils.LANGUAGE_ASPECT_HYPOTHETICAL_URL,
               "" + conceptInstance.isGeneric() ) );
      }
      if ( conceptInstance.isPermanent() ) {
         dx.addExtension( FHIRUtils.createExtension( FHIRUtils.LANGUAGE_ASPECT_PERMENENT_URL,
               "" + conceptInstance.isPermanent() ) );
      }
      if ( conceptInstance.inPatientHistory() ) {
         dx.addExtension( FHIRUtils.createExtension( FHIRUtils.LANGUAGE_ASPECT_HISTORICAL_URL,
               "" + conceptInstance.inPatientHistory() ) );
      }


      // add section information
      for ( Extension ex : createSectionExtensions( conceptInstance ) ) {
         dx.addExtension( ex );
      }
   }

   /*
    * get TNM modifier extentions for a TNM instnace
    */
   public static Collection<ConceptInstance> getTNM_Modifiers( final ConceptInstance conceptInstance ) {
      // only return text that matches a predefined list
      // return
      // ConceptInstanceUtil.getDiagnosticTests(conceptInstance).stream().
      // filter(t ->
      // FHIRConstants.TNM_MODIFIER_LIST.contains(t.getIdentifiedAnnotation().getCoveredText())).
      // collect( Collectors.toList());
      return ConceptRelationUtil.getRelatedConcepts( conceptInstance, OwlConstants.DIAGNOSTIC_TEST_URI ).stream()
            .filter( IsTnmModifier )
            .collect( Collectors.toList() );
   }

   static private final Predicate<ConceptInstance> IsTnmModifier = ci -> ci.getAnnotations().stream()
         .map( Annotation::getCoveredText ).anyMatch( FHIRConstants.TNM_MODIFIER_LIST::contains );

   public static Extension createTNM_ModifierExtension( final ConceptInstance conceptInstance ) {
      // TODO: it is a hack, but it is the best I can do for now
      // String mod = i.getIdentifiedAnnotation().getCoveredText();
      final String mod = conceptInstance.getCoveredText();
      // Need to resolve this with melanoma
      return FHIRUtils.createExtension( FHIRUtils.TNM_MODIFIER_URL,
            OwlConstants.BREAST_CANCER_OWL + "#" + mod + "_modifier" );
   }


   /**
    * save condition evidence
    *
    * @param ci
    * @return
    */
   public static ConditionEvidenceComponent createConditionEvidence( ConceptInstance ci ) {
      CodeableConcept cc = getCodeableConcept( ci );
      ConditionEvidenceComponent evidence = new ConditionEvidenceComponent();
      evidence.setCode( cc );
      // get linked resource
      Element element = getResource( ci );
      if ( element != null )
         evidence.addDetail( FHIRUtils.getResourceReference( element ) );
      return evidence;
   }

   /**
    * save condition evidence
    *
    * @param id
    * @return
    */
   public static ConditionEvidenceComponent createConditionEvidence( String id ) {
      ConditionEvidenceComponent evidence = new ConditionEvidenceComponent();
      evidence.addDetail( FHIRUtils.getResourceReference( id ) );
      return evidence;
   }

   static private final List<String> ORDINALS = Arrays.asList(
         OwlConstants.CANCER_OWL + "#Negative", OwlConstants.CANCER_OWL + "#Positive" );

   /**
    * get
    *
    * @param observation
    * @return
    */
   public static ConceptInstance getInterpretation( final ConceptInstance observation ) {
      ConceptInstance anyValue = observation.getValues().stream()
            .filter( ci -> isSubClassOf( ci, FHIRConstants.ORDINAL_INTERPRETATION ) )
            .sorted( ( ci1, ci2 ) -> -1 * (ORDINALS.indexOf( ci1.getUri() ) - ORDINALS.indexOf( ci2.getUri() )) )
            .findFirst().orElse( null );
      if ( anyValue == null ) {
         anyValue = ConceptRelationUtil.getRelatedConcepts( observation, FHIRConstants.ORDINAL_INTERPRETATION ).stream()
               .sorted( ( ci1, ci2 ) -> -1 * (ORDINALS.indexOf( ci1.getUri() ) - ORDINALS.indexOf( ci2.getUri() )) )
               .findFirst().orElse( null );
      }
      return anyValue;
   }


   public static boolean isSubClassOf( ConceptInstance instance, String branch ) {
      String conceptURL = getConceptURI( instance );
      return OntologyUtils.getInstance().hasSubClass( branch, conceptURL );
   }


   /**
    * get value
    *
    * @param observation
    * @return
    */
   public static ConceptInstance getValue( ConceptInstance observation ) {
      for ( ConceptInstance i : observation.getValues() ) {
         if ( cTAKESUtils.equals( i, FHIRConstants.QUANTITY_URI ) ) {
            return i;
         }
      }
      return null;
   }


   /**
    * get observation method
    *
    * @param observation
    * @return
    */
   public static ConceptInstance getMethod( ConceptInstance observation ) {
      for ( ConceptInstance ci : ConceptRelationUtil.getRelatedConcepts( observation, OwlConstants.DIAGNOSTIC_TEST_URI ) ) {
         if ( !FHIRConstants.TNM_MODIFIER_LIST.contains( ci.getCoveredText() ) ) {
            return ci;
         }
      }
      // set procedure method
		/*	ConceptInstanceUtil.getDiagnosticTests(observation).stream().filter(NotTnmModifier).forEach(ci -> {
			ob.setMethod(cTAKESUtils.getCodeableConcept(ci));
			for (Extension ex : cTAKESUtils.createMentionExtensions(ci))
				ob.addExtension(ex);
		});*/
      return null;
   }


}

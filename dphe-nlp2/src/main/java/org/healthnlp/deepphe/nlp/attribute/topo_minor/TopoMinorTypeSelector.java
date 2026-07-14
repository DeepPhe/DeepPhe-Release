package org.healthnlp.deepphe.nlp.attribute.topo_minor;


import org.apache.ctakes.core.util.log.LogFileWriter;
import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.topo_minor.brain.*;
import org.healthnlp.deepphe.nlp.attribute.topo_minor.breast.*;
import org.healthnlp.deepphe.nlp.attribute.topo_minor.crc.*;
import org.healthnlp.deepphe.nlp.attribute.topo_minor.lung.*;
import org.healthnlp.deepphe.nlp.attribute.topo_minor.ovary.*;
import org.healthnlp.deepphe.nlp.attribute.xn.*;
import org.healthnlp.deepphe.nlp.concept.UriConcept;

import java.util.*;


/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class TopoMinorTypeSelector extends DefaultXnAttributeNormalizer implements AttributeInfoCollector {

   static private final Collection<String> MAJOR_SITES = new HashSet<>();
   static private final int[] FACILITY0 = new int[]{ 2, 3, 4, 5, 6, 8, 9 };
   static private final int[] FACILITY = new int[]{
         10, 11, 13, 14, 30, 31, 32, 67, 15, 16, 17, 77, 62, 68, 22, 24, 25,
         70, 71, 72,
         18, 21,
         34,
         50,
         53, 54,
         48, 57,
         44, 51, 60, 63
   };
   static {
      MAJOR_SITES.add( "C00" );
      Arrays.stream( FACILITY0 ).forEach( n -> MAJOR_SITES.add( "C0" + n ) );
      Arrays.stream( FACILITY ).forEach( n -> MAJOR_SITES.add( "C" + n ) );
   }

   private UriConcept _neoplasm;
   private String[] _relationTypes;

   public void init( final UriConcept neoplasm, final String... relationTypes ) {
      _neoplasm = neoplasm;
      _relationTypes = relationTypes;
   }

   public UriConcept getNeoplasm() {
      return _neoplasm;
   }

   public String[] getRelationTypes() {
      return _relationTypes;
   }

   // Right now only uses to highest-scoring topography major.
   // It might be better to rejoin topo major and minor in the future.
   static private String getTopoMajor( final Map<String,List<XnAttributeValue>> dependencies ) {
      final List<XnAttributeValue> topoMajors = dependencies.getOrDefault( "Topography, major", Collections.emptyList() );
      if ( topoMajors.isEmpty() ) {
         return "";
      }
      final String value = topoMajors.get( 0 ).getValue();
      if ( value.length() > 3 ) {
         return value.substring( 0, 3 );
      }
      return value;
   }

   // TODO - right now only the most confident topo major is used.
   // Need to make a combined infoCollector and normalizer that accepts one of each and distributes.
   static private Collection<String> getTopoMajors( final Map<String,List<XnAttributeValue>> dependencies ) {
      final List<XnAttributeValue> topoMajors = dependencies.getOrDefault( "Topography, major", Collections.emptyList() );
      if ( topoMajors.isEmpty() ) {
         return Collections.emptyList();
      }
      final Collection<String> values = new HashSet<>( topoMajors.size() );
      for ( XnAttributeValue major : topoMajors ) {
         final String value = major.getValue();
         LogFileWriter.add( "TopoMinor Major " + value );
         if ( value.length() > 3 ) {
            values.add( value.substring( 0, 3 ) );
         } else {
            values.add( value );
         }
      }
      return values;
   }

   static public MultiCollectorNormalizer getMultiCollectorNormalizer(
         final Map<String,List<XnAttributeValue>> dependencies ) {
      final Collection<String> topoMajors = getTopoMajors( dependencies );
      final Collection<CombinedCollectorNormalizer> combos = new HashSet<>();
      for ( String major : topoMajors ) {
         final AttributeInfoCollector collector = getAttributeInfoCollector( major );
         if ( collector == null ) {
            continue;
         }
         final XnAttributeNormalizer normalizer = getAttributeNormalizer( major );
         if ( normalizer == null ) {
            continue;
         }
         combos.add( new CombinedCollectorNormalizer( collector, normalizer ));
      }
      return new MultiCollectorNormalizer( combos );
   }


//   static public Supplier<AttributeInfoCollector> getAttributeInfoCollector( final Map<String,List<XnAttributeValue>> dependencies ) {
//      final String topoMajor = getTopoMajor( dependencies );
////      LogFileWriter.add( "TopoMinorSelector TopoMajor=" + topoMajor  );
//      if ( topoMajor.equals( "C80" ) ) {
//         return TopoMinorTypeSelector::new;
//      }
//      final boolean hasMinorSite = MAJOR_SITES.contains( topoMajor );
//      if ( !hasMinorSite ) {
//         return TopoMinorTypeSele
//         ctor::new;
//      }
//      // For some reason a switch on topoMajor does not work!  It thinks every case/value is the first case/value.
//      if ( topoMajor.equals( "C50" ) ) {
//         return BreastInfoCollector::new;
//      } else if ( topoMajor.equals( "C18" ) ) {
//            return ColonInfoCollector::new;
//      } else if ( topoMajor.equals( "C21" ) ) {
//            return AnusInfoCollector::new;
//      } else if ( topoMajor.equals( "C34" ) ) {
//            return LungInfoCollector::new;
//      } else if ( topoMajor.equals( "C70" ) ) {
//            return MeningesInfoCollector::new;
//      } else if ( topoMajor.equals( "C71" ) ) {
//            return BrainInfoCollector::new;
//      } else if ( topoMajor.equals( "C72" ) ) {
//            return NerveInfoCollector::new;
//      } else if ( topoMajor.equals( "C48" ) ) {
//            return PeritoneumInfoCollector::new;
//      } else if ( topoMajor.equals( "C57" ) ) {
//            return GenitaliaInfoCollector::new;
//      }
//      return TopoMinorTypeSelector::new;
//   }
//
//   static public Supplier<XnAttributeNormalizer> getAttributeNormalizer( final Map<String,List<XnAttributeValue>> dependencies ) {
//      final String topoMajor = getTopoMajor( dependencies );
//      if ( topoMajor.equals( "C80" ) ) {
//         return TopoMinorTypeSelector::new;
//      }
//      final boolean hasMinorSite = MAJOR_SITES.contains( topoMajor );
//      if ( !hasMinorSite ) {
//         return TopoMinorTypeSelector::new;
//      }
//      if ( topoMajor.equals( "C50" ) ) {
//         return BreastNormalizer::new;
//      } else if ( topoMajor.equals( "C18" ) ) {
//         return ColonNormalizer::new;
//      } else if ( topoMajor.equals( "C21" ) ) {
//         return AnusNormalizer::new;
//      } else if ( topoMajor.equals( "C34" ) ) {
//         return LungNormalizer::new;
//      } else if ( topoMajor.equals( "C70" ) ) {
//         return MeningesNormalizer::new;
//      } else if ( topoMajor.equals( "C71" ) ) {
//         return BrainNormalizer::new;
//      } else if ( topoMajor.equals( "C72" ) ) {
//         return NerveNormalizer::new;
//      } else if ( topoMajor.equals( "C48" ) ) {
//         return PeritoneumNormalizer::new;
//      } else if ( topoMajor.equals( "C57" ) ) {
//         return GenitaliaNormalizer::new;
//      }
//      return TopoMinorTypeSelector::new;
//   }

   static private AttributeInfoCollector getAttributeInfoCollector( final String topoMajor ) {
//      LogFileWriter.add( "TopoMinorSelector TopoMajor=" + topoMajor  );
      if ( topoMajor.equals( "C80" ) ) {
         return null;
//         return new TopoMinorTypeSelector();
      }
      final boolean hasMinorSite = MAJOR_SITES.contains( topoMajor );
      if ( !hasMinorSite ) {
         return null;
//         return new TopoMinorTypeSelector();
      }
      // For some reason a switch on topoMajor does not work!  It thinks every case/value is the first case/value.
      if ( topoMajor.equals( "C50" ) ) {
         return new BreastInfoCollector();
      } else if ( topoMajor.equals( "C18" ) ) {
         return new ColonInfoCollector();
      } else if ( topoMajor.equals( "C21" ) ) {
         return new AnusInfoCollector();
      } else if ( topoMajor.equals( "C34" ) ) {
         return new LungInfoCollector();
      } else if ( topoMajor.equals( "C70" ) ) {
         return new MeningesInfoCollector();
      } else if ( topoMajor.equals( "C71" ) ) {
         return new BrainInfoCollector();
      } else if ( topoMajor.equals( "C72" ) ) {
         return new NerveInfoCollector();
      } else if ( topoMajor.equals( "C48" ) ) {
         return new PeritoneumInfoCollector();
      } else if ( topoMajor.equals( "C57" ) ) {
         return new GenitaliaInfoCollector();
      }
      return null;
//         return new TopoMinorTypeSelector();
   }

   static private XnAttributeNormalizer getAttributeNormalizer( final String topoMajor ) {
      if ( topoMajor.equals( "C80" ) ) {
         return null;
//         return new TopoMinorTypeSelector();
      }
      final boolean hasMinorSite = MAJOR_SITES.contains( topoMajor );
      if ( !hasMinorSite ) {
         return null;
//         return new TopoMinorTypeSelector();
      }
      if ( topoMajor.equals( "C50" ) ) {
         return new BreastNormalizer();
      } else if ( topoMajor.equals( "C18" ) ) {
         return new ColonNormalizer();
      } else if ( topoMajor.equals( "C21" ) ) {
         return new AnusNormalizer();
      } else if ( topoMajor.equals( "C34" ) ) {
         return new LungNormalizer();
      } else if ( topoMajor.equals( "C70" ) ) {
         return new MeningesNormalizer();
      } else if ( topoMajor.equals( "C71" ) ) {
         return new BrainNormalizer();
      } else if ( topoMajor.equals( "C72" ) ) {
         return new NerveNormalizer();
      } else if ( topoMajor.equals( "C48" ) ) {
         return new PeritoneumNormalizer();
      } else if ( topoMajor.equals( "C57" ) ) {
         return new GenitaliaNormalizer();
      }
      return null;
//         return new TopoMinorTypeSelector();
   }

   public String getNormalNoValue() {
      return "9";
   }

//   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
////      final String topoMajor = getTopoMajor( dependencies );
////      _bestCode = topoMajor.equals( "C80" ) ? "3" : "9";
//      _bestCode = "9";
//   }

   public String getNormalValue( final UriConcept concept ) {
      return getNormalNoValue();
   }


   //   C18.0	Cecum
//C18.1	Appendix
//C18.2	Ascending colon; Right colon
//C18.3	Hepatic flexure of colon
//C18.4	Transverse colon
//C18.5	Splenic flexure of colon
//C18.6	Descending colon; Left colon
//C18.7	Sigmoid colon
//C18.8	Overlapping lesion of colon
//C18.9	Colon, NOS
//ICD-O-2/3	Term
//Rectosigmoid junction
//C19.9	Rectosigmoid junction
//ICD-O-2/3	Term
//Rectum
//C20.9	Rectum, NOS
//ICD-O-2/3	Term
//Anus and Anal canal
//C21.0	Anus, NOS (excludes Skin of anus and Perianal skin C44.5)
//C21.1	Anal canal
//C21.2	Cloacogenic zone
//C21.8	Overlapping lesion of rectum, anus and anal canal
//
//   Lung = pneumo-, pulmono-, broncho-, bronchiolo-, alveolar, hilar, Breathing = -pnea
//
//ICD-O-2/3	Term
//C34.0	Main bronchus
//C34.1	Upper lobe, lung
//C34.2	Middle lobe, lung (right lung only)
//C34.3	Lower lobe, lung
//C34.8	Overlapping lesion of lung
//C34.9	Lung, NOS
//C33.9	Trachea, NOS
//
//   C50.0	Nipple
//C50.1	Central portion of breast
//C50.2	Upper-inner quadrant of breast (UIQ)
//C50.3	Lower-inner quadrant of breast (LIQ)
//C50.4	Upper-outer quadrant of breast (UOQ)
//C50.5	Lower-outer quadrant of breast (LOQ)
//C50.6	Axillary tail of breast
//C50.8	Overlapping lesion of breast
//C50.9	Breast, NOS (excludes Skin of breast C44.5); multi-focal neoplasm in more than one quadrant of the breast.
//
//   Urinary Bladder = bladder, vesicle, vesico, cysto-
//
//ICD-O-2/3 Codes	ICD-O-2/3 Term
//C67.0	Trigone of bladder
//C67.1	Dome of bladder
//C67.2	Lateral wall of bladder
//C67.3	Anterior wall of bladder
//C67.4	Posterior wall of bladder
//C67.5	Bladder neck
//C67.6	Ureteric orifice
//C67.7	Urachus
//C67.8	Overlapping lesion of bladder
//C67.9	Bladder, NOS
//
//
//   Esophagus
//   ICD-O-2/3 Codes	ICD-O-2/3 Term
//   C15.0	Cervical esophagus
//   C15.1	Thoracic esophagus
//   C15.2	Abdominal esophagus
//   C15.3	Upper third of esophagus
//   C15.4	Middle third of esophagus
//   C15.5	Lower third of esophagus
//   C15.8	Overlapping lesion of esophagus
//   C15.9	Esophagus, NOS
//
//         Stomach
//   ICD-O-2/3 Codes	ICD-O-2/3 Term
//   C16.0	Cardia, NOS
//   C16.1	Fundus of stomach
//   C16.2	Body of stomach
//   C16.3	Gastric antrum
//   C16.4	Pylorus
//   C16.5	Lesser curvature of stomach, NOS
//(not classifiable to C16.1 to C16.4)
//   C16.6	Greater curvature of stomach, NOS
//(not classifiable to C16.0 to C16.4)
//   C16.8	Overlapping lesion of stomach
//   C16.9	Stomach, NOS
//
//   Small Intestine
//   ICD-O-2/3 Codes	ICD-O-2/3 Term
//   C17.0	Duodenum
//   C17.1	Jejunum
//   C17.2	Ileum (excludes Ileocecal valve C18.0)
//   C17.3	Meckel diverticulum (site of neoplasm)
//   C17.8	Overlapping lesion of small intestine
//   C17.9	Small intestine, NOS
//
//
//   ICD-O Lymph Node Site Codes
//   Code	Site
//   C77.0	Lymph nodes of head, face and neck
//   C77.1	Intrathoracic lymph nodes
//   C77.2	Intra-abdominal lymph nodes
//   C77.3	Lymph nodes of axilla or arm
//   C77.4	Lymph nodes of inguinal region or leg
//   C77.5	Pelvic lymph nodes
//   C77.8	Lymph nodes of multiple regions
//   C77.9	Lymph node, NOS
//
//   C53.0	Endocervix
//C53.1	Exocervix
//C53.8	Overlapping lesion of cervix uteri
//C53.9	Cervix uteri
//
//   C54.0	Isthmus uteri
//C54.1	Endometrium
//C54.2	Myometrium
//C54.3	Fundus uteri
//C54.8	Overlapping lesion of corpus uteri
//C54.9	Corpus uteri
//
//   C48.1	Specified parts of peritoneum
//C48.2	Peritoneum, NOS
//C48.8	Overlapping lesion of retroperitoneum and peritoneum
//C56.9	Ovary
//C57.0	Fallopian tube
//C57.1	Broad Ligament
//C57.2	Round ligament
//C57.3	Parametrium
//C57.4	Uterine adnexa
//C57.7	Other specified parts of female genital organs
//C57.8	Overlapping lesion of female genital organs
//C57.9	Female genital tract, NOS
//C58.9	Placenta
//
//   C62.0	Undescended testis (site of neoplasm)
//C62.1	Descended testis
//C62.9	Testis, NOS
//
//   C68.0	Urethra
//C68.1	Paraurethral gland
//C68.8	Overlapping lesion of urinary organs
//C68.9	Urinary system, NOS
//
//   C22.0	Liver
//C22.1	Intrahepatic bile duct
//
//Gallbladder
//ICD-O-3	Term
//C23.9	Gallbladder
//
//Other and unspecified biliary tract
//ICD-O-3	Term
//C24.0	Extrahepatic bile duct
//C24.1	Ampulla of Vater
//C24.8	Overlapping lesion of biliary tract
//C24.9	Biliary tract, NOS
//
//Pancreas
//ICD-O-3	Term
//C25.0	Head of pancreas
//C25.1	Body of pancreas
//C25.2	Tail of pancreas
//C25.3	Pancreatic duct
//C25.4	Islets of Langerhans
//C25.7	Other specified parts of pancreas
//C25.8	Overlapping lesion of pancreas
//C25.9	Pancreas, NOS
//
//   C44.0	Skin of lip, NOS
//C44.1	Eyelid	Yes
//C44.2	External ear	Yes
//C44.3	Skin of other and unspecified parts of face	Yes
//C44.4	Skin of scalp and neck
//C44.5	Skin of trunk	Yes
//C44.6	Skin of upper limb and shoulder	Yes
//C44.7	Skin of lower limb and hip	Yes
//C44.8	Overlapping lesion of skin
//C44.9	Skin, NOS
//C51.0	Labium majus
//C51.1	Labium minus
//C51.2	Clitoris
//C51.8	Overlapping lesion of vulva
//C51.9	Vulva, NOS
//C60.0	Prepuce
//C60.1	Glans penis
//C60.2	Body of penis
//C60.8	Overlapping lesion of penis
//C60.9	Penis, NOS
//C63.2	Scrotum, NOS
//
//   C70.0	Cerebral meninges
//C70.1	Spinal meninges
//C70.9	Meninges, NOS
//
//Brain
//ICD-O-3	Term	Supratentorial	Infratentorial
//C71.0	Cerebrum	X
//Basal ganglia	X
//Central white matter	X
//Cerebral cortex	X
//Cerebral hemisphere	X
//Corpus striatum	X
//Globus pallidus	X
//Hypothalamus	 	X
//Insula	X
//Internal capsule	X
//Island of Reil	X
//Operculum	X
//Pallium	 	X
//Putamen	X
//Rhinencephalon	X
//Supratentorial brain, NOS	X
//Thalamus	 	X
//C71.1	Frontal lobe
//C71.2	Temporal lobe
//Hippocampus
//Uncus
//C71.3	Parietal lobe
//C71.4	Occipital lobe
//C71.5	Ventricle, NOS*
//Cerebral ventricle
//Choroid plexus, NOS*
//Choroid plexus of lateral ventricle	X
//Choroid plexus of third ventricle	X
//Ependyma*
//Lateral ventricle, NOS	X
//Third ventricle, NOS	X
//C71.6	Cerebellum, NOS	 	X
//Cerebellopontine angle	 	X
//Vermis of cerebellum	 	X
//C71.7	Brain stem	 	X
//Cerebral peduncle	 	X
//Basis pedunculi	 	X
//Choroid plexus of fourth ventricle	 	X
//Fourth ventricle, NOS	 	X
//Infratentorial brain, NOS	 	X
//Medulla oblongata	 	X
//Midbrain	 	X
//Olive	 	X
//Pons	 	X
//Pyramid	 	X
//C71.8	Overlapping lesion of brain
//Corpus callosum	X
//Tapetum	X
//C71.9	Brain, NOS*
//Intracranial site*
//Cranial fossa, NOS*
//Anterior cranial fossa	X
//Middle cranial fossa	X
//Posterior cranial fossa	 	X
//Suprasellar	X
//
//Spinal Cord and Other Central Nervous System
//ICD-O-3	Term
//C72.0	Spinal cord
//C72.1	Cauda equina
//C72.2	Olfactory nerve
//C72.3	Optic nerve
//C72.4	Acoustic nerve
//C72.5	Cranial nerve, NOS
//C72.8	Overlapping lesion of brain and central nervous system
//C72.9	Nervous system, NOS
//
//   C00.0	External upper lip
//C00.1	External lower lip
//C00.2	External lip, NOS
//C00.3	Mucosa of upper lip
//C00.4	Mucosa of lower lip
//C00.5	Mucosa of lip, NOS
//C00.6	Commissure of lip
//C00.8	Overlapping lesion of lip
//C00.9	Lip, NOS (excludes Skin of lip C44.0)
//
//Base of tongue
//ICD-O-3	Term
//C01.9	Base of tongue, NOS
//
//Other and unspecified parts of tongue
//ICD-O-3	Term
//C02.0	Dorsal surface of tongue, NOS
//C02.1	Border of tongue
//C02.2	Ventral surface of tongue, NOS
//C02.3	Anterior 2/3 of tongue, NOS
//C02.4	Lingual tonsil
//C02.8	Overlapping lesion of tongue
//C02.9	Tongue, NOS
//
//Gum
//ICD-O-3	Term
//C03.0	Upper gum
//C03.1	Lower gum
//C03.9	Gum, NOS
//
//Floor of mouth
//ICD-O-3	Term
//C04.0	Anterior floor of mouth
//C04.1	Lateral floor of mouth
//C04.8	Overlapping lesion of floor of mouth
//C04.9	Floor of mouth, NOS
//
//Palate
//ICD-O-3	Term
//C05.0	Hard palate
//C05.1	Soft palate, NOS (excludes Nasopharyngeal surface of soft palate C11.3)
//C05.2	Uvula
//C05.8	Overlapping lesion of palate
//C05.9	Palate, NOS
//
//Other and unspecified parts of mouth
//ICD-O-3	Term
//C06.0	Cheek mucosa
//C06.1	Vestibule of mouth
//C06.2	Retromolar area
//C06.8	Overlapping lesion of other and unspecified parts of mouth
//C06.9	Mouth, NOS
//
//Parotid gland
//ICD-O-3	Term
//C07.9	*Parotid gland
//
//Other and unspecified major salivary gland
//ICD-O-3	Term
//C08.0	*Submandibular gland
//C08.1	*Sublingual gland
//C08.8	Overlapping lesion of major salivary glands
//C08.9	*Major salivary gland, NOS (excludes minor salivary gland, NOS C06.9)
//
//Tonsil
//ICD-O-3	Term
//C09.0	*Tonsillar fossa
//C09.1	*Tonsillar pillar
//C09.8	Overlapping lesion of tonsil
//C09.9	*Tonsil, NOS (excludes lingual tonsil C02.4 and pharyngeal tonsil C11.1)
//
//Oropharynx
//ICD-O-3	Term
//C10.0	Vallecula
//C10.1	Anterior surface of epiglottis
//C10.2	Lateral wall of oropharynx
//C10.3	Posterior wall of oropharynx
//C10.4	Branchial cleft (site of neoplasm)
//C10.8	Overlapping lesion of oropharynx
//C10.9	Oropharynx, NOS
//
//Nasopharynx
//ICD-O-3	Term
//C11.0	Superior wall of nasopharynx
//C11.1	Posterior wall of nasopharynx
//C11.2	Lateral wall of nasopharynx
//C11.3	Anterior wall of nasopharynx
//C11.8	Overlapping lesion of nasopharynx
//C11.9	Nasopharynx, NOS
//
//Pyriform sinus
//ICD-O-3	Term
//C12.9	Pyriform sinus
//
//Hypopharynx
//ICD-O-3	Term
//C13.0	Postcricoid region
//C13.1	Hypopharyngeal aspect of aryepiglottic fold, NOS (excludes laryngeal aspect of aryepiglottic fold C32.1)
//C13.2	Posterior wall of hypopharynx
//C13.8	Overlapping lesion of hypopharynx
//C13.9	Hypopharynx, NOS
//
//Other and ill-defined sites in lip, oral cavity and pharynx
//ICD-O-3	Term
//C14.0	Pharynx, NOS
//C14.2	Waldeyer's ring
//C14.8	Overlapping lesion of lip, oral cavity and pharynx
//
//Nasal cavity and middle ear
//ICD-O-3	Term
//C30.0	*Nasal cavity (excludes Nose, NOS C76.0)
//C30.1	Middle ear
//
//Accessory sinuses
//ICD-O-3	Term
//C31.0	*Maxillary sinus
//C31.1	Ethmoid sinus
//C31.2	*Frontal sinus
//C31.3	Sphenoid sinus
//C31.8	Overlapping lesion of accessory sinuses
//C31.9	Accessory sinus, NOS
//
//Larynx
//ICD-O-3	Term
//C32.0	Glottis
//C32.1	Supraglottis
//C32.2	Subglottis
//C32.3	Laryngeal cartilage
//C32.8	Overlapping lesion of larynx
//C32.9	Larynx, NOS


}

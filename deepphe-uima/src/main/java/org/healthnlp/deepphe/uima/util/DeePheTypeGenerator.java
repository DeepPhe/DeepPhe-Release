package org.healthnlp.deepphe.uima.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.pitt.dbmi.nlp.noble.ontology.*;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.util.XMLUtils;

public class DeePheTypeGenerator {
   public static final String TYPE_PREFIX = "org.healthnlp.deepphe.uima.types.";
   public static final String CLASS_ATTRIBUTE = "Attribute";
   public static final String CLASS_ANNOTATION = "Annotation";
   public static final String CLASS_MODIFIER = "Modifier";
   public static final String CLASS_LINGUISTIC_MODIFIER = "LinguisticModifier";
   public static final String CLASS_SUMMARY = "Summary";
   public static final String CLASS_MODEL = "Model";
   private IOntology ontology;
   private Map<String, String> nameMap;

   public DeePheTypeGenerator( IOntology ont ) {
      ontology = ont;
      nameMap = new HashMap<String, String>();
   }


   private String getName( IClass c ) {
      String name = c.getLabels().length > 0 ? c.getLabels()[ 0 ] : c.getName();

      name = name.replaceAll( "\\W+", "" );

      // hadcode for Patient as there is a conflict between DomainOntology#Patient and nlpCancer#Patient
      if ( "Patient".equals( name ) && c.getURI().toString().contains( "Cancer" ) )
         name = "PatientSummary";

      //return c.getName();
      if ( nameMap.containsKey( name ) ) {
         // if we have a similar name with a different URI
         if ( !nameMap.get( name ).equals( "" + c.getURI() ) ) {
            name = name + "Summary";
            nameMap.put( name, "" + c.getURI() );
         }
      } else {
         nameMap.put( name, "" + c.getURI() );
      }

      return name;
   }


   private String getSuperType( IClass cls ) {
      //String superType = "uima.tcas.Annotation"; //uima.cas.TOP
      String superType = TYPE_PREFIX + "Fact"; //uima.cas.TOP
      IClass[] parents = cls.getDirectSuperClasses();
      for ( IClass s : parents ) {
         if ( !s.equals( ontology.getRoot() ) && !(s.getName().equals( CLASS_MODEL ) && parents.length > 1) ) {
            superType = TYPE_PREFIX + getName( s );
            break;
         }
      }
      return superType;
   }


   private Element createTypeDescription( Document doc, IClass cls ) {
      Element element = doc.createElement( "typeDescription" );

      Element e = doc.createElement( "name" );
      e.setTextContent( TYPE_PREFIX + getName( cls ) );
      element.appendChild( e );

      e = doc.createElement( "description" );
      for ( String str : cls.getComments() ) {
         e.setTextContent( str );
         break;
      }
      element.appendChild( e );

      String superType = getSuperType( cls );
      e = doc.createElement( "supertypeName" );
      e.setTextContent( superType );
      element.appendChild( e );

      Element features = doc.createElement( "features" );
      // add TOP level attribute
      /*
      if(superType.equals("uima.tcas.Annotation")){
			features.appendChild(createFeatureDesciption(doc,"hasURI"));
			features.appendChild(createFeatureDesciption(doc,"hasPreferredName"));
			features.appendChild(createFeatureDesciption(doc,"hasIdentifier"));
		}
		*/

      // add other features
      for ( Element fd : createFeatureDesciptions( doc, cls ) ) {
         features.appendChild( fd );
      }
      element.appendChild( features );

      return element;
   }

   private Element createTypeDescriptionRoot( Document doc ) {
      Element element = doc.createElement( "typeDescription" );

      Element e = doc.createElement( "name" );
      e.setTextContent( TYPE_PREFIX + "Fact" );
      element.appendChild( e );

      e = doc.createElement( "description" );
      e.setTextContent( "This type represents Ontology Fact" );
      element.appendChild( e );

      String superType = "uima.tcas.Annotation";
      e = doc.createElement( "supertypeName" );
      e.setTextContent( superType );
      element.appendChild( e );

      Element features = doc.createElement( "features" );

      // add TOP level attribute
      features.appendChild( createFeatureDesciption( doc, "hasURI" ) );
      features.appendChild( createFeatureDesciption( doc, "hasPreferredName" ) );
      features.appendChild( createFeatureDesciption( doc, "hasIdentifier" ) );
      features.appendChild( createFeatureDesciption( doc, "hasLabel" ) );
      features.appendChild( createFeatureDesciption( doc, "hasType" ) );
      features.appendChild( createFeatureDesciption( doc, "hasProvenanceFacts", null, "uima.cas.FSArray", TYPE_PREFIX + "Fact" ) );
      features.appendChild( createFeatureDesciption( doc, "hasProvenanceText", null, "uima.cas.StringArray", null ) );
      features.appendChild( createFeatureDesciption( doc, "hasDocumentOffset", null, "uima.cas.Integer", null ) );
      features.appendChild( createFeatureDesciption( doc, "hasAncestors", null, "uima.cas.StringArray", null ) );
      features.appendChild( createFeatureDesciption( doc, "hasProperties", null, "uima.cas.FSArray", TYPE_PREFIX + "Property" ) );
      features.appendChild( createFeatureDesciption( doc, "hasRecordedDate" ) );
      features.appendChild( createFeatureDesciption( doc, "hasDocumentIdentifier" ) );
      features.appendChild( createFeatureDesciption( doc, "hasDocumentType" ) );
      features.appendChild( createFeatureDesciption( doc, "hasPatientIdentifier" ) );
      features.appendChild( createFeatureDesciption( doc, "hasTemporalOrder", null, "uima.cas.Integer", null ) );


      element.appendChild( features );

      return element;
   }

   private Element createTypeDescriptionList( Document doc ) {
      Element element = doc.createElement( "typeDescription" );

      Element e = doc.createElement( "name" );
      e.setTextContent( TYPE_PREFIX + "FactList" );
      element.appendChild( e );

      e = doc.createElement( "description" );
      e.setTextContent( "This type represents Fact list" );
      element.appendChild( e );

      String superType = "uima.tcas.Annotation";
      e = doc.createElement( "supertypeName" );
      e.setTextContent( superType );
      element.appendChild( e );

      Element features = doc.createElement( "features" );

      // add TOP level attribute
      features.appendChild( createFeatureDesciption( doc, "hasTypes", null, "uima.cas.StringArray", null ) );
      features.appendChild( createFeatureDesciption( doc, "hasCategory" ) );
      features.appendChild( createFeatureDesciption( doc, "hasFacts", null, "uima.cas.FSArray", TYPE_PREFIX + "Fact" ) );
      features.appendChild( createFeatureDesciption( doc, "hasProperties", null, "uima.cas.FSArray", TYPE_PREFIX + "Property" ) );

      element.appendChild( features );

      return element;
   }


   private Element createTypeDescriptionProperty( Document doc ) {
      Element element = doc.createElement( "typeDescription" );

      Element e = doc.createElement( "name" );
      e.setTextContent( TYPE_PREFIX + "Property" );
      element.appendChild( e );

      e = doc.createElement( "description" );
      e.setTextContent( "Utility for storing property value pairs" );
      element.appendChild( e );

      String superType = "uima.tcas.Annotation";
      e = doc.createElement( "supertypeName" );
      e.setTextContent( superType );
      element.appendChild( e );

      Element features = doc.createElement( "features" );

      // add TOP level attribute
      features.appendChild( createFeatureDesciption( doc, "name" ) );
      features.appendChild( createFeatureDesciption( doc, "value" ) );

      element.appendChild( features );

      return element;
   }

   private List<Element> createFeatureDesciptions( Document doc, IClass cls ) {
      List<Element> list = new ArrayList<Element>();

      // look at restrictions
      for ( ILogicExpression exp : Arrays.asList( cls.getEquivalentRestrictions(), cls.getDirectNecessaryRestrictions() ) ) {
         for ( Object obj : exp ) {
            if ( obj instanceof IRestriction ) {
               IRestriction r = (IRestriction) obj;
               //if(isOKRestriction(cls,r))
               Element e = createFeatureDesciption( doc, r, !isOKRestriction( cls, r ) );
               if ( !contains( list, e ) )
                  list.add( e );
            }
         }
      }
      // add generic stuff to models
      if ( cls.hasSuperClass( ontology.getClass( "Model" ) ) ) {
         list.add( createFeatureDesciption( doc, "hasContent", "generic content", "uima.cas.FSArray", TYPE_PREFIX + "FactList" ) );
      }

      return list;
   }


   private boolean contains( List<Element> list, Element e ) {
      String prop = getFeaturePropertyName( e );
      for ( Element ee : list ) {
         if ( prop.equals( getFeaturePropertyName( ee ) ) )
            return true;
      }
      return false;
   }

   private String getFeaturePropertyName( Element e ) {
      NodeList l = e.getElementsByTagName( "name" );
      if ( l.getLength() > 0 ) {
         Element ee = (Element) l.item( 0 );
         return ee.getTextContent().trim();
      }
      return null;
   }


   private boolean isOKRestriction( IClass cls, IRestriction r ) {
      for ( Object o : cls.getNecessaryRestrictions() ) {
         if ( o instanceof IRestriction ) {
            IRestriction rr = (IRestriction) o;
            if ( !r.equals( rr ) && rr.getProperty().equals( r.getProperty() ) )
               return false;
         }
      }
      return true;
   }


   private Element createFeatureDesciption( Document doc, String name ) {
      return createFeatureDesciption( doc, name, null, null, null );
   }

   private Element createFeatureDesciption( Document doc, String name, String desc, String range, String elementType ) {
      Element element = doc.createElement( "featureDescription" );

      Element e = doc.createElement( "name" );
      e.setTextContent( name );
      element.appendChild( e );

      e = doc.createElement( "description" );
      if ( desc != null )
         e.setTextContent( desc );
      element.appendChild( e );


      // for now just handle single slot
      if ( range == null )
         range = "uima.cas.String";

      e = doc.createElement( "rangeTypeName" );
      e.setTextContent( range );
      element.appendChild( e );

      if ( elementType != null ) {
         e = doc.createElement( "elementType" );
         e.setTextContent( elementType );
         element.appendChild( e );
      }

      // allow multiple references
      e = doc.createElement( "multipleReferencesAllowed" );
      e.setTextContent( "true" );
      element.appendChild( e );

      return element;
   }


   private Element createFeatureDesciption( Document doc, IRestriction r, boolean rename ) {
      Element element = doc.createElement( "featureDescription" );

      // for now just handle single slot
      String propertyName = r.getProperty().getName();
      String range = "uima.cas.String";
      String elementType = null;

      Object value = r.getParameter().getOperand();
      //if(r.getParameter().size() == 1){
      // if type worthy class
      if ( value instanceof IClass ) {
         range = "uima.cas.FSArray";
         IClass v = (IClass) value;
         if ( isTypeWorthy( v ) ) {
            elementType = TYPE_PREFIX + getName( v );
            if ( rename ) {
               propertyName = propertyName + getName( v );
               // no time, hard code for now, for sume reason a different patient is returned
               if ( "Patient".equals( v.getName() ) ) {
                  elementType += "Summary";
               }
            }
         } else {
            elementType = TYPE_PREFIX + "Fact";
         }
         //}else if("hasOutcome".equals(propertyName)){
         //	range = "uima.cas.FSArray";
         //	elementType = TYPE_PREFIX+"Event";
      } else if ( value instanceof Float ) {
         range = "uima.cas.Float";
      } else if ( value instanceof Double ) {
         range = "uima.cas.Double";
      } else if ( value instanceof Integer ) {
         range = "uima.cas.Integer";
      } else if ( value instanceof Boolean ) {
         range = "uima.cas.Boolean";
      } else if ( "hasSpan".equals( r.getProperty().getName() ) ) {
         range = "uima.cas.StringArray";
      }
		/*}else{
			System.err.println(r);
		}*/


      Element e = doc.createElement( "name" );
      e.setTextContent( propertyName );
      element.appendChild( e );

      e = doc.createElement( "description" );
      for ( String str : r.getProperty().getComments() ) {
         e.setTextContent( str );
         break;
      }
      element.appendChild( e );
      e = doc.createElement( "rangeTypeName" );
      e.setTextContent( range );
      element.appendChild( e );

      if ( elementType != null ) {
         e = doc.createElement( "elementType" );
         e.setTextContent( elementType );
         element.appendChild( e );
      }

      // allow multiple references
      e = doc.createElement( "multipleReferencesAllowed" );
      e.setTextContent( "true" );
      element.appendChild( e );

      return element;
   }


   /**
    * THIS IS WHERE I START CONTSTRACTING THE CAS
    *
    * @param doc
    * @return
    */

   private List<Element> createTypeDescriptions( Document doc ) {
      Element prop = createTypeDescriptionProperty( doc );
      Element root = createTypeDescriptionRoot( doc );
      Element flist = createTypeDescriptionList( doc );

      List<Element> list = new ArrayList<Element>();
      list.add( root );
      list.add( prop );
      list.add( flist );

      for ( IClass cls : ontology.getRoot().getSubClasses() ) {
         if ( isTypeWorthy( cls ) )
            list.add( createTypeDescription( doc, cls ) );
      }

      return list;
   }


   private boolean isTypeWorthy( IClass cls ) {
      if ( cls == null )
         return false;
      IClass annotation = ontology.getClass( CLASS_ANNOTATION );
      IClass attribute = ontology.getClass( CLASS_ATTRIBUTE );  //"Attribute"
      IClass modifier = ontology.getClass( CLASS_MODIFIER );
      IClass summary = ontology.getClass( CLASS_SUMMARY );
      IClass model = ontology.getClass( CLASS_MODEL );
      IClass linguistic = ontology.getClass( CLASS_LINGUISTIC_MODIFIER );

      if ( cls.hasSuperClass( model ) )
         return true;


      for ( IClass cat : Arrays.asList( annotation, attribute, modifier, summary ) ) {
         if ( cls.equals( cat ) || cls.hasDirectSuperClass( cat ) ) {
            return true;
         }
         // skip linguistic modifiers
         if ( cls.hasSuperClass( linguistic ) )
            return false;
         if ( cls.hasSuperClass( cat ) && (!cls.getDirectNecessaryRestrictions().isEmpty() || !cls.getEquivalentRestrictions().isEmpty()) ) {
            return true;
         }
      }

      return false;
   }


   private Element createTypeSystemElement( Document doc ) {
      Element root = doc.createElement( "typeSystemDescription" );
      root.setAttribute( "xmlns", "http://uima.apache.org/resourceSpecifier" );

      Element e = doc.createElement( "name" );
      e.setTextContent( "TypeSystem" );
      root.appendChild( e );

      e = doc.createElement( "description" );
      e.setTextContent( "DeepPhe Phenotype TypeSystem" );
      root.appendChild( e );

      e = doc.createElement( "version" );
      e.setTextContent( "1.0" );
      root.appendChild( e );

      root.appendChild( doc.createElement( "vendor" ) );

      Element types = doc.createElement( "types" );


      for ( Element td : createTypeDescriptions( doc ) ) {
         types.appendChild( td );
      }

      root.appendChild( types );

      return root;
   }


   /**
    * save output
    *
    * @param output
    * @throws ParserConfigurationException
    * @throws IOException
    * @throws TransformerException
    * @throws FileNotFoundException
    */
   public void save( File output ) throws ParserConfigurationException, FileNotFoundException, TransformerException, IOException {
      // initialize document and root
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      Document doc = factory.newDocumentBuilder().newDocument();

      // create DOM object
      doc.appendChild( createTypeSystemElement( doc ) );

      // write out XML
      XMLUtils.writeXML( doc, new FileOutputStream( output ) );
   }


   public static void main( String[] args ) throws IOntologyException, FileNotFoundException, ParserConfigurationException, TransformerException, IOException {
      if ( args.length == 0 ) {
         System.err.println( "Usage: java " + DeePheTypeGenerator.class.getName() + " <output TypeSystem.xml>" );
         System.exit( 1 );
      }
      File file = new File( args[ 0 ] );
      if ( !file.getParentFile().exists() )
         file.getParentFile().mkdirs();
      IOntology ont = OOntology.loadOntology( "/home/tseytlin/Work/DeepPhe/data/ontology/nlpCancer.owl" ); //"http://ontologies.dbmi.pitt.edu/deepphe/nlpCancer.owl"
      //"http://ontologies.dbmi.pitt.edu/deepphe/cancer.owl" ///home/tseytlin/Work/DeepPhe/data/ontology/cancer.owl
      System.out.println( "input: " + ont.getURI() );
      DeePheTypeGenerator dtg = new DeePheTypeGenerator( ont );
      dtg.save( file );
      System.out.println( "output: " + file.getAbsolutePath() );
   }

}

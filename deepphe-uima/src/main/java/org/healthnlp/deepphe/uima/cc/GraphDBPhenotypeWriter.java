package org.healthnlp.deepphe.uima.cc;

import org.apache.ctakes.core.cc.AbstractOutputFileWriter;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.fhir.fact.TextMention;
import org.healthnlp.deepphe.fhir.summary.CancerSummary;
import org.healthnlp.deepphe.fhir.summary.MedicalRecord;
import org.healthnlp.deepphe.fhir.summary.Summary;
import org.healthnlp.deepphe.fhir.summary.TumorSummary;
import org.healthnlp.deepphe.uima.ae.GraphDBConstants;
import org.healthnlp.deepphe.uima.fhir.PhenotypeResourceFactory;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.io.fs.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;


public class GraphDBPhenotypeWriter extends AbstractOutputFileWriter {
   private final Logger LOGGER = Logger.getLogger( "GraphDBPhenotypeConsumerAE" );

//	public static final String PARAM_DBPATH = "DBPATH";
//	public static final String PARAM_USERNAME = "USERNAME";
//	public static final String PARAM_PASSWORD = "PASSWORD";

   private final Collection<String> _usedDirectories = new HashSet<>();

   private UniqueFactory.UniqueNodeFactory patientFactory;
   private GraphDatabaseService graphDatabase;

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas, final String outputDir,
                          final String documentId, final String fileName ) throws IOException {
      if ( _usedDirectories.add( outputDir ) ) {
         graphDatabase = initializeGraphDatabase( outputDir );
      }
      LOGGER.info( "Writing Summary Files for " + documentId + " ..." );
      final MedicalRecord record = PhenotypeResourceFactory.loadMedicalRecord( jCas );
      LOGGER.info( "adding medical record for " + record.getPatient().getDisplayText() + " to a database" );
      saveToGraph( graphDatabase, record );
      LOGGER.info( "done saving record for " + record.getPatient().getDisplayText() + " to a database" );
   }

   public GraphDatabaseService initializeGraphDatabase( final String dbPath ) throws IOException {
      File file = new File( dbPath );
      if ( file.exists() ) {
         FileUtils.deleteRecursively( file );
      }
      if ( graphDatabase != null ) {
         graphDatabase.shutdown();
      }
      graphDatabase = new GraphDatabaseFactory().newEmbeddedDatabase( file );
      if ( !graphDatabase.isAvailable( 500 ) ) {
         throw new IOException( "Could not initialize neo4j connection for: " + dbPath );
      }
      return graphDatabase;
   }


   @Override
   public void destroy() {
      if ( graphDatabase != null ) {
         graphDatabase.shutdown();
      }
      super.destroy();
   }


   public void saveToGraph( GraphDatabaseService graphDB, MedicalRecord mr ) {
      try ( Transaction tx = graphDB.beginTx() ) {

         Node patientNode = getPatientFactory( graphDB ).getOrCreate( "name", mr.getPatient().getPatientName() );

         //Commenting this as it is currently not being populated.
//			PatientSummary psummary = mr.getPatientSummary();
//			if(psummary!=null){
//				saveSummaryToGraph(psummary,patientNode,graphDB);
//			}

         CancerSummary cancerSummary = mr.getCancerSummary();
         if ( cancerSummary != null ) {
            Node cancerNode = graphDB.createNode( GraphDBConstants.Nodes.Cancer );
            patientNode.createRelationshipTo( cancerNode, GraphDBConstants.Relationships.hasCancer );

            populateSummaryNode( cancerNode, cancerSummary );

            saveSummaryToGraph( cancerSummary, cancerNode, graphDB );

            if ( cancerSummary.getPhenotype() != null ) {
               saveSummaryToGraph( cancerSummary.getPhenotype(), cancerNode, graphDB );
            }

            for ( TumorSummary tumorSummary : cancerSummary.getTumors() ) {
               Node tumorNode = graphDB.createNode( GraphDBConstants.Nodes.Tumor );
               cancerNode.createRelationshipTo( tumorNode, GraphDBConstants.Relationships.hasTumor );

               populateSummaryNode( tumorNode, tumorSummary );

               saveSummaryToGraph( tumorSummary, tumorNode, graphDB );

               if ( tumorSummary.getPhenotype() != null ) {
                  saveSummaryToGraph( tumorSummary.getPhenotype(), tumorNode, graphDB );
               }
            }
         }

         for ( Report report : mr.getReports() ) {
            Node reportNode = graphDB.createNode( GraphDBConstants.Nodes.Report );
            patientNode.createRelationshipTo( reportNode, GraphDBConstants.Relationships.hasReport );
            reportNode.setProperty( GraphDBConstants.PROPERTY_IDENTIFIER, report.getResourceIdentifier() );
            reportNode.setProperty( "id", report.getResourceIdentifier() );
            reportNode.setProperty( "title", report.getTitle() );
            reportNode.setProperty( "text", report.getReportText() );
            reportNode.setProperty( "principalDate", FHIRUtils.getDateAsString( report.getDate() ) );
            reportNode.setProperty( "type", FHIRUtils.getDocumentType( report.getType() ) );

         }


         tx.success();
      }

   }


   private UniqueFactory.UniqueNodeFactory getPatientFactory( GraphDatabaseService graphDB ) {

      if ( patientFactory == null ) {
         try ( Transaction tx = graphDB.beginTx() ) {
            UniqueFactory.UniqueNodeFactory result = new UniqueFactory.UniqueNodeFactory( graphDB, "patients" ) {
               @Override
               protected void initialize( Node created, Map<String, Object> properties ) {
                  created.addLabel( GraphDBConstants.Nodes.Patient );
                  created.setProperty( "name", properties.get( "name" ) );
               }
            };
            tx.success();
            patientFactory = result;
         }
      }

      return patientFactory;
   }


   private void saveSummaryToGraph( Summary summary, Node node, GraphDatabaseService graphDB ) {
      for ( String category : summary.getFactCategories() ) {
         FactList fl = summary.getFacts( category );

         for ( Fact f : fl ) {
            Node factNode = graphDB.createNode( GraphDBConstants.Nodes.Fact );
            Relationship rel = node.createRelationshipTo( factNode, GraphDBConstants.getRelationship( category ) );
            rel.setProperty( "name", category );
            saveFactToGraph( f, factNode, graphDB );
         }
      }
   }

   private void saveFactToGraph( Fact f, Node factNode, GraphDatabaseService graphDB ) {

      populateFactNode( factNode, f );
      createProvenanceTextNodes( f, factNode, graphDB );

      //we no longer save all provenance facts just the textmentions so this is commented.
//		for(Fact pf:f.getProvenanceFacts()){
//			Node pFactNode = graphDB.createNode(GraphDBConstants.Nodes.Fact);
//			populateFactNode(pFactNode,pf);
//			Relationship rel = factNode.createRelationshipTo(pFactNode,GraphDBConstants.Relationships.hasProvenance);
//			rel.setProperty("name","hasProvenance");
//
//		}
      for ( Fact cf : f.getContainedFacts() ) {
         Node cFactNode = graphDB.createNode( GraphDBConstants.Nodes.Fact );
         populateFactNode( cFactNode, cf );
         createProvenanceTextNodes( cf, cFactNode, graphDB );
         Relationship rel = factNode.createRelationshipTo( cFactNode, GraphDBConstants.getRelationship( cf.getType() ) );
         rel.setProperty( "name", cf.getType() );
      }
   }

   private void populateSummaryNode( Node summaryNode, Summary summary ) {
      summaryNode.setProperty( GraphDBConstants.PROPERTY_IDENTIFIER, summary.getResourceIdentifier() );
   }

   private void populateFactNode( Node factNode, Fact f ) {
      factNode.setProperty( GraphDBConstants.PROPERTY_IDENTIFIER, f.getIdentifier() );
      factNode.setProperty( "name", f.getName() );
      factNode.setProperty( "prettyName", f.getLabel() );
      factNode.setProperty( "type", f.getType() );
      factNode.setProperty( "uri", f.getUri() );

      if ( f.getRulesApplied() != null )
         factNode.setProperty( "rulesApplied", f.getContainedProvenanceRules().toArray( new String[]{} ) );
   }

   private void createProvenanceTextNodes( Fact f, Node factNode, GraphDatabaseService graphDB ) {

      for ( TextMention tm : f.getContainedProvenanceText() ) {
         if ( tm.getDocumentIdentifier() != null ) {
            //ignore junk text mentions
            if ( tm.getStart() == 0 && tm.getEnd() == 0 )
               continue;

            Node tmNode = graphDB.createNode( GraphDBConstants.Nodes.TextMention );


            tmNode.setProperty( "startOffset", tm.getStart() );
            tmNode.setProperty( "endOffset", tm.getEnd() );
            tmNode.setProperty( "text", tm.getText() );
            tmNode.setProperty( "documentId", tm.getDocumentIdentifier() );

            if ( tm.getDocumentTitle() != null )
               tmNode.setProperty( "documentName", tm.getDocumentTitle() );

            if ( tm.getDocumentSection() != null )
               tmNode.setProperty( "documentSection", tm.getDocumentSection() );

            if ( tm.getDocumentType() != null )
               tmNode.setProperty( "documentType", tm.getDocumentType() );

            Relationship rel = factNode.createRelationshipTo( tmNode, GraphDBConstants.Relationships.hasTextProvenance );
            rel.setProperty( "name", "hasTextProvenance" );
         }

      }
   }


}

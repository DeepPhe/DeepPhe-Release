package org.healthnlp.deepphe.viz.neo4j;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.*;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.RelationConstants.HAS_DIAGNOSIS;
import static org.healthnlp.deepphe.neo4j.RelationConstants.HAS_STAGE;
import static org.healthnlp.deepphe.viz.neo4j.DataUtil.SUMMARY_FACT_RELATION_TYPE;
import static org.healthnlp.deepphe.viz.neo4j.DataUtil.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/6/2018
 */
public class PatientFunctions {

   static private final Logger LOGGER = Logger.getLogger( "PatientFunctions" );

   // This field declares that we need a GraphDatabaseService
   // as context when any procedure in this class is invoked
   @Context
   public GraphDatabaseService graphDb;

   // This gives us a log instance that outputs messages to the
   // standard log, normally found under `data/log/console.log`
   @Context
   public Log log;



   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            COHORT DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @UserFunction( name = "deepphe.getCohortData" )
   @Description( "return all stages and corresponding patients in cohort as json text.  Some redundant information" )
   public String getCohortData() {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Collection<Node> patientNodes = DataUtil.getAllPatientNodes( graphDb );
         for ( Node patientNode : patientNodes ) {
            // get the major stage values for the patient
            final List<String> stages = new ArrayList<>();
            final Collection<Node> cancerNodes
                  = SearchUtil.getOutRelatedNodes( graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION );
            for ( Node cancerNode : cancerNodes ) {
               SearchUtil.getOutRelatedNodes( graphDb, cancerNode, HAS_STAGE ).stream()
                         .map( n -> n.getProperty( NAME_KEY ) )
                         .map( DataUtil::objectToString )
                         .map( PatientFunctions::getPrettyStage )
                         .forEach( stages::add );
            }
            if ( stages.isEmpty() ) {
               continue;
            }
            stages.sort( String.CASE_INSENSITIVE_ORDER );
            final Map<String, String> patientProperties = new HashMap<>( 6 );
            patientProperties.put( "birthday", DataUtil.objectToString( patientNode.getProperty( PATIENT_BIRTH_DATE ) ) );
            patientProperties.put( "lastEncounterDate", DataUtil.objectToString( patientNode.getProperty( PATIENT_LAST_ENCOUNTER ) ) );
            patientProperties.put( "patientId", DataUtil.objectToString( patientNode.getProperty( NAME_KEY ) ) );
            patientProperties.put( "patientName", DataUtil.objectToString( patientNode.getProperty( PATIENT_NAME ) ) );
            patientProperties.put( "firstEncounterDate", DataUtil.objectToString( patientNode.getProperty( PATIENT_FIRST_ENCOUNTER ) ) );
            patientProperties.put( "postmenopausal", Math.random() > 0.5 ? "true" : "false" );
            // For each major stage, add a patient object
            for ( String stage : stages ) {
               final JSONObject anonymousJson = new JSONObject();
               final JSONArray rowArray = new JSONArray();
               final JSONObject patientObject = new JSONObject();
               patientObject.putAll( patientProperties );
               rowArray.add( patientObject );
               rowArray.add( stage );
               anonymousJson.put( "row", rowArray );
               dataArray.add( anonymousJson );
            }
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "p", "f.prettyName" ).toJSONString();
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            PATIENT COLLECTION DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @UserFunction( name = "deepphe.getPatientsTumorInfo" )
   @Description( "return patient tumors as a simple json string.  Redundant information" )
   public String getPatientsTumorInfo( @Name( "patientIds" ) List<String> patientIds ) {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {

         for ( String patientId : patientIds ) {
            final Node patientNode = SearchUtil.getObjectNode( graphDb, patientId );
            if ( patientNode == null ) {
               continue;
            }
            final Collection<Node> cancerNodes
                  = SearchUtil.getOutRelatedNodes( graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION );
            for ( Node cancerNode : cancerNodes ) {
               final Collection<Node> tumorNodes = SearchUtil.getAllOutRelatedNodes( graphDb, cancerNode );
               for ( Node tumorNode : tumorNodes ) {
                  final String tumorId = DataUtil.objectToString( tumorNode.getProperty( NAME_KEY ) );
                  for ( Relationship relation : tumorNode.getRelationships( TUMOR_HAS_FACT_RELATION, Direction.OUTGOING ) ) {
                     final String relationName = objectToString( relation.getProperty( SUMMARY_FACT_RELATION_TYPE ) );
                     final JSONObject anonymousJson = new JSONObject();
                     final JSONArray rowArray = new JSONArray();
                     rowArray.add( patientId );
                     rowArray.add( tumorId );
                     rowArray.add( relationName );

                     final JSONObject targetObject = new JSONObject();
                     targetObject.put( "rulesApplied", new JSONArray() );
                     final Node targetNode = relation.getOtherNode( tumorNode );
                     String semantic = "";
                     for ( Label label : targetNode.getLabels() ) {
                        if ( isSemanticType( label.name() ) ) {
                           semantic = label.name();
                           break;
                        }
                     }
                     final Node classNode = DataUtil.getInstanceClass( graphDb, targetNode );
                     final String classId = DataUtil.objectToString( classNode.getProperty( NAME_KEY ) );
                     targetObject.put( "prettyName", DataUtil.objectToString( classNode.getProperty( PREF_TEXT_KEY ) ) );
                     targetObject.put( "name", classId );
                     targetObject.put( "id", DataUtil.objectToString( targetNode.getProperty( NAME_KEY ) ) );
                     targetObject.put( "type", semantic );
                     targetObject.put( "uri", classId );

                     rowArray.add( targetObject );
                     rowArray.add( null );
                     rowArray.add( null );
                     anonymousJson.put( "row", rowArray );
                     dataArray.add( anonymousJson );
                  }
               }
            }
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "p.name", "tumor.id", "tumorFactReln.name", "fact", "rel", "f" ).toJSONString();
   }



   @UserFunction( name = "deepphe.getDiagnosis" )
   @Description( "return patient diagnoses as a simple json string.  Redundant information" )
   public String getDiagnosis( @Name( "patientIds" ) List<String> patientIds ) {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {
         for ( String patientId : patientIds ) {
            final Node patientNode = SearchUtil.getObjectNode( graphDb, patientId );
            if ( patientNode == null ) {
               continue;
            }
            final Collection<String> diagnoses = new HashSet<>();
            final Collection<Node> cancerNodes
                  = SearchUtil.getOutRelatedNodes( graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION );
            for ( Node cancerNode : cancerNodes ) {
               SearchUtil.getOutRelatedNodes( graphDb, cancerNode, HAS_DIAGNOSIS ).stream()
                         .map( n -> DataUtil.getPreferredText( graphDb, n ) )
                         .forEach( diagnoses::add );

               SearchUtil.getOutRelatedNodes( graphDb, cancerNode, CANCER_HAS_TUMOR_RELATION ).stream()
                         .map( t -> SearchUtil.getOutRelatedNodes( graphDb, t, HAS_DIAGNOSIS ) )
                         .flatMap( Collection::stream )
                         .map( n -> DataUtil.getPreferredText( graphDb, n ) )
                         .forEach( diagnoses::add );
            }
            if ( diagnoses.isEmpty() ) {
               continue;
            }
            final List<String> diagnosisList = new ArrayList<>( diagnoses );
            diagnosisList.sort( String.CASE_INSENSITIVE_ORDER );
            for ( String diagnosis : diagnosisList ) {
               final JSONObject anonymousJson = new JSONObject();
               final JSONArray rowArray = new JSONArray();
               rowArray.add( patientId );
               rowArray.add( diagnosis );
               anonymousJson.put( "row", rowArray );
               dataArray.add( anonymousJson );
            }
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "p.name", "fact.prettyName" ).toJSONString();
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            PATIENT DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @UserFunction( name = "deepphe.getTimelineData" )
   @Description( "return all note information in the corpus as json text.  Some redundant information" )
   public String getTimelineData( @Name( "patientId" ) String patientId ) {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patientNode = SearchUtil.getObjectNode( graphDb, patientId );
         if ( patientNode == null ) {
            tx.success();
            return wrapResults( dataArray, "r.id", "r.principalDate", "r.title", "r.type", "episode", "p" ).toJSONString();
         }
         // get the notes for the patient
         final Collection<Node> notes
               = SearchUtil.getOutRelatedNodes( graphDb, patientNode, SUBJECT_HAS_NOTE_RELATION );
         if ( notes.isEmpty() ) {
            tx.success();
            return wrapResults( dataArray, "r.id", "r.principalDate", "r.title", "r.type", "episode", "p" ).toJSONString();
         }
         final Map<String, String> patientProperties = new HashMap<>( 6 );
         patientProperties.put( "birthday", DataUtil.objectToString( patientNode.getProperty( PATIENT_BIRTH_DATE ) ) );
         patientProperties.put( "lastEncounterDate", DataUtil.objectToString( patientNode.getProperty( PATIENT_LAST_ENCOUNTER ) ) );
         patientProperties.put( "patientId", DataUtil.objectToString( patientNode.getProperty( NAME_KEY ) ) );
         patientProperties.put( "patientName", DataUtil.objectToString( patientNode.getProperty( PATIENT_NAME ) ) );
         patientProperties.put( "firstEncounterDate", DataUtil.objectToString( patientNode.getProperty( PATIENT_FIRST_ENCOUNTER ) ) );
         patientProperties.put( "postmenopausal", Math.random() > 0.5 ? "true" : "false" );
         // For each note, add a patient object
         for ( Node note : notes ) {
            final JSONObject anonymousJson = new JSONObject();
            final JSONArray rowArray = new JSONArray();
            rowArray.add( DataUtil.objectToString( note.getProperty( NAME_KEY ) ) );
            rowArray.add( DataUtil.unpackDateTime( DataUtil.objectToString( note.getProperty( NOTE_DATE ) ) ) );
            rowArray.add( DataUtil.objectToString( note.getProperty( NOTE_NAME ) ) );
            final Node classNode = DataUtil.getInstanceClass( graphDb, note );
            rowArray.add( DataUtil.objectToString( classNode.getProperty( NAME_KEY ) ) );
            rowArray.add( DataUtil.objectToString( note.getProperty( NOTE_EPISODE ) ) );

            final JSONObject patientObject = new JSONObject();
            patientObject.putAll( patientProperties );
            rowArray.add( patientObject );
            anonymousJson.put( "row", rowArray );
            dataArray.add( anonymousJson );
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "r.id", "r.principalDate", "r.title", "r.type", "episode", "p" ).toJSONString();
   }


   @UserFunction( name = "deepphe.getPatientInfo" )
   @Description( "return patient information as a simple json string." )
   public String getPatientInfo( @Name( "patientId" ) String patientId ) {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patientNode = SearchUtil.getObjectNode( graphDb, patientId );
         if ( patientNode == null ) {
            tx.success();
            return wrapResults( dataArray, "p" ).toJSONString();
         }
         final JSONObject anonymousJson = new JSONObject();
         final JSONArray rowArray = new JSONArray();
         final JSONObject patientObject = new JSONObject();
         patientObject.put( "birthday", DataUtil.objectToString( patientNode.getProperty( PATIENT_BIRTH_DATE ) ) );
         patientObject.put( "lastEncounterDate", DataUtil.objectToString( patientNode.getProperty( PATIENT_LAST_ENCOUNTER ) ) );
         patientObject.put( "patientId", DataUtil.objectToString( patientNode.getProperty( NAME_KEY ) ) );
         patientObject.put( "patientName", DataUtil.objectToString( patientNode.getProperty( PATIENT_NAME ) ) );
         patientObject.put( "firstEncounterDate", DataUtil.objectToString( patientNode.getProperty( PATIENT_FIRST_ENCOUNTER ) ) );
         patientObject.put( "postmenopausal", Math.random() > 0.5 ? "true" : "false" );
         rowArray.add( patientObject );
         anonymousJson.put( "row", rowArray );
         dataArray.add( anonymousJson );
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "p" ).toJSONString();
   }


   @UserFunction( name = "deepphe.getCancerSummary" )
   @Description( "return patient cancer information as a simple json string." )
   public String getCancerSummary( @Name( "patientId" ) String patientId ) {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patientNode = SearchUtil.getObjectNode( graphDb, patientId );
         if ( patientNode == null ) {
            tx.success();
            return wrapResults( dataArray, "cancer.id", "cancerFactReln.name", "fact" ).toJSONString();
         }
         final Collection<Node> cancerNodes
               = SearchUtil.getOutRelatedNodes( graphDb, patientNode, SUBJECT_HAS_CANCER_RELATION );
         for ( Node cancerNode : cancerNodes ) {
            final String cancerId = DataUtil.objectToString( cancerNode.getProperty( NAME_KEY ) );
            for ( Relationship relation : cancerNode.getRelationships( CANCER_HAS_FACT_RELATION, Direction.OUTGOING ) ) {
               final String relationName = objectToString( relation.getProperty( SUMMARY_FACT_RELATION_TYPE ) );
               final JSONObject anonymousJson = new JSONObject();
               final JSONArray rowArray = new JSONArray();
               rowArray.add( cancerId );
               rowArray.add( relationName );
               final JSONObject targetObject = new JSONObject();
               targetObject.put( "rulesApplied", new JSONArray() );
               final Node targetNode = relation.getOtherNode( cancerNode );
               String semantic = "";
               for ( Label label : targetNode.getLabels() ) {
                  if ( isSemanticType( label.name() ) ) {
                     semantic = label.name();
                     break;
                  }
               }
               final Node classNode = DataUtil.getInstanceClass( graphDb, targetNode );
               final String classId = DataUtil.objectToString( classNode.getProperty( NAME_KEY ) );
               if ( HAS_STAGE.equals( relationName ) ) {
                  targetObject.put( "prettyName", getPrettyStage( classId ) );
               } else {
                  targetObject.put( "prettyName", DataUtil.objectToString( classNode.getProperty( PREF_TEXT_KEY ) ) );
               }
               targetObject.put( "name", classId );
               targetObject.put( "id", DataUtil.objectToString( targetNode.getProperty( NAME_KEY ) ) );
               targetObject.put( "type", semantic );
               targetObject.put( "uri", classId );

               rowArray.add( targetObject );
               anonymousJson.put( "row", rowArray );
               dataArray.add( anonymousJson );
            }
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "cancer.id", "cancerFactReln.name", "fact" ).toJSONString();
   }


   @UserFunction( name = "deepphe.getTumorSummary" )
   @Description( "return cancer information as a simple json string.  The input patientId is ignored." )
   public String getTumorSummary( @Name( "patientId" ) String patientId, @Name( "cancerId" ) String cancerId ) {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node cancerNode = SearchUtil.getObjectNode( graphDb, cancerId );
         if ( cancerNode == null ) {
            tx.success();
            return wrapResults( dataArray, "tumor.id", "tumorFactReln.name", "fact", "rel", "f" ).toJSONString();
         }
         final Collection<Node> tumorNodes
               = SearchUtil.getOutRelatedNodes( graphDb, cancerNode, CANCER_HAS_TUMOR_RELATION );
         for ( Node tumorNode : tumorNodes ) {
            final String tumorId = DataUtil.objectToString( tumorNode.getProperty( NAME_KEY ) );
            for ( Relationship relation : tumorNode.getRelationships( TUMOR_HAS_FACT_RELATION, Direction.OUTGOING ) ) {
               final String relationName = objectToString( relation.getProperty( SUMMARY_FACT_RELATION_TYPE ) );
               final JSONObject anonymousJson = new JSONObject();
               final JSONArray rowArray = new JSONArray();
               rowArray.add( tumorId );
               rowArray.add( relationName );
               final JSONObject targetObject = new JSONObject();
               targetObject.put( "rulesApplied", new JSONArray() );
               final Node targetNode = relation.getOtherNode( tumorNode );
               String semantic = "";
               for ( Label label : targetNode.getLabels() ) {
                  if ( isSemanticType( label.name() ) ) {
                     semantic = label.name();
                     break;
                  }
               }
               final Node classNode = DataUtil.getInstanceClass( graphDb, targetNode );
               final String classId = DataUtil.objectToString( classNode.getProperty( NAME_KEY ) );
               targetObject.put( "prettyName", DataUtil.objectToString( classNode.getProperty( PREF_TEXT_KEY ) ) );
               targetObject.put( "name", classId );
               targetObject.put( "id", DataUtil.objectToString( targetNode.getProperty( NAME_KEY ) ) );
               targetObject.put( "type", semantic );
               targetObject.put( "uri", classId );

               rowArray.add( targetObject );
               rowArray.add( null );
               rowArray.add( null );
               anonymousJson.put( "row", rowArray );
               dataArray.add( anonymousJson );
            }
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "tumor.id", "tumorFactReln.name", "fact", "rel", "f" ).toJSONString();
   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            FACT DATA
   //
   /////////////////////////////////////////////////////////////////////////////////////////


   @UserFunction( name = "deepphe.getReport" )
   @Description( "return all note information as json text.  Some redundant information" )
   public String getReport( @Name( "reportId" ) String reportId ) {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node noteNode = SearchUtil.getObjectNode( graphDb, reportId );
         if ( noteNode == null ) {
            tx.success();
            return wrapResults( dataArray, "r.text", "n.text", "n.startOffset", "n.endOffset" ).toJSONString();
         }
         final String noteText = DataUtil.objectToString( noteNode.getProperty( NOTE_TEXT ) );
         final int noteLength = noteText.length();
         final Collection<Node> mentionNodes
               = SearchUtil.getOutRelatedNodes( graphDb, noteNode, NOTE_HAS_TEXT_MENTION_RELATION );
         boolean wroteNote = false;
         for ( Node mentionNode : mentionNodes ) {
            final int begin = DataUtil.objectToInt( mentionNode.getProperty( TEXT_SPAN_BEGIN ) );
            final int end = DataUtil.objectToInt( mentionNode.getProperty( TEXT_SPAN_END ) );
            if ( begin >= 0 && end > begin && end <= noteLength ) {
               final JSONObject anonymousJson = new JSONObject();
               final JSONArray rowArray = new JSONArray();
               rowArray.add( noteText );
               rowArray.add( noteText.substring( begin, end ) );
               rowArray.add( begin );
               rowArray.add( end );
               anonymousJson.put( "row", rowArray );
               dataArray.add( anonymousJson );
               wroteNote = true;
            }
         }
         if ( !wroteNote ) {
            final JSONObject anonymousJson = new JSONObject();
            final JSONArray rowArray = new JSONArray();
            rowArray.add( noteText );
            rowArray.add( null );
            rowArray.add( null );
            rowArray.add( null );
            anonymousJson.put( "row", rowArray );
            dataArray.add( anonymousJson );
         }
         tx.success();
      } catch ( MultipleFoundException mfE ) {
         //         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "r.text", "n.text", "n.startOffset", "n.endOffset" ).toJSONString();
   }


   @UserFunction( name = "deepphe.getFact" )
   @Description( "return all fact information as json text.  Some redundant information" )
   public String getFact( @Name( "factId" ) String factId ) {
      final JSONArray dataArray = new JSONArray();
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node factNode = SearchUtil.getObjectNode( graphDb, factId );
         if ( factNode == null ) {
            tx.success();
            return wrapResults( dataArray, "fact", "rel", "n" ).toJSONString();
         }


         final Collection<String> factRules = Collections.singletonList( "fact-discovery" );
         final Node classNode = DataUtil.getInstanceClass( graphDb, factNode );
         final String factPrefText = DataUtil.objectToString( classNode.getProperty( PREF_TEXT_KEY ) );
         final String factUri = DataUtil.objectToString( classNode.getProperty( NAME_KEY ) );
         String semantic = "";
         for ( Label label : factNode.getLabels() ) {
            if ( isSemanticType( label.name() ) ) {
               semantic = label.name();
               break;
            }
         }

         boolean wroteFact = false;

         final Iterable<Relationship> factRelations
               = factNode.getRelationships( FACT_HAS_RELATED_FACT_RELATION, Direction.OUTGOING );
         for ( Relationship factRelation : factRelations ) {
            final JSONObject anonymousJson = new JSONObject();
            final JSONArray rowArray = new JSONArray();

            final JSONObject factJson = new JSONObject();
            final JSONArray rulesArray = new JSONArray();
            rulesArray.addAll( factRules );
            factJson.put( "rulesApplied", rulesArray );
            factJson.put( "prettyName", factPrefText );
            factJson.put( "name", factUri );
            factJson.put( "id", factId );
            factJson.put( "type", semantic );
            factJson.put( "uri", factUri );
            rowArray.add( factJson );
            wroteFact = true;

            final JSONObject nameJson = new JSONObject();
            nameJson.put( "name", DataUtil.objectToString( factRelation.getProperty( FACT_RELATION_TYPE ) ) );
            rowArray.add( nameJson );

            final Node otherFact = factRelation.getOtherNode( factNode );
            final Collection<String> otherFactRules = Collections.singletonList( "fact-discovery" );
            final Node otherClassNode = DataUtil.getInstanceClass( graphDb, otherFact );
            final String otherFactPrefText = DataUtil.objectToString( otherClassNode.getProperty( PREF_TEXT_KEY ) );
            final String otherFactUri = DataUtil.objectToString( otherClassNode.getProperty( NAME_KEY ) );
            String otherSemantic = "";
            for ( Label label : otherFact.getLabels() ) {
               if ( isSemanticType( label.name() ) ) {
                  semantic = label.name();
                  break;
               }
            }

            final JSONObject otherFactJson = new JSONObject();
            final JSONArray otherRulesArray = new JSONArray();
            otherRulesArray.addAll( otherFactRules );
            otherFactJson.put( "rulesApplied", otherRulesArray );
            otherFactJson.put( "prettyName", otherFactPrefText );
            otherFactJson.put( "name", otherFactUri );
            otherFactJson.put( "id", DataUtil.objectToString( otherFact.getProperty( NAME_KEY ) ) );
            otherFactJson.put( "type", otherSemantic );
            otherFactJson.put( "uri", otherFactUri );
            rowArray.add( otherFactJson );

            anonymousJson.put( "row", rowArray );
            dataArray.add( anonymousJson );
         }



         final Collection<Node> mentionNodes
               = SearchUtil.getOutRelatedNodes( graphDb, factNode, FACT_HAS_TEXT_MENTION_RELATION );
         for ( Node mentionNode : mentionNodes ) {
            final Collection<Node> noteNodes
                  = SearchUtil.getInRelatedNodes( graphDb, mentionNode, NOTE_HAS_TEXT_MENTION_RELATION );
            if ( noteNodes.size() != 1 ) {
               continue;
            }
            final Node noteNode = new ArrayList<>( noteNodes ).get( 0 );
            final String noteText = DataUtil.objectToString( noteNode.getProperty( NOTE_TEXT ) );
            final int noteLength = noteText.length();
            final Node noteClassNode = DataUtil.getInstanceClass( graphDb, noteNode );
            final String noteType = DataUtil.objectToString( noteClassNode.getProperty( PREF_TEXT_KEY ) );
            final String noteId = DataUtil.objectToString( noteNode.getProperty( NAME_KEY ) );
            final String noteName = DataUtil.objectToString( noteNode.getProperty( NOTE_NAME ) );
            String patientId = "";
            final Collection<Node> patientNodes
                  = SearchUtil.getInRelatedNodes( graphDb, noteNode, SUBJECT_HAS_NOTE_RELATION );
            if ( patientNodes.size() == 1 ) {
               patientId = DataUtil.objectToString( new ArrayList<>( patientNodes ).get( 0 ).getProperty( NAME_KEY ) );
            }
            final int begin = DataUtil.objectToInt( mentionNode.getProperty( TEXT_SPAN_BEGIN ) );
            final int end = DataUtil.objectToInt( mentionNode.getProperty( TEXT_SPAN_END ) );
            if ( begin >= 0 && end > begin && end <= noteLength ) {
               final JSONObject anonymousJson = new JSONObject();
               final JSONArray rowArray = new JSONArray();

               final JSONObject factJson = new JSONObject();
               final JSONArray rulesArray = new JSONArray();
               rulesArray.addAll( factRules );
               factJson.put( "rulesApplied", rulesArray );
               factJson.put( "prettyName", factPrefText );
               factJson.put( "name", factUri );
               factJson.put( "id", factId );
               factJson.put( "type", semantic );
               factJson.put( "uri", factUri );
               rowArray.add( factJson );
               wroteFact = true;

               final JSONObject nameJson = new JSONObject();
               nameJson.put( "name", "hasTextProvenance" );
               rowArray.add( nameJson );

               final JSONObject mentionJson = new JSONObject();
               mentionJson.put( "endOffset", end );
               mentionJson.put( "startOffset", begin );
               mentionJson.put( "documentType", noteType );
               mentionJson.put( "documentSection", "Not Implemented" );
               mentionJson.put( "documentId", noteId );
               mentionJson.put( "documentName", noteName );
               mentionJson.put( "text", noteText.substring( begin, end ) );
               if ( !patientId.isEmpty() ) {
                  mentionJson.put( "patientId", patientId );
               }
               rowArray.add( mentionJson );

               anonymousJson.put( "row", rowArray );
               dataArray.add( anonymousJson );
            }
         }

         if ( !wroteFact ) {
            // If the fact did not come explicitly from text (for instance, a diagnosis), we still have to write json
            final JSONObject anonymousJson = new JSONObject();
            final JSONArray rowArray = new JSONArray();

            final JSONObject factJson = new JSONObject();
            final JSONArray rulesArray = new JSONArray();
            rulesArray.addAll( factRules );
            factJson.put( "rulesApplied", rulesArray );
            factJson.put( "prettyName", factPrefText );
            factJson.put( "name", factUri );
            factJson.put( "id", factId );
            factJson.put( "type", semantic );
            factJson.put( "uri", factUri );
            rowArray.add( factJson );

            rowArray.add( null );

            rowArray.add( null );

            anonymousJson.put( "row", rowArray );
            dataArray.add( anonymousJson );
         }

         tx.success();
      } catch ( MultipleFoundException mfE ) {
         //         throw new RuntimeException( mfE );
      }
      return wrapResults( dataArray, "fact", "rel", "n" ).toJSONString();
   }


   static private JSONObject wrapResults( final JSONArray dataArray, final String... columns ) {
      final JSONObject mainObject = new JSONObject();
      final JSONArray resultsArray = new JSONArray();
      final JSONObject resultsObject = new JSONObject();
      final JSONArray columnsArray = new JSONArray();

      columnsArray.addAll( Arrays.asList( columns ) );

      resultsObject.put( "columns", columnsArray );
      resultsObject.put( "data", dataArray );

      resultsArray.add( resultsObject );

      mainObject.put( "results", resultsArray );
      mainObject.put( "errors", new JSONArray() );

      return mainObject;
   }

   static private String getPrettyName( final String uri ) {
      if ( uri.startsWith( "Stage_" ) ) {
         return getPrettyStage( uri );
      }
      return uri;
   }

   static private String getPrettyStage( final String name ) {
      final String uri = name.substring( 0, 8 );
      switch ( uri ) {
         case "Stage_Un":
            return "Stage Unknown";
         case "Stage_0_":
            return "Stage 0";
         case "Stage_0i":
            return "Stage 0";
         case "Stage_0a":
            return "Stage 0";
         case "Stage_Is":
            return "Stage 0";
         case "Stage_1_":
            return "Stage I";
         case "Stage_1m":
            return "Stage I";
         case "Stage_1A":
            return "Stage IA";
         case "Stage_1B":
            return "Stage IB";
         case "Stage_1C":
            return "Stage IC";
         case "Stage_2_":
            return "Stage II";
         case "Stage_2A":
            return "Stage IIA";
         case "Stage_2B":
            return "Stage IIB";
         case "Stage_2C":
            return "Stage IIC";
         case "Stage_3_":
            return "Stage III";
         case "Stage_3A":
            return "Stage IIIA";
         case "Stage_3B":
            return "Stage IIIB";
         case "Stage_3C":
            return "Stage IIIC";
         case "Stage_4_":
            return "Stage IV";
         case "Stage_4A":
            return "Stage IVA";
         case "Stage_4B":
            return "Stage IVB";
         case "Stage_4C":
            return "Stage IVC";
         case "Stage_5_":
            return "Stage V";
      }
      return name;
   }

}

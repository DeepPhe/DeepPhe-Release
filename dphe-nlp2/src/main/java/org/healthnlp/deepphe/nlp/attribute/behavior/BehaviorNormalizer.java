package org.healthnlp.deepphe.nlp.attribute.behavior;


import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.attribute.xn.DefaultXnAttributeNormalizer;
import org.healthnlp.deepphe.nlp.attribute.xn.XnAttributeValue;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class BehaviorNormalizer extends DefaultXnAttributeNormalizer {

   static private Collection<String> BENIGN_URIS;
   static private Collection<String> BORDERLINE_URIS;
   static private Collection<String> IN_SITU_URIS;
   static private Collection<String> MALIGNANT_URIS;
   static private Collection<String> METASTATIC_URIS;
   static private final int LYMPH_WINDOW = 30;
   static private final double LYMPH_REDUCTION = 2;


   public void init( final AttributeInfoCollector infoCollector, final Map<String, List<XnAttributeValue>> dependencies,
                     final long mentionCount ) {
      if ( MALIGNANT_URIS == null ) {
         MALIGNANT_URIS = new HashSet<>( UriInfoCache.getInstance().getUriBranch( "Invasive" ) );
         MALIGNANT_URIS.addAll( UriInfoCache.getInstance().getUriBranch( "Malignant" ) );
         BENIGN_URIS = new HashSet<>( UriInfoCache.getInstance().getUriBranch( "Benign" ) );
         BORDERLINE_URIS = new HashSet<>( UriInfoCache.getInstance().getUriBranch( "Borderline" ) );
         BORDERLINE_URIS.addAll( UriInfoCache.getInstance().getUriBranch( "Microinvasive" ) );
         IN_SITU_URIS = new HashSet<>( UriInfoCache.getInstance().getUriBranch( "InSitu" ) );
         IN_SITU_URIS.addAll( UriInfoCache.getInstance().getUriBranch( "Premalignant" ) );
         IN_SITU_URIS.addAll( UriInfoCache.getInstance().getUriBranch( "Non_Malignant" ) );
         IN_SITU_URIS.addAll( UriInfoCache.getInstance().getUriBranch( "Noninvasive" ) );
         METASTATIC_URIS = new HashSet<>( UriInfoCache.getInstance().getUriBranch( "Metaplastic" ) );
         METASTATIC_URIS.addAll( UriInfoCache.getInstance().getUriBranch( "Metastasis" ) );
         METASTATIC_URIS.addAll( UriInfoCache.getInstance().getUriBranch( "Metastatic" ) );
      }
      super.init( infoCollector, dependencies, mentionCount );
//      NeoplasmSummaryCreator.addDebug( "Behavior best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getNormalNoValue() {
      return "3";
   }

//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // The Cancer Registry default is 3.
//         return "3";
//      }
//
//      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
//      int bestCode = -1;
//      long bestCodesCount = 0;
//      for ( Map.Entry<Integer,Long> codeCount : intCountMap.entrySet() ) {
//         final long count = codeCount.getValue();
//         if ( count > bestCodesCount ) {
//            bestCodesCount = count;
//            bestCode = codeCount.getKey();
//         } else if ( count == bestCodesCount ) {
//            if ( bestCode == 3 || codeCount.getKey() == 3 ) {
//               bestCode = 3;
//            } else {
//               bestCode = Math.max( bestCode, codeCount.getKey() );
//            }
//         }
//      }
//      setBestCodesCount( (int)bestCodesCount );
//      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( intCountMap.size() );
//      NeoplasmSummaryCreator.addDebug( "BehaviorNormalizer "
//                                       + intCountMap.entrySet().stream()
//                                                 .map( e -> e.getKey() + ":" + e.getValue() )
//                                                 .collect( Collectors.joining(",") ) + " = "
//                                       + bestCode +"\n");
////      return bestCode < 0 ? "3" : bestCode+"";
//      // CR doesn't do much benign, default to malignant.
//      return bestCode <= 0 ? "3" : bestCode+"";
//   }

   private String getOppositeUri( final UriConcept uriConcept ) {
      final String uri = uriConcept.getUri();
      if ( METASTATIC_URIS.contains( uri ) ) {
         return "Borderline";
      } else if ( MALIGNANT_URIS.contains( uri ) ) {
         return "InSitu";
      } else if ( IN_SITU_URIS.contains( uri ) ) {
         return "Invasive";
      } else if ( BORDERLINE_URIS.contains( uri ) ) {
         return "Invasive";
      } else  if ( BENIGN_URIS.contains( uri ) ) {
         return "Invasive";
      }
      return uri;
   }

//   public String getTextValue( final UriConcept uriConcept ) {
//      if ( uriConcept.isNegated() ) {
//         return getNormalValue( getOppositeUri( uriConcept ) );
//      }
//      return getNormalValue( uriConcept.getUri() );
//   }


   public String getNormalValue( final UriConcept concept ) {
      final String uri = concept.getUri();
      if ( METASTATIC_URIS.contains( uri ) ) {
         return "3";
         // Cancer registries do not use behavior code 6
         //return 6;
      }
      if ( MALIGNANT_URIS.contains( uri ) ) {
         return "3";
      }
      if ( IN_SITU_URIS.contains( uri ) ) {
         return "2";
      }
      if ( BORDERLINE_URIS.contains( uri ) ) {
         return "1";
      }
      if ( BENIGN_URIS.contains( uri ) ) {
//         return 0;
         return "3";
      }
//      NeoplasmSummaryCreator.addDebug( "No Behavior code for " + uri +"\n");
      return getNormalNoValue();
   }


//   public double getConfidence() {
//      int lymphCount = 0;
//      for ( CrConceptAggregate aggregate : getAggregates() ) {
//         for ( Mention mention : aggregate.getMentions() ) {
//            final int mentionBegin = mention.getBegin();
//            if ( mentionBegin <= LYMPH_WINDOW ) {
//               continue;
//            }
//            final Note note = NoteNodeStore.getInstance()
//                                           .get( mention.getNoteId() );
//            if ( note == null ) {
////                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
//               continue;
//            }
//            final String preText = note.getText()
//                                       .substring( mentionBegin - LYMPH_WINDOW, mentionBegin )
//                                       .toLowerCase();
//            NeoplasmSummaryCreator.addDebug( "Behavior Candidate and pretext "
//                                             + note.getText()
//                                                   .substring( mentionBegin - LYMPH_WINDOW,
//                                                               mention.getEnd() )
//                                             + "\n" );
//            if ( preText.contains( "lymph node" ) ) {
//               NeoplasmSummaryCreator.addDebug( "Tracking Behavior uri "
//                                                + mention.getClassUri() + "\n" );
//               lymphCount++;
//            }
//         }
//      }
//      final double confidence = super.getRelations()
//                                     .stream()
//                                     .mapToDouble( ConceptAggregateRelation::getConfidence )
//                                     .average()
//                                     .orElse( 0 );
//      return Math.min( 10, confidence - LYMPH_REDUCTION*lymphCount );
//   }


}

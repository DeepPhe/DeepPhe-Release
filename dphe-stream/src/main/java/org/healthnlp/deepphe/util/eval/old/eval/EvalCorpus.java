package org.healthnlp.deepphe.util.eval.old.eval;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.EvalSummarizer.PATIENT_ID;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/23/2019
 */
final class EvalCorpus extends AbstractEvalObject {

   private Collection<EvalPatient> _patients;
   private final Collection<EvalNeoplasm> _neoplasmPairs;

   EvalCorpus( final Collection<EvalPatient> patients ) {
      _patients = patients;
      _neoplasmPairs = patients.stream()
                               .map( EvalPatient::getNeoplasmPairs )
                               .flatMap( Collection::stream )
                               .collect( Collectors.toList() );
   }

   Collection<EvalPatient> getPatients() {
      return _patients;
   }

   void fillAttributeScores( final Map<String, Double> attributeTPs,
                             final Map<String, Double> attributeTNs,
                             final Map<String, Double> attributeFPs,
                             final Map<String, Double> attributeFNs,
                             final Collection<String> requiredNames,
                             final Collection<String> scoringNames,
                             final boolean simple ) {
      for ( EvalNeoplasm neoplasm : _neoplasmPairs ) {
         if ( simple && neoplasm.getF1() == 0 ) {
            continue;
         }
         for ( String name : scoringNames ) {
            double tp = attributeTPs.computeIfAbsent( name, d -> 0d );
            double tn = attributeTNs.computeIfAbsent( name, d -> 0d );
            double fp = attributeFPs.computeIfAbsent( name, d -> 0d );
            double fn = attributeFNs.computeIfAbsent( name, d -> 0d );
            final String gold = neoplasm.getGoldAttribute( name );
            final String system = neoplasm.getSystemAttribute( name );
            final EvalUris evalUris = new EvalUris( name, gold, system );
            attributeTPs.put( name, tp + evalUris.getTP_() );
            attributeTNs.put( name, tn + evalUris.getTN_() );
            attributeFPs.put( name, fp + evalUris.getFP_() );
            attributeFNs.put( name, fn + evalUris.getFN_() );
         }
         for ( String name : requiredNames ) {
            if ( !PATIENT_ID.equalsIgnoreCase( name ) ) {
               double tp = attributeTPs.computeIfAbsent( name, d -> 0d );
               attributeTPs.put( name, tp + 1 );
            }
         }
      }
   }

   public double calcTP() {
//         return ( getTP_() > 0 ) ? 1 : 0;
      return _neoplasmPairs.stream()
                           .mapToDouble( EvalNeoplasm::getTP )
                           .sum();
   }

   public double calcTN() {
      return 0;
   }

   public double calcFP() {
//         return 0;
      return _neoplasmPairs.stream()
                           .mapToDouble( EvalNeoplasm::getFP )
                           .sum();
   }

   public double calcFN() {
//         return ( getFN_() > 0 ) ? 1 : 0;
      return _neoplasmPairs.stream()
                           .mapToDouble( EvalNeoplasm::getFN )
                           .sum();
   }

   public double calcTP_() {
      return _neoplasmPairs.stream()
                           .mapToDouble( EvalNeoplasm::getTP_ )
                           .sum();
   }

   public double calcTN_() {
      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getTN_ ).sum();
   }

   public double calcFP_() {
      return _neoplasmPairs.stream()
                           .mapToDouble( EvalNeoplasm::getFP_ )
                           .sum();
   }

   public double calcFN_() {
      return _neoplasmPairs.stream()
                           .mapToDouble( EvalNeoplasm::getFN_ )
                           .sum();
   }

   public double getFullScore() {
      return _neoplasmPairs.stream()
                           .mapToDouble( EvalNeoplasm::getFullScore )
                           .sum();
   }

}

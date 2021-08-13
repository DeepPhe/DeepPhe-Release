//package org.healthnlp.deepphe.util.eval;
//
//
//import java.util.Collection;
//import java.util.Collections;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 5/23/2019
// */
//final class EvalPatient extends AbstractEvalObject implements IdOwner {
//   private final String _id;
//   private final Collection<EvalNeoplasm> _neoplasmPairs;
//
//   private EvalPatient( final String id, final EvalNeoplasm pair ) {
//      this( id, Collections.singletonList( pair ) );
//   }
//
//   EvalPatient( final String id, final Collection<EvalNeoplasm> neoplasmPairs ) {
//      _id = id;
//      _neoplasmPairs = neoplasmPairs;
//   }
//
//   public String getId() {
//      return _id;
//   }
//
//   Collection<EvalNeoplasm> getNeoplasmPairs() {
//      return _neoplasmPairs;
//   }
//
//   boolean isValid() {
//      return _neoplasmPairs.stream().allMatch( EvalNeoplasm::isValid );
//   }
//
//   public double calcTP() {
//      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getTP ).sum();
//   }
//
//   public double calcTN() {
//      return 0;
//   }
//
//   public double calcFP() {
//      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getFP ).sum();
//   }
//
//   public double calcFN() {
//      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getFN ).sum();
//   }
//
//   public double calcTP_() {
//      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getTP_ ).sum();
//   }
//
//   public double calcTN_() {
//      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getTN_ ).sum();
//   }
//
//   public double calcFP_() {
//      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getFP_ ).sum();
//   }
//
//   public double calcFN_() {
//      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getFN_ ).sum();
//   }
//
//   public double getFullScore() {
//      return _neoplasmPairs.stream().mapToDouble( EvalNeoplasm::getFullScore ).sum();
//   }
//
//}

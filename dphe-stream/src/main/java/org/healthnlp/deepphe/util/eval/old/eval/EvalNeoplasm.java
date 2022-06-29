package org.healthnlp.deepphe.util.eval.old.eval;

import java.util.Collection;
import java.util.Collections;

import static org.healthnlp.deepphe.EvalSummarizer.PATIENT_ID;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/23/2019
 */
final class EvalNeoplasm extends AbstractEvalObject {
   private final NeoplasmSummary _gold;
   private final NeoplasmSummary _system;

   EvalNeoplasm( final NeoplasmSummary gold, final NeoplasmSummary system ) {
      _gold = gold;
      _system = system;
   }

   NeoplasmSummary getGoldSummary() {
      return _gold;
   }

   NeoplasmSummary getSystemSummary() {
      return _system;
   }

   String getGoldAttribute( final String name ) {
      if ( _gold == null ) {
         return "";
      }
      return _gold.getAttribute( name );
   }

   String getSystemAttribute( final String name ) {
      if ( _system == null ) {
         return "";
      }
      return _system.getAttribute( name );
   }

   Collection<String> getRequiredNames() {
      if ( _gold == null ) {
         return Collections.emptyList();
      }
      return _gold.getRequiredAttributes().keySet();
   }

   Collection<String> getScoringNames() {
      if ( _gold == null ) {
         return Collections.emptyList();
      }
      return _gold.getScoringAttributes().keySet();
   }

   boolean isValid() {
      if ( _gold == null || _system == null ) {
         return true;
      }
      for ( String name : getRequiredNames() ) {
         if ( PATIENT_ID.equalsIgnoreCase( name ) ) {
            continue;
         }
         final String goldValue = getGoldAttribute( name );
         final String systemValue = getSystemAttribute( name );
         if ( !MatchUtil.isAnyMatch( name, goldValue, systemValue ) ) {
            return false;
         }
      }
      return true;
   }

   public double calcTP() {
      if ( _gold == null || _system == null ) {
         return 0;
      }
      for ( String name : getRequiredNames() ) {
         if ( PATIENT_ID.equalsIgnoreCase( name ) ) {
            continue;
         }
         final String goldValue = getGoldAttribute( name );
         final String systemValue = getSystemAttribute( name );
         if ( !MatchUtil.isAnyMatch( name, goldValue, systemValue ) ) {
            return 0;
         }
      }
      return 1;
   }

   public double calcTN() {
      return 0;
   }

   public double calcFP() {
      if ( _gold == null && _system != null ) {
         return 1;
      }
      return 0;
   }

   public double calcFN() {
      if ( _gold != null && _system == null ) {
         return 1;
      }
      return 0;
   }

   public double calcTP_() {
      if ( _gold == null || _system == null ) {
         return 0;
      }
      double score = 0;
      for ( String name : getScoringNames() ) {
         final String gold = getGoldAttribute( name );
         final String system = getSystemAttribute( name );
         final EvalUris evalUris = new EvalUris( name, gold, system );
         score += evalUris.getTP_();
      }
      score += getRequiredNames().size() - 1;
      return score;
   }

   public double calcTN_() {
      if ( _gold == null || _system == null ) {
         return 0;
      }
      double score = 0;
      for ( String name : getScoringNames() ) {
         final String gold = getGoldAttribute( name );
         final String system = getSystemAttribute( name );
         final boolean gEmpty = gold == null || gold.isEmpty();
         final boolean sEmpty = system == null || system.isEmpty();
         if ( gEmpty && sEmpty ) {
            score++;
         }
      }
      return score;
   }

   public double calcFP_() {
      if ( _gold == null ) {
         return 1;
      }
      if ( _system == null ) {
         return 0;
      }
      double score = 0;
      for ( String name : getScoringNames() ) {
         final String gold = getGoldAttribute( name );
         final String system = getSystemAttribute( name );
         final EvalUris evalUris = new EvalUris( name, gold, system );
         score += evalUris.getFP_();
      }
      return score;
   }

   public double calcFN_() {
      if ( _system == null ) {
         return 1;
      }
      double score = 0;
      for ( String name : getScoringNames() ) {
         final String gold = getGoldAttribute( name );
         final String system = getSystemAttribute( name );
         final EvalUris evalUris = new EvalUris( name, gold, system );
         score += evalUris.getFN_();
      }
      return score;
   }

   public double getFullScore() {
      double required = 0;
      for ( String name : getRequiredNames() ) {
         if ( PATIENT_ID.equalsIgnoreCase( name ) ) {
            continue;
         }
         final String goldValue = getGoldAttribute( name );
         final String systemValue = getSystemAttribute( name );
         if ( goldValue.equalsIgnoreCase( systemValue ) ) {
            required++;
         }
      }
      return required + getF1_s();
   }

   public boolean equals( final Object other ) {
      if ( other instanceof EvalNeoplasm ) {
         if ( _gold == null ) {
            return ((EvalNeoplasm)other)._gold == null && ((EvalNeoplasm)other)._system.equals( _system );
         } else if ( _system == null ) {
            return ((EvalNeoplasm)other)._system == null && ((EvalNeoplasm)other)._gold.equals( _gold );
         }
         return ((EvalNeoplasm)other)._system.equals( _system ) && ((EvalNeoplasm)other)._gold.equals( _gold );
      }
      return false;
   }

   public int hashCode() {
      int hc = 0;
      if ( _gold != null ) {
         hc += _gold.hashCode();
      }
      if ( _system != null ) {
         hc += 3 * _system.hashCode();
      }
      return hc;
   }
}

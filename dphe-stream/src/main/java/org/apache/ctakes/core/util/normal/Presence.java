package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.owner.CodeOwner;
import org.apache.ctakes.core.util.owner.TextsOwner;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
enum Presence implements CodeOwner, TextsOwner {
   // There are various classes for Equivocal Not_Classified Not_Defined Not_Otherwise_Specified Not_Reported
   // Undetermined_Finding Unequivocal Unevaluable Uninterpretable Unobtainable Unplanned Unrecognized
   // Unsolved ... Missing ... Inconclusive Incomplete
   // Indeterminate Insufficient_Information_Available
   // No_Information_Available No_Reason_Given Not_Answered Not_Applicable Not_Ascertained
   // Not_Asked Not_Available Not_Done Not_Identified Not_Obtained ... Unspecified
   ABSENT( "C0332197", "Absent", "absent", "not present" ),
   PRESENT( "C0150312", "Present", "present" ),
   PENDING( "C1547325", "Pending", "pending" ),
   UNKNOWN( "C0439673", "Unknown", "unknown", "unclassifiable", "unclassified",
            "not classified", "cannot be classified",
            "not assessable", "not assessed", "cannot be assessed",
            "cannot be identified",
            "not definitely identified", "not definitively identified", "not identified",
            "not determined", "cannot be determined", "undetermined",
            "indeterminate", "not seen", "insufficient", "equivocal", "borderline",
            "not submitted", "not provided", "not requested",
            "not given", "not performed", "not applicable", "n/a" );

   final private String _cui;
   final private String _uri;
   final private String[] _texts;
   Presence( final String cui, final String uri, final String... texts ) {
      _cui = cui;
      _uri = uri;
      _texts = texts;
   }

   public String getCui() {
      return _cui;
   }

   public String getUri() {
      return _uri;
   }

   public String[] getTexts() {
      return _texts;
   }

}

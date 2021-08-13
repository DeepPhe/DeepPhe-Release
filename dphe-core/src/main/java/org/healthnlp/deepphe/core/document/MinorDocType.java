package org.healthnlp.deepphe.core.document;

import org.apache.ctakes.core.util.doc.NoteSpecs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.healthnlp.deepphe.core.document.MajorDocType.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/12/2018
 */
public enum MinorDocType implements DocType {
   NOTE( CLINICAL, "NOTE" ),
   PGN( CLINICAL, "PGN" ),
   DS( CLINICAL, "DS" ),
   SP( PATHOLOGY, "SP" ),
   PATH( PATHOLOGY, "PATH" ),
   RAD( RADIOLOGY, "RAD" ),
   CLINIC( UNKNOWN, NoteSpecs.ID_NAME_CLINICAL_NOTE );

   private final MajorDocType _majorDocType;
   private final Collection<String> _codes;

   MinorDocType( final MajorDocType majorDocType, final String... codes ) {
      _majorDocType = majorDocType;
      if ( codes.length == 0 ) {
         _codes = Collections.emptyList();
      } else {
         _codes = Arrays.asList( codes );
      }
   }

   public Collection<SectionType> getWantedSections() {
      return _majorDocType.getWantedSections();
   }

   public boolean isThisDocType( final String code ) {
      return _codes.contains( code );
   }

   public MajorDocType getMajorDocType() {
      return _majorDocType;
   }

   static public MajorDocType getMajorDocType( final String code ) {
      return getMinorDocType( code ).getMajorDocType();
   }

   static public MinorDocType getMinorDocType( final String code ) {
      if ( code == null || code.isEmpty() ) {
         return CLINIC;
      }
      final String name = code.trim();
      for ( MinorDocType type : values() ) {
         if ( type.isThisDocType( name ) ) {
            return type;
         }
      }
      return CLINIC;
   }

}

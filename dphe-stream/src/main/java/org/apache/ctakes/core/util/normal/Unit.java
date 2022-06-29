package org.apache.ctakes.core.util.normal;

import org.apache.ctakes.core.util.Pair;

/**
 * @author SPF , chip-nlp
 * @since {12/21/2021}
 */
enum Unit implements TextListNormal {
   PERCENT( "percent", "%" ),
   MILLIGRAM( "milligram", "mg", "mgs", "milligrams" ),
   MICROGRAM( "microgram", "mcg", "ug", "micrograms" ),
   KILOOGRAM( "kilogram", "kg", "kilograms" ),
   GRAM( "gram", "g", "gm", "gr", "grams" ),
   OUNCE( "ounce", "oz", "ounces" ),
   MILLICURIE( "millicurie", "mic", "mci", "millicuries" ),
   MILLILITER( "milliliter", "ml", "millilitre", "milliliters", "millilitres" ),
   MICROLITER( "microliter", "ul", "microlitre", "microliters", "microlitres" ),
   LITER( "liter", "l", "litre", "liters", "litres" ),
   SQUARE_MILLIMETER( "square millimeter", "mm2", "square millimeters" ),
   MILLIMETER( "millimeter", "mm", "millimeters" ),
   SQUARE_CENTIMETER( "square centimeter", "cm2", "square centimeters" ),
   CUBIC_CENTIMETER( "cubic centimeter", "cc", "c3", "cubic centimeters" ),
   CENTIMETER( "centimeter", "cm", "centimeters" ),
   MILLIEQUIVALENT( "milliequivalent", "meq" ),
   MEDICAL_MAINTENANCE_UNIT( "mmu" ),
   BLOOD_ACTIVATOR_UNIT( "bau" ),
   UNIT( "unit", "unt", "ud", "au", "units" ), // unit dose (ud), arbitrary unit (au)
   TITRADOSE( "titradose" ),
   VIAL( "vial", "vil", "vials" ),
   PACKET( "packet", "pkt", "packets" );

   final private String[] _texts;

   Unit( final String... texts ) {
      _texts = texts;
   }

   public String getNormalName() {
      return name();
   }

   public String[] getTexts() {
      return _texts;
   }

   public Pair<Integer> getSquareSpan( final String fullText, final Pair<Integer> unitSpan ) {
      return NormalizeUtil.getAnyNumberableSpan( fullText, unitSpan.getValue1(), "sq", "sq.", "square" );
   }

   public Pair<Integer> getCubicSpan( final String fullText, final Pair<Integer> unitSpan ) {
      return NormalizeUtil.getAnyNumberableSpan( fullText, unitSpan.getValue1(), "cu", "cu.", "cubic" );
   }

   public Pair<Integer> getPerSpan( final String fullText, final Pair<Integer> unitSpan ) {
      if ( unitSpan.getValue1() > 0 && fullText.charAt( unitSpan.getValue1() - 1 ) == '/' ) {
         return new Pair<>( unitSpan.getValue1() - 1, unitSpan.getValue1() );
      }
      return NormalizeUtil.getAnyNumberableSpan( fullText, unitSpan.getValue1(), "/", "per" );
   }

   public String replaceText( final String text ) {
      Pair<Integer> unitSpan = getSpan( text );
      if ( unitSpan.getValue1() < 0 ) {
         return text;
      }
      final StringBuilder sb = new StringBuilder();
      int previousEnd = 0;
      while ( unitSpan.getValue1() >= 0 ) {
         Pair<Integer> replacementSpan = unitSpan;
         String replacement = name();
         final Pair<Integer> squareSpan = getSquareSpan( text, unitSpan );
         if ( squareSpan.getValue1() >= 0 ) {
            replacementSpan = new Pair<>( squareSpan.getValue1(), replacementSpan.getValue2() );
            replacement = "SQUARE " + replacement;
         } else {
            final Pair<Integer> cubicSpan = getCubicSpan( text, unitSpan );
            if ( cubicSpan.getValue1() >= 0 ) {
               replacementSpan = new Pair<>( cubicSpan.getValue1(), replacementSpan.getValue2() );
               replacement = "CUBIC " + replacement;
            }
         }
         final Pair<Integer> perSpan = getPerSpan( text, replacementSpan );
         if ( perSpan.getValue1() >= 0 ) {
            replacementSpan = new Pair<>( perSpan.getValue1(), replacementSpan.getValue2() );
            replacement = "PER " + replacement;
         }
         if ( replacementSpan.getValue1() > 0 ) {
            sb.append( text, previousEnd, replacementSpan.getValue1() )
              .append( " " );
         }
         sb.append( replacement );
         if ( replacementSpan.getValue2() < text.length() ) {
            sb.append( " " );
         }
         previousEnd = replacementSpan.getValue2();
         unitSpan = getSpan( text, previousEnd );
      }
      if ( previousEnd < text.length() ) {
         sb.append( text, previousEnd, text.length() );
      }
      return sb.toString();
   }
}

package org.healthnlp.deepphe.constant;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {4/11/2022}
 */
final public class CuiConstants {

   private CuiConstants() {
   }

   static private final Collection<Long> CANCER_GRADES = Arrays.asList( 1513302L, 1513374L, 1519275L, 3537125L, 205619L,
                                                                        1302551L, 1320484L, 1282907L,
                                                                        205616L, 205615L, 205617L, 205618L,
                                                                        475269L, 475270L, 475271L, 475272L );

   static public Collection<Long> getCancerGrades() {
      return CANCER_GRADES;
   }



}

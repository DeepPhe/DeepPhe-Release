package org.apache.ctakes.cancer.util;

import java.util.Comparator;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/20/2015
 */
public enum SpanOffsetComparator implements Comparator<SpannedEntity> {
   INSTANCE;

   public static SpanOffsetComparator getInstance() {
      return INSTANCE;
   }

   @Override
   public int compare( final SpannedEntity entity1, final SpannedEntity entity2 ) {
      return entity1.getStartOffset() - entity2.getStartOffset();
   }

}

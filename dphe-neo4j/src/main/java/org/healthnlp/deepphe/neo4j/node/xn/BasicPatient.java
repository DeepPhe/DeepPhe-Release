package org.healthnlp.deepphe.neo4j.node.xn;

/**
 * @author SPF , chip-nlp
 * @since {2/29/2024}
 */
public class BasicPatient {
   private String id;
   private String name;
   private String gender;
   private String birth;
   private String death;

   public BasicPatient( final String id, final String name, final String gender,
                        final String birth, final String death ) {
      this.id = id;
      this.name = name;
      this.gender = gender;
      this.birth = birth;
      this.death = death;
   }

}

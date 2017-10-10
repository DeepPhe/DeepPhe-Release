package org.healthnlp.deepphe.uima.drools;

public class Domain {
   private String domainName;
   private String ontologyURI;

   public Domain( String domainName, String ontologyURI ) {
      setDomainName( domainName );
      this.ontologyURI = ontologyURI;
   }

   public String getDomainName() {
      return domainName;
   }

   public void setDomainName( String domainName ) {
      this.domainName = domainName;
   }

   public String getOntologyURI() {
      return ontologyURI;
   }

   public void setOntologyURI( String ontologyURI ) {
      this.ontologyURI = ontologyURI;
   }

   public static void printMemoryStats() {
      int mb = 1024 * 1024;
      Runtime runtime = Runtime.getRuntime();

      System.out.println( "##### Heap utilization statistics [MB] #####" );

      //Print used memory
      System.out.println( "Used Memory:"
            + (runtime.totalMemory() - runtime.freeMemory()) / mb );

      //Print free memory
      System.out.println( "Free Memory:"
            + runtime.freeMemory() / mb );

      //Print total available memory
      System.out.println( "Total Memory:" + runtime.totalMemory() / mb );

      //Print Maximum available memory
      System.out.println( "Max Memory:" + runtime.maxMemory() / mb );
   }

}

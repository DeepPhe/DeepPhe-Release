package org.healthnlp.deepphe.util;

import org.healthnlp.deepphe.fhir.Element;
import org.healthnlp.deepphe.fhir.fact.Fact;

import java.util.HashMap;
import java.util.Map;

public enum FHIRRegistry {
   INSTANCE;

   private Map<Object, Element> registry;
   private Map<Object, Fact> factRegistry;

   private FHIRRegistry() {
      registry = new HashMap<Object, Element>();
      factRegistry = new HashMap<Object, Fact>();
   }

   public static FHIRRegistry getInstance() {
      return INSTANCE;
   }

   public Element getElement( Object id ) {
      return registry.get( id );
   }

   public Fact getFact( Object id ) {
      return factRegistry.get( id );
   }

   public int getSize() {
      return registry.size() + factRegistry.size();
   }

   public void clear() {
      registry.clear();
      factRegistry.clear();
   }

   public void addElement( Element e ) {
      registry.put( e.getResourceIdentifier(), e );
   }

   public void addFact( Fact e ) {
      factRegistry.put( e.getIdentifier(), e );
   }

   public void addElement( Element e, Object id ) {
      registry.put( id, e );
      addElement( e );
   }

   public void addFact( Fact e, Object id ) {
      factRegistry.put( id, e );
      addFact( e );
   }

   public boolean hasElement( Object id ) {
      return registry.containsKey( id );
   }

   public boolean hasFact( Object id ) {
      return factRegistry.containsKey( id );
   }
}

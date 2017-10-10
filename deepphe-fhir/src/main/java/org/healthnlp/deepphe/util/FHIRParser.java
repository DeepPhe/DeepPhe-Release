package org.healthnlp.deepphe.util;

import org.hl7.fhir.instance.formats.XmlParser;

/*
 Copyright (c) 2011-2013, HL7, Inc.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, 
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this 
 list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 this list of conditions and the following disclaimer in the documentation 
 and/or other materials provided with the distribution.
 * Neither the name of HL7 nor the names of its contributors may be used to 
 endorse or promote products derived from this software without specific 
 prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 POSSIBILITY OF SUCH DAMAGE.

 */

// Generated on Tue, Sep 30, 2014 18:08+1000 for FHIR v0.0.82

import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.utilities.Utilities;


/**
 * a copy of XML Composer to take care of CancerPhenotype
 *
 * @author tseytlin
 */

public class FHIRParser extends XmlParser {
   /*protected void composeTumor(String name, Cancer.Tumor element) throws Exception {
      if (element != null) {
			composeElementAttributes(element);
			xml.open(FHIR_NS, name);
			composeBackboneElements(element);
			composeCodeableConcept("type", element.getType());
			for (Condition.ConditionLocationComponent e : element.getBodySite())
				composeConditionConditionLocationComponent("location", e);
			for (Condition.ConditionEvidenceComponent e : element.getPhenotypicFactors())
				composeConditionConditionEvidenceComponent("phenotypicFactor", e);
			for (Condition.ConditionEvidenceComponent e : element.getTreatmentFactors())
				composeConditionConditionEvidenceComponent("treatmentFactor", e);
			for (Condition.ConditionEvidenceComponent e : element.getGenomicFactors())
				composeConditionConditionEvidenceComponent("genomicFactor", e);
			for (Condition.ConditionEvidenceComponent e : element.getRelatedFactors())
				composeConditionConditionEvidenceComponent("relatedFactor", e);
			
			xml.close(FHIR_NS, name);
		}
	}
	
	protected void composeComposition(String name, Composition element) throws Exception {
	    if (element != null) {
	      composeDomainResourceAttributes(element);
	      xml.open(FHIR_NS, name);
	      composeDomainResourceElements(element);
	      if (element.hasIdentifier()) {
	        composeIdentifier("identifier", element.getIdentifier());
	      }
	      if (element.hasDateElement()) {
	        composeDateTime("date", element.getDateElement());
	      }
	      if (element.hasType()) {
	        composeCodeableConcept("type", element.getType());
	      }
	      if (element.hasClass_()) {
	        composeCodeableConcept("class", element.getClass_());
	      }
	      if (element.hasTitleElement()) {
	        composeString("title", element.getTitleElement());
	      }
	      if (element.hasStatusElement())
	        composeEnumeration("status", element.getStatusElement(), new Composition.CompositionStatusEnumFactory());
	      if (element.hasConfidentialityElement()) {
	        composeCode("confidentiality", element.getConfidentialityElement());
	      }
	      if (element.hasSubject()) {
	        composeReference("subject", element.getSubject());
	      }
	      if (element.hasAuthor()) { 
	        for (Reference e : element.getAuthor()) 
	          composeReference("author", e);
	      }
	      if (element.hasAttester()) { 
	        for (Composition.CompositionAttesterComponent e : element.getAttester()) 
	          composeCompositionCompositionAttesterComponent("attester", e);
	      }
	      if (element.hasCustodian()) {
	        composeReference("custodian", element.getCustodian());
	      }
	      if (element.hasEvent()) { 
	        for (Composition.CompositionEventComponent e : element.getEvent()) 
	          composeCompositionCompositionEventComponent("event", e);
	      }
	      if (element.hasEncounter()) {
	        composeReference("encounter", element.getEncounter());
	      }
	      if (element.hasSection()) { 
	        for (Composition.SectionComponent e : element.getSection()) 
	          composeCompositionSectionComponent("section", e);
	      }
	      xml.close(FHIR_NS, name);
	    }
	  }
	
	

	protected void composeCancer(String name, Cancer element) throws Exception {
		if (element != null) {
			composeResourceAttributes(element);
			xml.open(FHIR_NS, name);
			composeResourceElements(element);
			for (Identifier e : element.getIdentifier())
				composeIdentifier("identifier", e);
			composeResourceReference("subject", element.getSubject());
			composeResourceReference("encounter", element.getEncounter());
			composeResourceReference("asserter", element.getAsserter());
			composeDate("dateAsserted", element.getDateAsserted());
			composeCodeableConcept("code", element.getCode());
			composeCodeableConcept("category", element.getCategory());
			if (element.getStatus() != null)
				composeEnumeration("status", element.getStatus(), new Condition.ConditionStatusEnumFactory());
			composeCodeableConcept("certainty", element.getCertainty());
			composeCodeableConcept("severity", element.getSeverity());
			composeType("onset", element.getOnset());
			composeType("abatement", element.getAbatement());
			composeConditionConditionStageComponent("stage", element.getStage());
			for (Condition.ConditionEvidenceComponent e : element.getEvidence())
				composeConditionConditionEvidenceComponent("evidence", e);
			for (Condition.ConditionLocationComponent e : element.getLocation())
				composeConditionConditionLocationComponent("location", e);
			for (Condition.ConditionRelatedItemComponent e : element.getRelatedItem())
				composeConditionConditionRelatedItemComponent("relatedItem", e);
			composeString("notes", element.getNotes());
			
			// now lets add the tumor stuff
			for(Cancer.Tumor t : element.getTumors()){
				composeTumor("tumor", t);
			}
			
			
			xml.close(FHIR_NS, name);
		}
	}



	protected void composeResource(Resource resource) throws Exception {
		if (resource instanceof Cancer)
			composeCancer("CancerPhenotype", (Cancer) resource);
		else 
			super.composeResource(resource);
	}*/

}

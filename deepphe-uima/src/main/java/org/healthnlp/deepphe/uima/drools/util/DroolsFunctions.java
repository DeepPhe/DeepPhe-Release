package org.healthnlp.deepphe.uima.drools.util;

import org.drools.core.spi.KnowledgeHelper;
import org.healthnlp.deepphe.fact.*;
import org.healthnlp.deepphe.summary.*;
import org.healthnlp.deepphe.uima.drools.*;
import org.healthnlp.deepphe.util.*;

public class DroolsFunctions {
	
	public static void setTumorType(Domain domain, MedicalRecord record, Fact cancerFact, Fact bodySiteFact, 
			MergedTumor mt, String tType, String ruleName, boolean isPrimary, KnowledgeHelper kh ) {
		Fact bsFact = mt.getTumorSiteFactSet().iterator().next();
		String newId = mt.getTumorSummaryID();
		BodySiteFact tf = (BodySiteFact)FactFactory.createFact(bodySiteFact, FHIRConstants.BODY_SITE, 
					domain.getOntologyURI()+"#"+mt.getBodySite(), FHIRConstants.RECORD_SUMMARY);
		tf.addContainerIdentifier(cancerFact.getSummaryId());

		tf.setSummaryId(newId);
		tf.getProvenanceFacts().addAll(mt.getBodySideFactSet());
		tf.addRulesApplied(ruleName);
		kh.insert(new IdMapper(bodySiteFact.getId(), tf.getId(), bodySiteFact.getSummaryId(), newId));
		kh.insert(tf);
		
		Fact tumorTypeF = FactFactory.createTumorFactModifier(domain.getOntologyURI()+"#"+tType, 
					tf, cancerFact, FHIRConstants.TUMOR_SUMMARY, FHIRConstants.HAS_TUMOR_TYPE, 
					FHIRConstants.RECORD_SUMMARY, FHIRConstants.TUMOR_TYPE);
														
		tumorTypeF.setIdentifier(tumorTypeF.getName()+"-"+newId);
		tumorTypeF.setSummaryId(tf.getSummaryId());
		
		if(!mt.getBodySide().equals("")){
			Fact tumorsideF = FactFactory.createTumorFactModifier(domain.getOntologyURI()+"#"+mt.getBodySide(), tf, 
						cancerFact, FHIRConstants.TUMOR_SUMMARY, FHIRConstants.HAS_BODY_MODIFIER, 
						FHIRConstants.RECORD_SUMMARY, FHIRConstants.LATERALITY);
			tumorsideF.setSummaryId(tf.getSummaryId()); 
			kh.insert (tumorsideF);
		}

		mt.addBodyModifiersToRecordFact(tf, cancerFact, domain.getOntologyURI(), newId);
		
		record.getCancerSummary().getTumorSummaryByIdentifier(newId).addFact(FHIRConstants.HAS_TUMOR_TYPE, tumorTypeF);
		record.getCancerSummary().getTumorSummaryByIdentifier(newId).addFact(FHIRConstants.HAS_BODY_SITE, tf);
		if(isPrimary)
			record.getCancerSummary().addFact(FHIRConstants.HAS_BODY_SITE, tf);

		kh.insert (tumorTypeF);
	
		FactHelper.addFactToSummary(tf, record.getCancerSummary(), newId, domain.getOntologyURI());
		
		mt.setReadyForRetraction(true);
		mt.addRulesApplied(ruleName);
		kh.update (mt);
	}

}

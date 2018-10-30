package org.healthnlp.deepphe.util;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.fact.BodySiteFact;
import org.healthnlp.deepphe.fact.ConditionFact;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.summary.CancerSummary;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.healthnlp.deepphe.util.FHIRConstants.*;


public class FHIRUtils {

	private static final Logger LOGGER = Logger.getLogger( "FhirUtils" );
	public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a z");
	public static final String DEFAULT_LANGUAGE = "English";
	public static final String SCHEMA_UMLS = "NCI Metathesaurus";
	public static final String SCHEMA_RXNORM = "RxNORM";
	public static final String SCHEMA_REFERENCE = "FHIR_ID";
	public static final String DOCUMENT_HEADER_REPORT_TYPE = "Record Type";
	public static final String DOCUMENT_HEADER_PRINCIPAL_DATE = "Principal Date";
	public static final String DOCUMENT_HEADER_PATIENT_NAME = "DphePatient Name";


	public static final String MENTION_URL = "http://hl7.org/fhir/mention";
	public static final String SECTION_URL = "http://hl7.org/fhir/section";
	public static final String DOCUMENT_TITLE_URL = "http://hl7.org/fhir/document_title";
	public static final String DIMENSION_URL = "http://hl7.org/fhir/dimension";
	public static final String STAGE_URL = "http://hl7.org/fhir/stage";
	public static final String TNM_MODIFIER_URL = "http://hl7.org/fhir/TNM_modifier";
	public static final String FHIR_VALUE_PREFIX = "http://hl7.org/fhir/";
	public static final String LANGUAGE_ASPECT_MODALITY_URL = "http://hl7.org/fhir/modality";
	public static final String LANGUAGE_ASPECT_DOC_TIME_REL_URL = "http://hl7.org/fhir/doc_time_rel"; 
	public static final String LANGUAGE_ASPECT_NEGATED_URL = "http://hl7.org/fhir/negation";
	public static final String LANGUAGE_ASPECT_UNCERTAIN_URL = "http://hl7.org/fhir/uncertainty";
	public static final String LANGUAGE_ASPECT_GENERIC_URL = "http://hl7.org/fhir/generic";
	public static final String LANGUAGE_ASPECT_CONDITIONAL_URL = "http://hl7.org/fhir/conditionality";
	public static final String LANGUAGE_ASPECT_INTERMITTENT_URL = "http://hl7.org/fhir/intermittency";
	public static final String LANGUAGE_ASPECT_HYPOTHETICAL_URL = "http://hl7.org/fhir/hypothetical";
	public static final String LANGUAGE_ASPECT_PERMENENT_URL = "http://hl7.org/fhir/permanency";
	public static final String LANGUAGE_ASPECT_HISTORICAL_URL = "http://hl7.org/fhir/historical";
	public static final String LANGUAGE_ASPECT_EXPERIENCER = "http://hl7.org/fhir/experiencer";
	
	public static final String CANCER_URL = "http://ontologies.dbmi.pitt.edu/deepphe/cancer.owl";

	public static final String INTERPRETATION_POSITIVE = "Positive";
	public static final String INTERPRETATION_NEGATIVE = "Negative";

	public static final String ELEMENT = "DpheElement";
	public static final String COMPOSITION = "Composition";
	public static final String PATIENT = "DphePatient";
	public static final String DIAGNOSIS = "DiseaseDisorder";
	public static final String PROCEDURE = "ProcedureIntervention";
	public static final String OBSERVATION = "Observation";
	public static final String FINDING = "Finding";
	public static final String MEDICATION = "Medication_FHIR";
	public static final String ANATOMICAL_SITE = "AnatomicalSite";
	public static final String TUMOR_SIZE = "Tumor_Size";
	public static final String STAGE = "Generic_TNM_Finding";
	public static final String AGE = "Age";
	public static final String GENDER = "Gender";
	public static final String PHENOTYPIC_FACTOR = "PhenotypicFactor";
	public static final String GENOMIC_FACTOR = "GenomicFactor";
	public static final String TREATMENT_FACTOR = "TreatmentFactor";
	public static final String RELATED_FACTOR = "RelatedFactor";
	public static final String EVIDENCE = "Evidence";

	public static final String STAGE_REGEX = "p?(T[X0-4a-z]{1,4})(N[X0-4a-z]{1,4})(M[X0-4a-z]{1,4})";
	
	public static final long MILLISECONDS_IN_YEAR = (long) 1000 * 60 * 60 * 24 * 365;


	public static String getDateAsString(Date date){
		return dateTimeFormat.format(date);
	}

	/**
	 * parse date from string
	 * @param text -
	 * @return -
	 */
	public static Date getDate(String text){
		final DateFormat format = DateFormat.getDateInstance();
		try {
			return format.parse( text );
		} catch ( ParseException pE ) {
			LOGGER.error( "Could not parse date from " + text, pE );
			return new Date();
		}
	}


	public static String getLanguageAspectLabel( String url ) {
		int x = url.lastIndexOf( "/" );
		if ( x > -1 )
			return url.substring( x + 1 );
		return url;
	}


}

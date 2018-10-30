package org.healthnlp.deepphe.uima.drools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.healthnlp.deepphe.util.FHIRConstants;

public class GenericToBreastTNMMapper {
	
	static List <String> tBreast = null;
	static List<String> tTis_SuffixList = new ArrayList<String>(Arrays.asList("DCIS", "LCIS", "Paget"));

	static List <String> nPathologicBreast = null;
	static List <String> nN0_PathSuffixList = new ArrayList<String>(Arrays.asList("i_plus", "i_minus", "mol_plus", "mol_minus"));
	static List <String> nClinicalBreast = null;
	static List <String> mPathologicBreast = null;
	static List <String> mClinicalBreast = null;
	
	
	public static Set<String> getBreastClassification(String prefix, String genericCategory, 
						String genericValue, List<String> suffixList){

		Set<String> toret = null;
		switch (genericCategory) {
			case FHIRConstants.HAS_T_CLASSIFICATION:
				toret = getBreastTClassification(prefix, genericValue, suffixList);
				break;
			case 	FHIRConstants.HAS_N_CLASSIFICATION:
				toret = getBreastNClassification(prefix, genericValue, suffixList);	
				break;
			case 	FHIRConstants.HAS_M_CLASSIFICATION:
				toret = getBreastMClassification(prefix, genericValue, suffixList);	
				break;
		}		
		return toret;
		
	}
	
	
	public static Set<String> getBreastTClassification(String prefix, String genericValue, List<String> suffixList){
		Set<String> breast_TClassifSet = new HashSet<String>();
		String genV = genericValue.replace("hasGeneric", "");
		genV = genV.substring(0,  genV.indexOf("_"));
		if(!hasGenericBreastValue(prefix, "TClassification", genV))
			return breast_TClassifSet;
		
		String pref = "c";
		if(prefix.equals(FHIRConstants.P_MODIFIER))
			pref = "p";
		String suff = "";
		if(suffixList != null && suffixList.size() > 0){
			if(genV.equals("Tis")){	
				for(String suffix : suffixList){
					suff = "";
					if(tTis_SuffixList.contains(suffix)){
						suff = "_"+suffix;
						breast_TClassifSet.add(pref+genV+suff+"_Stage_Finding");
					}
				}	
			} else if(genV.equals("T1") && suffixList.contains("mic"))
				breast_TClassifSet.add(pref+genV+"mic_Stage_Finding");
		}
		else {
			
			breast_TClassifSet.add(pref+genV+"_Stage_Finding");	
		}
			
		return breast_TClassifSet;
	}
	
	public static Set<String>  getBreastNClassification(String prefix, String genericValue, List<String> suffixList){
		Set<String> breast_NClassifSet = new HashSet<String>();
		String genV = genericValue.replace("hasGeneric", "");
		genV = genV.substring(0,  genV.indexOf("_"));
		if(!hasGenericBreastValue(prefix, "NClassification", genV))
			return breast_NClassifSet;
		
		String pref = "c";
		if(prefix.equals(FHIRConstants.P_MODIFIER))
			pref = "p";

		String suff = "";
		if(suffixList != null && suffixList.size() > 0){
			if(pref.equals("p") && genV.equals("N0")){	
				for(String suffix : suffixList){
					suff = "";
					if(nN0_PathSuffixList.contains(suffix)){
						suff = "_"+suffix;
						breast_NClassifSet.add(pref+genV+suff+"_Stage_Finding");
					}
				}
			} else if(genV.equals("N1") && suffixList.contains("mi")){
				breast_NClassifSet.add(pref+genV+"mi_Stage_Finding");
			}
		}
		else
			breast_NClassifSet.add(pref+genV+"_Stage_Finding");
		
	    return breast_NClassifSet;
	}
	
	public static Set<String>  getBreastMClassification(String prefix, String genericValue, List<String> suffixList){
		Set<String> breast_MClassifSet = new HashSet<String>();
		String genV = genericValue.replace("hasGeneric", "");
		genV = genV.substring(0,  genV.indexOf("_"));
		if(!hasGenericBreastValue(prefix, "MClassification", genV))
			return breast_MClassifSet;
		
		String pref = "c";
		if(prefix.equals(FHIRConstants.P_MODIFIER))
			pref = "p";

		String suff = "";
		if(suffixList != null && suffixList.size() > 0 && pref.equals("c") && genV.equals("M0") && suffixList.contains("i_plus")){
				breast_MClassifSet.add(pref+genV+"_i_plus_Stage_Finding");
		}
		else
			breast_MClassifSet.add(pref+genV+"_Stage_Finding");
		
	    return breast_MClassifSet;
	}
	
	public static boolean hasGenericBreastValue(String prefix, String category, String genericValue){
		if(!prefix.equals("p_modifier") && !prefix.equals("c_modifier"))
			return false;
		
		String prefType = prefix+"_"+category;
		List<String> lookInList = null;
		
		switch (prefType) {
			case "p_modifier_TClassification":
				lookInList = get_T_List();
				break;
			case "c_modifier_TClassification":
				lookInList = get_T_List();
				break;
			case "p_modifier_NClassification":
				lookInList = getPathologic_N_List();
				break;
			case "c_modifier_NClassification":
				lookInList = getClinical_N_List();
				break;
			case "p_modifier_MClassification":
				lookInList = getPathologic_M_List();
				break;
			case "c_modifier_MClassification":
				lookInList = getClinical_M_List();
				break;
			default:
				break;
		}
		
		if(lookInList != null)
			return lookInList.contains(genericValue);
		else
			return false;
			
	}
	
	public static List<String> get_T_List(){
		if(tBreast != null) return tBreast;
		tBreast = new ArrayList<String>();	
		String[] tBreasArr = new String[] {"T0", "T1", "T1a", "T1b", "T1c", "T1mic","T2", "T3", "T4","T4a", "T4b", "T4c","T4d", "Tis", "TX"};
		Collections.addAll(tBreast, tBreasArr); 
		return tBreast;
	}
	
	public static List<String> getClinical_N_List(){
		if(nClinicalBreast != null) return nClinicalBreast;
		nClinicalBreast = new ArrayList<String>();
		String[] tBreasArr = new String[] {"N0", "N1", "N1mic","N2", "N2a", "N2b","N3", "N3a", "N3b", "N3c","NX"};
		Collections.addAll(nClinicalBreast, tBreasArr); 
		return nClinicalBreast;
	}
	
	public static List<String> getPathologic_N_List(){
		if(nPathologicBreast != null) return nPathologicBreast;
		nPathologicBreast = new ArrayList<String>();
		String[] tBreasArr = new String[] {"N0", "N1", "N1a", "N1b", "N1c", "N1mic","N2", "N2a", "N2b","N3", "N3a", "N3b", "N3c","NX"};
		Collections.addAll(nPathologicBreast, tBreasArr); 
		return nPathologicBreast;
	}
	
	public static List<String> getPathologic_M_List(){
		if(mPathologicBreast != null) return mPathologicBreast;
		mPathologicBreast = new ArrayList<String>();
		String[] tBreasArr = new String[] {"M1", "MX"};
		Collections.addAll(mPathologicBreast, tBreasArr); 
		return mPathologicBreast;
	}
	
	public static List<String> getClinical_M_List(){
		if(mClinicalBreast != null) return mClinicalBreast;
		mClinicalBreast = new ArrayList<String>();
		String[] tBreasArr = new String[] {"M0", "M1"};
		Collections.addAll(mClinicalBreast, tBreasArr); 
		return mClinicalBreast;
	}
	
	public static void main(String[] args){
		// T
		String prefix = FHIRConstants.C_MODIFIER;
		String genericValue="T1_Stage_Finding";
		List<String> suffixList = new ArrayList<String>(Arrays.asList("mic"));
		System.out.println("T: "+getBreastTClassification(prefix, genericValue, suffixList));
		
		// N
		String prefixN = FHIRConstants.P_MODIFIER;
		String genericValueN="N0_Stage_Finding";
		List<String> suffixListN = new ArrayList<String>(Arrays.asList("i_plus"));
		System.out.println("N: "+getBreastNClassification(prefixN, genericValueN, suffixListN));
		
		// M
		String prefixM = FHIRConstants.C_MODIFIER;
		String genericValueM="M0_Stage_Finding";
		List<String> suffixListM = new ArrayList<String>(Arrays.asList("i_plus"));
		System.out.println("M: "+getBreastMClassification(prefixM, genericValueM, suffixListM));
	}

}

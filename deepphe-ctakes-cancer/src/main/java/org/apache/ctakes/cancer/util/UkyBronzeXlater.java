package org.apache.ctakes.cancer.util;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/1/2017
 */
public class UkyBronzeXlater {

   static private final Logger LOGGER = Logger.getLogger( "UkyBronzeXlater" );

   static private final String ICD_O_3_MAP_IN = "C:\\Spiffy\\prj_darth_phenome\\data\\icd_o_3\\sitetype_icdo3_d20150918.csv";

   static private final String UKY_BRONZE_IN = "C:\\Spiffy\\prj_darth_phenome\\data\\uky\\pilot\\KentuckyPilotDataNov2017\\DeepPhe.csv";
   static private final String UKY_BRCA_CANCER_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\uky\\pilot\\DeepPhe_BrCa_Gold_Cancer.csv";
   static private final String UKY_BRCA_TUMOR_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\uky\\pilot\\DeepPhe_BrCa_Gold_Tumor.csv";
   static private final String UKY_SKIN_CANCER_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\uky\\pilot\\DeepPhe_Skin_Gold_Cancer.csv";
   static private final String UKY_SKIN_TUMOR_OUT = "C:\\Spiffy\\prj_darth_phenome\\data\\uky\\pilot\\DeepPhe_Skin_Gold_Tumor.csv";

   static private final int CANCER_COLUMN_COUNT = 15;
   static private final int TUMOR_COLUMN_COUNT = 25;

   static private Collection<String> getSiteCodes( final String text ) {
      // Create collection from things like "C690-C691  C693  C695-C698"
      final String[] splits = text.split( "\\s+" );
      if ( splits.length == 0 ) {
         return Collections.emptyList();
      }
      return Arrays.stream( splits )
            .map( UkyBronzeXlater::getNumberedCodes )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }

   static private Collection<String> getNumberedCodes( final String text ) {
      final String[] splits = StringUtil.fastSplit( text, '-' );
      if ( splits.length == 1 ) {
         return Collections.singletonList( text );
      }
      final Collection<String> codes = new ArrayList<>();
      final int start = getCint( splits[ 0 ] );
      final int stop = getCint( splits[ 1 ] );
      for ( int i=start; i<=stop; i++ ) {
         codes.add( String.format( "C%03d", i ) );
      }
      return codes;
   }

   static private int getCint( final String text ) {
      String numberText = text;
      if ( text.startsWith( "C" ) ) {
         numberText = text.substring( 1 );
      }
      try {
         return Integer.parseInt( numberText );
      } catch ( NumberFormatException nfE ) {
         LOGGER.severe( "Could not parse number from " + text + "\n" + nfE.getMessage() );
      }
      return -1;
   }

   static private String getCancerCode( final String text ) {
      final String[] splits = StringUtil.fastSplit( text, '/' );
      return splits[ 0 ];
   }

   static private String getSiteText( final String text ) {
      final String lowerCase = text.replaceAll( " NOS", "" ).trim().toLowerCase();
      final StringBuilder sb = new StringBuilder();
      for ( int i=0; i<lowerCase.length(); i++ ) {
         if ( i==0 || lowerCase.charAt( i-1 ) == ' ' ) {
            sb.append( Character.toUpperCase( lowerCase.charAt( i ) ) );
         } else {
            sb.append( lowerCase.charAt( i ) );
         }
      }
      String spaceLess = sb.toString().replaceAll( " ", "" );
      return spaceLess;
   }

   static private String createTnm( final String code, final String tnm ) {
      if ( code.isEmpty() ) {
         return "";
      }
      return tnm + code.substring( 1 );
   }

   static private String createStage( final String code ) {
      if ( code.isEmpty() ) {
         return "";
      }
      return "Stage " + code.replaceAll( "1", "I" )
            .replaceAll( "2", "II" )
            .replaceAll( "3", "III" )
            .replaceAll( "4", "IV" );
   }

   static private String getStage( final String pStage, final String cStage ) {

      if ( pStage.equals( "88" ) || pStage.equals( "99" ) ) {
         if ( cStage.equals( "88" ) || cStage.equals( "99" ) ) {
            return "";
         }
         return createStage( cStage );
      }
      return createStage( pStage );
   }

   public static void main( final String... args ) {
      // FROM UKY BRONZE
//            0            1           2               3                    4             5              6                 7                    8                 9           10                   11             12                13                      14                         15                         16                   17                   18                         19                   20             21    22       23    24    25             26                         27                   28                      29                   30               31        32             33                34          35                   36                         37                   38             39                40                      41                      42                            43                44             45             46                      47                            48                   49                50                      51                         52                53                54                55                      56                   57                      58                   59                   60                      61                      62                         63                      64                      65                         66                67                68                   69                   70                71          72                   73                   74                   75                      76            77        78                   79                   80                   81                         82                         83                      84                   85                        86                    87                   88                   89                         90                   91                92                      93                94                95                   96                97          98                99                  100                  101               102               103                  104                     105               106                  107                  108            109               110            111               112                     113                  114                        115                     116            117            118               119            120            121         122               123               124                        125                  126                        127             128              129               130               131            132                  133               134               135               136            137                     138               139                  140                           141                  142                        143                     144                     145                  146                        147                     148               149                  150            151         152                  153                     154                     155                        156                        157            158             159                 160                     161                  162                        163                164              165            166               167                     168            169      170         171            172               173               174         175         176      177               178                  179            180         181                  182                     183             184              185            186               187                  188          189              190               191               192               193         194               195               196                  197                  198                  199                     200                           201                        202                  203                        204                        205                     206                           207                  208                           209                        210                  211                           212                      213                       214                     215                           216                     217                     218                        219                        220                     221                        222                        223                  224                  225                     226                  227                  228                     229               230                  231                  232                  233                  234                  235               236                     237                  238                  239                     240                     241               242                     243                  244                  245                  246                  247                        248                  249                  250                     251                  252                     253               254                        255                     256               257               258                     259                  260            261                  262                  263               264                  265                     266                  267                     268                  269                        270                        271                     272                        273                        274                     275                  276                     277                     278                     279                        280                  281                        282                     283                     284                     285                  286                     287                     288                  289                  290                     291                  292                  293                     294                  295                  296             297                 298                  299                  300                     301                  302               303               304                        305                  306                     307                  308                  309               310                     311               312                  313             314              315               316               317            318            319            320               321                  322               323                     324                        325                        326                  327                        328                     329                   330                     331                        332                  333                     334                  335               336               337                  338                     339               340                        341                  342                  343                     344               345               346                  347                  348                  349            350            351         352               353                        354               355            356                     357                  358                  359                  360                        361               362                     363                     364                  365                        366                     367                  368              369             370                           371                     372                       373                   374                     375                  376                        377                     378                        379                     380                     381                     382                        383                        384                  385                     386                     387                     388                     389                        390                  391                           392                     393                     394                     395                     396                  397                        398                     399                     400                        401                  402                     403                  404            405                  406                  407                  408                        409                  410                        411                     412                        413                     414                  415                  416                     417                  418                  419                     420                     421                     422                     423                        424                  425                     426                  427                     428                  429                  430                  431            432                           433                  434                     435               436               437                  438                  439                  440               441                     442                     443                     444                        445                     446                     447                     448               449               450            451            452            453            454            455            456         457               458            459               460            461            462               463            464            465            466               467            468            469               470                  471                  472         473            474                  475               476                  477               478                     479               480            481               482                  483                  484                     485               486                           487               488               489         490         491                  492                  493                     494                     495                        496                        497                     498          499          500
//      Record_Type,Registry_Type,Reserved_00,NAACCR_Record_Version,NPI_Registry_ID,Registry_ID,Tumor_Record_Number,Patient_ID_Number,Patient_System_ID_Hosp,Reserved_01,Addr_at_DX_City,Addr_at_DX_State,Addr_at_DX_Postal_Code,County_at_DX,Census_Tract_1970_80_90,Census_Block_Grp_1970_90,Census_Cod_Sys_1970_80_90,Census_Tr_Cert_1970_80_90,Census_Tract_2000,Census_Block_Group_2000,Census_Tr_Certainty_2000,Marital_Status_at_DX,Race_1,Race_2,Race_3,Race_4,Race_5,Race_Coding_Sys_Current,Race_Coding_Sys_Original,Spanish_Hispanic_Origin,Computed_Ethnicity,Computed_Ethnicity_Source,Sex,Age_at_Diagnosis,Date_of_Birth,Date_of_Birth_Flag,Birthplace,Census_Occ_Code_1970_2000,Census_Ind_Code_1970_2000,Occupation_Source,Industry_Source,Text_Usual_Occupation,Text_Usual_Industry,Census_Occ_Ind_Sys_70_00,NHIA_Derived_Hisp_Origin,Race_NAPIIA_derived_API,IHS_Link,GIS_Coordinate_Quality,RuralUrban_Continuum_1993,RuralUrban_Continuum_2003,Census_Tract_2010,Census_Block_Group_2010,Census_Tr_Certainty_2010,Addr_at_DX_Country,Addr_Current_Country,Birthplace_State,Birthplace_Country,FollowUp_Contact_Country,Place_of_Death_State,Place_of_Death_Country,Census_Ind_Code_2010,Census_Occ_Code_2010,Census_Tr_Poverty_Indictr,County_at_DX_Geocode1990,County_at_DX_Geocode2000,County_at_DX_Geocode2010,County_at_DX_Geocode2020,RuralUrban_Continuum_2013,Reserved_02,Sequence_Number_Central,Date_of_Diagnosis,Date_of_Diagnosis_Flag,Primary_Site,Laterality,Histology_92_00_ICD_O_2,Behavior_92_00_ICD_O_2,Histologic_Type_ICD_O_3,Behavior_Code_ICD_O_3,Grade,Grade_Path_Value,Grade_Path_System,Site_Coding_Sys_Current,Site_Coding_Sys_Original,Morph_Coding_Sys_Current,Morph_Coding_Sys_Originl,Diagnostic_Confirmation,Type_of_Reporting_Source,Casefinding_Source,Ambiguous_Terminology_DX,Date_Conclusive_DX,Date_Conclusive_DX_Flag,Mult_Tum_Rpt_as_One_Prim,Date_of_Mult_Tumors,Date_of_Mult_Tumors_Flag,Multiplicity_Counter,Reserved_03,NPI_Reporting_Facility,Reporting_Facility,NPI_Archive_FIN,Archive_FIN,Accession_Number_Hosp,Sequence_Number_Hospital,Abstracted_By,Date_of_1st_Contact,Date_of_1st_Contact_Flag,Date_of_Inpt_Adm,Date_of_Inpt_Adm_Flag,Date_of_Inpt_Disch,Date_of_Inpt_Disch_Flag,Inpatient_Status,Class_of_Case,Primary_Payer_at_DX,Reserved_15,RX_Hosp_Surg_App_2010,RX_Hosp_Surg_Prim_Site,RX_Hosp_Scope_Reg_LN_Sur,RX_Hosp_Surg_Oth_Reg_Dis,RX_Hosp_Reg_LN_Removed,Reserved_16,RX_Hosp_Radiation,RX_Hosp_Chemo,RX_Hosp_Hormone,RX_Hosp_BRM,RX_Hosp_Other,RX_Hosp_DX_Stg_Proc,RX_Hosp_Palliative_Proc,RX_Hosp_Surg_Site_98_02,RX_Hosp_Scope_Reg_98_02,RX_Hosp_Surg_Oth_98_02,Reserved_04,TNM_Path_Staged_By,TNM_Clin_Staged_By,Mets_at_DX_Bone,Mets_at_DX_Brain,Mets_at_Dx_Distant_LN,Mets_at_DX_Liver,Mets_at_DX_Lung,Mets_at_DX_Other,Tumor_Size_Clinical,Tumor_Size_Pathologic,Tumor_Size_Summary,Derived_SEER_Path_Stg_Grp,Derived_SEER_Clin_Stg_Grp,Derived_SEER_Cmb_Stg_Grp,Derived_SEER_Combined_T,Derived_SEER_Combined_N,Derived_SEER_Combined_M,Derived_SEER_Cmb_T_Src,Derived_SEER_Cmb_N_Src,Derived_SEER_Cmb_M_Src,SEER_Primary_Tumor,SEER_Regional_Nodes,SEER_Mets,Derived_SS2017,Directly_Assigned_SS2017,NPCR_Derived_Clin_Stg_Grp,NPCR_Derived_Path_Stg_Grp,SEER_Summary_Stage_2000,SEER_Summary_Stage_1977,EOD_Tumor_Size,EOD_Extension,EOD_Extension_Prost_Path,EOD_Lymph_Node_Involv,Regional_Nodes_Positive,Regional_Nodes_Examined,EOD_Old_13_Digit,EOD_Old_2_Digit,EOD_Old_4_Digit,Coding_System_for_EOD,TNM_Edition_Number,TNM_Path_T,TNM_Path_N,TNM_Path_M,TNM_Path_Stage_Group,TNM_Path_Descriptor,Reserved_19,TNM_Clin_T,TNM_Clin_N,TNM_Clin_M,TNM_Clin_Stage_Group,TNM_Clin_Descriptor,Reserved_20,Pediatric_Stage,Pediatric_Staging_System,Pediatric_Staged_By,Tumor_Marker_1,Tumor_Marker_2,Tumor_Marker_3,Lymph_vascular_Invasion,CS_Tumor_Size,CS_Extension,CS_Tumor_Size_Ext_Eval,CS_Lymph_Nodes,CS_Lymph_Nodes_Eval,CS_Mets_at_DX,CS_Mets_Eval,CS_Mets_at_Dx_Bone,CS_Mets_at_Dx_Brain,CS_Mets_at_Dx_Liver,CS_Mets_at_Dx_Lung,CS_Site_Specific_Factor_1,CS_Site_Specific_Factor_2,CS_Site_Specific_Factor_3,CS_Site_Specific_Factor_4,CS_Site_Specific_Factor_5,CS_Site_Specific_Factor_6,CS_Site_Specific_Factor_7,CS_Site_Specific_Factor_8,CS_Site_Specific_Factor_9,CS_Site_Specific_Factor10,CS_Site_Specific_Factor11,CS_Site_Specific_Factor12,CS_Site_Specific_Factor13,CS_Site_Specific_Factor14,CS_Site_Specific_Factor15,CS_Site_Specific_Factor16,CS_Site_Specific_Factor17,CS_Site_Specific_Factor18,CS_Site_Specific_Factor19,CS_Site_Specific_Factor20,CS_Site_Specific_Factor21,CS_Site_Specific_Factor22,CS_Site_Specific_Factor23,CS_Site_Specific_Factor24,CS_Site_Specific_Factor25,CS_PreRx_Tumor_Size,CS_PreRx_Extension,CS_PreRx_Tum_Sz_Ext_Eval,CS_PreRx_Lymph_Nodes,CS_PreRx_Reg_Nodes_Eval,CS_PreRx_Mets_at_DX,CS_PreRx_Mets_Eval,CS_PostRx_Tumor_Size,CS_PostRx_Extension,CS_PostRx_Lymph_Nodes,CS_PostRx_Mets_at_DX,Derived_AJCC_6_T,Derived_AJCC_6_T_Descript,Derived_AJCC_6_N,Derived_AJCC_6_N_Descript,Derived_AJCC_6_M,Derived_AJCC_6_M_Descript,Derived_AJCC_6_Stage_Grp,Derived_AJCC_7_T,Derived_AJCC_7_T_Descript,Derived_AJCC_7_N,Derived_AJCC_7_N_Descript,Derived_AJCC_7_M,Derived_AJCC_7_M_Descript,Derived_AJCC_7_Stage_Grp,Derived_PreRx_7_T,Derived_PreRx_7_T_Descrip,Derived_PreRx_7_N,Derived_PreRx_7_N_Descrip,Derived_PreRx_7_M,Derived_PreRx_7_M_Descrip,Derived_PreRx_7_Stage_Grp,Derived_PostRx_7_T,Derived_PostRx_7_N,Derived_PostRx_7_M,Derived_PostRx_7_Stge_Grp,Derived_SS1977,Derived_SS2000,Derived_Neoadjuv_Rx_Flag,Derived_AJCC_Flag,Derived_SS1977_Flag,Derived_SS2000_Flag,CS_Version_Input_Current,CS_Version_Input_Original,CS_Version_Derived,SEER_Site_Specific_Fact_1,SEER_Site_Specific_Fact_2,SEER_Site_Specific_Fact_3,SEER_Site_Specific_Fact_4,SEER_Site_Specific_Fact_5,SEER_Site_Specific_Fact_6,ICD_Revision_Comorbid,Comorbid_Complication_1,Comorbid_Complication_2,Comorbid_Complication_3,Comorbid_Complication_4,Comorbid_Complication_5,Comorbid_Complication_6,Comorbid_Complication_7,Comorbid_Complication_8,Comorbid_Complication_9,Comorbid_Complication_10,Secondary_Diagnosis_1,Secondary_Diagnosis_2,Secondary_Diagnosis_3,Secondary_Diagnosis_4,Secondary_Diagnosis_5,Secondary_Diagnosis_6,Secondary_Diagnosis_7,Secondary_Diagnosis_8,Secondary_Diagnosis_9,Secondary_Diagnosis_10,NPCR_Specific_Field,Reserved_05,Date_Initial_RX_SEER,Date_Initial_RX_SEER_Flag,Date_1st_Crs_RX_CoC,Date_1st_Crs_RX_CoC_Flag,RX_Date_Surgery,RX_Date_Surgery_Flag,RX_Date_Mst_Defn_Srg,RX_Date_Mst_Defn_Srg_Flag,RX_Date_Surg_Disch,RX_Date_Surg_Disch_Flag,RX_Date_Radiation,RX_Date_Radiation_Flag,RX_Date_Rad_Ended,RX_Date_Rad_Ended_Flag,RX_Date_Systemic,RX_Date_Systemic_Flag,RX_Date_Chemo,RX_Date_Chemo_Flag,RX_Date_Hormone,RX_Date_Hormone_Flag,RX_Date_BRM,RX_Date_BRM_Flag,RX_Date_Other,RX_Date_Other_Flag,RX_Date_DX_Stg_Proc,RX_Date_Dx_Stg_Proc_Flag,RX_Summ_Treatment_Status,RX_Summ_Surg_Prim_Site,RX_Summ_Scope_Reg_LN_Sur,RX_Summ_Surg_Oth_Reg_Dis,RX_Summ_Reg_LN_Examined,RX_Summ_Surgical_Approch,RX_Summ_Surgical_Margins,RX_Summ_Reconstruct_1st,Reason_for_No_Surgery,RX_Summ_DX_Stg_Proc,RX_Summ_Palliative_Proc,RX_Summ_Radiation,RX_Summ_Rad_to_CNS,RX_Summ_Surg_Rad_Seq,RX_Summ_Transplnt_Endocr,RX_Summ_Chemo,RX_Summ_Hormone,RX_Summ_BRM,RX_Summ_Other,Reason_for_No_Radiation,RX_Coding_System_Current,Reserved_18,Rad_Regional_Dose_cGy,Rad_No_of_Treatment_Vol,Rad_Treatment_Volume,Rad_Location_of_RX,Rad_Regional_RX_Modality,Rad_Boost_RX_Modality,Rad_Boost_Dose_cGy,RX_Summ_Systemic_Sur_Seq,RX_Summ_Surgery_Type,Readm_Same_Hosp_30_Days,RX_Summ_Surg_Site_98_02,RX_Summ_Scope_Reg_98_02,RX_Summ_Surg_Oth_98_02,Reserved_06,Subsq_RX_2nd_Course_Date,Subsq_RX_2ndCrs_Date_Flag,Subsq_RX_2nd_Course_Surg,Subsq_RX_2nd_Scope_LN_SU,Subsq_RX_2nd_Surg_Oth,Subsq_RX_2nd_Reg_LN_Rem,Subsq_RX_2nd_Course_Rad,Subsq_RX_2nd_Course_Chemo,Subsq_RX_2nd_Course_Horm,Subsq_RX_2nd_Course_BRM,Subsq_RX_2nd_Course_Oth,Subsq_RX_3rd_Course_Date,Subsq_RX_3rdCrs_Date_Flag,Subsq_RX_3rd_Course_Surg,Subsq_RX_3rd_Scope_LN_Su,Subsq_RX_3rd_Surg_Oth,Subsq_RX_3rd_Reg_LN_Rem,Subsq_RX_3rd_Course_Rad,Subsq_RX_3rd_Course_Chemo,Subsq_RX_3rd_Course_Horm,Subsq_RX_3rd_Course_BRM,Subsq_RX_3rd_Course_Oth,Subsq_RX_4th_Course_Date,Subsq_RX_4thCrs_Date_Flag,Subsq_RX_4th_Course_Surg,Subsq_RX_4th_Scope_LN_Su,Subsq_RX_4th_Surg_Oth,Subsq_RX_4th_Reg_LN_Rem,Subsq_RX_4th_Course_Rad,Subsq_RX_4th_Course_Chemo,Subsq_RX_4th_Course_Horm,Subsq_RX_4th_Course_BRM,Subsq_RX_4th_Course_Oth,Subsq_RX_Reconstruct_Del,Reserved_07,Over_ride_SS_NodesPos,Over_ride_SS_TNM_N,Over_ride_SS_TNM_M,Over_ride_Acsn_Class_Seq,Over_ride_HospSeq_DxConf,Over_ride_CoC_Site_Type,Over_ride_HospSeq_Site,Over_ride_Site_TNM_StgGrp,Over_ride_Age_Site_Morph,Over_ride_SeqNo_DxConf,Over_ride_Site_Lat_SeqNo,Over_ride_Surg_DxConf,Over_ride_Site_Type,Over_ride_Histology,Over_ride_Report_Source,Over_ride_Ill_define_Site,Over_ride_Leuk_Lymphoma,Over_ride_Site_Behavior,Over_ride_Site_EOD_DX_Dt,Over_ride_Site_Lat_EOD,Over_ride_Site_Lat_Morph,Site_73_91_ICD_O_1,Histology_73_91_ICD_O_1,Behavior_73_91_ICD_O_1,Grade_73_91_ICD_O_1,ICD_O_2_Conversion_Flag,CRC_CHECKSUM,SEER_Coding_Sys_Current,SEER_Coding_Sys_Original,CoC_Coding_Sys_Current,CoC_Coding_Sys_Original,Vendor_Name,SEER_Type_of_Follow_Up,SEER_Record_Number,Diagnostic_Proc_73_87,Date_Case_Initiated,Date_Case_Completed,Date_Case_Completed_CoC,Date_Case_Last_Changed,Date_Case_Report_Exported,Date_Case_Report_Received,Date_Case_Report_Loaded,Date_Tumor_Record_Availbl,ICD_O_3_Conversion_Flag,Over_ride_CS_1,Over_ride_CS_2,Over_ride_CS_3,Over_ride_CS_4,Over_ride_CS_5,Over_ride_CS_6,Over_ride_CS_7,Over_ride_CS_8,Over_ride_CS_9,Over_ride_CS_10,Over_ride_CS_11,Over_ride_CS_12,Over_ride_CS_13,Over_ride_CS_14,Over_ride_CS_15,Over_ride_CS_16,Over_ride_CS_17,Over_ride_CS_18,Over_ride_CS_19,Over_ride_CS_20,Reserved_08,Date_of_Last_Contact,Date_of_Last_Contact_Flag,Vital_Status,Cancer_Status,Quality_of_Survival,Follow_Up_Source,Next_Follow_Up_Source,Addr_Current_City,Addr_Current_State,Addr_Current_Postal_Code,County_Current,Reserved_17,Recurrence_Date_1st,Recurrence_Date_1st_Flag,Recurrence_Type_1st,Follow_Up_Contact_City,Follow_Up_Contact_State,Follow_Up_Contact_Postal,Cause_of_Death,ICD_Revision_Number,Autopsy,Place_of_Death,Follow_up_Source_Central,Date_of_Death_Canada,Date_of_Death_CanadaFlag,Unusual_Follow_Up_Method,Surv_Date_Active_Followup,Surv_Flag_Active_Followup,Surv_Mos_Active_Followup,Surv_Date_Presumed_Alive,Surv_Flag_Presumed_Alive,Surv_Mos_Presumed_Alive,Surv_Date_DX_Recode,Reserved_09,NPI_Physician_Primary_Surg,NPI_Physician_3,NPI_Physician_4,State_Requestor_Items,epath_files,patient_id

      // TO OUR -GOLD- CANCER ANNOTATIONS
//                0           1            2                3                   4             5                    6                         7                       8                      9                10                    11                         12                             13                    14              15
//            *cancer ID|*patient ID|body location|body location laterality|Temporality|clinical stage|clinical T classification|clinical N classification|clinical M classification|-clinical prefix|-clinical suffix|pathologic T classification|pathologic N classification|pathologic M classification|-pathologic prefix|-pathologic suffix

      // TO OUR -GOLD- TUMOR ANNOTATIONS
//                         0                     1            2             3                  4                            5                              6               7          8          9             10               11               12                       13                      14                   15                         16                  17                    18                             19                         20                           21                             22                                23                               24                       25
//      -geographically determined (yes/no)|*patient ID|*cancer  link|*body location|*body location laterality|body location clockface position|body location quadrant|Diagnosis|tumor type|-cancer type|-histologic type|-tumor extent|er status interpretation|-er status numeric value|-er status method|pr status interpretation|-pr status numeric value|-pr status method|her2neu status interpretation|-her2neu status numeric value|-her2neu status method|-radiologic tumor size (mm)|-radiologic tumor size procedure method|-pathologic tumor size (mm)|-pathologic aggregate tumor size (mm)|calcifications

      // Use seer icd-o-3 sitetype map in data/seer/icd_o_3    Split column 4 by '/' character.  [0] = UKY histology (76).  [1] = UKY behavior (77).  Column 5 is disease/diagnosis text
//      UKY site 72  is icd-o-3 code: C445 C446 C447 C502, etc.
//      UKY laterality 73  is numeric: (see below)
//      UKY histology 76  is icd-o-3: 8720 8743 8523, etc.
//      UKY pathologic TNM (170-172) does not have type.  e.g. "pX" instead of "pTX"  same with clinical (176-178)
//      UKY TNM derived Stage (173 (p), 179 (c)) does not state "Stage".  e.g. "2C"  Also may be '88' or '99' for N/A

      // Laterality
//      0	Not a paired site
//      1	Right: origin of primary
//      2	Left: origin of primary
//      3	Only one side involved, right or left origin unspecified
//      4	Bilateral involvement at time of diagnosis, lateral origin unknown for a single primary; or both ovaries involved simultaneously, single histology; bilateral retinoblastomas; bilateral Wilms' tumors
//      5	Paired site: midline tumor
//      9	Paired site, but no information concerning laterality

      final Map<String,String> laterality = new HashMap<>( 2 );
      laterality.put( "1", "Right" );
      laterality.put( "2", "Left" );
      laterality.put( "4", "Bilateral" );
      final Map<String,String> siteCodes = new HashMap<>();
      final Map<String,String> cancerCodes = new HashMap<>();

      try ( final BufferedReader reader
                  = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( ICD_O_3_MAP_IN ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] columns = StringUtil.fastSplit( line, ',' );
            if ( columns.length > 0 ) {
               final String siteText = getSiteText( columns[ 1 ] );
               getSiteCodes( columns[ 0 ] ).forEach( c -> siteCodes.put( c, siteText ) );
               cancerCodes.put( getCancerCode( columns[ 4 ] ), columns[ 5 ].replaceAll( " NOS", "" ).trim() );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.severe( ioE.getMessage() );
         System.exit( 1 );
      }

      boolean firstLine = true;
      try ( final BufferedReader reader
                  = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( UKY_BRONZE_IN ) ) );
            final BufferedWriter brcaCancerWriter
                  = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( UKY_BRCA_CANCER_OUT ) ) );
            final BufferedWriter brcaTumorWriter
                  = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( UKY_BRCA_TUMOR_OUT ) ) );
            final BufferedWriter skinCancerWriter
               = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( UKY_SKIN_CANCER_OUT ) ) );
            final BufferedWriter skinTumorWriter
               = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( UKY_SKIN_TUMOR_OUT ) ) ) ) {
         brcaCancerWriter.write( "*cancer ID,*patient ID,body location,body location laterality,Temporality,clinical stage,clinical T classification,clinical N classification,clinical M classification,-clinical prefix,-clinical suffix,pathologic T classification,pathologic N classification,pathologic M classification,-pathologic prefix,-pathologic suffix\n" );
         brcaTumorWriter.write( "-geographically determined (yes/no),*patient ID,*cancer  link,*body location,*body location laterality,body location clockface position,body location quadrant,Diagnosis,tumor type,-cancer type,-histologic type,-tumor extent,er status interpretation,-er status numeric value,-er status method,pr status interpretation,-pr status numeric value,-pr status method,her2neu status interpretation,-her2neu status numeric value,-her2neu status method,-radiologic tumor size (mm),-radiologic tumor size procedure method,-pathologic tumor size (mm),-pathologic aggregate tumor size (mm),calcifications\n" );
         skinCancerWriter.write( "*cancer ID,*patient ID,body location,body location laterality,Temporality,clinical stage,clinical T classification,clinical N classification,clinical M classification,-clinical prefix,-clinical suffix,pathologic T classification,pathologic N classification,pathologic M classification,-pathologic prefix,-pathologic suffix\n" );
         skinTumorWriter.write( "-geographically determined (yes/no),*patient ID,*cancer  link,*body location,*body location laterality,body location clockface position,body location quadrant,Diagnosis,tumor type,-cancer type,-histologic type,-tumor extent,er status interpretation,-er status numeric value,-er status method,pr status interpretation,-pr status numeric value,-pr status method,her2neu status interpretation,-her2neu status numeric value,-her2neu status method,-radiologic tumor size (mm),-radiologic tumor size procedure method,-pathologic tumor size (mm),-pathologic aggregate tumor size (mm),calcifications\n" );
         String line = reader.readLine();
         final StringBuilder sb = new StringBuilder();
         while ( line != null ) {
            if ( firstLine || line.trim().isEmpty() ) {
               firstLine = false;
               line = reader.readLine();
               continue;
            }
            final String[] bronzeColumns = StringUtil.fastSplit( line, ',' );
            final String location = siteCodes.get( bronzeColumns[ 72 ] );
               // Cancer
               sb.append( getCancerId2( bronzeColumns[ 500 ], laterality.getOrDefault( bronzeColumns[ 73 ], "" ), location ) );
               sb.append( ",patient" );
               sb.append( bronzeColumns[ 500 ] );
               sb.append( ',' );
               sb.append( location );
               sb.append( ',' );
               sb.append( laterality.getOrDefault( bronzeColumns[ 73 ], "" ) );
               sb.append( ",Current" );
               sb.append( "," + getStage( bronzeColumns[ 173 ], bronzeColumns[ 179 ] ) );
               sb.append( "," + createTnm( bronzeColumns[ 176 ], "T" ) );
            sb.append( "," + createTnm( bronzeColumns[ 177 ], "N" ) );
            sb.append( "," + createTnm( bronzeColumns[ 178 ], "M" ) );
            sb.append( "," + createTnm( bronzeColumns[ 170 ], "T" ) );
            sb.append( "," + createTnm( bronzeColumns[ 171 ], "N" ) );
            sb.append( "," + createTnm( bronzeColumns[ 172 ], "M" ) );
               sb.append( '\n' );
               if ( location.equals( "Breast" ) ) {
                  brcaCancerWriter.write( sb.toString() );
               } else {
                  skinCancerWriter.write( sb.toString() );
               }
            sb.setLength( 0 );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.severe( ioE.getMessage() );
      }
   }

   static private String getCancerId2( final String patientId, final String laterality, final String location ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "cancer_patient" );
      sb.append( patientId );
      sb.append( '_' );
      sb.append( laterality );
      sb.append( '_' );
      sb.append( location );
      sb.append( "_Current" );
      return sb.toString();
   }


   static private String getLaterality( final String fullLaterality ) {
      if ( fullLaterality.equals( "Left" ) || fullLaterality.equals( "Right" ) ) {
         return fullLaterality;
      }
      return "";
   }

   static private String getDiagnosis( final String fullDiagnosis ) {
      String postOf = getPostOf( fullDiagnosis );
      String preSemi = getPreSemiColon( postOf );
      String preParenth = getPreParenthesis( preSemi );
      return preParenth;
   }

   static private String getQuadrant( final String fullLocation ) {
      int index = fullLocation.indexOf( "quadrant" );
      if ( index < 0 ) {
         return "";
      }
      return fullLocation.substring( 0, index + 8 ).trim();
   }

   // Distant, Regional; bla Nodes only, Localized, [empty]
   static private String getTumorType( final String lsuTumor ) {
      if ( lsuTumor.equals( "Localized" ) ) {
         return "PrimaryTumor";
      } else if ( lsuTumor.startsWith( "Regional" ) ) {
         return "Regional_Metastasis";
      } else if ( lsuTumor.equals( "Distant" ) ) {
         return "Metastasis";
      }
      return "";
   }

   static private String getDuctLob( final String diagnosis ) {
      if ( diagnosis.toLowerCase().contains( "duct" ) ) {
         return "Ductal";
      } else if ( diagnosis.toLowerCase().startsWith( "lobular" ) ) {
         return "Lobular";
      }
      return "";
   }

   static private String getInvasion( final String diagnosis ) {
      if ( diagnosis.toLowerCase().contains( "spreading" ) ) {
         return "Invasive_Lesion";
      } else if ( diagnosis.toLowerCase().startsWith( "infiltrating" ) ) {
         return "Invasive_Lesion";
      }
      return "In_Situ_Lesion";
   }

   static private String getPostOf( final String fullLocation ) {
      int index = fullLocation.indexOf( " of " );
      if ( index < 0 ) {
         return fullLocation;
      }
      return fullLocation.substring( index + 4 ).trim();
   }

   static private String getPreSemiColon( final String fullText ) {
      int index = fullText.indexOf( ";" );
      if ( index < 0 ) {
         return fullText;
      }
      return fullText.substring( 0, index );
   }

   static private String getPreParenthesis( final String fullText ) {
      int index = fullText.indexOf( '(' );
      if ( index < 0 ) {
         return fullText;
      }
      return fullText.substring( 0, index );
   }

}

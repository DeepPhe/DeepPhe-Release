package org.healthnlp.deepphe.nlp.ae.attribute;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.attribute.biomarker.BiomarkerNormalizer;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.ctakes.ner.group.dphe.DpheGroup.GENE_PRODUCT;
import static org.apache.ctakes.ner.group.dphe.DpheGroup.TEST_RESULT;

/**
 * @author SPF , chip-nlp
 * @since {4/2/2021}
 */
@PipeBitInfo(
      name = "BiomarkerFinder",
      description = "Finds Biomarker values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR )
final public class BiomarkerFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "BiomarkerFinder" );


   static private final Pair<Integer> NO_SPAN = new Pair<>( -1, -1 );

   static private final String REGEX_METHOD
         = "IHC|Immunohistochemistry|ISH|(?:(?:Fluorecent )?IN SITU HYBRIDIZATION)|DUAL ISH"
           + "|FISH|(?:Nuclear Staining)";
   // for

   static private final String REGEX_RECOMMEND = "recommend(?:ed|ing|s)?|if";
   static private final Pattern PATTERN_RECOMMEND = Pattern.compile( REGEX_RECOMMEND, Pattern.CASE_INSENSITIVE );

   static private final String REGEX_TEST = "(?:Test(?:s|ed|ing)?|assess(?:ed|ment)|Methods?|Analysis)";

   static private final String REGEX_LEVEL = "Levels?|status|expression|results?|scores?";

//   static private final String REGEX_IS = "is|are|was";

   static private final String REGEX_STRONGLY = "weakly|strongly|greatly";
   static private final String REGEX_ELEVATED = "rising|increasing|elevated|elvtd|raised|increased|strong|amplifi(?:ed|cation)";
   static private final String REGEX_FALLING = "falling|decreasing|low|lowered|decreased|weak";
   static private final String REGEX_STABLE = "stable";


   static private final String REGEX_GT_LT = "(?:(?:Greater|>|Higher|Less|<|Lower)(?: than ?)?)?"
                                             + "(?: or )?(?:Greater|>|Higher|Less|<|Lower|Equal|=)(?: than|to "
                                             + "?)?";

//   static private final String REGEX_POSITIVE = "\\+?pos(?:itive|itivity)?|\\+(?:pos)?|overexpression";
//   static private final String REGEX_NEGATIVE = "\\-?neg(?:ative)?|\\-(?:neg)?|(?:not amplified)|(?:no [a-z] detected)";
   static private final String REGEX_POSITIVE = "\\+?pos(?:itive|itivity)?|overexpression|present|noted|identified|detected|express(?:ed)?|w\\/ ";
   static private final String REGEX_NUM_POSITIVE = "3\\+|\\+3";

   static private final String REGEX_NEGATIVE = "-?neg(?:ative)?|unamplified|absent|"
         + "(?:no(?:t|ne| [a-z0-9]*)?(?: are| is| were| was)? (?:amplifi(?:ed|cation)|present(?:ed|ation)?|noted?|identif(?:ied|y|ication)|detect(?:ed|ion)?|express(?:ed|ion)))|"
                                                + "(?:non-? ?detected)|(?:does not have)|w\\/o";
   static private final String REGEX_NUM_NEGATIVE = "0|1\\+|\\+1";

   static private final String REGEX_MUTANT = "(?:mutation (?:present|noted|identified|detected|positive|carrier))";
   static private final String REGEX_NOT_MUTANT = "(?:not? mutant)|(?:mutation (?:negative|absent))"
         + "|(?:no (?:deleterious |pathogenic )?(?:mutation|variant|rearrangement)s?)|(?:Wild ?-?type)|wt";

   static private final String REGEX_UNKNOWN
         = "unknown|indeterminate|equivocal|borderline|undetermined|inconclusive|(?:not (?:known|determined|conclusive))";
   static private final Pattern PATTERN_UNKNOWN = Pattern.compile( REGEX_UNKNOWN, Pattern.CASE_INSENSITIVE );


   static private final String REGEX_NUM_BORDERLINE = "2\\+|\\+2";

   // More often then not this show up in hypothetical language.
   static private final String REGEX_HER2_FISH = "(?:4 or less)|(?:6 or greater)";

   static private final String REGEX_NOT_ASSESSED
         = "(?:not? (?:be(?:en)? )?(?:assess(?:ed|ing|ment)?|request(?:ed|ing)?|applicable|qualify|sufficient|test(?:ed|ing)?|evaluat(?:e|ed|ing|ion)))"
         + "|insufficient|pending|\\sN\\/?A";

   static private final Pattern PATTERN_TEST_ONLY
         = Pattern.compile( "(?:" + REGEX_METHOD  + ")|(?:" +  REGEX_TEST  + ")|(?:" +  REGEX_LEVEL
         + ")|(?:" + REGEX_NOT_ASSESSED + ")", Pattern.CASE_INSENSITIVE );


   static private final String REGEX_POS_NEG = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")";

   static private final String REGEX_POS_NEG_UNK
//         = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")|(?:" + REGEX_UNKNOWN + ")";
         = REGEX_POS_NEG + "|(?:" + REGEX_UNKNOWN + ")";

   static private final String REGEX_POS_NEG_UNK_MUTANT
         = REGEX_POS_NEG_UNK + "|(?:" + REGEX_NOT_MUTANT + ")|(?:" + REGEX_MUTANT + ")";

   static private final String REGEX_POS_NEG_UNK_NA
         = REGEX_POS_NEG_UNK + "|(?:" + REGEX_NOT_ASSESSED + ")";
//         = "(?:" + REGEX_POSITIVE
//         + ")|(?:" + REGEX_NEGATIVE
//         + ")|(?:" + REGEX_UNKNOWN
//         + ")|(?:" + REGEX_NOT_ASSESSED + ")";
   static private final String REGEX_POS_NEG_UNK_NA_MUTANT
         = REGEX_POS_NEG_UNK_NA + "|(?:" + REGEX_NOT_MUTANT + ")|(?:" + REGEX_MUTANT + ")";
//         = "(?:" + REGEX_POSITIVE
//         + ")|(?:" + REGEX_NEGATIVE
//         + ")|(?:" + REGEX_UNKNOWN
//         + ")|(?:" + REGEX_NOT_ASSESSED + ")";
   static private final String REGEX_POS_NEG_UNK_NA_NUM
      = REGEX_POS_NEG_UNK_NA_MUTANT
      + "|(?:" + REGEX_NUM_POSITIVE+ ")|(?:" + REGEX_NUM_NEGATIVE + ")|(?:" + REGEX_NUM_BORDERLINE + ")";
//         = "(?:" + REGEX_POSITIVE
//           + ")|(?:" + REGEX_NUM_POSITIVE
//           + ")|(?:" + REGEX_NEGATIVE
//           + ")|(?:" + REGEX_NUM_NEGATIVE
//           + ")|(?:" + REGEX_UNKNOWN
//           + ")|(?:" + REGEX_NUM_BORDERLINE
//           + ")|(?:" + REGEX_NOT_ASSESSED
//         + ")|(?:" + REGEX_NOT_MUTANT + ")";;


   static private final String REGEX_POS_MUTANT
         = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_MUTANT + ")";

   static private final String REGEX_NEG_MUTANT
         = "(?:" + REGEX_NEGATIVE + ")|(?:" + REGEX_NOT_MUTANT + ")";
   static private final Pattern PATTERN_POS_MUTANT = Pattern.compile( REGEX_POS_MUTANT, Pattern.CASE_INSENSITIVE );
   static private final Pattern PATTERN_NEG_MUTANT = Pattern.compile( REGEX_NEG_MUTANT, Pattern.CASE_INSENSITIVE );



   static private final String REGEX_0_9
         = "[0-9]|zero|one|two|three|four|five|six|seven|eight|nine";

   static private final String REGEX_NUMTEEN
         = "(?:1[0-9])|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen";
   static private final String REGEX_0_19 = REGEX_0_9 + "|" + REGEX_NUMTEEN;

   static private final String REGEX_NUMTY
         = "(?:[2-9]0)|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety";
   static private final String REGEX_0_99
         = "(?:" + REGEX_0_19 + ")|(?:(?:" + REGEX_NUMTY + ")(?: ?-? ?" + REGEX_0_9 + ")?)";

   static private final String REGEX_HUNDREDS
         = "(?:[1-9]00)|(?:(?:" + REGEX_0_9 + " ?-? )?hundred)";
   static private final String REGEX_0_999
         = "(?:" + REGEX_0_99 + ")|(?:" + REGEX_HUNDREDS + ")(: ?-? ?" + REGEX_0_99 + ")?)";

   static private final String REGEX_DECIMAL = "\\.[0-9]{1,4}";

   static private final String SKIP_ANALYSIS = "(?mutation (?:status|analysis))";


   private enum Biomarker {
//     uri   prefText    group        cui
//           uris   positiveUris   negativeUris    unknownUris    testUris
//           typeRegex   windowSize (20)   skipRegex ("")    valueRegex     canPrecede (false)

      ER_( "EstrogenReceptorStatus", "Estrogen Receptor Status", TEST_RESULT, "C1516974",
            Arrays.asList( "EstrogenReceptor", "EstrogenReceptorBeta","ESR2Status", "ESR1Gene", "ESR2Gene",
                  "ESRRAGene", "ESRRBGene", "EstrogenReceptorFamily",
                  "SteroidHormoneReceptorERR1", "SteroidHormoneReceptorERR2" ),
            Arrays.asList( "EstrogenReceptorPositive", "ESR2Positive", "EstrogenReceptorPositiveByImmunohistochemistry",
                  "EstrogenReceptorStatus3_add_" ),
            Arrays.asList( "EstrogenReceptorNegative", "EstrogenReceptorStatus1_add_", "ESR2Negative",
                  "EstrogenReceptorStatus0", "ESR1WtAllele", "ESR2WtAllele", "ESRRAWtAllele", "ESRRBWtAllele",
                  "Triple_sub_NegativeBreastCancerFinding" ),
            Arrays.asList( "EstrogenReceptorStatus2_add_", "EstrogenReceptorStatusUnknown" ),
            Arrays.asList( "ESR1StatusByImmunohistochemistry", "ESR2StatusByImmunohistochemistry" ),
            "(?:Estrogen|ER(?!B)A?(?:-IHC)?\\+?-?|ER:(\\s*DCIS)?(\\s*IS)?)",
           20, "",
           REGEX_POS_NEG_UNK_NA,
           true ),

      PR_( "ProgesteroneReceptorStatus", "Progesterone Receptor Status", TEST_RESULT, "C1514471",
            Arrays.asList( "PGRGene", "ProgesteroneReceptor", "ProgesteroneReceptorIsoformA" ),
            Arrays.asList( "ProgesteroneReceptorPositive", "ProgesteroneReceptorPositiveByImmunohistochemistry",
                  "ProgesteroneReceptorStatus3_add_" ),
            Arrays.asList( "ProgesteroneReceptorNegative", "ProgesteroneReceptorStatus0", "ProgesteroneReceptorStatus1_add_",
                  "PGRWtAllele", "Triple_sub_NegativeBreastCancerFinding" ),
            Arrays.asList( "ProgesteroneReceptorStatus2_add_", "Progesterone Receptor Status Unknown" ),
            Collections.emptyList(),
            "(?:Progesterone|Pg?RA?(?:-IHC)?\\+?-?|PR:(\\s*DCIS)?(\\s*IS)?)",
           20, "",
           REGEX_POS_NEG_UNK_NA,
           true ),

      HER2( "HER2_sl_NeuStatus", "HER2/Neu Status", TEST_RESULT, "C1512413",
            Arrays.asList( "ERBB2Gene", "ERBB2GeneProduct", "ReceptorTyrosine_sub_ProteinKinaseErbB_sub_2",
                  "ERBB2AmplificationStatus" ),
            Arrays.asList( "HER2_sl_NeuPositive", "HormoneReceptor_sl_HER2Positive", "ERBB2Overexpression",
                  "ERBB2GeneAlterationPositive", "HER2_sl_NeuPositiveByFISH", "HER2_sl_NeuPositiveByImmunohistochemistry",
                  "ERBB2GeneFusionPositive", "PresenceOfERBB2OverexpressingDisseminatedTumorCells",
                  "ERBB2FusionPositive"),
            Arrays.asList( "ERBB2GeneMutationNegative", "ERBB2GeneAmplificationNegative", "HER2_sl_NeuLowExpression",
                  "HER2_sl_NeuNegative", "HER2_sl_NeuNegativeByFISH", "HER2_sl_NeuNegativeByImmunohistochemistry",
                  "ERBB2WtAllele", "Triple_sub_NegativeBreastCancerFinding" ),
            Collections.singletonList( "HER2_sl_NeuStatusUnknown" ),
            Arrays.asList( "HER2_sl_NeuStatusByImmunohistochemistry", "HER2_sl_NeuExpressionByValidatedImmunohistochemistry",
                  "ERBB2MutationAnalysis"),
            "(?:HER-? ?2(?: ?\\/?-? ?neu)?(?:-IHC)?\\+?-?(?:\\s*ONCOGENE)?(?:\\s*\\(?ERBB2\\)?)?)",
            20, "",
            REGEX_POS_NEG_UNK_NA_NUM, true ),

      KI67( "AntigenKI_sub_67", "Antigen KI-67", GENE_PRODUCT,
            "M?KI ?-? ?67(?: Antigen)?",
            "",
            "(?:>|< ?)?[0-9]{1,2}(?:\\.[0-9]{1,2} ?)? ?%(?: positive)?",
            true ),

//     uri   prefText    group        cui
//           uris   positiveUris   negativeUris    unknownUris    testUris
//           typeRegex   windowSize (20)   skipRegex ("")    valueRegex     canPrecede (false)

      BRCA1( "BreastCancerType1SusceptibilityProtein", "Breast Cancer Type 1 Susceptibility Protein",
            GENE_PRODUCT, "C1528558",
            Arrays.asList( "BRCA1Gene", "BAP1Gene", "UbiquitinCarboxyl_sub_TerminalHydrolaseBAP1"  ),
            Arrays.asList( "BRCA1PolymorphismPositive", "BAP1LossOfFunctionMutationPositive" ),
            Arrays.asList( "BRCA1GeneMutationNegative", "BAP1Negative", "BRCA1WtAllele", "BAP1WtAllele" ),
            Collections.emptyList(),
            Arrays.asList( "BAP1MutationAnalysis", "BRCA1MutationAnalysis" ),
            "(?:BRCA(?:[: ]*1-?)?|BROVCA1|(?:Breast Cancer Type 1))"
                  + "(?: Susceptibility)?(?: Genes?)?(?: Polymorphisms?)?",
            20, "",
            REGEX_POS_NEG_UNK_NA_MUTANT, true ),

      BRCA2( "BreastCancerType2SusceptibilityProtein", "Breast Cancer Type 2 Susceptibility Protein",
            GENE_PRODUCT, "C2973986",
            Collections.singletonList( "BRCA2Gene" ),
            Collections.singletonList( "BRCA2PolymorphismPositive" ),
            Arrays.asList( "BRCA2GeneMutationNegative", "BRCA2WtAllele" ),
            Collections.emptyList(),
            Collections.singletonList( "BRCA2MutationAnalysis" ),
            "(?:BRCA(?:[: ]*1-?(?: or| and|\\/))? ?2-?|BROVCA2|FANCD1|(?:Breast Cancer Type 2))"
                  + "(?: Susceptibility)?(?: Genes?)?(?: Polymorphisms?)?",
            20, "",
            REGEX_POS_NEG_UNK_NA_MUTANT, true ),

//     uri   prefText    group        cui
//           uris   positiveUris   negativeUris    unknownUris    testUris
//           typeRegex   windowSize (20)   skipRegex ("")    valueRegex     canPrecede (false)

      PIK3CA( "Phosphatidylinositol4_cma_5_sub_Bisphosphate3_sub_KinaseCatalyticSubunitAlphaIsoform",
            "Phosphatidylinositol 4,5-Bisphosphate 3-Kinase Catalytic Subunit Alpha Isoform",
            GENE_PRODUCT, "C1451005",
            Collections.singletonList( "PIK3CAGene" ),
            Collections.singletonList( "PIK3CAGeneAlterationPositive" ),
            Arrays.asList( "PIK3CAGeneMutationNegative", "PIK3CAWtAllele" ),
            Collections.emptyList(),
            Collections.emptyList(),
            "(?:PIK3CA|PI3K|p110|PI3 ?-? ?Kinase)", 20, " ?-?(?: Subunit)?(?: Alpha)?",
            REGEX_POS_NEG_UNK_NA_MUTANT, true ),

      // From Dennis

      TP53( "CellularTumorAntigenP53",
            "Cellular Tumor Antigen p53",
            GENE_PRODUCT, "C0146283",
            Collections.singletonList( "TP53Gene" ),
            Arrays.asList( "TP53Positive", "TP53OverexpressionPositive", "TP53GeneAlterationPositive", "TP53_str_1Allele" ),
            Arrays.asList( "TP53Negative", "TP53LossOfNuclearExpression", "TP53GeneMutationNegative", "TP53WtAllele" ),
            Collections.singletonList( "TP53NuclearExpressionIntact" ),
            Arrays.asList( "TP53MutationAnalysis", "TP53MutationStatusBySequencing" ),
            "(?:TP53|p53|(?:protein 53)|(?:Phosphoprotein p53))", 20,
            "(?: protein)?(?: tumor suppressor)?",
            REGEX_POS_NEG_UNK_NA_MUTANT, true ),

      ALK( "ALKTyrosineKinaseReceptor", "ALK Tyrosine Kinase Receptor", GENE_PRODUCT,
            "(?:ALK\\+?-?|CD246\\+?-?|(?:Anaplastic Lymphoma (?:Receptor Tyrosine )?Kinase))"
                  + "(?: Fusion)?(?: Gene|Oncogene)?(?: Alteration)?",
            "",
            "(?:" + REGEX_POS_NEG_UNK_NA + ")|(?:no rearrangement)",
            true ),

      EGFR( "EpidermalGrowthFactorReceptor", "Epidermal Growth Factor Receptor", GENE_PRODUCT,
            "EGFR\\+?-?|HER1\\+?-?|ERBB\\+?-?|C-ERBB1\\+?-?|(?:Epidermal Growth Factor)(?: Receptor)?",
            "",
            REGEX_POS_NEG_UNK_NA_MUTANT,
            true ),

      // From Dennis
      BRAF( "Serine_sl_Threonine_sub_ProteinKinaseB_sub_Raf", "Serine/Threonine-Protein Kinase B-Raf",
            GENE_PRODUCT, "C1259929",
            Collections.singletonList( "BRAFGene" ),
            Collections.singletonList( "BRAFFusionPositive" ),
            Arrays.asList( "BRAFV600WildType", "BRAFV600ENegative", "BRAFGeneAlterationNegative",
                  "BRAFGeneMutationNegative", "BRAFGeneRearrangementNegative", "BRAFWtAllele" ),
            Collections.emptyList(),
            Arrays.asList( "BRAFRearrangementAnalysis", "BRAFMutationAnalysis", "BRAFV600EMutationAnalysis",
                  "BRAFV600KMutationAnalysis" ),
            "(?:Serine\\/Threonine-Protein Kinase )?B-?RAF1?(?: Gene)?(?: fusion|rearrangement|alteration)?",
            20, "",
            REGEX_POS_NEG_UNK_NA_MUTANT, false ),

      ROS1( "Proto_sub_OncogeneTyrosine_sub_ProteinKinaseROS", "Proto-Oncogene Tyrosine-Protein Kinase ROS",
            GENE_PRODUCT,
            "(?:Proto-Oncogene )?(?:ROS1\\+?-?|MCF3\\+?-?|C-ROS-1\\+?-?"
            + "|(?:ROS Proto-Oncogene 1)"
            + "|(?:Tyrosine-Protein Kinase ROS)"
            + "|(?:Receptor Tyrosine Kinase c-ROS Oncogene 1))"
            + "(?: Gene)?(?: Fusion|Alteration|Rearrangement)?",
            "",
            REGEX_POS_NEG_UNK_NA_MUTANT,
            true ),

      PDL1( "ProgrammedCellDeathProtein1", "Programmed Cell Death Protein 1", GENE_PRODUCT,
            "(?:PDL1|PD-L1|CD247|B7|B7-H|B7H1|PDCD1L1|PDCD1LG1|(?:Programmed Cell Death 1 Ligand 1))"
            + "(?: Antigen)?(?: Molecule)?",
            "",
            "[0-9]{1,2} ?%(?: high expression)?" ),

      MSI( "MicrosatelliteStable", "Microsatellite Stable", TEST_RESULT,
            "MSI|MSS|Microsatellite",
           "",
           "stable" ),

      KRAS( "GTPaseKRas", "GTPase KRas", GENE_PRODUCT,
            "(?:KRAS\\+?-?|C-K-RAS\\+?-?|KRAS2\\+?-?|KRAS-2\\+?-?|V-KI-RAS2\\+?-?|(?:Kirsten Rat Sarcoma Viral Oncogene Homolog))",
            "",
            REGEX_POS_NEG_UNK_NA_MUTANT,
            true ),

//     uri   prefText    group        cui
//           uris   positiveUris   negativeUris    unknownUris    testUris
//           typeRegex   windowSize (20)   skipRegex ("")    valueRegex     canPrecede (false)
      NRAS("GTPaseNras", "GTPase Nras", GENE_PRODUCT, "C3657629",
            Arrays.asList( "NRASGene", "OncogeneN_sub_RAS" ),
      Collections.singletonList( "NRASPositive" ),
      Arrays.asList( "NRASNegative", "NRASGeneMutationNegative", "NRASWtAllele",
            "NRASExon2MutationNegative", "NRASExon3MutationNegative", "NRASExon4MutationNegative" ),
      Collections.emptyList(),
      Collections.singletonList( "NRASMutationAnalysis" ),
            "(?:N-?RAS1?\\+?-?)(?: gene)?",
            20, "",
      REGEX_POS_NEG_UNK_NA_MUTANT,
            true ),

//     uri   prefText    group        cui
//           uris   positiveUris   negativeUris    testUris
//           typeRegex   windowSize   skipRegex ("")    valueRegex     canPrecede (false)
      KIT( "Mast_sl_StemCellGrowthFactorReceptorKit", "Mast/Stem Cell Growth Factor Receptor Kit",
      GENE_PRODUCT, "C0072470",
            Arrays.asList( "KITGene", "OncogeneKIT" ),
            Arrays.asList( "KITOverexpressionPositive", "KITPositive" ),
            Arrays.asList( "KITGeneMutationNegative", "KITNegative", "KITWtAllele" ),
      Collections.emptyList(),
      Collections.singletonList( "KITMutationAnalysis" ),
      "(?:KIT )(?:onco)?(?:gene)?", 20, "",
      REGEX_POS_NEG_UNK_NA_MUTANT,
      false ),


      // Gave the rest of the Melanoma genes / biomarkers to Dennis ...
      CTTNB1( "CateninBeta_sub_1", "Catenin Beta-1",
            GENE_PRODUCT, "C1531189",
            Collections.singletonList( "CTNNB1Gene" ),
            Collections.singletonList( "CTNNB1Positive" ),
            Arrays.asList( "CTNNB1GeneMutationNegative", "CTNNB1WtAllele" ),
            Collections.emptyList(),
            Collections.singletonList( "CTNNB1MutationAnalysis" ),
            "CTTNB1", 20, "",
            REGEX_POS_NEG_UNK_NA_MUTANT, false ),

      GNA11( "GuanineNucleotide_sub_BindingProteinSubunitAlpha_sub_11",
            "Guanine Nucleotide-Binding Protein Subunit Alpha-11",
            GENE_PRODUCT, "C2983090",
            Collections.singletonList( "GNA11Gene" ),
            Collections.emptyList(),
            Arrays.asList( "GNA11GeneMutationNegative", "GNA11WtAllele" ),
            Collections.emptyList(),
            Arrays.asList( "GNA11GeneExpressionAnalysis", "GNA11MutationAnalysis" ),
            "GNA11", 20, "",
            REGEX_POS_NEG_UNK_NA_MUTANT, false ),

      GNAQ( "GuanineNucleotide_sub_BindingProteinG_lpn_q_rpn_SubunitAlpha",
            "Guanine Nucleotide-Binding Protein G(q) Subunit Alpha",
            GENE_PRODUCT, "C1333650",
            Collections.singletonList( "GNAQGene" ),
            Collections.emptyList(),
            Arrays.asList( "GNAQGeneMutationNegative", "GNAQWtAllele" ),
            Collections.emptyList(),
            Arrays.asList( "GNAQGeneExpressionAnalysis", "GNAQMutationAnalysis" ),
            "GNAQ", 20, "",
            REGEX_POS_NEG_UNK_NA_MUTANT, false ),

      NF1( "Neurofibromin", "Neurofibromin",
            GENE_PRODUCT, "C0083725",
            Collections.singletonList( "NF1Gene" ),
            Collections.singletonList( "NF1Positive" ),
            Arrays.asList( "NF1GeneMutationNegative", "NF1WtAllele" ),
            Collections.emptyList(),
            Collections.singletonList( "NF1MutationAnalysis" ),
            "NF1", 20, "",
            REGEX_POS_NEG_UNK_NA_MUTANT, false ),




      //  Prostate_sub_SpecificAntigen  'Prostate-Specific Antigen'    'Gene Product'
      PSA( "Prostate_sub_SpecificAntigen", "Prostate-Specific Antigen", GENE_PRODUCT,
            "PSA(?: Prostate Specific Antigen)?|(?:Prostate Specific Antigen(?: [PSA])?)",
           "",
           "[0-9]{1,2}\\.[0-9]{1,4}" ),

     PSA_EL( "Prostate_sub_SpecificAntigenEl", "Prostate-Specific Antigen", GENE_PRODUCT,
           "PSA(?: Prostate Specific Antigen)?|(?:Prostate Specific Antigen(?: [PSA])?)",
          "",
             "(?:" + REGEX_ELEVATED + ")|(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")",
             true );

      final String _uri;
      final String _prefText;
      final DpheGroup _group;
      final String _cui;
      final Collection<String> _uris;
      final Collection<String> _positiveUris;
      final Collection<String> _negativeUris;
      final Collection<String> _unknownUris;
      final Collection<String> _testUris;
      final Pattern _typePattern;
      final int _windowSize;
      final boolean _checkSkip;
      final Pattern _skipPattern;
      final Pattern _valuePattern;
      final boolean _canPrecede;
      final boolean _plusMinus;

      Biomarker( final String uri, final String prefText, final DpheGroup group,
                 final String typeRegex, final String skipRegex, final String valueRegex ) {
         this( uri, prefText, group, typeRegex, 20, skipRegex, valueRegex, false );
      }
      Biomarker( final String uri, final String prefText, final DpheGroup group,
                 final String typeRegex, final String skipRegex, final String valueRegex,
                 final boolean canPrecede ) {
         this( uri, prefText, group, typeRegex, 20, skipRegex, valueRegex, canPrecede );
      }
      Biomarker( final String uri, final String prefText, final DpheGroup group,
                 final String typeRegex, final int windowSize, final String skipRegex,
                 final String valueRegex, final boolean canPrecede ) {
         this( uri, prefText, group, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
               Collections.emptyList(),
               Collections.emptyList(), typeRegex, windowSize, skipRegex, valueRegex, canPrecede );
      }
      Biomarker( final String uri, final String prefText, final DpheGroup group, final String cui,
                 final Collection<String> uris, final Collection<String> positiveUris,
                 final Collection<String> negativeUris, final Collection<String> unknownUris,
                 final Collection<String> testUris,
                 final String typeRegex, final int windowSize, final String skipRegex,
                 final String valueRegex, final boolean canPrecede ) {
         _uri = uri;
         _prefText = prefText;
         _group = group;
         _cui = cui;
         _uris = uris;
         _positiveUris = positiveUris;
         _negativeUris = negativeUris;
         _unknownUris = unknownUris;
         _testUris = testUris;
         _typePattern = Pattern.compile( typeRegex, Pattern.CASE_INSENSITIVE );
         _windowSize = windowSize;
         if ( skipRegex.isEmpty() ) {
            _checkSkip = false;
            _skipPattern = null;
         } else {
            _checkSkip = true;
            _skipPattern = Pattern.compile( skipRegex, Pattern.CASE_INSENSITIVE );
         }
         _valuePattern = Pattern.compile( valueRegex, Pattern.CASE_INSENSITIVE );
         _canPrecede = canPrecede;
//         _plusMinus = REGEX_POS_NEG_UNK.equals( valueRegex );
         // Since we are only checking the biomarker span, this should be ok.
         _plusMinus = true;
      }
   }

  /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Biomarkers and Values ..." );
      final String docText = jCas.getDocumentText();
      final Collection<Integer> annotationBegins = JCasUtil.select( jCas, IdentifiedAnnotation.class )
                                                           .stream()
                                                           .filter( a -> (a instanceof EventMention ||
                                                                 a instanceof AnatomicalSiteMention) )
                                                           .filter( a -> !(a instanceof SignSymptomMention) )
                                                           .map( IdentifiedAnnotation::getBegin )
                                                           .collect( Collectors.toSet() );
      final List<Pair<Integer>> sentenceSpans
            = JCasUtil.select( jCas, Sentence.class )
                      .stream()
                      .map( s -> new Pair<>( s.getBegin(), s.getEnd() ) )
            .sorted( Comparator.comparing( Pair::getValue1 ) )
                      .collect( Collectors.toList() );
      for ( Biomarker biomarker : Biomarker.values() ) {
         // Take care of similar uris that were found by dictionary lookup.
         // Don't worry about repeats - they should increase confidence.
         handleNerBiomarker( jCas, biomarker, docText );

         // Find biomarkers and values by regex.
         // Don't worry about repeats - they should increase confidence.
         final List<Pair<Integer>> biomarkerSpans = findBiomarkerSpans( biomarker, docText );

         addBiomarkerValues( jCas, biomarker, docText, biomarkerSpans, sentenceSpans, annotationBegins );
      }
   }

   static private Pair<Integer> spanAnnotation( final IdentifiedAnnotation annotation ) {
      return new Pair<>( annotation.getBegin(), annotation.getEnd() );
   }

   /**
    * Re-Add biomarkers with values found previously by dictionary lookup.
    * @param jCas -
    * @param biomarker -
    * @param docText -
    */
   static private void handleNerBiomarker( final JCas jCas, final Biomarker biomarker, final String docText ) {
      final Collection<Pair<Integer>> handledSpans = new HashSet<>();
      // Attempt to add biomarkers found by dictionary lookup, using the proper/suspected patterns for values.
      handleUrisSpecificValueType( jCas, biomarker, docText, biomarker._negativeUris, PATTERN_NEG_MUTANT, handledSpans );
      handleUrisSpecificValueType( jCas, biomarker, docText, biomarker._positiveUris, PATTERN_POS_MUTANT, handledSpans );
      handleUrisSpecificValueType( jCas, biomarker, docText, biomarker._unknownUris, PATTERN_UNKNOWN, handledSpans );
      handleUrisSpecificValueType( jCas, biomarker, docText, biomarker._testUris, PATTERN_TEST_ONLY, handledSpans );
      handleUrisSpecificValueType( jCas, biomarker, docText, biomarker._uris, PATTERN_TEST_ONLY, handledSpans );
      // Attempt to add biomarkers found by dictionary lookup, using all patterns for values according to type.
      handleUrisAnyValueType( jCas, biomarker, docText, biomarker._negativeUris, handledSpans );
      handleUrisAnyValueType( jCas, biomarker, docText, biomarker._positiveUris, handledSpans );
      handleUrisAnyValueType( jCas, biomarker, docText, biomarker._unknownUris, handledSpans );
      handleUrisAnyValueType( jCas, biomarker, docText, biomarker._testUris, handledSpans );
      handleUrisAnyValueType( jCas, biomarker, docText, biomarker._uris, handledSpans );
      handleUrisAnyValueType( jCas, biomarker, docText, Collections.singletonList( biomarker._uri ), handledSpans );
   }


   static private void handleUrisAnyValueType( final JCas jCas,
                                               final Biomarker biomarker,
                                               final String docText,
                                               final Collection<String> uris,
                                               final Collection<Pair<Integer>> handledSpans ) {
      handleUrisSpecificValueType( jCas, biomarker, docText, uris, biomarker._valuePattern, handledSpans );
   }

   static private void handleUrisSpecificValueType( final JCas jCas,
                                                    final Biomarker biomarker,
                                                    final String docText,
                                                    final Collection<String> uris,
                                                    final Pattern valuePattern,
                                                    final Collection<Pair<Integer>> handledSpans ) {
      for ( String uri : uris ) {
         final Collection<IdentifiedAnnotation> annotations = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, uri );
         handleBiomarkerAnnotations( jCas, biomarker, docText, annotations, valuePattern, handledSpans );
      }
   }

   static private void handleBiomarkerAnnotations( final JCas jCas,
                                                   final Biomarker biomarker,
                                                  final String docText,
                                                  final Collection<IdentifiedAnnotation> annotations,
                                                  final Pattern valuePattern,
                                                  final Collection<Pair<Integer>> handledSpans ) {
      for ( IdentifiedAnnotation annotation : annotations ) {
         final Pair<Integer> annotationSpan = spanAnnotation( annotation );
         if ( isSpanHandled( annotationSpan, handledSpans ) ) {
            continue;
         }
         if ( addSynonymValue( jCas, biomarker, docText, annotationSpan, valuePattern ) ) {
            // Remove the original non-valued annotation from the cas.
            annotation.removeFromIndexes( jCas );
            handledSpans.add( annotationSpan );
         }
      }
   }



//   static private boolean isSpanHandled( final IdentifiedAnnotation annotation,
//                                         final Collection<Pair<Integer>> handledSpans ) {
//      return isSpanHandled( spanAnnotation( annotation ), handledSpans );
//   }

   static private boolean isSpanHandled( final Pair<Integer> span, final Collection<Pair<Integer>> handledSpans ) {
      for ( Pair<Integer> handledSpan : handledSpans ) {
         if ( handledSpan.getValue1() <= span.getValue1()
               && handledSpan.getValue2() >= span.getValue2() ) {
            return true;
         }
      }
      return false;
   }


   // TODO Subsume


   static private List<Pair<Integer>> findBiomarkerSpans( final Biomarker biomarker, final String docText ) {
      try ( RegexSpanFinder finder = new RegexSpanFinder( biomarker._typePattern ) ) {
         return finder.findSpans( docText )
                      .stream()
                      .filter( s -> isWholeWord( docText, s ) )
                      .collect( Collectors.toList() );
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.warn( iaE.getMessage() );
         return Collections.emptyList();
      }
   }

   static private int getSentenceIndex( final Pair<Integer> biomarkerSpan,
                                        final List<Pair<Integer>> sentenceSpans ) {
      for ( int i=0; i<sentenceSpans.size(); i++ ) {
         if ( sentenceSpans.get( i ).getValue1() <= biomarkerSpan.getValue1()
               && sentenceSpans.get( i ).getValue2() >= biomarkerSpan.getValue2() ) {
            return i;
         }
      }
      return -1;
   }

//   static private Pair<Integer> getValueWindow( final Biomarker biomarker,
//                                                   final String docText,
//                                                   final Pair<Integer> biomarkerSpan,
//                                                   final Pair<Integer> sentenceSpan,
//                                                   final Collection<Integer> annotationBegins ) {
//      int begin = sentenceSpan.getValue1();
//      if ( biomarker._canPrecede ) {
//         final int precedingAnnotation = getPrecedingAnnotation( biomarkerSpan.getValue1(), annotationBegins );
//         begin = Math.max( precedingAnnotation, begin );
//         final int prePara = docText.substring( begin, biomarkerSpan.getValue1() ).lastIndexOf( "\n\n" );
//         if ( prePara >= 0 ) {
//            begin = begin + prePara + 2;
//         }
//      } else {
//         begin = biomarkerSpan.getValue1();
//      }
//      int end = sentenceSpan.getValue2();
//      final int followingAnnotation = getFollowingAnnotation( biomarkerSpan.getValue2(), docText.length(), annotationBegins );
//      end = Math.min( followingAnnotation, end );
//      final int postPara = docText.substring( biomarkerSpan.getValue2(), end ).indexOf( "\n\n" );
//      if ( postPara >= 0 ) {
//         end = biomarkerSpan.getValue2() + postPara;
//      }
//      return new Pair<>( begin, end );
//   }

   static private Pair<Integer> getExpandedWindow( final Biomarker biomarker,
                                                   final String docText,
                                                   final Pair<Integer> biomarkerSpan,
                                                   final Pair<Integer> currentWindowSpan,
                                                   final Pair<Integer> sentenceSpan,
                                                   final Collection<Integer> annotationBegins ) {
      int end = sentenceSpan.getValue2();
      if ( end != currentWindowSpan.getValue2() ) {
         final int followingAnnotation =
               getFollowingAnnotation( currentWindowSpan.getValue2(), docText.length(), annotationBegins );
         end = Math.min( followingAnnotation, end );
         final int postPara = docText.substring( biomarkerSpan.getValue2(), end ).indexOf( "\n\n" );
         if ( postPara >= 0 ) {
            end = biomarkerSpan.getValue2() + postPara;
         }
         if ( end != currentWindowSpan.getValue2() ) {
            return new Pair<>( currentWindowSpan.getValue1(), end );
         }
      }
      if ( !biomarker._canPrecede ) {
         return new Pair<>( currentWindowSpan.getValue1(), end );
      }
      int begin = sentenceSpan.getValue1();
      final int precedingAnnotation = getPrecedingAnnotation( currentWindowSpan.getValue1(), annotationBegins );
      begin = Math.max( precedingAnnotation, begin );
      final int prePara = docText.substring( begin, biomarkerSpan.getValue1() ).lastIndexOf( "\n\n" );
      if ( prePara >= 0 ) {
         begin = begin + prePara + 2;
      }
      return new Pair<>( begin, end );
   }


   static private void addBiomarkerValues( final JCas jCas,
                                           final Biomarker biomarker,
                                           final String docText,
                                           final List<Pair<Integer>> biomarkerSpans,
                                           final List<Pair<Integer>> sentenceSpans,
                                           final Collection<Integer> annotationBegins ) {
      if ( biomarkerSpans.isEmpty() ) {
         return;
      }
      for ( Pair<Integer> biomarkerSpan : biomarkerSpans ) {
         final Pair<Integer> valueSpan
               = getBiomarkerValueSpan( biomarker, docText, biomarkerSpan, sentenceSpans, annotationBegins );
         if ( !NO_SPAN.equals( valueSpan ) ) {
            addBiomarkerAndValue( jCas, biomarker, biomarkerSpan,
                  docText.substring( valueSpan.getValue1(), valueSpan.getValue2() ) );
         } else {
            // We still want to add the generic biomarker mention.
            addBiomarkerOnly( jCas, biomarker, biomarkerSpan );
         }
         // Add the new span begin to our list.
         annotationBegins.add( biomarkerSpan.getValue1() );
      }
   }


//
//   static private Pair<Integer> getBiomarkerValueSpanStatic( final Biomarker biomarker,
//                                                         final String docText,
//                                                         final Pair<Integer> biomarkerSpan,
//                                                         final List<Pair<Integer>> sentenceSpans,
//                                                         final Collection<Integer> annotationBegins ) {
//      final int sentenceI = getSentenceIndex( biomarkerSpan, sentenceSpans );
//      if ( sentenceI < 0 ) {
//         // Something is wrong.
//         return NO_SPAN;
//      }
//      final Pair<Integer> sentenceSpan = sentenceSpans.get( sentenceI );
//      final Pair<Integer> windowSpan = getValueWindow( biomarker, docText, biomarkerSpan,
//            sentenceSpan, annotationBegins );
//      Pair<Integer> valueSpan = getWindowValueSpan( biomarker, docText, biomarkerSpan, windowSpan );
//      if ( !NO_SPAN.equals( valueSpan ) ) {
//         return valueSpan;
//      }
//      if ( sentenceI < sentenceSpans.size()-1 ) {
//         // See if the biomarker has a following sentence with panel result.
//         final Pair<Integer> negPanelSpan = inNegativePanel( docText, sentenceI, sentenceSpans );
//         if ( !NO_SPAN.equals( negPanelSpan ) ) {
//            return negPanelSpan;
//         }
//      }
//      // As a last ditch, use a copy of the normalizer to get the value span.
//      final Pair<Integer> fudgeSpan
//            = FUDGE_NORMAL.getNormalSpan( docText.substring( windowSpan.getValue1(), windowSpan.getValue2() ) );
//      if ( !NO_SPAN.equals( fudgeSpan ) ) {
//         return new Pair<>( windowSpan.getValue1() + fudgeSpan.getValue1(), windowSpan.getValue1() + fudgeSpan.getValue2() );
//      }
//      return NO_SPAN;
//   }

   /**
    * This method uses a constantly expanding search window until a value is found within the biomarker's sentence.
    * This is used just in case the value is stated before or after some non-sign/symptom annotation, such as a finding.
    * @param biomarker -
    * @param docText -
    * @param biomarkerSpan -
    * @param sentenceSpans -
    * @param annotationBegins -
    * @return -
    */
   static private Pair<Integer> getBiomarkerValueSpan( final Biomarker biomarker,
                                                       final String docText,
                                                       final Pair<Integer> biomarkerSpan,
                                                       final List<Pair<Integer>> sentenceSpans,
                                                       final Collection<Integer> annotationBegins ) {
      final int sentenceI = getSentenceIndex( biomarkerSpan, sentenceSpans );
      if ( sentenceI < 0 ) {
         // Something is wrong.
         return NO_SPAN;
      }
      Pair<Integer> plusMinusSpan = getPlusMinusSpan( biomarker, docText, biomarkerSpan );
      if ( !NO_SPAN.equals( plusMinusSpan ) ) {
         return plusMinusSpan;
      }
      final Pair<Integer> sentenceSpan = sentenceSpans.get( sentenceI );
      // Just in case the biomarker was picked up by NER without the +/-
      if ( biomarkerSpan.getValue2() + 1 < sentenceSpan.getValue2() ) {
         plusMinusSpan = getPlusMinusSpan( biomarker, docText,
               new Pair<>( biomarkerSpan.getValue1(), biomarkerSpan.getValue2() + 1 ) );
         if ( !NO_SPAN.equals( plusMinusSpan ) ) {
            return plusMinusSpan;
         }
      }
      // Kludge for test recommendation and "if"
      final Pair<Integer> recommendSpan = getRecommendSpan( docText, biomarkerSpan, sentenceSpan );
      if ( !NO_SPAN.equals( recommendSpan ) ) {
         return recommendSpan;
      }
      Pair<Integer> currentWindowSpan = NO_SPAN;
      Pair<Integer> nextSpan = biomarkerSpan;
      Pair<Integer> valueSpan = NO_SPAN;
      while ( !nextSpan.equals( currentWindowSpan ) ) {
         currentWindowSpan = nextSpan;
         valueSpan = getWindowValueSpan( biomarker, docText, biomarkerSpan, currentWindowSpan );
         if ( !NO_SPAN.equals( valueSpan ) ) {
            return valueSpan;
         }
         nextSpan = getExpandedWindow( biomarker, docText,  biomarkerSpan, currentWindowSpan, sentenceSpan,
               annotationBegins );
      }
      // Use a copy of the normalizer to get the value span.  Not a great solution, but better than none.
      currentWindowSpan = NO_SPAN;
      nextSpan = biomarkerSpan;
      Pair<Integer> fudgeSpan = NO_SPAN;
      while ( !nextSpan.equals( currentWindowSpan ) ) {
         currentWindowSpan = nextSpan;
         fudgeSpan = FUDGE_WORDS.getNormalSpan( docText.substring( currentWindowSpan.getValue1(), currentWindowSpan.getValue2() ) );
         if ( !NO_SPAN.equals( fudgeSpan ) ) {
            return new Pair<>( currentWindowSpan.getValue1() + fudgeSpan.getValue1(),
                  currentWindowSpan.getValue1() + fudgeSpan.getValue2() );
         }
         nextSpan = getExpandedWindow( biomarker, docText,  biomarkerSpan, currentWindowSpan, sentenceSpan,
               annotationBegins );
      }
      // See if the biomarker has a following sentence with panel result.
      if ( sentenceI < sentenceSpans.size()-1 ) {
         final Pair<Integer> negPanelSpan = inNegativePanel( docText, sentenceI, sentenceSpans );
         if ( !NO_SPAN.equals( negPanelSpan ) ) {
            return negPanelSpan;
         }
      }
      return NO_SPAN;
   }



   // UPMC kludging
   static private final Collection<String> NEG_PANEL
         = Collections.singletonList(
         "no mutations were detected through sequencing or deletion/duplication studies of the following" );

   static private final Collection<String> NEG_PANEL_NEXT
         = Arrays.asList( "no pathogenic variants were detected",
         "sequencing and deletion/duplication studies did not identify" );

   static private Pair<Integer> inNegativePanel( final String docText, final int sentenceI,
                                                 final List<Pair<Integer>> sentenceSpans ) {
      final Pair<Integer> sentenceSpan = sentenceSpans.get( sentenceI );
      final String sentence = docText.substring( sentenceSpan.getValue1(), sentenceSpan.getValue2() )
                                         .toLowerCase();
      for ( String negPanel : NEG_PANEL ) {
         if ( sentence.startsWith( negPanel ) ) {
            return new Pair<>( sentenceSpan.getValue1(), sentenceSpan.getValue1() + negPanel.length() );
         }
      }
      if ( sentenceI < sentenceSpans.size()-1 ) {
         final Pair<Integer> nextSentenceSpan = sentenceSpans.get( sentenceI );
         final String nextSentence = docText.substring( nextSentenceSpan.getValue1(), nextSentenceSpan.getValue2() )
                                            .toLowerCase();
         for ( String negPanel : NEG_PANEL_NEXT ) {
            if ( nextSentence.startsWith( negPanel ) ) {
               return new Pair<>( nextSentenceSpan.getValue1(), nextSentenceSpan.getValue1() + negPanel.length() );
            }
         }
      }
      return NO_SPAN;
   }

   static private boolean addSynonymValue( final JCas jCas,
                                           final Biomarker biomarker,
                                           final String docText,
                                           final Pair<Integer> synonymSpan,
                                           final Pattern valuePattern ) {
      final Pair<Integer> valueSpan = getSynonymValueSpan( valuePattern, docText, synonymSpan );
      if ( !NO_SPAN.equals( valueSpan ) ) {
         addBiomarkerAndValue( jCas, biomarker, synonymSpan,
               docText.substring( valueSpan.getValue1(), valueSpan.getValue2() ) );
         return true;
      }
      return false;
   }

   static private Pair<Integer> getWindowValueSpan( final Biomarker biomarker,
                                                    final String docText,
                                                    final Pair<Integer> biomarkerSpan,
                                                    final Pair<Integer> windowSpan ) {
      final Pair<Integer> followingValueSpan = getFollowingValueSpan( biomarker, docText, biomarkerSpan, windowSpan );
      if ( !NO_SPAN.equals( followingValueSpan ) ) {
         return followingValueSpan;
      }
      return getPrecedingValueSpan( biomarker, docText, biomarkerSpan, windowSpan );
   }

   static private Pair<Integer> getPlusMinusSpan( final Biomarker biomarker,
                                                  final String docText,
                                                  final Pair<Integer> biomarkerSpan ) {
      if ( biomarker._plusMinus ) {
         final char c = docText.charAt( biomarkerSpan.getValue2()-1 );
         if ( (c == '+' || c == '-') && isWholeWord( docText, biomarkerSpan ) ) {
            return new Pair<>( biomarkerSpan.getValue2()-1, biomarkerSpan.getValue2() );
         }
      }
      return NO_SPAN;
   }

   static private Pair<Integer> getRecommendSpan( final String docText,
                                                  final Pair<Integer> biomarkerSpan,
                                                  final Pair<Integer> sentenceSpan ) {
      final int preBegin = Math.max( sentenceSpan.getValue1(), sentenceSpan.getValue1()-30 );
      final String preText = docText.substring( preBegin, biomarkerSpan.getValue1() ).toLowerCase();
      final int recommendBegin = preText.lastIndexOf( "recommend" );
      final int ifBegin = preText.lastIndexOf( " if " );
      if ( ifBegin > recommendBegin ) {
         return new Pair<>( preBegin + ifBegin, preBegin + ifBegin+4 );
      }
      if ( recommendBegin >= 0 ) {
         final int wordEnd = findWordEnd( preText, recommendBegin + 9 );
         return new Pair<>( preBegin + recommendBegin, preBegin + wordEnd );
      }
      return NO_SPAN;
   }

   static private Pair<Integer> getFollowingValueSpan( final Biomarker biomarker,
                                                       final String docText,
                                                       final Pair<Integer> biomarkerSpan,
                                                       final Pair<Integer> windowSpan ) {
      final String followingText = getFollowingText( biomarkerSpan, docText, windowSpan );
      if ( followingText.isEmpty() ) {
         return NO_SPAN;
      }
//      if ( biomarker._checkSkip ) {
//         final Matcher skipMatcher = biomarker._skipPattern.matcher( followingText );
//         if ( skipMatcher.find() ) {
//            return NO_SPAN;
//         }
//      }
      final Matcher matcher = biomarker._valuePattern.matcher( followingText );
      if ( matcher.find() ) {
         if ( isWholeValueWord( followingText, matcher.start(), matcher.end() ) ) {
            return new Pair<>( biomarkerSpan.getValue2() + matcher.start(), biomarkerSpan.getValue2() + matcher.end() );
         }
      }
      return NO_SPAN;
   }


   static private Pair<Integer> getPrecedingValueSpan( final Biomarker biomarker,
                                                       final String docText,
                                                       final Pair<Integer> biomarkerSpan,
                                                       final Pair<Integer> windowSpan ) {
      if ( !biomarker._canPrecede ) {
         return NO_SPAN;
      }
      final String prevText = getPrecedingText( biomarkerSpan, docText, windowSpan );
      if ( prevText.isEmpty() ) {
         return NO_SPAN;
      }
      final Matcher matcher = biomarker._valuePattern.matcher( prevText );
      Pair<Integer> lastMatch = null;
      while ( matcher.find() ) {
         if ( isWholeValueWord( prevText, matcher.start(), matcher.end() ) ) {
            lastMatch = new Pair<>( matcher.start(), matcher.end() );
         }
      }
      if ( lastMatch != null ) {
         return new Pair<>( windowSpan.getValue1() + lastMatch.getValue1(), windowSpan.getValue1() + lastMatch.getValue2() );
      }
      return NO_SPAN;
   }


   static private Pair<Integer> getSynonymValueSpan( final Pattern valuePattern,
                                                       final String docText,
                                                       final Pair<Integer> synonymSpan ) {
      final String synonymText = docText.substring( synonymSpan.getValue1(), synonymSpan.getValue2() );
      if ( synonymText.isEmpty() ) {
         return NO_SPAN;
      }
      final Matcher matcher = valuePattern.matcher( synonymText );
      Pair<Integer> lastMatch = null;
      while ( matcher.find() ) {
         if ( isWholeValueWord( synonymText, matcher.start(), matcher.end() ) ) {
            lastMatch = new Pair<>( matcher.start(), matcher.end() );
         }
      }
      if ( lastMatch != null ) {
         return new Pair<>( synonymSpan.getValue1() + lastMatch.getValue1(), synonymSpan.getValue1() + lastMatch.getValue2() );
      }
      return NO_SPAN;
   }


   static private void addBiomarkerAndValue( final JCas jCas, final Biomarker biomarker,
                                             final Pair<Integer> biomarkerSpan, final String valueText ) {
      AnnotationFactory.createAnnotation( jCas, biomarkerSpan.getValue1(), biomarkerSpan.getValue2(),
            biomarker._group, biomarker._uri, biomarker._cui, biomarker._prefText, valueText );
      // Kludge to create BRCA2 in addition to BRCA1 when text is just "BRCA"
      if ( biomarker == Biomarker.BRCA1 && jCas.getDocumentText().substring( biomarkerSpan.getValue1(),
            biomarkerSpan.getValue2() ).equalsIgnoreCase("BRCA"  ) ) {
         AnnotationFactory.createAnnotation( jCas, biomarkerSpan.getValue1(), biomarkerSpan.getValue2(),
               Biomarker.BRCA2._group, Biomarker.BRCA2._uri, Biomarker.BRCA2._cui, Biomarker.BRCA2._prefText, valueText );
      }
   }

//   static private void addBiomarkerWindowValue( final JCas jCas, final Biomarker biomarker,
//                                         final Pair<Integer> biomarkerSpan, final String windowText ) {
//      AnnotationFactory.createAnnotation( jCas, biomarkerSpan.getValue1(), biomarkerSpan.getValue2(),
//            biomarker._group, biomarker._uri, biomarker._cui, biomarker._prefText, windowText );
//   }

   static private void addBiomarkerOnly( final JCas jCas, final Biomarker biomarker,
                                         final Pair<Integer> biomarkerSpan ) {
      AnnotationFactory.createAnnotation( jCas, biomarkerSpan.getValue1(), biomarkerSpan.getValue2(),
            biomarker._group, biomarker._uri, biomarker._cui, biomarker._prefText );
   }


//   static private Pair<Integer> getSentenceSpan( final Pair<Integer> biomarkerSpan,
//                                                 final Collection<Pair<Integer>> sentenceSpans ) {
//      return sentenceSpans.stream()
//                         .filter( s -> s.getValue1() <= biomarkerSpan.getValue1()
//                                       && biomarkerSpan.getValue2() <= s.getValue2() )
//                         .findFirst()
//                         .orElse( biomarkerSpan );
//   }

//   static private int getPrecedingAnnotation( final Pair<Integer> biomarkerSpan,
//                                               final Collection<Integer> annotationBegins ) {
//      return annotationBegins.stream()
//                          .filter( b -> b < biomarkerSpan.getValue1() )
//                             .mapToInt( b -> b )
//                          .max()
//                          .orElse( 0 );
//   }
//
//   static private int getFollowingAnnotation( final Pair<Integer> biomarkerSpan,
//                                              final int textLength,
//                                              final Collection<Integer> annotationBegins ) {
//      return annotationBegins.stream()
//                             .filter( b -> b >= biomarkerSpan.getValue2() )
//                             .mapToInt( b -> b )
//                             .min()
//                             .orElse( textLength );
//   }


   static private int getPrecedingAnnotation( final int annotationBegin,
                                              final Collection<Integer> annotationBegins ) {
      return annotationBegins.stream()
                             .filter( b -> b < annotationBegin )
                             .mapToInt( b -> b )
                             .max()
                             .orElse( 0 );
   }

   static private int getFollowingAnnotation( final int annotationEnd,
                                              final int textLength,
                                              final Collection<Integer> annotationBegins ) {
      return annotationBegins.stream()
                             .filter( b -> b >= annotationEnd )
                             .mapToInt( b -> b )
                             .min()
                             .orElse( textLength );
   }




   static private String getPrecedingText( final Pair<Integer> biomarkerSpan,
                                           final String docText,
                                           final Pair<Integer> windowSpan ) {
      if ( windowSpan.getValue1().intValue() == biomarkerSpan.getValue1().intValue() ) {
         return "";
      }
      return docText.substring( windowSpan.getValue1(), biomarkerSpan.getValue1() );
   }


   static private String getFollowingText( final Pair<Integer> biomarkerSpan,
                                           final String docText,
                                           final Pair<Integer> windowSpan ) {
      if ( biomarkerSpan.getValue2().intValue() == windowSpan.getValue2().intValue() ) {
         return "";
      }
      String nextText = docText.substring( biomarkerSpan.getValue2(), windowSpan.getValue2() );
      return hideBracketOptions( nextText );
   }


   // Sometimes available value sets are in brackets.  e.g.  "ER: [pos;neg;unk] = neg"
   static private String hideBracketOptions(  final String windowText ) {

      final int startBracket = windowText.indexOf( '[' );
      if ( startBracket >= 0 ) {
         final int endBracket = windowText.indexOf( ']', startBracket );
         if ( endBracket > 0 ) {
            final char[] chars = windowText.toCharArray();
            for ( int i=startBracket+1; i<endBracket; i++ ) {
               chars[ i ] = 'V';
            }
            return new String( chars );
         }
      }
      return windowText;
   }

   // Why the heck don't word boundaries ever work in java?!
   static private boolean isWholeWord( final String text, final Pair<Integer> span ) {
      return isWholeWord( text, span.getValue1(), span.getValue2() );
   }

   // The : character can denote hypothetical/generic.
   // "HER2 negative:  HER2 gene/chromosome 17 ratio of less than 1.8, or an average
   //number of HER2 gene copies/cell (SNR) of 4 or less
   static private boolean isWholeValueWord( final String text, final int begin, final int end ) {
      final boolean wholeWord = isWholeWord( text, begin, end );
      if ( !wholeWord ) {
         return false;
      }
      if ( end == text.length() ) {
         return wholeWord;
      }
      return text.charAt( end ) != ':';
   }

   // Why the heck don't word boundaries ever work in java?!
   static private boolean isWholeWord( final String text, final int begin, final int end ) {
      // check to see if it is a number and only a number.
      if ( begin > 0 ) {
         if ( Character.isLetterOrDigit( text.charAt( begin-1 ) ) ) {
            return false;
         }
      }
      if ( end == text.length() ) {
         return true;
      }
      return !Character.isLetterOrDigit( text.charAt( end ) );
   }

   static private int findWordEnd( final String fullText, final int discoveryEnd ) {
      if ( discoveryEnd == fullText.length() ) {
         return discoveryEnd;
      }
      for ( int i=discoveryEnd; i<fullText.length(); i++ ) {
         if ( !Character.isLetterOrDigit( fullText.charAt( i ) ) ) {
            return i;
         }
      }
      return discoveryEnd;
   }

   private enum FUDGE_WORDS {
      Negative( "not possible", "no possibility" ),
      Can_Assess( "can assess", "can test", "meets criteria", "meets the current nccn criteria",
            "granted", "approved", "candidate", "applicable",
            "qualifies", "possibility", "possibl", "meets testing guidelines", "option", "recommend" ),
      Unknown( "suspicious" ),
      Positive( "expression" ),
      Not_Assessed( "n't assess", "n't test", "n't evaluate", "n't request",
         "not interest", "no interest", "uninterested", "not want", "n't want",
         "not qualif", "denied", "declined", "not meet criteria", "discussed testing",
         "inquir", "motivated to pursue", "interested", "wants", "has not had" ),
      Will_Assess( "will assess", "will test","will evaluate", "will be analyzed",
            "will be assess", "will be test", "will be evaluat", "will be completed",
            "set up for", "sample obtained", "obtained sample" );

      private final Collection<String> _text;
      FUDGE_WORDS( final String ... text ) {
         _text = new HashSet<>( Arrays.asList( text ) );
      }
      static private Pair<Integer> getNormalSpan( final String text ) {
         if ( text.isEmpty() ) {
            return NO_SPAN;
         }
         final String lower = text.toLowerCase();
         for ( FUDGE_WORDS normal : values() ) {
            for ( String norm : normal._text ) {
               final int normBegin = lower.indexOf( norm );
               if ( normBegin < 0 ) {
                  continue;
               }
               final int wordEnd = findWordEnd( lower, normBegin + norm.length() );
               if ( normal == Positive && lower.startsWith( "no " ) ) {
                  return new Pair<>( 0, wordEnd );
               }
               return new Pair<>( normBegin, wordEnd );
            }
         }
         return NO_SPAN;
      }
   }








   static private final String[] TEST_TEXT = {
         "-- Patient 1 --",
         "Focality Oncotype Dx: BRCA (Historical): Genetics: Genetic panel neg",
         "meets the current NCCN criteria for BRCA1/2 gene testing.",
         "discussed testing for mutations in the BRCA1/2 genes",
         "Evaluation of the ATM, BRCA1, BRCA2, CDH1, CHEK2, PALB2, PTEN and TP53 genes will be completed.",
         "Genetic testing was negative for any deleterious BRCA mutations.",
         "Her tumor was estrogen receptor positive, progesterone receptor negative, and Her-2/neu positive.  She is BRCA negative.",
         "-- Patient 2 --",
         "Genetic testing was negative including ATM, BARD1, BRCA1, BRCA2, CHEK2, MSH2, PALB2, PTEN, TP53, POLE, EPCAM, AND GREM1.",
         "Labs MammaPrint result was high risk BRCA 1 and 2 negative Review of Systems",
         "-- Patient 3 --",
         "She does understand the surgical implications of a pathogenic BRCA mutation.",
         "-- Patient 4 --",
         "Nottingham Score 9 Oncotype Dx: BRCA (Historical): Genetics:",
         "-- Patient 5 --",
         "Nottingham Score 7 BRCA: Oncotype Dx:",
         "-- Patient 6 --",
         "The ATM, BARD1, BRCA1, BRCA2, BRIP1, CDH1, CHEK2,  EPCAM (deletion/duplication only), MLH1, MSH2, MSH6, MUTYH, NBN, NF1, PALB2, PMS2, PTEN, RAD51C, RAD51D, STK11, and TP53 genes will be analyzed.",
         "Testing included evaluation of the following genes: ATM, BARD1, BRCA1, BRCA2, BRIP1, CDH1, CHEK2,  EPCAM (deletion/duplication only), MLH1, MSH2, MSH6, MUTYH, NBN, NF1, PALB2, PMS2, PTEN, RAD51C, RAD51D, STK11, and TP53.  No pathogenic variants were detected through sequencing or deletion/duplication studies.",
         "-- Patient 7 --",
         "BRCA1 and BRCA2 are genes that help control normal cell growth.",
         "Some people have a family history, but they don't have the BRCA mutation.",
         "To find out if you have the BRCA mutation, you can have a blood test.",
         "Why should you have BRCA testing?",
         "You may feel better if the test shows that you don't have a BRCA mutation. This is called a negative result.",
         "If the test shows that you do have a BRCA mutation, it's called a positive result.",
         "What are the risks of BRCA testing?  A negative test may give you a false sense of security.",
         "But a negative BRCA test does not mean that you will never have breast or ovarian cancer.",
         "A positive BRCA test does not mean that you will definitely get breast or ovarian cancer.",
         "women who do a gene test and find out that they have a BRCA gene change have some options",
         "find out that they have a BRCA gene change",
         "If you have a BRCA gene change, talk with your doctor.",
         "Patient did not check with insurance regarding BRCA testing.",
         "Has not had BRCA testing.",
         "Breast Cancer (BRCA) Gene Testing: Care Instructions",
         "To check with insurance regarding coverage for BRCA testing and notify the office if she would like to pursue.",
         "-- Patient 8 --",
         "Grade 3 Nottingham Score BRCA: Oncotype Dx:",
         "-- Patient 9 --",
         "Grade 2 Nottingham Score 7 BRCA:  1- and 2-  Oncotype Dx: basal phenotype",
         "-- Patient 10 --",
         "She has had BRCA testing and is positive for BRCA 1 mutation.",
         "she tested positive for BRCA 1 mutation with 187 deletion Ashkenazi Jewish mutation.",
         "BRCA1 mutation carrier.",
         "She is a BRCA1 mutation carrier with 187 deletion, Ashkenazi Jewish mutation.",
         "She has BRCA 1 mutation and is s/p bilateral",
         "Triple negative, BRCA1 positive breast cancer s/p neoadjuvant",
         "In terms of her BRCA mutation.",
         "She does not wish to make her family in [ADDRESS] aware of her BRCA mutation.",
         "She is BRCA carrier and NED from TNBC",
         "Other issues reviewed: Given BRCA discussed at length 40% risk of ovarian ca",
         "1.  BRCA 1 positive breast cancer status post neoadjuvant chemotherapy",
         "discuss prophylactic bilateral salping-oophorectomy given her BRCA 1 mutation.",
         "Known BRCA 1 mutation",
         "had a test for a gene mutation and is found to be BRCA positive.",
         "bilateral mastectomies due to BRCA gene mutation positivity.",
         "Unfortunately, she is also known to be BRCA-positive.",
         "----------------------------------",


         "Patient is ER+, PR-",
         "Test results for ER, PR, HER2 are all negative",
         "Elevated PSA values",

            "BRCA 1 mutation noted",
            "ovarian cancer and BRCA I mutation",
            "39 yo w/ BRCA 1 mutation",
            "She was noted to have BRCA mutation",

            "has been tested for BRCA1 and 2 and both were negative.",
            "of note does not have a BRCA",
            "She is BRCA negative",
            "negative for deleterious BRCA1 or BRCA2 gene mutation",
            "no deleterious mutations of BRCA 1 or 2",
            "negative for BRCA1 or BRCA2 gene mutation",
            "My Risk (BRCA) negative",
            "BRCA testing negative",
            "BRCA negative",
            "had a BRCA neg tested",
            "no mutations were detected through sequencing or deletion/duplication studies of the following genes: APC, ATM, BARD1, BMPR1A, BRCA1, BRCA2, BRIP1 ....",
            "Neg BRCA 1 and 2",
            "with lung mets (BRCA/BART neg)",
            "BRCA mutation was tested and she was told that it is negative",
            "reportedly tested negative for BRCA",
            "negative genetic testing for a BRCA1 or BRCA 2 mutation",
            "patient was negative for BRCA1 or 2 mutations",
            "Genetics: BRCA wt",
            "testing included the following genes: APC, ATM, BARD1, BMPR1A, BRCA1, BRCA2, BRIP1, PTEN and TP53.  No pathogenic variants were detected",
            "(to discuss results) which included the following genes: APC, ATM, BARD1, BMPR1A, BRCA1, BRCA2, BRIP1, PTEN and TP53. Sequencing and deletion/duplication studies did not identify any pathogenic variants (mutations).",

         "recommend that she have BRCA testing given that if she is BRCA positive",
         "She was also recommended to have BRCA testing given that she has a positive family history",
         "Regardless of BRCA testing we recommend",

            "possibility of her having a BRCA mutation",
            "possibly as the result of a BRCA1/2 gene pathogenic mutation",
            "meets testing guidelines for BRCA1/2 analysis",
         "consider the possibility of a BRCA1/2 gene mutation",
         "meets testing guidelines for BRCA testing",
         "option of a BRCA analysis or a cancer gene panel",
            "set up for [DATE] for BRCA testing",
         "inquiring today about BRCA testing",
         "motivated to pursue BRCA1/2 analysis",
         "interested in BRCA testing",
            "only approved for stage III or IV ovarian cancer with BRCA mutant",
            "Authorization granted for BRCA 1/2 testing",
            "patient meets criteria for BRCA testing",
         "candidate for BRCA testing;",
         "probably qualify for BRCA testing",
         "patient's BRCA testing has been denied",
         "Does not qualify for BRCA",
            "declined BRCA testing",
            "was not interested in BRCA testing",
            "does not want to do BRCA testing",
            "uninterested in BRCA testing",
            "The ATM, BRCA1, BRCA2, CDH1 and TP53 genes will be evaluated",
            "(sample obtained) for analysis of the ATM, BARD1, BRCA1, BRCA2, CDH1 ...",
            "not overly suspicious for hereditary predisposition to breast and ovarian cancer as a result of a BRCA1/2 gene mutation",

         "she would like to proceed with bilateral mastectomies with BRCA testing at a later date",
         "knowing her BRCA testing",
      "BRCA (Historical)",
            "BRCA -48-yr-old",
            "of newly diagnosed brca",
            "recently diagnosed brca",

      "BRCA1/2  Quest Diagnostics",
      "BRCA:",



         "HER2 IMMUNOHISTOCHEMISTRY: Using appropriate formalin fixed (8  96 hours),",
               "artificially reduce the HER2:CEP17 ratio. Some clinical trials (like the N9831",
               "trastuzumab benefit is independent of HER2/centromere 17 ratio and chromosome",
               "17 copy number. Therefore, average HER2 gene copy numbers per cell are",
               "17 copy numbers may be more informative in assessing HER2 gene amplification.",
               "of > 2.6 along with average HER2 signals of 4 or more. An additional report",
               "with HER2:RARA and HER2:SMS ratios will follow.",
               "The ratio of HER2 gene to SMS gene (HER2:SMS) is 1.36.",
               "The ratio of HER2 gene to RARA gene (HER2:RARA) is 1.26.",
               "the HER2/NEU gene (see previous report on the same case, GIS#11-BC481) showed",
               "no amplification of the HER2/NEU gene and showed changes suggestive of",
               "signals per cell was 4.15.  The average number of HER2 signals per cell",
               "(determined previously) was 5.22.  The ratio of HER2/NEU signals (ERBB2) to",
               "SMS signals is determined to be 1.36.  The ratio of HER2/NEU signals (ERBB2)",
               "HER2 IMMUNOHISTOCHEMISTRY: Using appropriate formalin fixed (8  96 hours),",
               "Her2/neu x 1     A",
               "(***PATH-NUMBER[1]; 10/12/11) has not been tested for HER2. Please request HER2",
               "HER2 IMMUNOHISTOCHEMISTRY: Using appropriate formalin fixed (8  96 hours),",
               "Her2/neu x 1     A",
               "and HER2 Neu +2. Also noted was ductal carcinoma in situ, nuclear",
               "HER2 IMMUNOHISTOCHEMISTRY: Using appropriate formalin fixed (8  96 hours)",
               "HER2/NEU:      2+",
               "HER2/NEU (FISH):      Not amplified",
               "showed HER2:SMS ratio of 1.36 and HER2:RARA ratio of 1.26. Analysis  with SMS",
               "showed no amplification of the HER2/NEU gene and showed changes suggestive of",
               "SUBJECTIVE: Person4 is a 60 year old premenopausal female diagnosed with right breast IIIC (T2, N3, M0) IDC, ER positive, H score 150, PR negative, H score 0, and Her2 +2, Fish ratio 1.48, copy number 4.40 breast cancer.",
               "ASSESSMENT AND PLAN:  Person4 is a 60 year old premenopausal female diagnosed with right breast IIIC (T2, N3, M0) IDC, ER positive, H score 150, PR negative, H score 0, and Her2 +2, Fish ratio 1.48, copy number 4.40 breast cancer.",
               "carcinoma show a ratio of HER2 (ERBB2) gene to the centromere of chromosome 17",
               "of 1.18 and Signal to Nucleus Ratio (SNR) of 2.7 indicating HER2 (ERBB2)",
               "Probe: Pathvysion HER2 (ERBB2) DNA Probe",
               "Probe Description:  The LSI HER2 (ERBB2) DNA probe is a 190 Kb SpectrumOrange",
               "directly labeled fluorescent DNA probe specific for the HER2 (ERBB2) gene",
               "HER2 negative:  HER2 gene/chromosome 17 ratio of less than 1.8, or an average",
               "number of HER2 gene copies/cell (SNR) of 4 or less.",
               "HER2 positive:  HER2 gene/chromosome 17 ratio of greater than 2.2, or an",
               "average number of HER2 gene copies/cell (SNR) of 6 or greater.",
               "HER2 borderline: HER2 gene/chromosome 17 ratio of 1.8- 2.2, or an average",
               "number of HER2 gene copies/cell (SNR) of greater than 4 to less than 6.",
               "IHER2 x 1     (none)",
               "The Her2/neu test is performed using Pathway HER-2/neu IVD monoclonal primary",
               "HER2/NEU:      2+"
   };

   public static void main( String[] args ) {
      for ( String example : TEST_TEXT ) {
         testText( example );
      }
   }

   static private void testText( final String text ) {
      final List<Pair<Integer>> sentenceSpans = Collections.singletonList( new Pair<>( 0, text.length() ) );
      final Collection<Integer> annotationBegins = Collections.emptyList();
      boolean found = false;
      for ( Biomarker biomarker : Biomarker.values() ) {
         // Find biomarkers and values by regex.
         // Don't worry about repeats - they should increase confidence.
         final List<Pair<Integer>> biomarkerSpans = findBiomarkerSpans( biomarker, text );
         for ( Pair<Integer> biomarkerSpan : biomarkerSpans ) {
            found = true;
            final String biomarkerText = text.substring( biomarkerSpan.getValue1(), biomarkerSpan.getValue2() );
            final Pair<Integer> valueSpan = getBiomarkerValueSpan( biomarker, text, biomarkerSpan, sentenceSpans, annotationBegins );
            final String valueText = NO_SPAN.equals( valueSpan ) ? "" : text.substring( valueSpan.getValue1(), valueSpan.getValue2() );
            if ( !valueText.isEmpty() ) {
               final String normalText = String.join( ";", BiomarkerNormalizer.getNormalValues( valueText ) );
               System.out.println( biomarker.name()
                     + " == " + normalText
                     + "   [" + biomarkerText + "] {"
                     + text.substring( valueSpan.getValue1(), valueSpan.getValue2() ) + "}    \"" + text );
            } else {
               System.out.println( biomarker.name() + "   [" + biomarkerText + "]    \"" + text );
            }
         }
      }
      if ( !found ) {
         System.out.println( "  Nothing found for " + text );
      }
   }


}

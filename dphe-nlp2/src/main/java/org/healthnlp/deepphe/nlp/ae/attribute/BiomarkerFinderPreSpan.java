package org.healthnlp.deepphe.nlp.ae.attribute;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.ner.creator.AnnotationCreator;
import org.apache.ctakes.ner.creator.DpheAnnotationCreator;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
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
final public class BiomarkerFinderPreSpan extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "BiomarkerFinder" );


   static private final Pair<Integer> NO_SPAN = new Pair<>( -1, -1 );

   static private final String REGEX_METHOD
         = "IHC|Immunohistochemistry|ISH|(?:IN SITU HYBRIDIZATION)|(?:DUAL ISH)"
           + "|FISH|(?:Fluorecent IN SITU HYBRIDIZATION)|(?:Nuclear Staining)";
   // for

   static private final String REGEX_TEST = "(?:mutation )?(?:Test|Method|Analysis|Status)";

   static private final String REGEX_LEVEL = "Level|status|expression|result|results|score";

//   static private final String REGEX_IS = "is|are|was";

   static private final String REGEX_STRONGLY = "weakly|strongly|greatly";
   static private final String REGEX_ELEVATED = "rising|increasing|elevated|elvtd|raised|increased|strong|amplified";
   static private final String REGEX_FALLING = "falling|decreasing|low|lowered|decreased|weak";
   static private final String REGEX_STABLE = "stable";


   static private final String REGEX_GT_LT = "(?:(?:Greater|>|Higher|Less|<|Lower)(?: than ?)?)?"
                                             + "(?: or )?(?:Greater|>|Higher|Less|<|Lower|Equal|=)(?: than|to "
                                             + "?)?";

//   static private final String REGEX_POSITIVE = "\\+?pos(?:itive|itivity)?|\\+(?:pos)?|overexpression";
//   static private final String REGEX_NEGATIVE = "\\-?neg(?:ative)?|\\-(?:neg)?|(?:not amplified)|(?:no [a-z] detected)";
   static private final String REGEX_POSITIVE = "\\+?pos(?:itive|itivity)?|overexpression|express|present|noted|w\\/ ";
   static private final String REGEX_NUM_POSITIVE = "3\\+";

   static private final String REGEX_NEGATIVE = "-?neg(?:ative)?|unamplified|(?:not amplified)|(?:no [a-z] detected)|"
                                                + "(?:non-? ?detected)|(?:not detected)|(?:not expressed)" +
         "|(?:not present)|(?:does not have)|(?:absent)|w\\/o";
   static private final String REGEX_NUM_NEGATIVE = "0|1\\+";

   static private final String REGEX_MUTANT = "(?:mutation (?:noted|present))";
   static private final String REGEX_NOT_MUTANT = "(?:not? mutant)|(?:mutation negative)"
         + "|(?:(?:no (?:deleterious |pathogenic )?(?:mutation|variant)s?))|(?:Wild ?-?type)|wt";

   static private final String REGEX_UNKNOWN
//         = "unknown|indeterminate|equivocal|borderline|(?:not assessed|requested|applicable)|\\sN\\/?A\\s";
         = "unknown|indeterminate|equivocal|borderline";
   static private final String REGEX_NUM_BORDERLINE = "2\\+";

   static private final String REGEX_NOT_ASSESSED
         = "(?:not assessed|requested|applicable)|insufficient|pending|\\sN\\/?A";


   static private final String REGEX_POS_NEG = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")";

   static private final String REGEX_POS_NEG_UNK
         = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")|(?:" + REGEX_UNKNOWN + ")";

   static private final String REGEX_POS_NEG_UNK_MUTANT
         = REGEX_POS_NEG_UNK + "|(?:" + REGEX_NOT_MUTANT + ")|(?:" + REGEX_MUTANT + ")";

   static private final String REGEX_POS_NEG_UNK_NA
         = "(?:" + REGEX_POSITIVE
         + ")|(?:" + REGEX_NEGATIVE
         + ")|(?:" + REGEX_UNKNOWN
         + ")|(?:" + REGEX_NOT_ASSESSED + ")";
   static private final String REGEX_POS_NEG_UNK_NA_MUTANT
         = REGEX_POS_NEG_UNK_NA + "|(?:" + REGEX_NOT_MUTANT + ")|(?:" + REGEX_MUTANT + ")";
//         = "(?:" + REGEX_POSITIVE
//         + ")|(?:" + REGEX_NEGATIVE
//         + ")|(?:" + REGEX_UNKNOWN
//         + ")|(?:" + REGEX_NOT_ASSESSED + ")";
   static private final String REGEX_POS_NEG_UNK_NA_NUM
         = "(?:" + REGEX_POSITIVE
           + ")|(?:" + REGEX_NUM_POSITIVE
           + ")|(?:" + REGEX_NEGATIVE
           + ")|(?:" + REGEX_NUM_NEGATIVE
           + ")|(?:" + REGEX_UNKNOWN
           + ")|(?:" + REGEX_NUM_BORDERLINE
           + ")|(?:" + REGEX_NOT_ASSESSED
         + ")|(?:" + REGEX_NOT_MUTANT + ")";;

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
            "(?:BRCA(?: ?1)?|BROVCA1|(?:Breast Cancer Type 1))"
                  + "(?: Susceptibility)?(?: Gene)?(?: Polymorphism)?",
            20, "",
            REGEX_POS_NEG_UNK_NA_MUTANT, true ),

      BRCA2( "BreastCancerType2SusceptibilityProtein", "Breast Cancer Type 2 Susceptibility Protein",
            GENE_PRODUCT, "C2973986",
            Collections.singletonList( "BRCA2Gene" ),
            Collections.singletonList( "BRCA2PolymorphismPositive" ),
            Arrays.asList( "BRCA2GeneMutationNegative", "BRCA2WtAllele" ),
            Collections.emptyList(),
            Collections.singletonList( "BRCA2MutationAnalysis" ),
            "(?:BRCA(?: ?1(?: or| and|\\/))? ?2|BROVCA2|FANCD1|(?:Breast Cancer Type 2))"
                  + "(?: Susceptibility)?(?: Gene)?(?: Polymorphism)?",
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
            "(?:Serine\\/Threonine-Protein Kinase )?B-?RAF1?(?: Gene)?(?: mutation|fusion|rearrangement|alteration)?",
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
            "(?:KRAS\\+?-?|C-K-RAS\\+?-?|KRAS2\\+?-?|KRAS-2\\+?-?|V-KI-RAS2\\+?-?|(?:Kirsten Rat Sarcoma Viral Oncogene Homolog))"
            + "(?: Wild ?-?type|wt)?(?: Gene Mutation)?",
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
            "(?:N-?RAS1?\\+?-?)(?: gene)?(?: mutation)?",
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
      static private final AnnotationCreator _annotationCreator = new DpheAnnotationCreator();

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
         _plusMinus = REGEX_POS_NEG_UNK.equals( valueRegex );
      }
   }

   final private class BiomarkerFound {
      final Biomarker _biomarker;
      final String _value;
      private BiomarkerFound( final Biomarker biomarker, final String value ) {
         _biomarker = biomarker;
         _value = value;
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
                                                           .filter( a -> ( a instanceof EventMention
                                                                           || a instanceof AnatomicalSiteMention ) )
                                                           .map( IdentifiedAnnotation::getBegin )
                                                           .collect( Collectors.toList() );
      final Collection<Pair<Integer>> sentenceSpans
            = JCasUtil.select( jCas, Sentence.class )
                      .stream()
                      .map( s -> new Pair<>( s.getBegin(), s.getEnd() ) )
                      .collect( Collectors.toList() );
      for ( Biomarker biomarker : Biomarker.values() ) {
         // Take care of similar uris that were found by dictionary lookup.
         // Don't worry about repeats - they should increase confidence.
         handleExistingBiomarkers( jCas, biomarker, docText, sentenceSpans, annotationBegins );
         // Find biomarkers and values by regex.
         // Don't worry about repeats - they should increase confidence.
         final List<Pair<Integer>> biomarkerSpans = findBiomarkerSpans( biomarker, docText );
         addBiomarkerValues( jCas, biomarker, docText, biomarkerSpans, sentenceSpans, annotationBegins );
      }
   }


   static private void handleExistingBiomarkers( final JCas jCas, final Biomarker biomarker,
                                                 final String docText,
                                                 final Collection<Pair<Integer>> sentenceSpans,
                                                 final Collection<Integer> annotationBegins ) {
      final Collection<Integer> handledBegins = new HashSet<>();
      for ( String uri : biomarker._negativeUris ) {
         final Collection<IdentifiedAnnotation> annotations = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, uri );
         for ( IdentifiedAnnotation annotation : annotations ) {
            addBiomarkerAndValue( jCas, biomarker, annotation.getBegin(), annotation.getEnd(), annotation.getCoveredText() );
            handledBegins.add( annotation.getBegin() );
         }
      }
      for ( String uri : biomarker._positiveUris ) {
         final Collection<IdentifiedAnnotation> annotations = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, uri );
         for ( IdentifiedAnnotation annotation : annotations ) {
            addBiomarkerAndValue( jCas, biomarker, annotation.getBegin(), annotation.getEnd(), annotation.getCoveredText() );
            handledBegins.add( annotation.getBegin() );
         }
      }
      for ( String uri : biomarker._unknownUris ) {
         final Collection<IdentifiedAnnotation> annotations = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, uri );
         for ( IdentifiedAnnotation annotation : annotations ) {
            addBiomarkerAndValue( jCas, biomarker, annotation.getBegin(), annotation.getEnd(), annotation.getCoveredText() );
            handledBegins.add( annotation.getBegin() );
         }
      }
      // For tests and generic mentions, attempt to add a value by regex but if one isn't found add the span anyway.
      // Let normalization handle the span for generic mentions.
      for ( String uri : biomarker._testUris ) {
         final Collection<IdentifiedAnnotation> annotations = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, uri );
         for ( IdentifiedAnnotation annotation : annotations ) {
            if ( !handledBegins.contains( annotation.getBegin() ) ) {
               final boolean handled = addBiomarkerValue( jCas, biomarker, docText, new Pair<>( annotation.getBegin(), annotation.getEnd() ),
                     sentenceSpans, annotationBegins );
               if ( !handled ) {
                  addBiomarkerAndValue( jCas, biomarker, annotation.getBegin(), annotation.getEnd(), annotation.getCoveredText() );
               }
               handledBegins.add( annotation.getBegin() );
            }
         }
      }
      for ( String uri : biomarker._uris ) {
         final Collection<IdentifiedAnnotation> annotations = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, uri );
         for ( IdentifiedAnnotation annotation : annotations ) {
            if ( !handledBegins.contains( annotation.getBegin() ) ) {
               final boolean handled = addBiomarkerValue( jCas, biomarker, docText, new Pair<>( annotation.getBegin(), annotation.getEnd() ),
                     sentenceSpans, annotationBegins );
               if ( !handled ) {
                  addBiomarkerAndValue( jCas, biomarker, annotation.getBegin(), annotation.getEnd(), annotation.getCoveredText() );
               }
               handledBegins.add( annotation.getBegin() );
            }
         }
      }
      final Collection<IdentifiedAnnotation> exact = Neo4jOntologyConceptUtil.getAnnotationsByUri( jCas, biomarker._uri );
      for ( IdentifiedAnnotation annotation : exact ) {
         if ( !handledBegins.contains( annotation.getBegin() ) ) {
            final boolean handled = addBiomarkerValue( jCas, biomarker, docText, new Pair<>( annotation.getBegin(), annotation.getEnd() ),
                  sentenceSpans, annotationBegins );
            if ( !handled ) {
               addBiomarkerAndValue( jCas, biomarker, annotation.getBegin(), annotation.getEnd(), annotation.getCoveredText() );
            }
         }
      }
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

   // Why the heck don't word boundaries ever work in java?!
   static private boolean isWholeWord( final String text, final Pair<Integer> span ) {
      return isWholeWord( text, span.getValue1(), span.getValue2() );
   }

   // Why the heck don't word boundaries ever work in java?!
   static private boolean isWholeWord( final String text, final int begin, final int end ) {
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


   static private void addBiomarkerValues( final JCas jCas,
                                           final Biomarker biomarker,
                                           final String docText,
                                           final List<Pair<Integer>> biomarkerSpans,
                                           final Collection<Pair<Integer>> sentenceSpans,
                                           final Collection<Integer> annotationBegins ) {
      if ( biomarkerSpans.isEmpty() ) {
         return;
      }
      for ( Pair<Integer> biomarkerSpan : biomarkerSpans ) {
         addBiomarkerValue( jCas, biomarker, docText, biomarkerSpan, sentenceSpans, annotationBegins );
      }
   }


   static private boolean addBiomarkerValue( final JCas jCas,
                                          final Biomarker biomarker,
                                          final String text,
                                          final Pair<Integer> biomarkerSpan,
                                          final Collection<Pair<Integer>> sentenceSpans,
                                          final Collection<Integer> annotationBegins ) {
      final Pair<Integer> sentenceSpan = getSentenceSpan( biomarkerSpan, sentenceSpans );
      final int followingAnnotation = getFollowingAnnotation( biomarkerSpan, text.length(), annotationBegins );
      if ( addValueFollowed( jCas, biomarker, text, biomarkerSpan, sentenceSpan, followingAnnotation ) ) {
         return true;
      }
      if ( biomarker._canPrecede ) {
         final int precedingAnnotation = getPrecedingAnnotation( biomarkerSpan, annotationBegins );
         return addValuePreceded( jCas, biomarker, text, biomarkerSpan, sentenceSpan, precedingAnnotation );
      }
      return false;
   }

   static private boolean addValueFollowed( final JCas jCas,
                                            final Biomarker biomarker,
                                            final String text,
                                            final Pair<Integer> biomarkerSpan,
                                            final Pair<Integer> sentenceSpan,
                                            final int followingAnnotation ) {
      if ( biomarker._plusMinus ) {
         final char c = text.charAt( biomarkerSpan.getValue2()-1 );
         if ( (c == '+' || c == '-') && isWholeWord( text, biomarkerSpan ) ) {
            addBiomarkerAndValue( jCas, biomarker, biomarkerSpan.getValue1(), biomarkerSpan.getValue2(),
                  text.substring( biomarkerSpan.getValue1(), biomarkerSpan.getValue2() ) );
            return true;
         }
      }

      final String nextText = getFollowingText( biomarker, biomarkerSpan, text, sentenceSpan, followingAnnotation );
      if ( nextText.isEmpty() ) {
         return false;
      }
      if ( biomarker._checkSkip ) {
         final Matcher skipMatcher = biomarker._skipPattern.matcher( nextText );
         if ( skipMatcher.find() ) {
            return false;
         }
      }
      final Matcher matcher = biomarker._valuePattern.matcher( nextText );
      if ( matcher.find() ) {
         final int matchBegin = biomarkerSpan.getValue2() + matcher.start();
         final int matchEnd = biomarkerSpan.getValue2() + matcher.end();
         if ( isWholeWord( text, matchBegin, matchEnd ) ) {
            addBiomarkerAndValue( jCas, biomarker, matchBegin, matchEnd, text.substring( matchBegin, matchEnd ) );
            return true;
         }
      }
      return false;
   }

   static private boolean addValuePreceded( final JCas jCas,
                                            final Biomarker biomarker,
                                            final String text,
                                            final Pair<Integer> biomarkerSpan,
                                            final Pair<Integer> sentenceSpan,
                                            final int precedingAnnotation ) {
      if ( !biomarker._canPrecede ) {
         return false;
      }
      final String prevText = getPrecedingText( biomarker, biomarkerSpan, text, sentenceSpan, precedingAnnotation );
      if ( prevText.isEmpty() ) {
         return false;
      }
      final Matcher matcher = biomarker._valuePattern.matcher( prevText );
      Pair<Integer> lastMatch = null;
      while ( matcher.find() ) {
         lastMatch = new Pair<>( matcher.start(), matcher.end() );
      }
      if ( lastMatch == null ) {
         return false;
      }
      final int matchBegin = biomarkerSpan.getValue1() - prevText.length() + lastMatch.getValue1();
      final int matchEnd = biomarkerSpan.getValue1() - prevText.length() + lastMatch.getValue2();
      if ( isWholeWord( text, matchBegin, matchEnd ) ) {
         addBiomarkerAndValue( jCas, biomarker, matchBegin, matchEnd, text.substring( matchBegin, matchEnd ) );
         return true;
      }
      return false;
   }

   static private Pair<Integer> getValueSpan( final Biomarker biomarker,
                                              final String text,
                                              final Pair<Integer> biomarkerSpan,
                                              final Pair<Integer> sentenceSpan,
                                              final Collection<Integer> annotationBegins ) {
      final int followingAnnotation = getFollowingAnnotation( biomarkerSpan, text.length(), annotationBegins );
      final Pair<Integer> followingValueSpan
            = getFollowingValueSpan( biomarker, text, biomarkerSpan, sentenceSpan, followingAnnotation );
      if ( !NO_SPAN.equals( followingValueSpan ) ) {
         return followingValueSpan;
      }
      if ( biomarker._canPrecede ) {
         final int precedingAnnotation = getPrecedingAnnotation( biomarkerSpan, annotationBegins );
         return getPrecedingValueSpan( biomarker, text, biomarkerSpan, sentenceSpan, precedingAnnotation );
      }
      return NO_SPAN;
   }

   static private Pair<Integer> getFollowingValueSpan( final Biomarker biomarker,
                                            final String text,
                                            final Pair<Integer> biomarkerSpan,
                                            final Pair<Integer> sentenceSpan,
                                            final int followingAnnotation ) {
      if ( biomarker._plusMinus ) {
         final char c = text.charAt( biomarkerSpan.getValue2()-1 );
         if ( (c == '+' || c == '-') && isWholeWord( text, biomarkerSpan ) ) {
            return new Pair<>( biomarkerSpan.getValue2()-1, biomarkerSpan.getValue2() );
         }
      }

      final String nextText = getFollowingText( biomarker, biomarkerSpan, text, sentenceSpan, followingAnnotation );
      if ( nextText.isEmpty() ) {
         return NO_SPAN;
      }
      if ( biomarker._checkSkip ) {
         final Matcher skipMatcher = biomarker._skipPattern.matcher( nextText );
         if ( skipMatcher.find() ) {
            return NO_SPAN;
         }
      }
      final Matcher matcher = biomarker._valuePattern.matcher( nextText );
      if ( matcher.find() ) {
         final int matchBegin = biomarkerSpan.getValue2() + matcher.start();
         final int matchEnd = biomarkerSpan.getValue2() + matcher.end();
         if ( isWholeWord( text, matchBegin, matchEnd ) ) {
            return new Pair<>( matchBegin, matchEnd );
         }
      }
      return NO_SPAN;
   }


   static private Pair<Integer> getPrecedingValueSpan( final Biomarker biomarker,
                                                       final String text,
                                                       final Pair<Integer> biomarkerSpan,
                                                       final Pair<Integer> sentenceSpan,
                                                       final int precedingAnnotation ) {
      if ( !biomarker._canPrecede ) {
         return NO_SPAN;
      }
      final String prevText = getPrecedingText( biomarker, biomarkerSpan, text, sentenceSpan, precedingAnnotation );
      if ( prevText.isEmpty() ) {
         return NO_SPAN;
      }
      final Matcher matcher = biomarker._valuePattern.matcher( prevText );
      Pair<Integer> lastMatch = null;
      while ( matcher.find() ) {
         lastMatch = new Pair<>( matcher.start(), matcher.end() );
      }
      if ( lastMatch == null ) {
         return NO_SPAN;
      }
      final int matchBegin = biomarkerSpan.getValue1() - prevText.length() + lastMatch.getValue1();
      final int matchEnd = biomarkerSpan.getValue1() - prevText.length() + lastMatch.getValue2();
      if ( isWholeWord( text, matchBegin, matchEnd ) ) {
         return new Pair<>( matchBegin, matchEnd );
      }
      return NO_SPAN;
   }

//   static private void addBiomarker( final JCas jCas,
//                                      final Biomarker biomarker,
//                                        final int begin, final int end ) {
////      LOGGER.info( "Adding Biomarker " + biomarker.name() + " "
////                   + jCas.getDocumentText().substring( valueSpanBegin,
////                                                       valueSpanEnd ) );
//      AnnotationFactory.createAnnotation( jCas, begin, end, DpheGroup.FINDING, biomarker.name(), "", biomarker.name() );
//      // Uses name as URI, value as span.
////      UriAnnotationFactory.createIdentifiedAnnotations( jCas,
////                                                        valueSpanBegin,
////                                                        valueSpanEnd,
////                                                        biomarker.name(),
////                                                        SemanticGroup.FINDING,
////                                                      "T033" );
//   }

   static private void addBiomarkerAndValue( final JCas jCas, final Biomarker biomarker,
                                             final int begin, final int end, final String value ) {
      AnnotationFactory.createAnnotation( jCas, begin, end, biomarker._group, biomarker._uri, biomarker._cui,
            biomarker._prefText, value );
   }


//         final String cui;
//      final String prefText;
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();
//      try ( Transaction tx = graphDb.beginTx() ) {
//         final Node graphNode = SearchUtil.getClassNode( graphDb, uri );
//         if ( graphNode == null ) {
////            LOGGER.warn( "No Class exists for URI " + uri );
//            return Collections.singletonList( createUnknownAnnotation( jcas, beginOffset, endOffset, uri ,
//                                                                       semanticTui ) );
//         }
//         cui = (String)graphNode.getProperty( CUI_KEY );
//         prefText = (String)graphNode.getProperty( PREF_TEXT_KEY );
//         tx.success();
//      } catch ( MultipleFoundException mfE ) {
//         LOGGER.error( mfE.getMessage(), mfE );
//         return Collections.singletonList( createUnknownAnnotation( jcas, beginOffset, endOffset, uri, semanticTui ) );
//      }

   static private Pair<Integer> getSentenceSpan( final Pair<Integer> biomarkerSpan,
                                                 final Collection<Pair<Integer>> sentenceSpans ) {
      return sentenceSpans.stream()
                         .filter( s -> s.getValue1() <= biomarkerSpan.getValue1()
                                       && biomarkerSpan.getValue2() <= s.getValue2() )
                         .findFirst()
                         .orElse( biomarkerSpan );
   }

   static private int getPrecedingAnnotation( final Pair<Integer> biomarkerSpan,
                                               final Collection<Integer> annotationBegins ) {
      return annotationBegins.stream()
                          .filter( b -> b < biomarkerSpan.getValue1() )
                             .mapToInt( b -> b )
                          .max()
                          .orElse( 0 );
   }

   static private int getFollowingAnnotation( final Pair<Integer> biomarkerSpan,
                                              final int textLength,
                                              final Collection<Integer> annotationBegins ) {
      return annotationBegins.stream()
                             .filter( b -> b >= biomarkerSpan.getValue2() )
                             .mapToInt( b -> b )
                             .min()
                             .orElse( textLength );
   }

   static private String getPrecedingText( final Biomarker biomarker,
                                           final Pair<Integer> biomarkerSpan,
                                           final String text,
                                           final Pair<Integer> sentenceSpan,
                                           final int precedingAnnotation ) {
      final int sentenceOrAnnotation = Math.max( precedingAnnotation, sentenceSpan.getValue1() );
//      final int windowSize = Math.max( 0, biomarkerSpan.getValue1() - biomarker._windowSize );
      final String prevText = text.substring( sentenceOrAnnotation, biomarkerSpan.getValue1() );
      // Check for end of paragraph
      final int pIndex = prevText.lastIndexOf( "\n\n" );
      if ( pIndex >= 0 ) {
         return prevText.substring( pIndex+2 );
      }
      return prevText;
   }


   static private String getFollowingText( final Biomarker biomarker,
                                            final Pair<Integer> biomarkerSpan,
                                            final String text,
                                           final Pair<Integer> sentenceSpan,
                                           final int followingAnnotation ) {
      final int sentenceOrAnnotation = Math.min( followingAnnotation, sentenceSpan.getValue2() );
      String nextText = text.substring( biomarkerSpan.getValue2(), sentenceOrAnnotation );
//      final int windowSize = Math.min( text.length(), biomarkerSpan.getValue2() + biomarker._windowSize );
//      String nextText = text.substring( biomarkerSpan.getValue2(), windowSize );
      // Check for end of paragraph
      final int pIndex = nextText.indexOf( "\n\n" );
      if ( pIndex == 0 ) {
         return "";
      }
      // Sometimes value sets are in brackets.  e.g.  "ER: [pos;neg;unk] = neg"
      final int startBracket = nextText.indexOf( '[' );
      if ( startBracket >= 0 ) {
         final int endBracket = nextText.indexOf( ']', startBracket );
         if ( endBracket > 0 ) {
            final char[] chars = nextText.toCharArray();
            for ( int i=startBracket+1; i<endBracket; i++ ) {
               chars[ i ] = 'V';
            }
            nextText = new String( chars );
         }
      }

      if ( pIndex > 0 ) {
         return nextText.substring( 0, pIndex );
      }
      return nextText;
   }

   static private final String[] TEST_TEXT = {
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
      
      "patient's BRCA testing has been denied",
            "Does not qualify for BRCA",
            "inquiring today about BRCA testing",
            "possibility of her having a BRCA mutation",
            "possibly as the result of a BRCA1/2 gene pathogenic mutation",
            "meets testing guidelines for BRCA1/2 analysis",
            "set up for [DATE] for BRCA testing",
            "meets testing guidelines for BRCA testing",
            "motivated to pursue BRCA1/2 analysis",
            "consider the possibility of a BRCA1/2 gene mutation",
            "option of a BRCA analysis or a cancer gene panel",
            "only approved for stage III or IV ovarian cancer with BRCA mutant",
            "Authorization granted for BRCA 1/2 testing",
            "patient meets criteria for BRCA testing",
            "interested in BRCA testing",
            "declined BRCA testing",
            "was not interested in BRCA testing",
            "does not want to do BRCA testing",
            "candidate for BRCA testing;",
            "knowing her BRCA testing",
            "uninterested in BRCA testing",
            "probably qualify for BRCA testing",
            "The ATM, BRCA1, BRCA2, CDH1 and TP53 genes will be evaluated",
            "(sample obtained) for analysis of the ATM, BARD1, BRCA1, BRCA2, CDH1 ...",
            "not overly suspicious for hereditary predisposition to breast and ovarian cancer as a result of a BRCA1/2 gene mutation",

      "BRCA (Historical)",
            "BRCA -48-yr-old",
            "of newly diagnosed brca",
            "recently diagnosed brca",

      "BRCA1/2  Quest Diagnostics",
      "BRCA:"

   };

   public static void main( String[] args ) {
      for ( String example : TEST_TEXT ) {
         testText( example );
      }
   }

   static private void testText( final String text ) {
      final Pair<Integer> sentenceSpan = new Pair<>( 0, text.length() );
      final Collection<Integer> annotationBegins = Collections.emptyList();
      boolean found = false;
      for ( Biomarker biomarker : Biomarker.values() ) {
         // Find biomarkers and values by regex.
         // Don't worry about repeats - they should increase confidence.
         final List<Pair<Integer>> biomarkerSpans = findBiomarkerSpans( biomarker, text );
         for ( Pair<Integer> biomarkerSpan : biomarkerSpans ) {
            found = true;
            final String biomarkerText = text.substring( biomarkerSpan.getValue1(), biomarkerSpan.getValue2() );
            final Pair<Integer> valueSpan = getValueSpan( biomarker, text, biomarkerSpan, sentenceSpan, annotationBegins );
            final String valueText = NO_SPAN.equals( valueSpan ) ? "" : text.substring( valueSpan.getValue1(), valueSpan.getValue2() );
            if ( !valueText.isEmpty() ) {
               final String normalText = String.join( ";", BiomarkerNormalizer.getNormalValues( valueText ) );
               System.out.println( biomarker.name()
                     + " == " + normalText
                     + "   [" + biomarkerText + "] {"
                     + text.substring( valueSpan.getValue1(), valueSpan.getValue2() ) + "}    \"" + text );
            } else {
               final String normalText = String.join( ";", BiomarkerNormalizer.getNormalValues( text ) );
               System.out.println( biomarker.name() + " ~~ " + normalText + "   [" + biomarkerText + "]    \"" + text );
            }
         }
      }
      if ( !found ) {
         System.out.println( "  Nothing found for " + text );
      }

   }


}

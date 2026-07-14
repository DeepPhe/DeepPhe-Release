package org.healthnlp.deepphe.nlp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ProtocolValueCounter {

    static private final String ALL_COLUMNS = "PatientID|famhx_anyca|famhx_brca|famhx_cvca|fhx_skinca|ca_breast|ca_ovary|ca_skin|ca_mel|phx_maligna|phx_nevi|phx_sun|phx_tan|comorb_htn|comorb_diabetes|comorb_heart|comorb_lung|comorb_liver|comorb_kidney|comorb_autoimm|mel_ulcer|ca_other|ca_hist_emr_brca|ca_hist_emr_ovca|ca_stage_sum|ca_grade_emr|ecog|mel_stage|mel_type|mel_hist|cut_mel_site|mel_met|ER|PR|HER2|BRCA1|BRCA2|PIK3CA|TP53|BRAF|NRAS|KIT|CTTNB1|GNA11|GNAQ|NF1|episodes|comorb_smk|doc_count";
    static private final List<String> COLUMNS = Arrays.asList( ALL_COLUMNS.split("\\|"));

    static private final int ca_skin = COLUMNS.indexOf( "ca_skin" );
    static private final int ca_mel = COLUMNS.indexOf( "ca_mel" );
    static private final int phx_maligna = COLUMNS.indexOf( "phx_maligna" );
    static private final int phx_nevi = COLUMNS.indexOf( "phx_nevi" );


    public static void main(String[] args) {
        final String dir = "C:\\spiffy\\protocol\\nlp_output_Feb_07_2025\\unzipped";
        final List<Map<String, Integer>> valueRows = new ArrayList<>(COLUMNS.size());
        for (int i = 0; i < COLUMNS.size(); i++) {
            valueRows.add(new HashMap<>());
        }
        for (File file : new File(dir).listFiles()) {
            System.out.println( "Reading " + file.getName() );
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line == null) {
                    System.exit(0);
                }
                while (line != null) {
                    final List<String> infos = Arrays.asList( line.split("\\|") );
                    if ( infos.get(0).equals(COLUMNS.get(0))) {
                        line = reader.readLine();
                        continue;
                    }
                    boolean melanoma = infos.get(ca_skin).contains( "yes" ) || infos.get(ca_mel).contains( "yes" )
                            || infos.get(phx_maligna).contains( "yes" ) || infos.get(phx_nevi).contains( "yes" ) ;
                    if ( !melanoma ) {
                        line = reader.readLine();
                        continue;
                    }
                    for (int i = 1; i < infos.size(); i++) {
                        final String[] values = infos.get(i).split(";");
                        for (String value : values) {
                            if (value.contains(",")) {
                                final Map<String, Integer> valueMap = valueRows.get(i);
                                final String vName = value.split(",")[0];
                                final int count = valueMap.getOrDefault(vName, 0);
                                valueMap.put(vName, count + 1);
                            }
                        }
                    }
                    line = reader.readLine();
                }
            } catch (IOException ioE) {
                System.err.println(ioE.getMessage());
            }
        }
        for (int i = 1; i < COLUMNS.size(); i++) {
            System.out.println(COLUMNS.get(i));
            final List<String> vNames = valueRows.get(i).keySet().stream().sorted().collect(Collectors.toList());
            for (String name : vNames) {
                int count = valueRows.get(i).getOrDefault(name, 0);
                System.out.println("   " + name + " : " + count);
            }
        }
    }
}

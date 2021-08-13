package org.healthnlp.deepphe.neo4j.util;

//for monday...i think the patient summary is generating the random patient ids, and then when the timeline goes to find that patient it's generating new patient ids and cant find a match
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.healthnlp.deepphe.neo4j.node.NewStructuredPatientData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.*;

public class StructuredPatientDataGenerator implements Iterable<NewStructuredPatientData> {
    final Random firstNameRand,
            lastNameRand,
            firstEncounterDateRand,
            genderRand,
            lastEncounterDateRand,
            birthDateRand,
            dayRand,
            monthRand;

    final String[] firstnames = new String[]{"mary", "patricia", "linda", "barbara", "elizabeth", "jennifer", "maria", "susan", "margaret", "dorothy", "lisa", "nancy", "karen", "betty", "helen", "sandra", "donna", "carol", "ruth", "sharon", "michelle", "laura", "sarah", "kimberly", "deborah", "jessica", "shirley", "cynthia", "angela", "melissa", "brenda", "amy", "anna", "rebecca", "virginia", "kathleen", "pamela", "martha", "debra", "amanda", "stephanie", "carolyn"};

    final String[] lastnames = new String[]{"anderson", "ashwoon", "aikin", "bateman", "bongard", "bowers", "boyd", "cannon", "cast", "deitz", "dewalt", "ebner", "frick", "hancock", "haworth", "hesch", "hoffman", "kassing", "knutson", "lawless", "lawicki", "mccord", "mccormack", "miller", "myers", "nugent", "ortiz", "orwig", "ory", "paiser", "pak", "pettigrew", "quinn", "quizoz", "ramachandran", "resnick", "sagar", "schickowski", "schiebel", "sellon", "severson", "shaffer", "solberg", "soloman", "sonderling", "soukup", "soulis", "stahl", "sweeney", "tandy", "trebil", "trusela", "trussel", "turco", "uddin", "uflan", "ulrich", "upson", "vader", "vail", "valente", "van zandt", "vanderpoel", "ventotla", "vogal", "wagle", "wagner", "wakefield", "weinstein", "weiss", "woo", "yang"};

    final Double MEAN_FOR_ENCOUNTERS_IN_YEARS = 1d; //will be within dist*std*mean from dob
    final Double STDDEV_FOR_ENCOUNTERS_IN_YEARS = 0.5d;

    final Double MEAN_FOR_BIRTHDATE_IN_YEARS = 55d; //will be within dist*std*mean from dob
    final Double STDDEV_FOR_BIRTHDATE_IN_YEARS = 8d;

    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static final long MILLS_IN_A_YEAR = 31556952000L;

    private Integer patientId = 1;
    final Random rand;

    final long rightNow = (new Date()).getTime();

    public StructuredPatientDataGenerator(long seed) {
        this.rand = new Random(seed);
        firstNameRand = new Random(rand.nextLong());
        lastNameRand = new Random(rand.nextLong());
        firstEncounterDateRand = new Random(rand.nextLong());
        genderRand = new Random(rand.nextLong());
        lastEncounterDateRand = new Random(rand.nextLong());
        birthDateRand = new Random(rand.nextLong());
        dayRand = new Random(rand.nextLong());
        monthRand = new Random(rand.nextLong());
    }


    private String getNextFirstName() {
        return firstnames[firstNameRand.nextInt(firstnames.length)];
    }
    private String getNextLastName() {
        return lastnames[lastNameRand.nextInt(lastnames.length)];
    }


    private String getDateOfEncounter(long ageInMillisRightNow, long ageAtTimeOfVisitInMillis) {
        Calendar c = new GregorianCalendar();
        c.add(Calendar.YEAR, -5);
        Integer yearsSinceBirth = Math.round(ageInMillisRightNow / MILLS_IN_A_YEAR);
        Integer ageAtEncounter = Math.round(ageAtTimeOfVisitInMillis / MILLS_IN_A_YEAR);
        c.add(Calendar.YEAR, ageAtEncounter-yearsSinceBirth);
        c.set(Calendar.MONTH, monthRand.nextInt(11)+1);
        YearMonth yearMonthObject = YearMonth.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
        int daysInMonth = yearMonthObject.lengthOfMonth(); //28
        c.set(Calendar.DAY_OF_MONTH, dayRand.nextInt(daysInMonth));
        return simpleDateFormat.format(c.getTime());
    }

    private String[] getNextFirstEncounterDate(long ageInMillisRightNow) {
        String[] result = new String[2];
        long millisSinceFirstVisit =  MILLS_IN_A_YEAR * Math.round(firstEncounterDateRand.nextGaussian() * STDDEV_FOR_ENCOUNTERS_IN_YEARS + MEAN_FOR_ENCOUNTERS_IN_YEARS);
        long ageAtFirstVisitInMillis = ageInMillisRightNow - millisSinceFirstVisit;
        result[0] = getDateOfEncounter(ageInMillisRightNow, ageAtFirstVisitInMillis);

        long millisUntilLastVisit =  Math.abs(MILLS_IN_A_YEAR * Math.round(lastEncounterDateRand.nextGaussian() * STDDEV_FOR_ENCOUNTERS_IN_YEARS + MEAN_FOR_ENCOUNTERS_IN_YEARS));
        long ageAtLastVisitInMillis = ageAtFirstVisitInMillis + millisUntilLastVisit;
        result[1] = getDateOfEncounter(ageInMillisRightNow, ageAtLastVisitInMillis);

        return result;

    }


    private String getNextGender() {
       return (genderRand.nextInt(1) == 0) ?  "M" :  "F";
    }
    //returns years
    private long getNextAgeInMillis() {
        return MILLS_IN_A_YEAR * Math.round(rand.nextGaussian() * STDDEV_FOR_BIRTHDATE_IN_YEARS + MEAN_FOR_BIRTHDATE_IN_YEARS);
    }

    private String getBirthDate(long birthDateInMillisSinceBirth) {
        Calendar c = new GregorianCalendar();
        c.add(Calendar.YEAR, -5);
        Integer yearsSinceBirth = Math.round(birthDateInMillisSinceBirth / MILLS_IN_A_YEAR);
        c.add(Calendar.YEAR, -yearsSinceBirth);
        return simpleDateFormat.format(c.getTime());
    }

    public String getNextPatientId() {
        DecimalFormat decimalFormat = new DecimalFormat("00");
        return "patient" + decimalFormat.format(patientId++);
    }

    protected NewStructuredPatientData next() {
        NewStructuredPatientData structuredPatientData = new NewStructuredPatientData();
        long ageInMillisRightNow = getNextAgeInMillis();
        structuredPatientData.setBirthdate(getBirthDate(ageInMillisRightNow));
        structuredPatientData.setFirstname(getNextFirstName());
        structuredPatientData.setLastname(getNextLastName());
        structuredPatientData.setGender("F");
        structuredPatientData.setPatientId(getNextPatientId());
        String[] encounterDates = getNextFirstEncounterDate(ageInMillisRightNow);
        structuredPatientData.setFirstEncounterDate(encounterDates[0]);
        structuredPatientData.setLastEncounterDate(encounterDates[1]);
                return structuredPatientData;
    }

    public static void main(String[] args) {
        StructuredPatientDataGenerator structuredPatientDataGenerator = new StructuredPatientDataGenerator(0);

        List<NewStructuredPatientData> structuredPatientData = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
         structuredPatientData.add((structuredPatientDataGenerator.next()));
        }

        for (int i = 0; i <= 7; i++) {
             NewStructuredPatientData patient = structuredPatientDataGenerator.next();
             patient.setPatientId("fake_patient"+i);
            structuredPatientData.add(patient);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(structuredPatientData);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("../dphe-neo4j-plugin/fake_patient_structured_data.json"));
            bw.write(json);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Iterator<NewStructuredPatientData> iterator() {
        return new RandomStructuredPatientDataIterator(this);
    }


}


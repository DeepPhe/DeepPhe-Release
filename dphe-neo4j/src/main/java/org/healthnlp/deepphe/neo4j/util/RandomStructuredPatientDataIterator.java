package org.healthnlp.deepphe.neo4j.util;

import org.healthnlp.deepphe.neo4j.node.NewStructuredPatientData;


import java.util.Iterator;

public class RandomStructuredPatientDataIterator implements Iterator<NewStructuredPatientData> {

    private final StructuredPatientDataGenerator structuredPatientDataGenerator;

    public RandomStructuredPatientDataIterator(StructuredPatientDataGenerator structuredPatientDataGenerator) {
        this.structuredPatientDataGenerator = structuredPatientDataGenerator;
    }

    ;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public NewStructuredPatientData next() {
        return structuredPatientDataGenerator.next();
    }
}

package org.apache.ctakes.cancer.ae;


import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Ignore;
import org.junit.Test;

public class TypeSystemTest {

    @Ignore
    @Test
    public void testTypeSystem() throws Exception {
		TypeSystemDescription typesystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription();
		typesystem.toXML(System.out);
	}

}

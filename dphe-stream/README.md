# dphe-stream

NLP Pipeline, Document-level
Summarization, Document and Patient -level

This is the main entry point for DeepPhe-Classsic. To build, use Java 8. 

    clone https://github.com/DeepPhe/dphe-onto-db.git
    mvn install
    
    clone https://github.com/DeepPhe/dphe-neo4j.git
    mvn install
    
    clone https://github.com/DeepPhe/dphe-core.git
    mvn install
    
    clone https://github.com/DeepPhe/dphe-stream.git
    mvn compile
    
    
Then 
    
    java org.healthnlp.deepphe.DocSummarizer TestDocId path/to/testDoc/File
    
    java org.healthnlp.deepphe.PatientSummarizer TestPatientId path/to/testDoc/File
    

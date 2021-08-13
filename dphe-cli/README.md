# dphe-cli
One entry point to the full DeepPhe pipeline, including NLP, and Document and Patient-Level Summarization

This repository contains a command-line-interface for running the deephe pipeline. 
It also contains the primary and secondary DeepPhe [Piper Files](https://cwiki.apache.org/confluence/display/CTAKES/Piper+Files).


## Using DeepPhe
1. [Name](../../wiki/Naming-Input-Files) the files you would like to process (Optional).
2. Start the [Neo4j server](https://neo4j.com/docs/operations-manual/3.5/).
3. Run the DeepPhe Translational base system using one of three methods:
   - Start a GUI that can run the system by executing bin\runDeepPheGui.
   - Run the system by executing bin\runDeepPhe.   Command-line parameters are required to run in this manner:
      -   InputDirectory (-i)     The directory containing clinical notes.
      -   OutputDirectory (-o)    The directory to which output should be written.
      -   StartNeo4j (-n)         Location of the Neo4j installation.  Do not use this if you do not wish to auto-start Neo4j.  If you use this option then the neo4j server will remain active after the pipeline ends.
      -   Neo4jUri (-r)           URI for the Neo4j Server.  Normally "bolt://127.0.0.1:7687"
      -   Neo4jUser (--user)      The username for Neo4j.  Normally "neo4j".
      -   Neo4jPass (--pass)      The password for Neo4j.  Normally "neo4j" until you change it.
   - If you are a developer, run using available Apache cTAKES Java classes for [Piper Files](https://cwiki.apache.org/confluence/display/CTAKES/Piper+Files).
4. View the results using [DeepPhe Data Visualizer](https://github.com/DeepPhe/DeepPhe-Viz-v2).

# How to run these scripts

## Prerequisites
1. A recent version of the Java 8 JDK
   1.  Note, these scripts have been successfully tested on (OS : Runtime):
       1. Ubuntu 20.04.2 LTS: OpenJDK Runtime Environment (build 1.8.0_292-8u292-b10-0ubuntu1~20.04-b10)
       2. macOS 11.4 (Big Sur): OpenJDK Runtime Environment Zulu11.43+55-CA (build 11.0.9.1+1-LTS)   
3. A Maven installation, configured to use the Java 8 JDK
4. The Git command line client
5. A linux/mac environment with the ability to run shell scripts from the command line and install software (i.e. neo4j)

## Instructions
1. Install Neo4j Community 3.5.12
   1. Mac/Linux:  https://go.neo4j.com/download-thanks.html?edition=community&release=3.5.12&flavour=unix
   1. Windows: https://go.neo4j.com/download-thanks.html?edition=community&release=3.5.12&flavour=winzip

1. Once Neo4j is installed, edit the file neo4j.conf from the /conf directory of your Neo4j installation.
   1. set the active database to "ontology.db"
      1. dbms.active_database=ontology.db

1. In the linux-mac-build-and-run-scripts folder, edit the 2.sh script to point the NEO4J_HOME variable to your Neo4j installation.
   1.  e.g. NEO4J_HOME="/opt/neo4j-community-3.5.12"

1. Run 1.sh. This will build the DeepPhe source code.  

1. Run 2.sh. This script starts the Neo4j Server.
   1.  <img width="522" alt="Screen Shot 2021-08-27 at 2 26 44 PM" src="https://user-images.githubusercontent.com/11561825/131172594-049f1cdf-92ec-4b8d-b447-ab90596e5098.png">
 

1. Login to [http://localhost:7474](localhost:7474) to change the password.  **This is a one-time requirement for new Neo4j installations.**  
   1. You will be prompted for a login and password.  Please enter 'neo4j' for both the username and password. 
   2. <img width="726" alt="Screen Shot 2021-08-27 at 11 52 28 AM" src="https://user-images.githubusercontent.com/11561825/131169534-6d204c8f-7968-4a16-ad83-c4d8b44fa5f3.png">
   3. You will then be prompted to enter a new password, enter 'neo4jpass'
   4. <img width="726" alt="Screen Shot 2021-08-27 at 11 52 49 AM" src="https://user-images.githubusercontent.com/11561825/131170121-fd661f62-a883-48f3-bb72-69893e3e91a5.png">
   5. If the password change took and the database is ready for DeepPhe, you should be taken to a screen that shows information about the database.
   6. <img width="726" alt="Screen Shot 2021-08-27 at 11 53 18 AM" src="https://user-images.githubusercontent.com/11561825/131169975-ca22f584-e117-4a94-a1ef-54515fa7cf7c.png">
   7. Next terminate the Neo4j database by pressing Control-C in the Neo4j console where you ran 2.sh.  This database was started using the default authentication credentiails, and those are now out of date.
   8. Finally, run 2.sh again to restart the server using the proper credentials 

1. Open a new terminal and run 3.sh.  This runs DeepPhe.  If this runs successfully, a COMPLETE message is displayed.
   1. <img width="690" alt="Screen Shot 2021-08-27 at 2 13 49 PM" src="https://user-images.githubusercontent.com/11561825/131171207-e247b804-d04b-4950-bbd3-6922ed8ee333.png">
 
1. Run 4.sh. This runs the DeepPhe Webservice/API.  There is no visual output, but a the terminal will show that a Node application has sarted.  Leave this terminal window open.
   1. <img width="522" alt="Screen Shot 2021-08-27 at 2 28 58 PM" src="https://user-images.githubusercontent.com/11561825/131172874-8cb443bb-f3b6-42ef-a51c-14122c2bafd4.png">
 
1. Open a new terminal and run 5.sh.  This starts DeepPhe Viz, the visualization interface for DeepPhe.  The initial screen should look similar to this:

![deepphe](https://user-images.githubusercontent.com/11561825/128786082-e3f427e5-a454-4ff6-9943-deeb7b58914b.png)



# Troubleshooting

1. After running 2.sh, verify that the fresh database is pre-popluated by visiting: http://120.0.0.1/7474 and logging in.  The user is "neo4j" and the password is "neo4jpass".  Once you connect, click on the database icon in the upper left hand corner and verify that you can see node labels and relationship types and not just empty values.

2.  At the end of this process you should have 3 terminals running showing any errors, make sure to check every terminal for errors.

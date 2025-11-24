# SimpleNotes
This is my Simple Notes app


So my app will help us to make our note 

*~Important Point~*
To run this, go to the terminal and run the following commands
Step 1:
# create lib dir and download sqlite-jdbc (example version)
mkdir .\lib
Step 2:
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar" -OutFile .\lib\sqlite-jdbc-3.42.0.0.jar
Step 3:
# compile and run (from src folder)
cd .\src
Step 4:
javac -cp ".;..\\lib\\sqlite-jdbc-3.42.0.0.jar" *.java
Step 5:
java -cp ".;..\\lib\\sqlite-jdbc-3.42.0.0.jar" NotesApp

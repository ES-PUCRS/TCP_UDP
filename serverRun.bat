@ECHO OFF

if NOT exist _class\ mkdir _class\

javac *.java -d _class
if ["%ERRORLEVEL%"]==["0"] (
	java -cp _class; UDPServer
)
rem exit
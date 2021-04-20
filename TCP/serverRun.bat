@ECHO OFF
javac *.java -d _class
if ["%ERRORLEVEL%"]==["0"] (
	java -cp _class; TCPServer
)
rem exit
@ECHO OFF
javac *.java -d class
if ["%ERRORLEVEL%"]==["0"] (
	java -cp class; TCPServer
)
rem exit
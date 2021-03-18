@ECHO OFF
javac *.java -d class

if ["%ERRORLEVEL%"]==["0"] (
	rem start cmd /k "java -cp class; TCPServer"
	start cmd /k "java -cp class; TCPServer & EXIT"
	java -cp class; TCPClient
)
@ECHO OFF
javac *.java -d class
if ["%ERRORLEVEL%"]==["0"] (
	start cmd /k "java -cp class; UDPServer & exit"
	java -cp class; UDPClient
)

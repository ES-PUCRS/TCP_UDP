@ECHO OFF
javac *.java -d _class
SET "invokeParam=%1"

if ["%ERRORLEVEL%"]==["0"] (
	if NOT defined invokeParam (
		start cmd /k "java -cp _class; TCPServer & EXIT"
	)
	if "%invokeParam%" EQU "1" (
		start cmd /k "java -cp _class; TCPServer"
	)
	java -cp _class; TCPClient
)
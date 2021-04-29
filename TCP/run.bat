@ECHO OFF
javac *.java -d _class
SET "invokeParam=%1"

if ["%ERRORLEVEL%"]==["0"] (
	if NOT defined invokeParam (
		start cmd /k "java -cp _class; UDPServer & EXIT"
	)
	if "%invokeParam%" EQU "1" (
		start cmd /k "java -cp _class; UDPServer"
	)
	java -cp _class; UDPClient
)
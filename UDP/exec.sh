#!/bin/bash

echo "Compilling"

if [ ! -d "./_class" ]
then
	mkdir _class
fi

javac ./*.java -d "./_class"

if [ $? -eq 0 ]
then
	if [ $1 -eq 0 ]
	then
		xterm -T "UDPServer" -ls -e java -cp ./_class UDPServer &
	elif [ $1 -eq 1 ]
	then
		xterm -T "UDPClient" -ls -e java -cp ./_class UDPClient &
	else
		xterm -T "UDPClient" -ls -e java -cp ./_class UDPClient &
		xterm -T "UDPServer" -ls -e java -cp ./_class UDPServer &
	fi
fi
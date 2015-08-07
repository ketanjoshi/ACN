#!/bin/bash

STR="$1"
NUM=1
kill $STR
for ((i=1; i<=18; i++)); do
	STR=$(($STR + $NUM))	
	kill $STR
done

#!/bin/bash

param="${1}"

THE_CLASSPATH=
PROGRAM_NAME=

for i in `ls ./libs/*.jar`
  do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done

rm output/*

if [ "${param: -5}" == ".java" ]
then
  PROGRAM_NAME=GenerateScheduleJavaInput.java

  cp "$1" "UseCase.java"
  wait

  sed -i s/"package schedule_generator;"/"import schedule_generator.*; "/g "UseCase.java"
  sed -i s/GeneratedCode/UseCase/g "UseCase.java"
  wait

  echo ${THE_CLASSPATH}

  javac -classpath ".:${THE_CLASSPATH}" "UseCase.java" 
  wait

  javac -classpath ".:${THE_CLASSPATH}" $PROGRAM_NAME
  wait

  java -classpath ".:${THE_CLASSPATH}" "${PROGRAM_NAME::-5}" > output.txt

  rm "GenerateScheduleJavaInput.class"
  rm "UseCase.java"
  rm "UseCase.class"
else
  echo "${1}"
  PROGRAM_NAME=GenerateScheduleJSONInput.java

  javac -classpath ".:${THE_CLASSPATH}" $PROGRAM_NAME
  wait

  echo "${1}"

  java -classpath ".:${THE_CLASSPATH}" "${PROGRAM_NAME::-5}" "$1" > output.txt

  rm "GenerateScheduleJSONInput.class"
  mv output.json "output/output.json"
fi

mv output.txt "output/$1.out"
mv log.txt "output/$1.log"


echo "Ending execution"
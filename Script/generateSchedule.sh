#!/bin/sh

cp "$1" "UseCase.java"
wait

sed -i s/"package schedule_generator;"/"import schedule_generator.*; "/g "UseCase.java"
sed -i s/GeneratedCode/UseCase/g "UseCase.java"
wait

THE_CLASSPATH=
PROGRAM_NAME=GenerateSchedule.java

for i in `ls ./libs/*.jar`
  do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done

# echo ${THE_CLASSPATH}

javac -classpath ".:${THE_CLASSPATH}" "UseCase.java" 
wait

javac -classpath ".:${THE_CLASSPATH}" $PROGRAM_NAME
wait

java -classpath ".:${THE_CLASSPATH}" GenerateSchedule > output.txt
 
mv output.txt "output/$1.out"
mv log.txt "output/$1.log"


rm "UseCase.java"
rm "UseCase.class"
rm "GenerateSchedule.class"

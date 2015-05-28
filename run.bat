@echo off

for /l %%x in (123, 1, 123) do (
  java -jar MASProject.jar -seed %%x -output output-%%x-ref.txt -treshold 50

  java -jar MASProject.jar -seed %%x -output output-%%x-time-15-agent.txt -treshold 50 -agent time;15000;0.25
  java -jar MASProject.jar -seed %%x -output output-%%x-time-15-parcel.txt -treshold 50 -parcel time;15000;1.0

  java -jar MASProject.jar -seed %%x -output output-%%x-time-60-agent.txt -treshold 50 -agent time;60000;0.25
  java -jar MASProject.jar -seed %%x -output output-%%x-time-60-parcel.txt -treshold 50 -parcel time;60000;1.0

  java -jar MASProject.jar -seed %%x -output output-%%x-time-120-agent.txt -treshold 50 -agent time;120000;0.25
  java -jar MASProject.jar -seed %%x -output output-%%x-time-120-parcel.txt -treshold 50 -parcel time;120000;1.0

  java -jar MASProject.jar -seed %%x -output output-%%x-tres-15-agent.txt -treshold 50 -agent tres;15000;0.25;1.25
  java -jar MASProject.jar -seed %%x -output output-%%x-tres-15-parcel.txt -treshold 50 -parcel tres;15000;1.0;5.0

  java -jar MASProject.jar -seed %%x -output output-%%x-tres-60-agent.txt -treshold 50 -agent tres;60000;0.25;1.25
  java -jar MASProject.jar -seed %%x -output output-%%x-tres-60-parcel.txt -treshold 50 -parcel tres;60000;1.0;5.0

  java -jar MASProject.jar -seed %%x -output output-%%x-tres-120-agent.txt -treshold 50 -agent tres;120000;0.25;1.25
  java -jar MASProject.jar -seed %%x -output output-%%x-tres-120-parcel.txt -treshold 50 -parcel tres;120000;1.0;5.0
)
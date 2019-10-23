#!/usr/bin/env bash
# You need at least Java 11 in order to run the jar
cd ../..

source export_java_home.sh
bash export_java_home.sh

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects/CAS Microservice/Reconstructed Models/domain/CAS.data" -i "Other Evaluation Projects/CAS Microservice/Derived Intermediate Models/data models/CAS.xmi" --invoke_only_specified_phases intermediate_model_validation --csv_file="Other Evaluation Projects/CAS Microservice/domain-analysis.csv" 

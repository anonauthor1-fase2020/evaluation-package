#!/usr/bin/env bash
# You need at least Java 11 in order to run the jar
cd ../..

source export_java_home.sh
bash export_java_home.sh

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects/Acme Air/Reconstructed Models/services/AcmeAir.services" -i "Other Evaluation Projects/Acme Air/Derived Intermediate Models/mapping models/wxs.xmi" --invoke_only_specified_phases intermediate_model_validation --service_metrics_set="Hirzalla" --service_metrics_set="Engel" --csv_file="Other Evaluation Projects/Acme Air/service-analysis.csv" 

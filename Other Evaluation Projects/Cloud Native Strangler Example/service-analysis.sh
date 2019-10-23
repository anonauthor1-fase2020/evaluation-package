#!/usr/bin/env bash
# You need at least Java 11 in order to run the jar
cd ../..

source export_java_home.sh
bash export_java_home.sh

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects/Cloud Native Strangler Example/Reconstructed Models/microservices/strangler.services" -i "Other Evaluation Projects/Cloud Native Strangler Example/Derived Intermediate Models/service models/strangler.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Other Evaluation Projects/Cloud Native Strangler Example/Derived Intermediate Models/service models/user.xmi" --service_metrics_set="Hirzalla" --service_metrics_set="Engel" --csv_file="Other Evaluation Projects/Cinema Administration/service-analysis.csv" 

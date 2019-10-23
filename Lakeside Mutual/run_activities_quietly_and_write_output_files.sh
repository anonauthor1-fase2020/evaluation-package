#!/usr/bin/env bash
# You need Python 3 to run the script
python3 activity-1-filecount.py > "Produced Result Files/activity-1-filecount.py.out"

# You need at least Java 11 in order to run the jar
cd ..

source export_java_home.sh
bash export_java_home.sh

## Domain analysis
java -jar static-analyzer-standalone.jar -s "Lakeside Mutual/Reconstructed Models/customer-core/customerCore.data" -i "Lakeside Mutual/Derived Intermediate Models/mapping models/customerCore/customerCore_customerCore.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/customerManagement/customerManagement_customerManagement.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/customerSelfServiceBackend/customerSelfServiceBackend_customerSelfServiceBackend.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/policyManagementBackend/policyManagementBackend_policyManagementBackend.xmi" --csv_file="Lakeside Mutual/Produced Result Files/domain-analysis.csv" > "Lakeside Mutual/Produced Result Files/activity-2-domain-analysis.sh.out"

## Service analysis (complete)
java -jar static-analyzer-standalone.jar -s "Lakeside Mutual/Reconstructed Models/customer-core/customerCore.services" -i "Lakeside Mutual/Derived Intermediate Models/service models/customerCore.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/customerManagement.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/customerSelfServiceBackend.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/policyManagementBackend.xmi" --service_metrics_set="Engel" --service_metrics_set="Hirzalla" --csv_file="Lakeside Mutual/Produced Result Files/service-analysis-complete.csv" > "Lakeside Mutual/Produced Result Files/activity-3-6-service-analysis-complete.sh.out"

java -jar static-analyzer-standalone.jar -s "Lakeside Mutual/Reconstructed Models/customer-core/customerCore.data" -i "Lakeside Mutual/Derived Intermediate Models/mapping models/customerCore/customerCore_customerCore.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/customerManagement/customerManagement_customerManagement.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/customerSelfServiceBackend/customerSelfServiceBackend_customerSelfServiceBackend.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/policyManagementBackend/policyManagementBackend_policyManagementBackend.xmi" 

## Service analysis (Engel et al. metrics)
java -jar static-analyzer-standalone.jar -s "Lakeside Mutual/Reconstructed Models/customer-core/customerCore.services" -i "Lakeside Mutual/Derived Intermediate Models/service models/customerCore.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/customerManagement.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/customerSelfServiceBackend.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/policyManagementBackend.xmi" --service_metrics_set="Engel" --csv_file="Lakeside Mutual/Produced Result Files/service-analysis-engel-metrics.csv" > "Lakeside Mutual/Produced Result Files/activity-3-6-service-analysis-engel-metrics.sh.out"

## Service analysis (Hirzalla et al. metrics)
java -jar static-analyzer-standalone.jar -s "Lakeside Mutual/Reconstructed Models/customer-core/customerCore.services" -i "Lakeside Mutual/Derived Intermediate Models/service models/customerCore.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/customerManagement.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/customerSelfServiceBackend.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/policyManagementBackend.xmi" --service_metrics_set="Hirzalla" --csv_file="Lakeside Mutual/Produced Result Files/service-analysis-hirzalla-metrics.csv" > "Lakeside Mutual/Produced Result Files/activity-3-6-service-analysis-hirzalla-metrics.sh.out"

## Service analysis (no metrics)
java -jar static-analyzer-standalone.jar -s "Lakeside Mutual/Reconstructed Models/customer-core/customerCore.services" -i "Lakeside Mutual/Derived Intermediate Models/service models/customerCore.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/customerManagement.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/customerSelfServiceBackend.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/service models/policyManagementBackend.xmi" --csv_file="Lakeside Mutual/Produced Result Files/service-analysis-no-metrics.csv"  > "Lakeside Mutual/Produced Result Files/activity-3-6-service-analysis-no-metrics.sh.out"

## Discussion line counts
cd "Lakeside Mutual"
bash discussion-line-counts.sh
mv linecounts.csv "Produced Result Files/linecounts.csv"

#!/usr/bin/env bash
# You need at least Java 11 in order to run the jar
cd ..

source export_java_home.sh
bash export_java_home.sh

java -jar static-analyzer-standalone.jar -s "Lakeside Mutual/Reconstructed Models/customer-core/customerCore.data" -i "Lakeside Mutual/Derived Intermediate Models/mapping models/customerCore/customerCore_customerCore.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/customerManagement/customerManagement_customerManagement.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/customerSelfServiceBackend/customerSelfServiceBackend_customerSelfServiceBackend.xmi" --additional_intermediate_model="Lakeside Mutual/Derived Intermediate Models/mapping models/policyManagementBackend/policyManagementBackend_policyManagementBackend.xmi" --csv_file="Lakeside Mutual/domain-analysis.csv"

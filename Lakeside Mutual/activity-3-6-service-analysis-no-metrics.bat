REM You need at least Java 11 in order to run the jar
cd ..

CALL export_java_home.bat

java -jar static-analyzer-standalone.jar -s "Lakeside Mutual\Reconstructed Models\customer-core\customerCore.services" -i "Lakeside Mutual\Derived Intermediate Models\service models\customerCore.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Lakeside Mutual\Derived Intermediate Models\service models\customerManagement.xmi" --additional_intermediate_model="Lakeside Mutual\Derived Intermediate Models\service models\customerSelfServiceBackend.xmi" --additional_intermediate_model="Lakeside Mutual\Derived Intermediate Models\service models\policyManagementBackend.xmi" --csv_file="Lakeside Mutual\service-analysis-no-metrics.csv" 
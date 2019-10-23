REM You need at least Java 11 in order to run the jar
cd ..\..

CALL export_java_home.bat

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects\Blog Post\Reconstructed Models\domain\BlogPost.data" -i "Other Evaluation Projects\Blog Post\Derived Intermediate Models\data models\BlogPost.xmi" --invoke_only_specified_phases intermediate_model_validation --additional_intermediate_model="Other Evaluation Projects\Blog Post\Derived Intermediate Models\data models\VO.xmi" --csv_file="Other Evaluation Projects\Blog Post\domain-analysis.csv" 

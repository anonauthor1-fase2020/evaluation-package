REM You need at least Java 11 in order to run the jar
cd ..\..

CALL export_java_home.bat

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects\Acme Air\Reconstructed Models\domain\AcmeAir.data" -i "Other Evaluation Projects\Acme Air\Derived Intermediate Models\data models\AcmeAir.xmi" --invoke_only_specified_phases intermediate_model_validation --csv_file="Other Evaluation Projects\Acme Air\domain-analysis.csv" 

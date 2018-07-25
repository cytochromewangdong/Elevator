

./gradlew clean build

java -jar ./build/libs/elevator-manager-0.1.0.jar

cd python
python elevatorclient.py -S IP:8080/elevator-manager

Or you can generate test files
cd python/generator
python generator.py

Then run the script 
python elevatorclient.py -S IP:8080/elevator-manager -F DATA_PATH





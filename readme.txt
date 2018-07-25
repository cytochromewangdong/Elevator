

./gradlew clean build

java -jar ./build/libs/elevator-manager-0.1.0.jar

cd python
python testclient.py -S IP:8080/elevator-manager



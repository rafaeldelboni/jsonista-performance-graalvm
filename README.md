# Jsonista Performance

## Results
Check [performance.csv](performance.csv) for the compiled results.

## Running

### Uberjar
You can generate an uberjar and execute it via java in the terminal:
```bash
# generate a service.jar in the root of this repository.
clj -X:uberjar
# execute it via java
java -jar target/service.jar
```

### Native Binary (GraalVM)
Configurations related to the compilation is in the folder `resources/META-INF/native-image/`
```bash
# generate a service.jar and later the native binary
./build-native.sh
# execute it
./target/service
```

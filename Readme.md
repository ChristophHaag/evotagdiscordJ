# Build

    mvn assembly:assembly -DdescriptorId=jar-with-dependencies

# Run

Create a text file with just the token, e.g. ~/token.txt.

    java -jar target/evotagdiscordJ-1.0-SNAPSHOT-jar-with-dependencies.jar ~/token.txt

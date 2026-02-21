
# Compile
javac -cp ".;d:\anti gravity\backend\src\main\resources\lib\*" TestDbConnection.java

# We need the mysql connector jar. Accessing it from maven repo typically implies local path.
# Let's try to find where the jar is or use the one from target if built.
# backend.jar contains dependencies in BOOT-INF/lib. 
# Extracting is too slow.
# Let's see if we can just run it using the classpath from the project if available?
# Alternative: Use "mvn exec:java"? No, complex.

# Simpler: Just try to run the main app with a different profile? No.

# Let's assume user has maven and we can just use a simple JDBC test if we find the jar.
# Let's search for mysql-connector jar.
$jar = Get-ChildItem -Path "C:\Users\darad\.m2\repository\com\mysql\mysql-connector-j" -Recurse -Filter "*.jar" | Select-Object -First 1
if (!$jar) {
    Write-Host "MySQL Connector Jar not found in .m2. Cannot run test easily."
    exit 1
}

Write-Host "Using Driver: $($jar.FullName)"

javac TestDbConnection.java
java -cp ".;$($jar.FullName)" TestDbConnection "Bbd@1415"

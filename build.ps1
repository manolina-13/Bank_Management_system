cd ..
cd simple-bank
$sourceFiles = Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { $_.FullName }
javac -d "WEB-INF/classes" -cp "WEB-INF/lib/*" -sourcepath "src" $sourceFiles
cd ..

echo "please ensure that you've added the config to the quacker, to enable appropriate checks, by putting the xml files into the config directory"

jv=$(java --version 2>&1)
if [[ ! "$jv" == *"openjdk 17"* ]]; then
    echo "$jv"
    echo "Java 17 is required. Please install OpenJDK 17."
    exit 1
fi

if [[ "$*" == *"--compile"* ]]; then
    echo "Proceeding with build..."
    ./sbt.sh compile
    ./sbt.sh package

fi

mkdir -p webapps
cp target/scala-2.1*/*.war webapps/quacker.war

if docker ps -a --format '{{.Names}}' | grep -q '^quacker$'; then
    docker rm -f quacker
fi

docker build -t quacker .
docker run -d -p 8666:8666 --name quacker -it quacker 

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

if docker ps -a --format '{{.Names}}' | grep -q '^quacker$'; then
    docker rm -f quacker
fi

cp target/scala-2.1*/*.war ./.kube/root.war

cd .kube
docker build -t quacker .
cd ..
docker run -d -p 8666:8080 \
    -v "$(pwd)/appConf:/var/appConf:ro" \
    -v "$(pwd)/monitoringDashboardConfig:/var/monitoringDashboardConfig:ro" \
    -e "QUACKER_APP_CONFIG_DIRECTORY_LOCATION=/var/appConf"  \
    -e "QUACKER_CONFIG_DIRECTORY_LOCATION=/var/monitoringDashboardConfig"  \
    --name quacker -it quacker 

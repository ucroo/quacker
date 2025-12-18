echo "please ensure that you've added the config to the quacker, to enable appropriate checks, by putting the xml files into the config directory"

./sbt.sh compile
./sbt.sh package
mkdir -p webapps
cp target/scala-2.11/*.war webapps/quacker.war

if docker ps -a --format '{{.Names}}' | grep -q '^quacker$'; then
    docker rm -f quacker
fi

docker build -t quacker .
docker run -d -p 443:443 --name quacker -it quacker 

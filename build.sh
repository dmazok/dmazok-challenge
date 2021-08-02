cd common
gradle clean build

cd ../rest-api
gradle bootBuildImage

cd ../service
gradle bootBuildImage

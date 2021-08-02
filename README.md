I've implemented the following employee state flow:

![""](uml/graph.png "Title")

To build the application use:
```shell script
cd common
gradle clean build

cd ../rest-api
gradle bootBuildImage

cd ../service
gradle bootBuildImage
```
To run it with docker-compose:
```shell script
docker-compose up
```
At http://localhost:8080/swagger-ui/ you can find the application's API description. 

To watch an employee status use:
```shell script
curl "http://localhost:8080/employees/{employeeId}/status"
```
Note: IDs for employees are generated sequentially starting from 1.



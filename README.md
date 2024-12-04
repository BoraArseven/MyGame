
Coveralls:[![Coverage Status](https://coveralls.io/repos/github/BoraArseven/MyGame/badge.svg?branch=main)](https://coveralls.io/github/BoraArseven/MyGame?branch=main)  <br>
Workflow Status Badge: [![Java 17 CI with Maven](https://github.com/BoraArseven/MyGame/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/BoraArseven/MyGame/actions/workflows/maven.yml)  <br>
SonarCloud Quality:[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
SonarCloud Bugs:[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=bugs)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
SonarCloud Code Smells: [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
Lines of Code:[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
SonarCloud Technical Debt: [![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
SonarCloud Maintainability Rating: [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
SonarCloud Vulnerabilities: [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
SonarCloud Security Rating: [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
SonarCloud Reliability Rating: [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
SonarCloud Duplicated Lines: [![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)  <br>
Latest Mutation Testing Report: https://boraarseven.github.io/MyGame/<br>
SonarCloud Coverage [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=BoraArseven_MyGame&metric=coverage)](https://sonarcloud.io/summary/new_code?id=BoraArseven_MyGame)<br><br>
This Project Uses Java 17.<br>

Usage:<br>
1) Generate the application jar with the command "mvn install package"<br>

2) Add a .env file with following (change the password with strong one):<br>
"
POSTGRES_DB=Game<br>
POSTGRES_PASSWORD=mysecretpassword<br>
DB_URL=jdbc:postgresql://mygamepostgres:5432/${POSTGRES_DB}<br>
"

3) Run start.sh.<br>

Database is saved to .db folder which is shared with containers, and if password is forgotten, please simply remove that folder.

Maven command might need sudo permissions.

This probably is not needed but if there is something wrong with GUI, the socat server can be runned with Socat.sh.


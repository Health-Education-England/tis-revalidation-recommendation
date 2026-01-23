# tis-revalidation-recommendation

Revalidation V2

# Prerequisite

- Java 17
- Maven
- Docker

## TODO
 - Provide `SENTRY_DSN` and `SENTRY_ENVIRONMENT` as environmental variables
   during deployment and need to make the SENTRY_ENVIRONMENT dynamic in the future.

# To Build

mvn clean install

# To Run

cd application

mvn clean package spring-boot:run

# To execute integration test

mvn clean verify -Pintegration-tests

Above command will setup docker environment on your local machine before it execute integration tests from integration-tests module.

# To access Swagger UI on local
http://localhost:8080/revalidation/swagger-ui/index.html

# Cron Jobs
## Gmc Nightly Doctor Sync (GmcDoctorNightlySyncService.startNightlyGmcDoctorSync)
This job ensures the list of doctors stored in DoctorsForDB is kept up to date with GMC Connect.
We do this because a Doctor's connection and revalidation info can change without notice to us.
This job consists of the following steps:
1. Send a "start" message to the Gmc Client Service (separate repository, not to be confused with service class in this project)
3. The Gmc Client Service returns a message per Designated Body, containing connected doctors
4. Each returned doctor's information is updated
5. Any doctors which were previously updated from the GMC but weren't in the last message have the connection removed

## Gmc Recommendation Status Check (GmcsendRecommendationStatusRequestToRabbit)
This job checks the current status of each Recommendation in GMC connect as we are not directly informed of Approval/Rejections.
This job consists of the following steps:
1. For each Recommendation awaiting GMC action, send a message to the Gmc Client Service (separate repository, not to be confused with service class in this project)
2. The Gmc Client Service will call the checkRecommendationStatus Api Endpoint at GMC Connect and return the result in a message
3. The returned message is used to update the TIS (doctorStatus) and GMC status of a doctor.
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
This is a necessary mitigation until the Connections Service is released.
This job consists of the following steps:
1. "Hide" all doctors by setting the existsInGmc flag to false
2. Send a "start" message to the Gmc Client Service (separate repository, not to be confused with service class in this project)
3. The Gmc Client Service returns a series of messages containing individual doctors
4. Each returned doctor's existsInGmc flag is set to true

NB: previously hiding doctors was achieved by modifying the designated body code to have a prefix of "last-"

## Gmc Recommendation Status Check (GmcsendRecommendationStatusRequestToRabbit)
This job checks the current status of each doctor in GMC connect as we are not directly informed of Approval/Rejections.
This job consists of the following steps:
1. For each Recommendation stored on out system, send a message to the Gmc Client Service (separate repository, not to be confused with service class in this project)
2. The Gmc Client Service will call the checkRecommendationStatus Api Endpoint at GMC Connect and return the result in a message
3. The returned message is used to update the TIS (doctorStatus) and GMC status of a trainee.
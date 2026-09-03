# Java Code Assignment

This is a short code assignment that explores various aspects of software development, including API implementation, documentation, persistence layer handling, and testing.

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

To compile and run this demo you will need:

- JDK 17+

In addition, you will need either a PostgreSQL database, or Docker to run one.

### Configuring JDK 17+

Make sure that `JAVA_HOME` environment variables has been set, and that a JDK 17+ `java` command is on the path.

## Building the demo

Execute the Maven build on the root of the project:

```sh
./mvnw package
```

## Running the demo

### Live coding with Quarkus

The Maven Quarkus plugin provides a development mode that supports
live coding. To try this out:

```sh
./mvnw quarkus:dev
```

In this mode you can make changes to the code and have the changes immediately applied, by just refreshing your browser.

    Hot reload works even when modifying your JPA entities.
    Try it! Even the database schema will be updated on the fly.

## Testing & Coverage

Run the full test suite (unit tests plus `@QuarkusTest` REST-layer tests):

```sh
./mvnw test
```

Run tests **and** enforce the 80% line-coverage gate (JaCoCo, bound to the Maven `verify`
phase - this is the same command the GitHub Actions CI workflow runs):

```sh
./mvnw verify
```

The HTML coverage report is written to `target/site/jacoco/index.html`. `mvn verify` fails the
build if line coverage drops below 80%; generated OpenAPI code and plain data-holder classes
(domain POJOs, JPA entities with no logic) are excluded from the ratio so the gate measures
actual business logic, not boilerplate.

The `@QuarkusTest` REST-layer tests need Docker running locally (Quarkus Dev Services spins up
a throwaway PostgreSQL container automatically) - no manual database setup is required, just
have Docker Desktop (or an equivalent) running before `./mvnw test`/`verify`.

## (Optional) Run Quarkus in JVM mode

When you're done iterating in developer mode, you can run the application as a conventional jar file.

First compile it:

```sh
./mvnw package
```

Next we need to make sure you have a PostgreSQL instance running (Quarkus automatically starts one for dev and test mode). To set up a PostgreSQL database with Docker:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

Connection properties for the Agroal datasource are defined in the standard Quarkus configuration file,
`src/main/resources/application.properties`.

Then run it:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```
    Have a look at how fast it boots.
    Or measure total native memory consumption...


## See the demo in your browser

Navigate to:

<http://localhost:8080/index.html>

Have fun, and join the team of contributors!

## CI

A GitHub Actions workflow at [`/.github/workflows/ci.yml`](../../.github/workflows/ci.yml) (repo
root, not this module - GitHub Actions only reads workflows from there) runs `./mvnw verify` on
every push/PR to `main`, so the 80% coverage gate above is enforced automatically.

## Troubleshooting

Using **IntelliJ**, in case the generated code is not recognized and you have compilation failures, you may need to add `target/.../jaxrs` folder as "generated sources".
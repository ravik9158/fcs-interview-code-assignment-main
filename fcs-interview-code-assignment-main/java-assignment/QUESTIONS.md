# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
It depends on which inconsistency we're talking about - not all of them are equally worth fixing.

Store (Panache active record, extends PanacheEntity) and Product (plain JPA entity + a
separate ProductRepository) do the exact same job - simple CRUD, no business rules - using two
different persistence patterns. That's arbitrary inconsistency, not a deliberate design choice,
and I would refactor it: pick one pattern (I'd lean toward the repository style Product already
uses, since it keeps persistence concerns out of the entity class) and apply it to both.

Warehouse's full hexagonal setup (domain model + ports + use cases + a separate DbWarehouse JPA
entity/adapter) is a different story. I would NOT collapse that into the same simple style as
Store/Product, because it earned its extra structure: Warehouse has real branching business
rules (business-unit-code uniqueness among active rows, per-location warehouse-count and
capacity quotas, and the archive-and-recreate semantics of "replace"). Building
CreateWarehouseUseCase/ReplaceWarehouseUseCase/ArchiveWarehouseUseCase as small classes behind
a WarehouseStore port meant their unit tests (CreateWarehouseUseCaseTest,
ReplaceWarehouseUseCaseTest) run as plain JUnit against hand-rolled in-memory fakes - no Quarkus
context, no database, sub-10ms per test. Store and Product have no comparable logic to isolate,
so giving them the same ceremony would be pure overhead with no payoff.

So: unify Store and Product (that's inconsistency without justification), leave Warehouse's
architecture alone (that's complexity that's earning its keep).
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Spec-first (Warehouse): the YAML is a single source of truth that both documents the contract
and generates the server interface, so implementation and documentation can't silently drift
apart. The real cost is that you're at the mercy of the generator. While implementing this
assignment, the generated createANewWarehouseUnit method returns the Warehouse DTO directly
instead of a jakarta.ws.rs.core.Response, so there was no way to return the 201 status the spec
itself documented for that endpoint without a workaround (a @ServerResponseFilter, or
hand-editing generated code, which doesn't survive regeneration). I ended up fixing this by
changing the spec to document 200, the status the tooling actually produces - the honest fix
given this generator's limitations, but it shows how spec-first ties you to what your generator
can express, not just what your spec says.

Hand-written (Product/Store): full control over status codes, response shape, and behaviour,
with less build-time machinery. The cost is that there's no machine-readable contract at all -
Product and Store have no OpenAPI spec, so nothing stops the code and any external
documentation from disagreeing, and no tooling can generate a client or validate requests
against a schema.

My choice for a project like this: hand-written for most endpoints, matching what's already
there for Product/Store. Spec-first is worth the friction only when there's a real external
consumer who needs an enforced, versioned contract (a public API, a mobile client team working
against your API before it's built) - and even then, only once the generator you're using is
mature enough not to fight you on basics like status codes.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
This is the strategy actually used while implementing this assignment, not a hypothetical one:

1. Business-rule-bearing logic first: the Warehouse use cases (create/replace/archive) and
   LocationGateway. This is where the branching logic and edge cases actually live (duplicate
   business-unit codes, location capacity/count quotas, capacity-vs-stock validation, the
   archive-and-recreate semantics of replace). These are plain JUnit tests against hand-rolled
   fakes of WarehouseStore/LocationResolver - fast, no framework startup, cheapest to write and
   highest signal per test.

2. REST-layer / integration tests next (@QuarkusTest + rest-assured): wiring, status codes,
   request/response mapping, and the things that only show up when the whole stack is running -
   e.g. the archiving test catching that listAllWarehousesUnits() originally leaked archived
   warehouses, or discovering the generated Warehouse DTO can't express a 201 status. Unit tests
   against fakes can't catch either of those; only exercising the real HTTP layer does.

3. Deliberately not testing plain data holders: domain POJOs (Warehouse, Location) and JPA
   entities with no logic (Store, Product, DbWarehouse, the generated OpenAPI beans) - there are
   no branches to cover, so a test there is just asserting a getter returns what a setter set.

To keep coverage effective over time rather than eroding, this needs to be enforced, not just
followed by convention: a JaCoCo check goal bound to the Maven verify phase fails the build
below 80% line coverage, and a GitHub Actions workflow runs that on every push/PR - so a
regression in coverage is a failed CI check, not something that quietly slips in over time.
```
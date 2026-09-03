# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
```txt
The core challenge is that most costs here aren't naturally 1:1 with a single Warehouse or
Store - they need an allocation rule to be split fairly. Transportation between a Warehouse and
a Store is clean (it's inherently tied to that pair), but labor and overhead at a Warehouse that
fulfils multiple Stores has to be allocated by some proxy (e.g. share of stock moved, share of
Product types handled) and whichever proxy you pick will be argued with by whoever's budget it
affects.

Questions I'd want answered before scoping this:
- What's the smallest unit we need to allocate cost to - per Warehouse, per Store, per
  Warehouse-Store pair (which this system already models via the new fulfilment
  association), or per Product?
- Are costs metered at the source (e.g. a transport invoice already itemized per route) or do
  we have to estimate/apportion them ourselves? The former is a data-integration problem, the
  latter is a policy problem - you need someone with authority to decide the allocation rule,
  not just an engineer to implement one.
- How is "overhead" defined, and does that definition already exist in Finance's chart of
  accounts, or would we be inventing a new taxonomy that then needs reconciling with their
  numbers later?
```

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
```txt
The strategies that fall directly out of this system's own data: consolidating stock so fewer,
better-utilized Warehouses serve a given Store (the new fulfilment feature's "max 3 warehouses
per store" cap already nudges toward this); flagging Warehouses that are near their location's
maxCapacity as under strain vs. ones sitting far below it as underused; and using the
Warehouse-replacement mechanism deliberately - not just reactively - to right-size capacity
when a location's demand profile changes, rather than only when an old Warehouse needs
replacing anyway.

How I'd prioritize: by expected savings vs. effort/risk, same as any cost initiative - but the
key input I don't have yet is actual cost data (Scenario 1). Consolidation only pays off if the
saved warehouse overhead outweighs the transportation cost of serving a Store from fewer,
possibly farther Warehouses - that's a real trade-off, not a free win, and needs real numbers
to evaluate rather than intuition. I'd start with a small, measurable pilot (one region, one
consolidation) with a defined before/after cost comparison, rather than a system-wide policy
change based on projections.
```

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
```txt
Without this integration, cost data lives in two places that inevitably drift - the
operational system (this one) tracks Warehouses/Stores/Products, Finance tracks the general
ledger, and reconciling them by hand is exactly the kind of manual process that produces stale,
disputed numbers precisely when someone urgently needs an accurate answer (e.g. end of quarter).
Integrating means the operational events that drive cost (a Warehouse created, a Product
fulfilled, a Warehouse archived/replaced) can flow into financial reporting close to real time,
instead of being re-entered or batch-reconciled later.

We already have a directly relevant precedent in this codebase: StoreResource integrates with
LegacyStoreManagerGateway, and the assignment's own Task 2 was about making sure that
integration only fires after the local transaction actually commits - otherwise the downstream
system can end up with a record that doesn't exist on our side. The same principle applies here:
a financial system integration needs to be commit-confirmed and needs a defined behavior for
partial failure (does a failed sync retry, alert, or block the operation?), not just "call an
API and hope."

Questions I'd want answered: is the financial system the source of truth for cost figures we'd
just be reporting into, or does it also need to feed constraints back (e.g. a budget cap) into
this system? That changes this from a one-way integration to a two-way one, which is a much
bigger scope decision to get agreement on before building anything.
```

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
```txt
Fulfilment costs are driven by decisions this system already tracks the shape of - opening a
new Warehouse, growing Product-Warehouse-Store fulfilment relationships toward their quota
limits, replacing a Warehouse - so forecasting isn't a separate discipline bolted on afterward,
it should be built on the same entities and history this system maintains, not a parallel model
with its own assumptions that can quietly diverge from operational reality.

Things I'd take into account: forecasting needs a time series, not just current state - which
means historical cost data needs to be preserved and queryable, not just the current snapshot
(this connects directly to Scenario 5 - archived Warehouses keeping their history is exactly the
kind of data a forecast would need to look back over). It also needs to distinguish committed
costs (a Warehouse that already exists, with known capacity/overhead) from projected ones (a
planned new Warehouse, or capacity we're assuming a fulfilment relationship will need) - mixing
the two makes a budget look precise when it's actually a guess wearing a committed number's
clothing.
```

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
```txt
This is the one scenario directly implemented in this codebase: ReplaceWarehouseUseCase
archives the old Warehouse (sets archivedAt, keeps the row) and creates a new one under the
same businessUnitCode, rather than deleting the old row and starting fresh. That design choice
exists for exactly the cost-control reason this scenario asks about: the business unit code is
the stable key that ties a location's cost history together across replacements, so archiving
instead of deleting means "how has this business unit's cost trended over its whole
lifetime, across however many physical Warehouses have occupied that role" stays answerable.
Delete the old row instead, and that continuity is gone the moment a Warehouse gets replaced -
the cost history resets to zero and looks like a brand-new operation with no baseline, even
though the business unit itself has years of history.

Preserving that history is also what makes "stay within budget" checkable at all for the new
Warehouse: the natural budget baseline for a replacement is "what did the old Warehouse under
this business unit code cost," not an arbitrary number - and the implementation already
enforces a related operational constraint (the new Warehouse's capacity must accommodate the
old one's stock, and stock must match) precisely so the replacement doesn't quietly change the
unit's operating profile out from under whatever budget was set based on the old one.
```

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.

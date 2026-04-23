# XtendM3 Database Performance Statistics Report

| Field            | Value                                                        |
|:-----------------|:-------------------------------------------------------------|
| **Extension**    | GetContactPerson                                             |
| **Type**         | ExtendM3Transaction                                          |
| **Program**      | EXT006MI                                                     |
| **Transaction**  | GetContactPer                                                |
| **File**         | TRANSACTION-EXT006MI-GetContactPer-ALL.groovy                |
| **Review Date**  | 2025-01-20                                                   |
| **Reviewer**     | Amazon Q                                                     |

---

## Database Operations Summary

| # | Table | Operation | Method | Index | Keys Used | Fields Selected | Location |
|:--|:------|:----------|:-------|:------|:----------|:----------------|:---------|
| 1 | CCUCON | `readAll` | `processContactPerson` | 10 | CCCONO, CCERTP, CCEMRE (3) | CCTX40, CCEMAL, CCCNPE | Outer |
| 2 | CUGEX1 | `readAll` | `processRecord` closure | 00 | F1CONO, F1FILE, F1PK01 (3) | F1CHB1 | Nested inside CCUCON closure |

---

## Database Performance Statistics

| Program | Table | Method | Operation | Records_Per_Call | Loop_Iterations | Nested_Level | Total_Potential_Reads | Risk_Level |
|:--------|:------|:-------|:----------|:-----------------|:----------------|:-------------|:----------------------|:-----------|
| GetContactPer | CCUCON | processContactPerson | readAll | 1 | 1 | 1 (outer) | 1 | 🟢 Low |
| GetContactPer | CUGEX1 | processRecord (closure) | readAll | 1 | 1 per CCUCON record | 2 (nested) | 1 × 1 = **1** | 🟢 Low |

**Total Potential Reads: 2 (at current maxRecords settings)**

---

## Nested readAll Analysis

```
processContactPerson()
└── CCUCON.readAll(ccuconContainer, 3, maxRecords=1, processRecord)    ← Outer: up to 1 record
    └── processRecord closure (per CCUCON record)
        └── CUGEX1.readAll(cugex1Container, 3, maxRecords=1, ...)      ← Inner: up to 1 per outer
```

### Current State
| Metric | Value |
|:-------|:------|
| CCUCON maxRecords | 1 |
| CUGEX1 maxRecords | 1 |
| Total reads (current) | 1 × 1 = **1** |
| Risk (current) | 🟢 Low |

### Projected Risk if maxRecords Increased

| CCUCON maxRecords | CUGEX1 maxRecords | Total Potential Reads | Risk Level |
|:------------------|:------------------|:----------------------|:-----------|
| 1 | 1 | 1 | 🟢 Low |
| 10 | 10 | 100 | 🟢 Low |
| 100 | 10 | 1,000 | 🟢 Low |
| 500 | 10 | 5,000 | 🟡 Medium |
| 1,000 | 10 | 10,000 | 🟡 Medium (at limit) |
| 1,000 | 11 | 11,000 | 🔴 **Critical — exceeds 10,000** |

> ⚠️ **Warning:** `maxRecords=1` on CCUCON may cause the extension to miss valid contact person records if more than one exists for the customer. Review whether this is intentional. If increased, ensure `CCUCON_maxRecords × CUGEX1_maxRecords ≤ 10,000`.

---

## Optimization Applied — DBAction Build Location

### Before (Previous Version — Anti-Pattern)
```groovy
Closure<?> processRecord = { DBContainer rec ->
  // Built on EVERY iteration — inefficient
  DBAction query1 = database.table("CUGEX1").index("00").selection("F1CHB1").build()
  DBContainer container1 = query1.getContainer()
  ...
}
```

### After (Current Version — Optimized ✅)
```groovy
// Built ONCE before the closure — efficient
DBAction cugex1Query = database.table("CUGEX1").index("00").selection("F1CHB1").build()
DBContainer cugex1Container = cugex1Query.getContainer()

Closure<?> processRecord = { DBContainer rec ->
  // Reuses cugex1Query and cugex1Container
  cugex1Container.set("F1CONO", inCono)
  cugex1Container.set("F1FILE", "CCUCON")
  cugex1Container.set("F1PK01", cnpe)
  cugex1Query.readAll(cugex1Container, 3, 1, processCugex1Record)
}
```

**Impact:** Eliminates repeated `DBAction` object construction on every CCUCON record iteration. With `maxRecords=1` the saving is minimal, but becomes significant if `maxRecords` is increased.

---

## Compliance Checklist

| Rule | Status | Detail |
|:-----|:-------|:-------|
| `readAll` uses 4-argument form | ✅ Pass | Both calls use `readAll(container, nrOfKeys, maxRecords, closure)` |
| `maxRecords` ≤ 10,000 | ✅ Pass | Both set to 1 |
| `selectAllFields()` not used | ✅ Pass | Explicit field selection on all queries |
| No `readAll` inside a for/while loop | ✅ Pass | Nested inside closure only |
| Combined reads ≤ 10,000 | ✅ Pass | Total = 1 (current settings) |
| DBAction built outside closure | ✅ Pass | cugex1Query built once outside processRecord |
| maxRecords sufficient for business logic | ⚠️ Review | maxRecords=1 on CCUCON may miss valid records |

---

## MI Calls Performance Notes

| MI Program | Transaction | Condition | Max Calls Per Execution |
|:-----------|:------------|:----------|:------------------------|
| OIS100MI | GetOrderHead | `if (inCuno.isEmpty())` — only when CUNO not provided | 1 |

MI call is guarded and executes at most once per transaction invocation. No performance concerns.

# XtendM3 Database Performance Statistics Report

| Field           | Value                                           |
|:----------------|:------------------------------------------------|
| **Extension**   | EXT480 — RetrieveMediaKeyCheck                  |
| **Type**        | ExtendM3Trigger                                 |
| **Program**     | MMS480                                          |
| **File**        | TRIGGER-MMS480-RetrieveMediaKeyCheck-ALL.groovy |
| **Review Date** | 2025-01-20                                      |
| **Reviewer**    | Amazon Q                                        |

---

## Database Operations Summary

| # | Table | Operation | Method | Index | Keys Used | Fields Selected |
|:--|:------|:----------|:-------|:------|:----------|:----------------|
| 1 | MHDISH | `readAll` | `retrieveMediaKeys` | 00 | OQCONO, OQDLIX, OQINOU (3) | OQCOAF, OQCONA, OQWHLO |
| 2 | MHDISL | `readAll` | `retrieveMediaKeys` | 00 | URCONO, URDLIX, URRORC (3) | URRIDN |

---

## Database Performance Statistics

| Program | Table | Method | Operation | Records_Per_Call | Loop_Iterations | Nested_Level | Total_Potential_Reads | Risk_Level |
|:--------|:------|:-------|:----------|:-----------------|:----------------|:-------------|:----------------------|:-----------|
| RetrieveMediaKeyCheck | MHDISH | retrieveMediaKeys | readAll | 1,000 | 1 | 1 | 1,000 | 🟢 Low |
| RetrieveMediaKeyCheck | MHDISL | retrieveMediaKeys | readAll | 1,000 | 1 | 1 | 1,000 | 🟢 Low |

**Total Potential Reads across all operations: 2,000**

---

## Performance Analysis

### MHDISH — readAll
```groovy
mhdishQuery.readAll(mhdishContainer, 3, 1000, processCoafRecord)
```
- Keys: OQCONO + OQDLIX + OQINOU (3 keys) — highly selective, targets a specific delivery number
- maxRecords: 1,000 ✅
- Not inside a loop ✅
- Not nested inside another readAll ✅
- Total potential reads: **1,000**
- Risk: 🟢 **Low** — well within the 10,000 limit

### MHDISL — readAll
```groovy
mhdislQuery.readAll(mhdislContainer, 3, 1000, processDlixRecord)
```
- Keys: URCONO + URDLIX + URRORC (3 keys) — highly selective, targets a specific delivery number with order category
- maxRecords: 1,000 ✅
- Not inside a loop ✅
- Not nested inside another readAll ✅
- Total potential reads: **1,000**
- Risk: 🟢 **Low** — well within the 10,000 limit

---

## Risk Assessment

| Risk Level | Threshold | Status |
|:-----------|:----------|:-------|
| 🟢 Low | < 1,000 reads per call | Both operations within limit |
| 🟡 Medium | 1,000 – 5,000 total reads | Not applicable |
| 🔴 Critical | > 10,000 total reads | **Not triggered** ✅ |

**Overall Risk: 🟢 Low — Total potential reads = 2,000 (within 10,000 limit)**

---

## Compliance Checklist

| Rule | Status | Detail |
|:-----|:-------|:-------|
| `readAll` uses 4-argument form | ✅ Pass | Both calls use `readAll(container, nrOfKeys, maxRecords, closure)` |
| `maxRecords` ≤ 10,000 | ✅ Pass | Both set to 1,000 |
| No `readAll` inside a loop | ✅ Pass | Neither call is inside a for/while/do-while loop |
| No nested `readAll` calls | ✅ Pass | No readAll inside another readAll closure |
| `selectAllFields()` not used | ✅ Pass | Explicit field selection used on all queries |
| Combined reads ≤ 10,000 | ✅ Pass | Total = 2,000 |

---

## MI Calls Performance Notes

The following MI calls are made conditionally and do not involve database loops:

| MI Program | Transaction | Condition | Max Calls Per Execution |
|:-----------|:------------|:----------|:------------------------|
| OIS100MI | GetOrderHead | `if(flag && orno)` — first valid ORNO only | 1 |
| MMS005MI | GetWarehouse | `if(whlo)` — only when WHLO is non-empty | 1 |
| CRS949MI | GetPartnerMedia | `if(cona)` — only when CONA is non-empty | 1 |

All MI calls are guarded and execute at most once per trigger invocation. No performance concerns.

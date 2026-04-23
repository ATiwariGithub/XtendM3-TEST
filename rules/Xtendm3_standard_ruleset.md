# XtendM3 Programming Standard

## JSON File Standards

### Description Requirements
- `description` value must not be blank
- `description` value must be concise and descriptive
- Abbreviations of usual terms like CO, PO, etc. are valid since character length is limited
- Generic descriptions like "test" or "list" should be more descriptive
- `name` value should not be the same as `description` value

### Field Standards
- User-defined fields such as UDN1, UDNx, UCA1, UCAx should be checked if used as generic field in a table
- EXT9XXMI is reserved for standard extensions and should not be used
- Required fields for table extensions: CONO, DIVI, RGDT, RGTM, LMDT, CHNO, CHID
- Generic fields are not allowed (e.g., COA1, COA2, COA3 or OBV1, OBV2, OBV3, or UDF1, UDF2, UDF3) for XtendM3 Tables with prefix EXT

## Groovy File Standards

### Naming Conventions
- **Method Names**: lowerCamelCase, no underscores, start with letter (except "main" method)
- **Variable Names**: lowerCamelCase, No underscores, start with letter
- **Constants**: ALL_CAPITALS with underscores (e.g., UNIT_OF_MEASURE)
- **Descriptive Names**: Use simple and descriptive names (e.g., validateDate, validateType)
- Report unused methods and variables

### Extension Structure
- Global variables and main method declared at the beginning of the program
- variables should be declared at the start of method

### Programming Practices
- Standard M3 fields must be validated against standard M3 tables when used as input to create new records or update tables
- Report unused methods and variables
-
### Prohibited Code Patterns

#### Logging Restrictions
- **Not Allowed**: `logger.info`, `logger.warning`, `logger.error`, `logger.trace`
- **Allowed**: `logger.debug`

#### General Restrictions
- Keyword `def` should not be present
- Usage of sleep or pause in code is not allowed
- `SimpleDateFormat` should not be used; use `DateTimeFormatter` instead
- Report unused import statements

#### Database Query Restrictions
- `selectAllFields()` should not be used; specify required fields only

**Example - Avoid:**
```groovy
DBAction dbaOCUSMA = database.table("OCUSMA").index("00").selectAllFields().build();
```

**Example - Correct:**
```groovy
DBAction dbaOCUSMA = database.table("OCUSMA").index("00").selection("OKCONO", "OKCUNO", "OKCUNM").build();
```

#### Database Performance Limits
- `readAll` third parameter (pageSize/maxRecords) must not exceed 10,000
- `readAll` signature: `DBAction.readAll(DBContainer keyContainer, int nrOfKeys, int nrOfRecords, Closure callback)`
- Variables used as third parameter must not exceed 10,000, including calculated values
- When `readAll` is called within another `readAll` closure, ensure combined effect doesn't exceed 10,000 total operations
- When `readAll` is called inside a loop (for, while, do-while), the total database read count across all loop iterations must not exceed 10,000 records
- Calculate total potential reads as: loop_iterations × readAll_pageSize ≤ 10,000


## Audit Trail Fields

### Database Insert Operations
When using `.insert()`, set these audit trail fields:
- RGDT, RGTM, LMDT, CHID, CHNO

### Database Update Operations
When using `.update()`, set these audit trail fields:
- LMDT, CHID, CHNO

This applies to all database operations including direct `.insert()`/`.update()` calls and operations within readLock closures.

**Example - Correct Update:**
```groovy
lockedResult.set("EXLMDT", utility.call("GeneralManageTime","getServerDate"))
lockedResult.set("EXCHID", program.getUser())
lockedResult.set("EXCHNO", (int)lockedResult.get("EXCHNO")+1)
lockedResult.update()
```

## Code Documentation

### Method Comments
- All methods (except main) must have comments describing functionality
- JavaDoc style recommended but not required
- Both JavaDoc (`/** */`) and regular block comments (`/* */`) acceptable
- Comment convention must be consistent throughout the extension

### Program Header
Every extension requires a program header including:
- Name
- Type
- Script author
- Date
- Description
- Revision history

**Example:**
```groovy
/****************************************************************************************
 Extension Name: EXT000MI/transactionName
 Type: ExtendM3Transaction
 Script Author: 
 Date: 
 Description:
 * Description of script functionality 
  
 Revision History:
 Name                    Date             Version          Description of Changes
 Revision Author         2022-01-01       1.0              Descriptive text here
 Revision Author         2022-02-02       1.1              Outlining what's been updated
 Revision Author         2022-03-03       1.2              In the current revision
*****************************************************************************************/
```

## Code Review Reporting Standards

### Mandatory Requirements
- **MANDATORY**: Generate comprehensive Markdown files reports for all XtendM3 code reviews

### Database Performance Statistics

#### Performance Monitoring
When potential record reads exceed 10,000, generate statistics with columns:
- Program
- Table
- Method Chain
- Operation
- Records_Per_Call
- Loop_Iterations (if applicable)
- Nested_Level
- Total_Potential_Reads
- Risk_Level

For readAll operations inside loops, calculate:
- Total_Potential_Reads = Loop_Iterations × Records_Per_Call × Nested_ReadAll_Count
- Risk_Level = "Critical" if Total_Potential_Reads > 10,000

For database performance violations, automatically generate Markdown analysis:
```
Program,Table,Method Chain, Operation,Records_Per_Call,Loop_Iterations,Nested_Level,Total_Potential_Reads,Risk_Level
```

### Additional Requirements
- Highlight violations exceeding 10,000 database operations limit
- Provide total potential database reads calculation and risk assessment
- Store reports in organized folder reports

### Supporting Files
Generate additional files:
- Markdown documentation for comprehensive reporting
- Database statistics in tabular format with risk levels in Markdown format
- File names should prefix today's date, YYYYMMDD,  if existing increment with version
- Put files in folder reports if not existing create folder

### Report Format Requirements
- Create formatted Mark Down files with:
- Violations summary with severity-based highlighting (Critical in red, High/Medium color-coded)
- Database performance analysis with risk assessments
- Line number references for easy navigation
- Auto-fitted columns and formatted headers
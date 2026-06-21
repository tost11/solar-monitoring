# Permission Management System

This document describes the complete permission management system used in the Solar Monitoring application.

## Overview

The application uses a multi-layered permission system that controls access to solar system data through:
- **Ownership** - Direct ownership of a solar system
- **User Permissions** - Three-tier access levels (ADMIN, MANAGE, VIEW)
- **Public Modes** - Three levels of public visibility (ALL, PRODUCTION, NONE)
- **Manual Override Flags** - System-level settings that bypass certain permission restrictions

## Permission Levels

The system defines four access levels based on the user's relationship to a solar system:

| Level | Description | Source |
|-------|-------------|--------|
| **Owner** | User who owns the system | `SolarSystem.ownedBy` field |
| **ADMIN** | Full administrative access via delegation | `Manages.permission = ADMIN` |
| **MANAGE** | Management access with some restrictions | `Manages.permission = MANAGE` |
| **VIEW** | Read-only access | `Manages.permission = VIEW` |

**Java Reference:** `lib/src/main/java/de/tostsoft/solarmonitoring/lib/model/Permissions.java`

### Operations by Permission Level

| Operation | Owner | ADMIN | MANAGE | VIEW |
|-----------|:-----:|:-----:|:------:|:----:|
| View all data including prices | ✓ | ✓ | ✓ | ✓ |
| View consumption data | ✓ | ✓ | ✓ | ✓ |
| View production data | ✓ | ✓ | ✓ | ✓ |
| Modify system settings | ✓ | ✓ | ✓ | ✗ |
| Add/remove managers | ✓ | ✓ | ✗ | ✗ |
| Delete system | ✓ | ✗ | ✗ | ✗ |
| Change public mode | ✓ | ✓ | ✗ | ✗ |

**Test Coverage:** Authenticated user data access verified in `SolarSystemPermissionTest.testIntegrationProductionModePermissions()` and `SolarSystemTotalDataPermissionTest.testOwnerAndManagersSeeAllDataWithPricing()`

## Public Access Modes

Systems can be made publicly accessible with different visibility levels:

| Mode | Description | Java Constant |
|------|-------------|---------------|
| **NONE** | No public access - authentication required | `PublicMode.NONE` |
| **PRODUCTION** | Public can view production data only | `PublicMode.PRODUCTION` |
| **ALL** | Public can view all data (except prices) | `PublicMode.ALL` |

**Java Reference:** `lib/src/main/java/de/tostsoft/solarmonitoring/lib/model/enums/PublicMode.java`

**Test Coverage:** PRODUCTION and ALL modes tested in `SolarSystemPermissionTest` (7 integration tests) and `SolarSystemTotalDataPermissionTest` (12 tests). NONE mode tested in `SolarSystemPublicModeNoneTest` (4 tests).

## Data Visibility Matrix

### System Information Fields

| Field | Owner/Authenticated | Public (PRODUCTION) | Public (ALL) |
|-------|:-------------------:|:-------------------:|:------------:|
| name | ✓ | ✓ | ✓ |
| description | ✓ | ✓ | ✓ |
| maxInstalledSolarPower | ✓ | ✓ | ✓ |
| buildingDate | ✓ | ✓ | ✓ |
| maxInverterOutputPower | ✓ | ✗ | ✗ |
| batteryCapacity | ✓ | ✗ | ✗ |
| electricityPrice | ✓ | ✗ | ✗ |
| electricityPriceFeedIn | ✓ | ✗ | ✗ |
| publicName (field itself) | ✓ | ✗ | ✗ |

**Notes:**
- Public viewers see `publicName` as `name` if it is set
- Owners/authenticated users see `viewName` as `name` and `publicName` separately

**Java Reference:** `backend/src/main/java/de/tostsoft/solarmonitoring/app/Converter.java:136-151`

**Test Coverage:** System information field filtering verified in `SolarSystemPermissionTest.testIntegrationProductionModePermissions()`. The publicName substitution behavior tested in `SolarSystemNameFieldTest` (3 tests).

### Graph Data Types

#### Production Graphs (Always Visible in PRODUCTION or ALL Mode)

| Graph Filter | Description |
|--------------|-------------|
| INPUT_WATT_DC | Solar panel power input (DC) |
| INPUT_VOLTAGE_DC | Solar panel voltage (DC) |
| INPUT_AMPERE_DC | Solar panel current (DC) |
| *(other production filters)* | Other production-related measurements |

#### Consumption/Battery/Grid Graphs (Hidden in PRODUCTION Mode for Non-Owners)

**Output Measurements:**
- OUTPUT_WATT_DC, OUTPUT_WATT_AC, OUTPUT_WATT_COMBINED
- OUTPUT_VOLTAGE_DC, OUTPUT_VOLTAGE_AC
- OUTPUT_AMPERE_DC, OUTPUT_AMPERE_AC
- OUTPUT_FREQUENCY
- OUTPUT_TOTAL_CONSUMPTION

**Battery Measurements:**
- BATTERY_WATT
- BATTERY_VOLTAGE
- BATTERY_AMPERE
- BATTERY_SOC (State of Charge)

**Grid Measurements:**
- GRID_WATT
- GRID_VOLTAGE
- GRID_AMPERE
- GRID_FREQUENCY

**Temperature:**
- MORE_TEMPERATURE (hidden for public in PRODUCTION mode)

**Java Reference:** `backend/src/main/java/de/tostsoft/solarmonitoring/app/Converter.java:66-96`

**Test Coverage:** Graph filtering by public mode comprehensively tested:
- **Integration tests** (verify HTTP responses): `SolarSystemPermissionTest.testIntegrationProductionModePermissions()`, `testShowGridInfoFalseHidesGridGraphs()`, `testShowGridInfoTrueShowsGridGraphs()`, `testHasTemperatureFalseHidesTemperatureSection()`, `testHasTemperatureTrueShowsTemperatureSection()`, `testHasTemperatureFalseForPublicInProductionMode()`
- **Unit tests** (converter logic only): `testPublicViewerProductionModeRemovesConsumptionFilters()`, `testPublicViewerAllModeKeepsAllFilters()`, `testProductionModeWithAllConsumptionFilters()`, `testOwnerSeesAllFilters()`, `testProductionModeMixedFilters()`, `testProductionModeWithNoConsumptionFilters()`, `testNullGraphFilter()`, `testEmptyGraphFilter()`

### Total Values (Energy Totals)

| Field | Owner/Authenticated | Public (PRODUCTION) | Public (ALL) | With Override Flag |
|-------|:-------------------:|:-------------------:|:------------:|:------------------:|
| producedKWH | ✓ | ✓ | ✓ | ✓ |
| producedKWHPrice | ✓ | ✗ | ✗ | ✓ |
| consumedKWH | ✓ | ✗ | ✓ | ✓ |
| gridConsumedKWH | ✓ | ✗ | ✓ | ✓ |
| gridFeedInKWH | ✓ | ✗ | ✓ | ✓ |
| calcOverallConsumedKWH | ✓ | ✗ | ✓ | ✓ |
| consumedKWHPrice | ✓ | ✗ | ✗ | ✓ (if also ALL mode) |
| gridConsumedKWHPrice | ✓ | ✗ | ✗ | ✓ (if also ALL mode) |
| gridFeedInKWHPrice | ✓ | ✗ | ✗ | ✓ (if also ALL mode) |
| calcOverallConsumedKWHPrice | ✓ | ✗ | ✗ | ✓ (if also ALL mode) |

**Notes:**
- "Override Flag" refers to `totalPricingPublicOverride = true`
- Consumption prices require both the override flag AND ALL mode (nested condition)
- Daily values follow the same visibility rules

**Java Reference:** `backend/src/main/java/de/tostsoft/solarmonitoring/app/controller/InfluxController.java:338-404`

**Test Coverage:** Total values visibility comprehensively tested in `SolarSystemTotalDataPermissionTest`:
- `testPublicProductionModeWithoutOverrideSeesOnlyProductionDataNoPricing()` - PRODUCTION mode without override
- `testPublicProductionModeWithOverrideSeesProductionDataAndPricing()` - PRODUCTION mode with override
- `testPublicAllModeWithoutOverrideSeesAllDataNoPricing()` - ALL mode without override
- `testPublicAllModeWithOverrideSeesAllDataAndAllPricing()` - ALL mode with override
- `testOwnerAndManagersSeeAllDataWithPricing()` - Authenticated users see all data
- `testAllPermissionCombinationsSequentially()` - Sequential permission transitions

## Manual Override Flags

These flags are configured per system in the `ViewData` model and override standard permission rules.

**Java Reference:** `lib/src/main/java/de/tostsoft/solarmonitoring/lib/model/ViewData.java`

### totalPricingPublicOverride

**Type:** Boolean  
**Default:** false

**Purpose:** Allows public viewers to see price data even when they normally wouldn't have access.

**When It Takes Effect:**
- Shows `producedKWHPrice` to public viewers in PRODUCTION mode
- Shows consumption prices (`consumedKWHPrice`, `gridConsumedKWHPrice`, `gridFeedInKWHPrice`) to public viewers in ALL mode
- Applied at lines: `InfluxController.java:339, 348, 363, 395`

**Use Case:** When you want to publicly share your system's financial savings without revealing consumption patterns (use with PRODUCTION mode).

**Test Coverage:** ✓ Tested in `SolarSystemTotalDataPermissionTest.testPublicProductionModeWithOverrideSeesProductionDataAndPricing()` and `testPublicAllModeWithOverrideSeesAllDataAndAllPricing()`

### productionForTotalPricing

**Type:** Boolean  
**Default:** Not specified (null)

**Purpose:** **Frontend-only flag** that controls whether production pricing is included in UI cost/savings calculations. This does NOT filter backend API responses.

**When It Takes Effect:**
- Frontend reads this flag and adjusts pricing displays and calculations
- Backend ignores this flag and always returns all pricing data based on permissions
- Applies to pricing shown in UI dashboards and reports

**Backend Behavior:** The backend ignores this flag. Pricing visibility is controlled by:
- `totalPricingPublicOverride` flag (explicitly controls public pricing visibility)
- PublicMode (PRODUCTION hides pricing from public unless override is true)
- User permissions (authenticated users see all pricing they have access to)

**Use Case:** Adjust frontend pricing displays to show gross costs (without production offset) or net costs (with production factored in).

**Test Coverage:** N/A - Frontend-only flag, no backend test needed

### hideTotalConsumption

**Type:** Boolean  
**Default:** false

**Purpose:** **Frontend-only flag** that controls UI display of consumption totals. This does NOT filter backend API responses.

**When It Takes Effect:**
- Frontend reads this flag and hides consumption UI elements
- Backend ignores this flag and always returns all data based on PublicMode and user permissions
- Applies to all users (owner, managers, viewers)

**Backend Behavior:** The backend ignores this flag. Data filtering is controlled by:
- PublicMode (PRODUCTION hides consumption from public)
- User permissions (authenticated users see all data they have access to)

**Use Case:** Hide consumption displays in the UI while keeping the data available for authenticated users or for programmatic access.

**Test Coverage:** N/A - Frontend-only flag, no backend test needed

### showGridInfo

**Type:** Boolean  
**Default:** false

**Purpose:** Controls visibility of grid-related information and graphs.

**When It Takes Effect:**
- Shows/hides grid measurements (GRID_WATT, GRID_VOLTAGE, GRID_AMPERE, GRID_FREQUENCY)
- Affects both graph display and data availability

**Use Case:** Island systems without grid connection can hide irrelevant grid data.

**Test Coverage:** ✗ Not tested

### hasTemperature

**Type:** Boolean  
**Default:** System-specific

**Purpose:** Controls whether the temperature section/accordion is displayed.

**When It Takes Effect:**
- Automatically set to `false` for public viewers in PRODUCTION mode
- Otherwise follows system configuration

**Use Case:** Systems with temperature sensors can enable this; systems without sensors should disable it.

**Java Reference:** `backend/src/main/java/de/tostsoft/solarmonitoring/app/Converter.java:95`

**Test Coverage:** ✓ Fully tested - MORE_TEMPERATURE filter removal tested in `SolarSystemPermissionTest.testProductionModeWithAllConsumptionFilters()`, and hasTemperature field behavior tested in `testHasTemperatureFalseHidesTemperatureSection()`, `testHasTemperatureTrueShowsTemperatureSection()`, and `testHasTemperatureFalseForPublicInProductionMode()`.

### totalFilter

**Type:** Set<String>  
**Default:** Empty set

**Purpose:** Blacklist of field names to exclude from total data display.

**When It Takes Effect:**
- Filters specified fields from the total values response
- Automatically expands to include "Price2" variants

**Special Behavior:**
- If you add `"SomeFieldPrice"` to the filter, it automatically also blocks `"SomeFieldPrice2"`
- Expansion logic at: `InfluxController.java:324-330`

**Use Case:** Fine-grained control to hide specific metrics (e.g., hide feedInPrice but show other prices).

**Example:**
```java
totalFilter = Set.of("gridFeedInKWHPrice");
// Automatically blocks: "gridFeedInKWHPrice" AND "gridFeedInKWHPrice2"
```

**Test Coverage:** ✓ Tested in `DailyCalculationTotalFilterTest` with 4 test methods covering production, consumption, grid consumed, and grid feed-in field filtering.

### graphFilter

**Type:** Set<GraphFilter>  
**Default:** System-specific

**Purpose:** Whitelist of graph types that are allowed for display.

**When It Takes Effect:**
- Defines which graph types can be shown
- Further filtered by permission level and public mode

**Use Case:** Customize which graphs appear for a specific system (e.g., a battery-less system hides battery graphs).

**Test Coverage:** ✓ Tested in multiple `SolarSystemPermissionTest` methods that verify graphFilter behavior with different permission levels and public modes.

## Permission Check Locations (Developer Reference)

### Service Layer

**File:** `backend/src/main/java/de/tostsoft/solarmonitoring/app/service/SolarSystemService.java`

| Method | Purpose |
|--------|---------|
| `findSystemWithOwnedBy(String systemId)` | Checks if the current user owns the system (line 364) |
| `findSystemWithFullAccess(String systemId)` | Verifies ADMIN permission or ownership (line 370) |
| `findSystemWithMangeAccess(String systemId)` | Checks for ADMIN or MANAGE permission (line 387) |
| `sysemtToAccesPair(SolarSystem system)` | Maps system to access level, returns null if no access (line 404) |
| `getSystemWithUserFromContextOrPublic()` | Main entry point handling both authenticated and public access (line 155) |

### Data Filtering Layer

**File:** `backend/src/main/java/de/tostsoft/solarmonitoring/app/Converter.java`

| Method | Purpose |
|--------|---------|
| `convertToViewDataDTO(ViewData, PublicMode, boolean)` | Filters graph data based on public mode (lines 61-96) |
| `convertToSystemInformationsDTO(SystemInformations, boolean)` | Filters sensitive system information (lines 136-151) |

### Query Layer

**File:** `backend/src/main/java/de/tostsoft/solarmonitoring/app/service/InfluxService.java`

| Method | Purpose |
|--------|---------|
| `generatePublicQueryParameters()` | Restricts database queries to production data only (line 316) |

**Public Query Filter:** When `onlyProduction = true`, queries are limited to:
- `InputWattDC`
- `InputVoltageDC`
- `InputAmpereDC`

This filtering happens at the database query level for efficiency.

### Controller Layer

**File:** `backend/src/main/java/de/tostsoft/solarmonitoring/app/controller/InfluxController.java`

| Method | Purpose |
|--------|---------|
| `totalValuesToJsonObject()` | Complex logic for filtering total values visibility (lines 313-416) |

## Usage Examples

### Example 1: Owner Viewing Their System

**Scenario:** System owner logs in and views their own system.

**Access Level:** Owner

**Visible Data:**
- All production graphs and values
- All consumption graphs and values
- All prices (production and consumption)
- All system information fields including sensitive data
- Temperature data (if configured)
- Grid information (if configured)

**Override Flags:** Manual override flags do not affect owner view.

**Test Reference:** This scenario is tested in `SolarSystemPermissionTest.testOwnerSeesAllFilters()` and `SolarSystemTotalDataPermissionTest.testOwnerAndManagersSeeAllDataWithPricing()`

### Example 2: Public Viewer with PRODUCTION Mode

**Scenario:** Anonymous visitor accesses a system with `PublicMode.PRODUCTION`.

**Access Level:** Public (no authentication)

**Visible Data:**
- Production graphs only (INPUT_WATT_DC, INPUT_VOLTAGE_DC, INPUT_AMPERE_DC)
- `producedKWH` totals
- Basic system info (name, description, maxInstalledSolarPower, buildingDate)

**Hidden Data:**
- All consumption graphs (OUTPUT_*, BATTERY_*, GRID_*)
- All prices (unless `totalPricingPublicOverride = true`)
- Temperature data
- Sensitive system info (batteryCapacity, electricityPrice, maxInverterOutputPower)

**Test Reference:** This scenario is tested in `SolarSystemPermissionTest.testPublicViewerProductionModeRemovesConsumptionFilters()` and `SolarSystemTotalDataPermissionTest.testPublicProductionModeWithoutOverrideSeesOnlyProductionDataNoPricing()`

### Example 3: Public Viewer with ALL Mode

**Scenario:** Anonymous visitor accesses a system with `PublicMode.ALL`.

**Access Level:** Public (no authentication)

**Visible Data:**
- All production graphs
- All consumption graphs
- All battery graphs
- All grid graphs
- All totals (producedKWH, consumedKWH, gridConsumedKWH, gridFeedInKWH)
- Basic system info

**Hidden Data:**
- All prices (unless `totalPricingPublicOverride = true`)
- Sensitive system info (batteryCapacity, electricityPrice, maxInverterOutputPower)

**Test Reference:** This scenario is tested in `SolarSystemPermissionTest.testPublicViewerAllModeKeepsAllFilters()` and `SolarSystemTotalDataPermissionTest.testPublicAllModeWithoutOverrideSeesAllDataNoPricing()`

### Example 4: User with VIEW Permission

**Scenario:** User has been granted VIEW permission via the Manages relationship.

**Access Level:** VIEW

**Visible Data:**
- All graphs (production, consumption, battery, grid)
- All totals including prices
- All system information including sensitive fields
- Temperature data (if configured)

**Key Difference from Owner:**
- Cannot modify system settings
- Cannot add/remove other managers
- Access persists regardless of public mode changes

**Test Reference:** This scenario is tested in `SolarSystemPermissionTest.testIntegrationProductionModePermissions()` which verifies that VIEW permission users see the same data as owners.

### Example 5: Override Flag in Action

**Scenario:** System owner wants to publicly share savings without revealing consumption patterns.

**Configuration:**
- `PublicMode.PRODUCTION`
- `totalPricingPublicOverride = true`

**Public Viewer Sees:**
- Production graphs and values
- `producedKWH` totals
- `producedKWHPrice` (thanks to override flag)

**Public Viewer Does NOT See:**
- Consumption data
- Consumption prices (requires both override AND ALL mode)

**Test Reference:** This scenario is tested in `SolarSystemTotalDataPermissionTest.testPublicProductionModeWithOverrideSeesProductionDataAndPricing()` which verifies that the override flag enables production pricing but not consumption data/pricing in PRODUCTION mode.

## Test Coverage

This section documents which permission behaviors are verified by automated tests and identifies gaps in test coverage.

### Test Suite Overview

The permission system is tested across multiple test files:

| Test File | Purpose | Test Methods |
|-----------|---------|--------------|
| `SolarSystemPermissionTest.java` | Graph filtering and system info visibility | 15 methods |
| `SolarSystemTotalDataPermissionTest.java` | Total data values and pricing visibility | 12 methods |
| `SolarSystemPublicModeNoneTest.java` | PublicMode.NONE access control | 4 methods |
| `SolarSystemNameFieldTest.java` | publicName substitution behavior | 3 methods |
| `DailyCalculationTotalFilterTest.java` | totalFilter flag behavior | 4 methods |
| `MangesTest.java` | Permission relationship management | 1 method |
| `DeleteTest.java` | User deletion cascade and permission cleanup | 3 methods |

**Test File Paths:**
- `backend/src/test/java/de/tostsoft/solarmonitoring/app/solarsystem/`
- `backend/src/test/java/de/tostsoft/solarmonitoring/app/data/`
- `backend/src/test/java/de/tostsoft/solarmonitoring/app/manages/`
- `backend/src/test/java/de/tostsoft/solarmonitoring/app/user/`

### Test Architecture

The permission test suite uses two complementary testing approaches:

**Integration Tests (79% of tests - 27/34):**
- Make actual HTTP GET/POST requests to REST endpoints
- Verify response status codes (200, 401, 403, 404)
- Assert specific fields are present/absent in response JSON
- Test with different authentication contexts (owner, admin, manager, viewer, public)
- Examples: `SolarSystemPermissionTest.testIntegrationProductionModePermissions()`, all tests in `SolarSystemTotalDataPermissionTest`

**Unit Tests (21% of tests - 8/34):**
- Call `Converter.convertToViewDataDTO()` directly without HTTP layer
- Verify converter logic in isolation
- Faster execution but don't verify end-to-end permission flow
- Located in `SolarSystemPermissionTest`: `testOwnerSeesAllFilters()`, `testPublicViewerProductionModeRemovesConsumptionFilters()`, `testPublicViewerAllModeKeepsAllFilters()`, `testProductionModeWithNoConsumptionFilters()`, `testProductionModeWithAllConsumptionFilters()`, `testProductionModeMixedFilters()`, `testNullGraphFilter()`, `testEmptyGraphFilter()`

Integration tests provide the primary verification that permissions work correctly in production. Unit tests supplement by testing edge cases in the conversion logic.

### Test Coverage Matrix

| Permission Feature | Status | Test Class | Test Method |
|--------------------|--------|------------|-------------|
| Owner sees all graphs | ✓ | SolarSystemPermissionTest | testOwnerSeesAllFilters |
| PRODUCTION mode filters graphs | ✓ | SolarSystemPermissionTest | testPublicViewerProductionModeRemovesConsumptionFilters |
| ALL mode keeps all graphs | ✓ | SolarSystemPermissionTest | testPublicViewerAllModeKeepsAllFilters |
| Mixed production/consumption graphs | ✓ | SolarSystemPermissionTest | testProductionModeMixedFilters |
| All consumption graphs removed | ✓ | SolarSystemPermissionTest | testProductionModeWithAllConsumptionFilters |
| Production-only graphs preserved | ✓ | SolarSystemPermissionTest | testProductionModeWithNoConsumptionFilters |
| Owner sees all data+pricing | ✓ | SolarSystemTotalDataPermissionTest | testOwnerAndManagersSeeAllDataWithPricing |
| VIEW permission sees all data | ✓ | SolarSystemPermissionTest | testIntegrationProductionModePermissions |
| MANAGE permission sees all data | ✓ | SolarSystemPermissionTest | testIntegrationProductionModePermissions |
| Permission level changes | ✓ | SolarSystemPermissionTest | testIntegrationPermissionChanges |
| PRODUCTION without override hides prices | ✓ | SolarSystemTotalDataPermissionTest | testPublicProductionModeWithoutOverrideSeesOnlyProductionDataNoPricing |
| PRODUCTION with override shows production prices | ✓ | SolarSystemTotalDataPermissionTest | testPublicProductionModeWithOverrideSeesProductionDataAndPricing |
| ALL without override hides prices | ✓ | SolarSystemTotalDataPermissionTest | testPublicAllModeWithoutOverrideSeesAllDataNoPricing |
| ALL with override shows all prices | ✓ | SolarSystemTotalDataPermissionTest | testPublicAllModeWithOverrideSeesAllDataAndAllPricing |
| All permission combinations | ✓ | SolarSystemTotalDataPermissionTest | testAllPermissionCombinationsSequentially |
| maxInverterOutputPower hidden from public | ✓ | SolarSystemPermissionTest | testIntegrationProductionModePermissions |
| maxInstalledSolarPower visible to public | ✓ | SolarSystemPermissionTest | testIntegrationProductionModePermissions |
| totalFilter blocks production fields | ✓ | DailyCalculationTotalFilterTest | checkDailyCalculationCorrect |
| totalFilter blocks consumption fields | ✓ | DailyCalculationTotalFilterTest | checkDailyCalculationConsumedCorrect |
| totalFilter blocks grid consumed fields | ✓ | DailyCalculationTotalFilterTest | checkDailyCalculationGridConsumedCorrect |
| totalFilter blocks grid feed-in fields | ✓ | DailyCalculationTotalFilterTest | checkDailyCalculationGridFeedInCorrect |
| Permission soft delete | ✓ | MangesTest | checkDeleteAttWorking |
| User deletion cascades permissions | ✓ | DeleteTest | UserDeleteCheckAllRelationDeleted |
| Null graphFilter handling | ✓ | SolarSystemPermissionTest | testNullGraphFilter |
| Empty graphFilter handling | ✓ | SolarSystemPermissionTest | testEmptyGraphFilter |
| ADMIN permission access | ✓ | SolarSystemPermissionTest | testIntegrationProductionModePermissions |
| hideTotalConsumption flag | N/A | N/A | Frontend-only flag |
| showGridInfo flag | ✓ | SolarSystemPermissionTest | testShowGridInfoFalseHidesGridGraphs |
| hasTemperature flag behavior | ✓ | SolarSystemPermissionTest | testHasTemperatureFalseHidesTemperatureSection |
| productionForTotalPricing flag | N/A | N/A | Frontend-only flag |
| PublicMode.NONE denies access | ✓ | SolarSystemPublicModeNoneTest | testPublicModeNoneDeniesPublicAccess |
| Database query filtering (onlyProduction) | ✓ | SolarSystemTotalDataPermissionTest | testPublicQueriesOnlyProductionData |
| publicName substitution | ✓ | SolarSystemNameFieldTest | testPublicNameSubstitutionForPublicViewer |

**Legend:**
- ✓ = Fully tested
- ⚠ = Partially tested (indirect coverage or incomplete scenarios)
- ✗ = Not tested

### Tested Scenarios Details

#### Graph Filtering Tests (SolarSystemPermissionTest)

**testOwnerSeesAllFilters()**
- Verifies owners see all configured graph filters regardless of public mode
- Tests with `PublicMode.PRODUCTION` and `isOwner=true`
- Confirms all 3 test filters remain visible (INPUT_FREQUENCY, BATTERY_SOC, GRID_WATT)

**testPublicViewerProductionModeRemovesConsumptionFilters()**
- Verifies PRODUCTION mode filters out consumption-related graphs for non-owners
- Tests removal of OUTPUT_WATT_AC and BATTERY_SOC
- Confirms only INPUT_FREQUENCY (production) remains

**testPublicViewerAllModeKeepsAllFilters()**
- Verifies ALL mode permits all graph filters for public viewers
- Tests with `PublicMode.ALL` and `isOwner=false`
- Confirms all 3 filters kept (INPUT_FREQUENCY, BATTERY_SOC, GRID_WATT)

**testProductionModeWithAllConsumptionFilters()**
- Tests comprehensive filtering when owner has all consumption/battery/grid graphs configured
- Verifies all 18 consumption-related filters are removed
- Confirms MORE_TEMPERATURE filter is removed for public viewers

**testIntegrationProductionModePermissions()**
- End-to-end integration test with multiple users
- Tests Owner, Viewer (VIEW), Manager (MANAGE), and Public access
- Verifies authenticated users see all 5 graphs regardless of permission level
- Confirms public viewers in PRODUCTION mode see only 2 production graphs
- Validates maxInverterOutputPower is hidden from public but visible to authenticated users

**testIntegrationPermissionChanges()**
- Tests permission level transitions: VIEW → MANAGE → ADMIN
- Confirms graph visibility remains consistent across permission changes

#### Total Data Permission Tests (SolarSystemTotalDataPermissionTest)

**testOwnerAndManagersSeeAllDataWithPricing()**
- Verifies owner and all permission levels (ADMIN, MANAGE, VIEW) see complete data
- Tests visibility of: producedKWH, consumedKWH, gridConsumedKWH, gridFeedInKWH
- Confirms all pricing fields visible: producedKWHPrice, consumedKWHPrice, grid prices
- Validates daily totals (producedKWHDay, consumedKWHDay, etc.)

**testPublicProductionModeWithoutOverrideSeesOnlyProductionDataNoPricing()**
- Tests `PublicMode.PRODUCTION` with `totalPricingPublicOverride=false`
- Confirms public viewers see: producedKWH, producedKWHDay
- Confirms public viewers do NOT see: any prices, consumption, grid data

**testPublicProductionModeWithOverrideSeesProductionDataAndPricing()**
- Tests `PublicMode.PRODUCTION` with `totalPricingPublicOverride=true`
- Confirms override enables: producedKWHPrice, producedKWHPriceDay
- Confirms override does NOT enable: consumption data or consumption prices

**testPublicAllModeWithoutOverrideSeesAllDataNoPricing()**
- Tests `PublicMode.ALL` with `totalPricingPublicOverride=false`
- Confirms public viewers see: all production and consumption data
- Confirms public viewers do NOT see: any pricing fields

**testPublicAllModeWithOverrideSeesAllDataAndAllPricing()**
- Tests `PublicMode.ALL` with `totalPricingPublicOverride=true`
- Confirms public viewers see: all production and consumption data
- Confirms override enables: all pricing fields (production and consumption)

**testAllPermissionCombinationsSequentially()**
- Tests all combinations on same system: Owner → PRODUCTION (no override) → PRODUCTION (with override) → ALL (no override) → ALL (with override)
- Validates proper state transitions

#### totalFilter Tests (DailyCalculationTotalFilterTest)

**checkDailyCalculationCorrect()**
- Tests totalFilter can hide producedKWH and related fields
- Verifies filtering at database and API response level

**checkDailyCalculationConsumedCorrect()**
- Tests totalFilter can hide consumedKWH fields

**checkDailyCalculationGridConsumedCorrect()**
- Tests totalFilter can hide gridConsumedKWH fields

**checkDailyCalculationGridFeedInCorrect()**
- Tests totalFilter can hide gridFeedInKWH fields

### Previously Untested Scenarios (Now Tested)

All previously untested permission features now have comprehensive test coverage:

#### 1. showGridInfo Flag ✓
**Tests Added:**
- `SolarSystemPermissionTest.testShowGridInfoFalseHidesGridGraphs()`
- `SolarSystemPermissionTest.testShowGridInfoTrueShowsGridGraphs()`

**Coverage:** Verifies grid graphs (GRID_WATT, GRID_VOLTAGE, GRID_AMPERE, GRID_FREQUENCY) are filtered based on flag.

#### 2. hasTemperature Flag ✓
**Tests Added:**
- `SolarSystemPermissionTest.testHasTemperatureFalseHidesTemperatureSection()`
- `SolarSystemPermissionTest.testHasTemperatureTrueShowsTemperatureSection()`
- `SolarSystemPermissionTest.testHasTemperatureFalseForPublicInProductionMode()`

**Coverage:** Complete direct testing of hasTemperature field behavior and automatic override for public viewers.

#### 3. PublicMode.NONE ✓
**Test Class Created:** `SolarSystemPublicModeNoneTest`

**Tests Added:**
- `testPublicModeNoneDeniesPublicAccess()` - Verifies HTTP 401/403 for public
- `testPublicModeNoneAllowsOwnerAccess()` - Verifies owner can access
- `testPublicModeNoneAllowsExplicitPermissionAccess()` - Verifies users with permissions can access
- `testPublicModeNoneHidesSystemFromPublicListing()` - Verifies system hidden from public list

**Coverage:** Complete access control testing for NONE mode.

#### 6. ADMIN Permission Operations ✓
**Action Taken:**
- Uncommented existing ADMIN test in `SolarSystemPermissionTest.testIntegrationProductionModePermissions()`

**Coverage:** Verifies ADMIN users see all data like owners.

#### 7. Database Query Filtering (onlyProduction) ✓
**Tests Added:**
- `SolarSystemTotalDataPermissionTest.testPublicQueriesOnlyProductionData()`
- `SolarSystemTotalDataPermissionTest.testAuthenticatedUserQueriesAllData()`

**Coverage:** Verifies public queries are restricted to production data, authenticated users get all data.

#### 8. publicName Substitution ✓
**Test Class Created:** `SolarSystemNameFieldTest`

**Tests Added:**
- `testPublicNameSubstitutionForPublicViewer()` - Verifies public sees publicName as 'name'
- `testViewNameVisibleToAuthenticatedUsers()` - Verifies authenticated users see viewName and publicName separately
- `testPublicNameNullFallsBackToViewName()` - Verifies fallback behavior

**Coverage:** Complete name field substitution testing for public vs authenticated access.

### Running Permission Tests

Execute permission tests from project root:

```bash
# All permission tests
mvn test -Dtest=SolarSystemPermissionTest
mvn test -Dtest=SolarSystemTotalDataPermissionTest
mvn test -Dtest=SolarSystemPublicModeNoneTest
mvn test -Dtest=SolarSystemNameFieldTest

# Specific test method
mvn test -Dtest=SolarSystemPermissionTest#testIntegrationProductionModePermissions
mvn test -Dtest=SolarSystemPublicModeNoneTest#testPublicModeNoneDeniesPublicAccess

# All tests in solarsystem package
mvn test -Dtest="de.tostsoft.solarmonitoring.app.solarsystem.*Test"
```

**Prerequisites:**
- MongoDB and InfluxDB running (via `docker-compose-env.yml`)
- Spring profile: `local`

## Summary

The permission system follows this hierarchy:

1. **Owner** - Full unrestricted access
2. **Authenticated Users (ADMIN/MANAGE/VIEW)** - Full data access, limited operations based on level
3. **Public (with ALL mode)** - All graphs and totals, no prices, no sensitive system info
4. **Public (with PRODUCTION mode)** - Production graphs only, no consumption/battery/grid, no prices
5. **Public (with NONE mode)** - No access

**Manual override flags** can selectively expose specific data (like prices) to public viewers without changing their overall access level.

# playwright-demo (Java + Playwright + TestNG)

Small demo project that runs UI smoke checks with **Playwright for Java** + **TestNG**, executed via **Maven**.
It validates that key pages on `https://jiteconsult.com` contain visible, non-empty headings.

## What’s inside

- **Playwright (Java)**: browser automation
- **TestNG**: test framework (groups + suite XML)
- **Maven**: build & run (Surefire)

Key files:

- `pom.xml`  
  Maven dependencies + Surefire config.

- `src/test/resources/testng-smoke.xml`  
  TestNG suite definition (smoke run).

- `src/test/java/org/example/BaseTest.java`  
  Playwright-safe lifecycle:
  - 1 Playwright + 1 Browser per suite
  - 1 BrowserContext + 1 Page per test method (ThreadLocal)

- `src/test/java/org/example/HeadingsTest.java`  
  DataProvider-driven smoke test that visits pages and prints headings.

## Prerequisites

- Java (recommended: 17+; local can be 11 if your pom targets 11)
- Maven 3.8+
- Playwright browsers installed (first time)

### Install Playwright browsers

```bash
mvn -q exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"

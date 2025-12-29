package org.example;

import com.microsoft.playwright.*;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.nio.file.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class BaseTest {

  private static Playwright playwright;
  private static Browser browser;

  private final ThreadLocal<BrowserContext> tlContext = new ThreadLocal<>();
  private final ThreadLocal<Page> tlPage = new ThreadLocal<>();

  protected Page page() { return tlPage.get(); }
  protected BrowserContext context() { return tlContext.get(); }

  private static final Path ARTIFACTS_DIR   = Paths.get("target");
  private static final Path SCREENSHOT_DIR  = ARTIFACTS_DIR.resolve("screenshots");
  private static final Path TRACE_DIR       = ARTIFACTS_DIR.resolve("traces");
  private static final Path VIDEO_DIR       = ARTIFACTS_DIR.resolve("videos");

  @BeforeSuite(alwaysRun = true)
  public void beforeSuite() throws Exception {
    boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
    String browserName = System.getProperty("browser", "chromium").toLowerCase();

    Files.createDirectories(SCREENSHOT_DIR);
    Files.createDirectories(TRACE_DIR);
    Files.createDirectories(VIDEO_DIR);

    playwright = Playwright.create();

    BrowserType type;
    if ("firefox".equals(browserName)) type = playwright.firefox();
    else if ("webkit".equals(browserName)) type = playwright.webkit();
    else type = playwright.chromium();

    browser = type.launch(new BrowserType.LaunchOptions().setHeadless(headless));
  }

  @BeforeMethod(alwaysRun = true)
  public void beforeMethod(ITestResult result) {
    boolean video = Boolean.parseBoolean(System.getProperty("video", "false"));
    boolean trace = Boolean.parseBoolean(System.getProperty("trace", "true"));

    Browser.NewContextOptions ctxOptions = new Browser.NewContextOptions();

    if (video) {
      ctxOptions
          .setRecordVideoDir(VIDEO_DIR)
          .setRecordVideoSize(1280, 720);
    }

    BrowserContext context = browser.newContext(ctxOptions);
    Page page = context.newPage();

    if (trace) {
      context.tracing().start(new Tracing.StartOptions()
          .setScreenshots(true)
          .setSnapshots(true)
          .setSources(true)
      );
    }

    tlContext.set(context);
    tlPage.set(page);
  }

  @AfterMethod(alwaysRun = true)
  public void afterMethod(ITestResult result) {
    boolean trace = Boolean.parseBoolean(System.getProperty("trace", "true"));
    boolean screenshotOnFail = Boolean.parseBoolean(System.getProperty("screenshotOnFail", "true"));

    BrowserContext ctx = tlContext.get();
    Page page = tlPage.get();

    String testId = buildTestId(result);

    // 1) Screenshot on failure (needs page still open)
    if (screenshotOnFail && !result.isSuccess()) {
      try {
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(SCREENSHOT_DIR.resolve(testId + ".png"))
            .setFullPage(true));
      } catch (Exception ignored) {}
    }

    // 2) Stop tracing (before closing context)
    if (trace && ctx != null) {
      try {
        ctx.tracing().stop(new Tracing.StopOptions()
            .setPath(TRACE_DIR.resolve(testId + ".zip")));
      } catch (Exception ignored) {}
    }

    // 3) Close context (video file is finalized on close)
    try { if (ctx != null) ctx.close(); } catch (Exception ignored) {}

    // 4) Rename/copy video to a readable filename (optional but nice)
    try {
      if (page != null && page.video() != null) {
        Path raw = page.video().path(); // available after close
        if (raw != null && Files.exists(raw)) {
          Path named = VIDEO_DIR.resolve(testId + ".webm");
          Files.copy(raw, named, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    } catch (Exception ignored) {}

    tlPage.remove();
    tlContext.remove();
  }

  @AfterSuite(alwaysRun = true)
  public void afterSuite() {
    try { if (browser != null) browser.close(); } catch (Exception ignored) {}
    try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
  }

  private static String buildTestId(ITestResult result) {
    String cls = result.getTestClass().getRealClass().getSimpleName();
    String method = result.getMethod().getMethodName();

    String params = "";
    Object[] p = result.getParameters();
    if (p != null && p.length > 0) {
      params = Arrays.stream(p)
          .map(o -> o == null ? "null" : o.toString())
          .collect(Collectors.joining("_", "_", ""));
    }

    return sanitize(cls + "_" + method + params);
  }

  private static String sanitize(String s) {
    return s.replaceAll("[^a-zA-Z0-9._-]+", "_");
  }
}

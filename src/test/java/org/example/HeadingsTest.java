package org.example;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

public class HeadingsTest extends BaseTest {

  @DataProvider(name = "pages", parallel = false)
  public Object[][] pages() {
    return new Object[][]{
        {"home",        "https://jiteconsult.com/"},
        {"how-it-works","https://jiteconsult.com/how-it-works"},
        {"packages",    "https://jiteconsult.com/packages"},
        {"coaching",    "https://jiteconsult.com/coaching"},
        {"our-mission", "https://jiteconsult.com/our-mission"},
        {"about-us",    "https://jiteconsult.com/about-us"},
        {"contact",     "https://jiteconsult.com/contact"}
    };
  }

  @Test(dataProvider = "pages", groups = {"smoke", "ui"})
  public void headingsArePresentAndNonEmpty(String pageName, String url) {
    Page page = page();   // ThreadLocal page from BaseTest

    page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    page.waitForLoadState(LoadState.NETWORKIDLE);

    dismissCookieBannerIfPresent(page);

    // 1) Collect visible, non-empty headings from HTML tags (h1..h6) + ARIA role=heading
    Locator tagHeadings = page.locator("h1, h2, h3, h4, h5, h6");
    Locator ariaHeadings = page.getByRole(AriaRole.HEADING);

    List<String> visibleHeadings = new ArrayList<>();
    visibleHeadings.addAll(collectVisibleNonEmptyTexts(tagHeadings));
    visibleHeadings.addAll(collectVisibleNonEmptyTexts(ariaHeadings));
    visibleHeadings = dedupePreserveOrder(visibleHeadings);

    Assert.assertTrue(
        !visibleHeadings.isEmpty(),
        "[" + pageName + "] No visible non-empty headings found on " + url
    );

    // 2) Optional SEO check: warn if there is no H1 (do not fail by default)
    int visibleNonEmptyH1 = countVisibleNonEmpty(page.locator("h1"));
    int visibleNonEmptyAriaH1 = countVisibleNonEmpty(
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))
    );

    boolean requireH1 = Boolean.parseBoolean(System.getProperty("requireH1", "false"));
    if ((visibleNonEmptyH1 + visibleNonEmptyAriaH1) == 0) {
      String msg = "[" + pageName + "] WARN: No visible non-empty H1 (tag <h1> or role=heading level=1) on " + url;
      System.out.println(msg);
      if (requireH1) {
        Assert.fail(msg);
      }
    }

    // 3) Fail only if there are VISIBLE blank HTML heading tags (h1..h6)
    List<String> visibleBlanks = collectVisibleBlankHeadingDebug(tagHeadings);
    Assert.assertEquals(
        visibleBlanks.size(), 0,
        "[" + pageName + "] Found visible blank HTML heading tag(s) on " + url + ":\n" + String.join("\n", visibleBlanks)
    );

    System.out.println("\n=== " + pageName + " === " + url);
    System.out.println("H1 visible non-empty: tag=" + visibleNonEmptyH1 + ", aria(level=1)=" + visibleNonEmptyAriaH1);
    System.out.println("Headings found: " + visibleHeadings.size());
    visibleHeadings.forEach(h -> System.out.println(" - " + h));
  }

  private void dismissCookieBannerIfPresent(Page page) {
    try {
      Pattern p = Pattern.compile("accept|allow|ok|akkoord|alles accepteren|agree|consent", Pattern.CASE_INSENSITIVE);
      Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(p)).first();
      if (btn.count() > 0 && btn.isVisible()) {
        btn.click(new Locator.ClickOptions().setTimeout(1500));
      }
    } catch (Exception ignored) {
      // ignore
    }
  }

  private int countVisibleNonEmpty(Locator locator) {
    int count = locator.count();
    int ok = 0;
    for (int i = 0; i < count; i++) {
      Locator el = locator.nth(i);
      if (isVisibleFast(el)) {
        String t = normalize(safeInnerText(el));
        if (!t.isBlank()) ok++;
      }
    }
    return ok;
  }

  private List<String> collectVisibleNonEmptyTexts(Locator locator) {
    List<String> out = new ArrayList<>();
    int count = locator.count();
    for (int i = 0; i < count; i++) {
      Locator el = locator.nth(i);
      if (isVisibleFast(el)) {
        String t = normalize(safeInnerText(el));
        if (!t.isBlank()) out.add(t);
      }
    }
    return out;
  }

  // Helpful debug: show which visible heading is blank + a small outerHTML snippet
  private List<String> collectVisibleBlankHeadingDebug(Locator locator) {
    List<String> out = new ArrayList<>();
    int count = locator.count();
    for (int i = 0; i < count; i++) {
      Locator el = locator.nth(i);
      if (isVisibleFast(el)) {
        String t = normalize(safeInnerText(el));
        if (t.isBlank()) {
          String html = "";
          try { html = String.valueOf(el.evaluate("e => e.outerHTML")); } catch (Exception ignored) {}
          if (html.length() > 200) html = html.substring(0, 200) + "...";
          out.add(" - index " + i + ": " + html);
        }
      }
    }
    return out;
  }

  private boolean isVisibleFast(Locator el) {
  try {
    return el.isVisible(); // no deprecated API
  } catch (Exception e) {
    return false;
  }
}

  private String safeInnerText(Locator el) {
    try { return el.innerText(); } catch (Exception e) { return ""; }
  }

  private static String normalize(String s) {
    if (s == null) return "";
    return s.replace('\u00A0', ' ')
        .replaceAll("\\s+", " ")
        .trim();
  }

  private static List<String> dedupePreserveOrder(List<String> items) {
    return new ArrayList<>(new LinkedHashSet<>(items));
  }
}

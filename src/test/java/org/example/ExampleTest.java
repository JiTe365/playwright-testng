package org.example;

import com.microsoft.playwright.*;
import org.testng.annotations.Test;

public class ExampleTest {

  @Test
  public void opensExampleDotCom() {
    try (Playwright playwright = Playwright.create()) {
      Browser browser = playwright.chromium().launch(
          new BrowserType.LaunchOptions().setHeadless(true)
      );

      Page page = browser.newPage();
      page.navigate("https://jiteconsult.com");

      System.out.println("Final URL: " + page.url());
      System.out.println("Title    : " + page.title());

      assert page.title().toLowerCase().contains("home");

      browser.close();
    }
  }
}




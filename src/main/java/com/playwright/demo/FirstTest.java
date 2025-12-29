package com.playwright.demo;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class FirstTest {
    public static void main(String[] args) {
        // Create Playwright instance
        try (Playwright playwright = Playwright.create()) {
            // Launch a Chromium browser
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false) // set true for headless
            );

            // Open a new page
            Page page = browser.newPage();

            // Navigate to Playwright website
            page.navigate("https://playwright.dev");

            // Print page title
            System.out.println("Page title: " + page.title());

            // Close browser
            browser.close();
        }
    }
}
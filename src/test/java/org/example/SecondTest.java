package org.example;

import org.testng.annotations.Test;

public class SecondTest {
  @Test
  public void secondTestRuns() {
    System.out.println("SecondTest is running");
    assert true;
  }
}

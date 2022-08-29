package com.box.l10n.mojito;

import java.io.IOException;

public class RestartableApplication {

  public static void main(String[] args) throws IOException {
    killRunningApplication();
    Application.main(args);
  }

  /**
   * TODO(P1) move this out of the code, this to be able run/debug stuff in Netbeans
   * https://netbeans.org/bugzilla/show_bug.cgi?id=245474
   *
   * <p>see springApplication.addListeners(new ApplicationPidListener("application.pid"));
   *
   * @throws IOException
   */
  private static void killRunningApplication() throws IOException {
    String[] cmd = {"/bin/sh", "-c", "kill -9 $(cat application.pid)"};
    Process p = Runtime.getRuntime().exec(cmd);
  }
}

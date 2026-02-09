package com.box.l10n.mojito.react;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Serves the new frontend (built into public/n) for client routes while letting static assets
 * (/n/assets/**) be handled by the resource pipeline.
 */
@Controller
public class NewFrontendController {

  @RequestMapping({
    "/n",
    "/n/",
    // Forward any non-asset, non-file path under /n/** to the SPA index
    "/n/{path:^(?!assets$|index\\.html$)[^.]*$}",
    "/n/{path:^(?!assets$|index\\.html$)[^.]*$}/**"
  })
  public String forwardNewApp() {
    return "forward:/n/index.html";
  }
}

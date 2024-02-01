package com.box.l10n.mojito.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.Reader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mustache.MustacheResourceTemplateLoader;
import org.springframework.stereotype.Component;

/**
 * @author jeanaurambault
 */
@Component
public class MustacheTemplateEngine {

  @Autowired Mustache.Compiler compiler;

  @Autowired MustacheResourceTemplateLoader mustacheResourceTemplateLoader;

  public String render(String templateName, MustacheBaseContext mustacheContext) {
    try {
      Reader template = mustacheResourceTemplateLoader.getTemplate(templateName);
      Template compile = compiler.compile(template);
      return compile.execute(mustacheContext);
    } catch (Exception e) {
      throw new RuntimeException("Can't render template: " + templateName, e);
    }
  }
}

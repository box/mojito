package com.box.l10n.mojito.rest.cli;

import com.ibm.icu.text.MessageFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jaurambault
 */
@RestController
public class CliWS {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CliWS.class);

    @Value("${info.build.version}")
    String version;

    @Autowired @Qualifier("GitInfoWebapp")
    GitInfo gitInfo;

    @Value("${cli.url}")
    String cliUrl;

    @Value("${cli.file}")
    String cliFile;

    /**
     * Entry point do download the CLI that correspond to this server version.
     * 
     * Serve a local file (referenced in "cli.file" property) if it exists or 
     * redirects to a URL build out of the "cli.url" property which can be a
     * template with following placeholder: {version}, {gitCommit} and 
     * {gitCommitShort}
     * 
     * By default Github release is referenced, so that it works for standard 
     * releases process.
     * 
     * @param httpServletResponse
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/cli/mojito-cli.jar", method = RequestMethod.GET)
    @ResponseBody
    public FileSystemResource getFile(HttpServletResponse httpServletResponse) throws IOException {

        Path cliFilePath = Paths.get(cliFile);

        if (Files.exists(cliFilePath)) {
            return new FileSystemResource(cliFilePath.toFile());
        } else {
            httpServletResponse.sendRedirect(getUrl());
            return null;
        }
    }

    public String getUrl() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("version", version);
        arguments.put("gitCommit", gitInfo.getCommit().getId());
        arguments.put("gitShortCommit", gitInfo.getCommit().getId().substring(0, 7));

        String format = MessageFormat.format(cliUrl, arguments);
        return format;
    }
}

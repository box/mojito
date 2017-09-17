package com.box.l10n.mojito.rest.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * To optionally load info about git
 *
 * @author jaurambault
 */
@Component("GitInfoWebapp")
@ConfigurationProperties(prefix = "git", locations = "classpath:git.properties")
public class GitInfo {

    private String branch;

    private final Commit commit = new Commit();

    public String getBranch() {
        return this.branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Commit getCommit() {
        return this.commit;
    }

    public static class Commit {

        private String id;

        private String time;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTime() {
            return this.time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }

}

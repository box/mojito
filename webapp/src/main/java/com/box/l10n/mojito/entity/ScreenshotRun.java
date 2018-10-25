package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Used to group together screenshots for example when coming from the same test
 * suite execution.
 *
 * @author jaurambault
 */
@Entity
@Table(name = "screenshot_run",
        indexes = {
            @Index(name = "UK__SCREENSHOT_RUN__NAME", columnList = "name", unique = true)
        }
)
public class ScreenshotRun extends SettableAuditableEntity {

    @ManyToOne
    @JoinColumn(name = "repository_id", foreignKey = @ForeignKey(name = "FK__SCREENSHOT_RUN__REPOSITORY__ID"), nullable = false)
    private Repository repository;

    @Column(name = "name")
    private String name;

    @JsonDeserialize(as = LinkedHashSet.class)
    @OneToMany(mappedBy = "screenshotRun", fetch = FetchType.EAGER)
    Set<Screenshot> screenshots = new LinkedHashSet<>();

    @Column(name = "lastSuccessfulRun")
    Boolean lastSuccessfulRun = false;

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Screenshot> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(Set<Screenshot> screenshots) {
        this.screenshots = screenshots;
    }

    public Boolean getLastSuccessfulRun() {
        return lastSuccessfulRun;
    }

    public void setLastSuccessfulRun(Boolean lastSuccessfulRun) {
        this.lastSuccessfulRun = lastSuccessfulRun;
    }

}

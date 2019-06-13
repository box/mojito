package com.box.l10n.mojito.service.branch.notification.phabricator;

import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.box.l10n.mojito.utils.ServerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {
        BranchNotificationMessageBuilderPhabricator.class,
        BranchNotificationMessageBuilderPhabricatorTest.class,
        BranchUrlBuilder.class,
        ServerConfig.class})
@EnableAutoConfiguration
@IntegrationTest("spring.datasource.initialize=false")
public class BranchNotificationMessageBuilderPhabricatorTest {

    @Autowired
    BranchNotificationMessageBuilderPhabricator branchNotificationMessageBuilderPhabricator;

    @Test
    public void getNewMessage() {
        String newMessage = branchNotificationMessageBuilderPhabricator.getNewMessage("branchTest", Arrays.asList("string1", "string2"));
        assertEquals("We received your strings! Please **add screenshots** as soon as possible and **wait for translations** " +
                "before releasing. [→ Go to Mojito](http://localhost:8080/branches?searchText=branchTest&deleted=false)\n" +
                "\n" +
                "**Strings:**\n" +
                " - string1\n" +
                " - string2", newMessage);
    }


    @Test
    public void getUpdatedMessage() {
        String updatedMessage = branchNotificationMessageBuilderPhabricator.getUpdatedMessage("branchTest", Arrays.asList("string1", "string2"));
        assertEquals("Your branch was updated with new strings! Please **add screenshots** as soon as possible and **wait for translations** before " +
                "releasing. [→ Go to Mojito](http://localhost:8080/branches?searchText=branchTest&deleted=false)\n" +
                "\n" +
                "**Strings:**\n" +
                " - string1\n" +
                " - string2", updatedMessage);
    }

}
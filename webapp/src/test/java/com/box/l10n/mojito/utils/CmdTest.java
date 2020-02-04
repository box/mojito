package com.box.l10n.mojito.utils;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(properties = "spring.datasource.initialize=false",
        classes = {Cmd.class, CmdTest.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CmdTest {

    @Value("#{ T(com.box.l10n.mojito.utils.Cmd).getOutputIfSuccess('echo injection') }")
    String injection;

    @Before
    public void before() {
        Assume.assumeTrue(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX);
    }

    @Test
    public void getOutputIfSuccess() throws Exception {
        Cmd cmd = new Cmd();
        String output = cmd.getOutputIfSuccess("echo coucou");
        Assert.assertEquals("coucou", output);
    }

    @Test
    public void valueInjection() throws Exception {
        Assert.assertEquals("injection", injection);
    }
    
    @Test
    public void exceptionIfFailure() throws Exception {
        Cmd cmd = new Cmd();
        try {
            String output = cmd.getOutputIfSuccess("ls somefilethatdoesntexistfortesting");
        } catch (RuntimeException re) {
            Assert.assertEquals("Command must succeed:ls somefilethatdoesntexistfortesting", re.getMessage());
        }
    }

}
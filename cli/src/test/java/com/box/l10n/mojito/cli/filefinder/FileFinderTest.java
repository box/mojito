package com.box.l10n.mojito.cli.filefinder;

import com.box.l10n.mojito.cli.filefinder.file.AndroidStringsFileType;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.MacStringsFileType;
import com.box.l10n.mojito.cli.filefinder.file.PropertiesFileType;
import com.box.l10n.mojito.cli.filefinder.file.PropertiesNoBasenameFileType;
import com.box.l10n.mojito.cli.filefinder.file.ReswFileType;
import com.box.l10n.mojito.cli.filefinder.file.ResxFileType;
import com.box.l10n.mojito.cli.filefinder.file.XliffFileType;
import com.box.l10n.mojito.cli.filefinder.file.XliffNoBasenameFileType;
import com.box.l10n.mojito.test.IOTestBase;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jaurambault
 */
public class FileFinderTest extends IOTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(FileFinderTest.class);

    @Test
    public void findProperties() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(false, new PropertiesFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(PropertiesFileType.class, next.fileType.getClass());
        assertEquals(getInputResourcesTestDir("source").toString() + "/filefinder.properties", next.getPath().toString());
        assertEquals("filefinder_fr_FR.properties", next.getTargetPath("fr_FR"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/sub/filefinder2.properties", next.getPath().toString());
        assertEquals("sub/filefinder2_fr_FR.properties", next.getTargetPath("fr_FR"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/filefinder_fr.properties", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/filefinder_fr_FR.properties", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/sub/filefinder2_fr.properties", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findPropertiesNoBasename() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(false, new PropertiesNoBasenameFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(PropertiesNoBasenameFileType.class, next.fileType.getClass());
        assertEquals(getInputResourcesTestDir("source").toString() + "/en.properties", next.getPath().toString());
        assertEquals("fr-FR.properties", next.getTargetPath("fr-FR"));

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr-FR.properties", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr.properties", itTargets.next().getPath().toString());

        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findPropertiesNoBasenameEnUs() throws IOException, FileFinderException {
        PropertiesNoBasenameFileType propertiesNoBasenameFileType = new PropertiesNoBasenameFileType();
        propertiesNoBasenameFileType.getLocaleType().setSourceLocale("en-US");

        FileFinder fileFinder = initFileFinder(false, propertiesNoBasenameFileType);

        ArrayList<FileMatch> sources = fileFinder.getSources();
        Collections.sort(sources);

        ArrayList<FileMatch> targets = fileFinder.getTargets();
        Collections.sort(targets);

        Iterator<FileMatch> itSources = sources.iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/en-US.properties", next.getPath().toString());
        assertEquals(PropertiesNoBasenameFileType.class, next.fileType.getClass());
        assertEquals("fr-FR.properties", next.getTargetPath("fr-FR"));

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr-FR.properties", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr.properties", itTargets.next().getPath().toString());

        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findXliff() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(false);

        ArrayList<FileMatch> sources = fileFinder.getSources();
        Collections.sort(sources);

        ArrayList<FileMatch> targets = fileFinder.getTargets();
        Collections.sort(targets);

        Iterator<FileMatch> itSources = sources.iterator();

        FileMatch next = itSources.next();
        assertEquals(XliffFileType.class, next.fileType.getClass());
        assertEquals(getInputResourcesTestDir("source").toString() + "/filefinder.xliff", next.getPath().toString());
        assertEquals("filefinder_fr-FR.xliff", next.getTargetPath("fr-FR"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/sub/filefinder2.xliff", next.getPath().toString());
        assertEquals("sub/filefinder2_fr-FR.xliff", next.getTargetPath("fr-FR"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/filefinder_fr-FR.xliff", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/filefinder_fr.xliff", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/sub/filefinder2_fr.xliff", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findXliffNoBasename() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(false, new XliffNoBasenameFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(XliffNoBasenameFileType.class, next.fileType.getClass());
        assertEquals(getInputResourcesTestDir("source").toString() + "/en.xliff", next.getPath().toString());
        assertEquals("fr.xliff", next.getTargetPath("fr"));

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr.xliff", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/ko.xliff", itTargets.next().getPath().toString());

        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findInSameDirectory() throws FileFinderException {
        FileFinder fileFinder = initFileFinder(true, new PropertiesFileType(), new XliffFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();
        assertEquals(getInputResourcesTestDir().toString() + "/filefinder.properties", itSources.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/sub/filefinder2.properties", itSources.next().getPath().toString());
        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir().toString() + "/filefinder_fr.properties", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/filefinder_fr_FR.properties", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/sub/filefinder2_fr.properties", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findResw() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(false, new ReswFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/en/Resources.resw", next.getPath().toString());
        assertEquals("fr/Resources.resw", next.getTargetPath("fr"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/en/Resources2.resw", next.getPath().toString());
        assertEquals("fr/Resources2.resw", next.getTargetPath("fr"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/en-GB/Resources.resw", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr/Resources.resw", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr/Resources2.resw", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findReswInSameDirectory() throws IOException, FileFinderException {

        FileFinder fileFinder = initFileFinder(true, new ReswFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/en/Resources.resw", next.getPath().toString());
        assertEquals("fr/Resources.resw", next.getTargetPath("fr"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/en/Resources2.resw", next.getPath().toString());
        assertEquals("fr/Resources2.resw", next.getTargetPath("fr"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir().toString() + "/en-GB/Resources.resw", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/fr/Resources.resw", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/fr/Resources2.resw", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findResx() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(false, new ResxFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/FileFinder1.resx", next.getPath().toString());
        assertEquals("FileFinder1.fr.resx", next.getTargetPath("fr"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/sub/FileFinder2.resx", next.getPath().toString());
        assertEquals("sub/FileFinder2.fr.resx", next.getTargetPath("fr"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/FileFinder1.en-GB.resx", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/FileFinder1.fr.resx", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/sub/FileFinder2.fr.resx", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findResxInSameDirectory() throws IOException, FileFinderException {

        FileFinder fileFinder = initFileFinder(true, new ResxFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/FileFinder1.resx", next.getPath().toString());
        assertEquals("FileFinder1.fr.resx", next.getTargetPath("fr"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/FileFinder2.resx", next.getPath().toString());
        assertEquals("FileFinder2.fr.resx", next.getTargetPath("fr"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir().toString() + "/FileFinder1.en-GB.resx", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/FileFinder1.fr.resx", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/FileFinder2.fr.resx", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findResxWithSourceRegex() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(true, ".*/Localization/.*", new ResxFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/sub1/Localization/Resources.resx", next.getPath().toString());
        assertEquals("sub1/Localization/Resources.fr.resx", next.getTargetPath("fr"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/sub2/Localization/Resources.resx", next.getPath().toString());
        assertEquals("sub2/Localization/Resources.fr.resx", next.getTargetPath("fr"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir().toString() + "/sub1/Localization/Resources.en-GB.resx", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/sub1/Localization/Resources.fr.resx", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/sub2/Localization/Resources.fr.resx", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findMacStrings() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(false, new MacStringsFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/en.lproj/InfoPlist.strings", next.getPath().toString());
        assertEquals("fr.lproj/InfoPlist.strings", next.getTargetPath("fr"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/en.lproj/Localizable.strings", next.getPath().toString());
        assertEquals("fr.lproj/Localizable.strings", next.getTargetPath("fr"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/en-GB.lproj/InfoPlist.strings", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/en-GB.lproj/Localizable.strings", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr.lproj/InfoPlist.strings", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/fr.lproj/Localizable.strings", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findMacStringsInSameDirectory() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(true, new MacStringsFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/en.lproj/InfoPlist.strings", next.getPath().toString());
        assertEquals("fr.lproj/InfoPlist.strings", next.getTargetPath("fr"));

        next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/en.lproj/Localizable.strings", next.getPath().toString());
        assertEquals("fr.lproj/Localizable.strings", next.getTargetPath("fr"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir().toString() + "/en-GB.lproj/InfoPlist.strings", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/en-GB.lproj/Localizable.strings", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/fr.lproj/InfoPlist.strings", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/fr.lproj/Localizable.strings", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findAndroidStrings() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(false, new AndroidStringsFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir("source").toString() + "/res/values/strings.xml", next.getPath().toString());
        assertEquals("res/values-fr/strings.xml", next.getTargetPath("fr"));
        assertEquals("res/values-en-rGB/strings.xml", next.getTargetPath("en-GB"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir("target").toString() + "/res/values-en-rGB/strings.xml", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir("target").toString() + "/res/values-fr/strings.xml", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findAndroidStringsInSameDirectory() throws IOException, FileFinderException {
        FileFinder fileFinder = initFileFinder(true, new AndroidStringsFileType());

        Iterator<FileMatch> itSources = fileFinder.getSources().iterator();

        FileMatch next = itSources.next();
        assertEquals(getInputResourcesTestDir().toString() + "/res/values/strings.xml", next.getPath().toString());
        assertEquals("res/values-fr/strings.xml", next.getTargetPath("fr"));
        assertEquals("res/values-en-rGB/strings.xml", next.getTargetPath("en-GB"));

        assertFalse(itSources.hasNext());

        Iterator<FileMatch> itTargets = fileFinder.getTargets().iterator();
        assertEquals(getInputResourcesTestDir().toString() + "/res/values-en-rGB/strings.xml", itTargets.next().getPath().toString());
        assertEquals(getInputResourcesTestDir().toString() + "/res/values-fr/strings.xml", itTargets.next().getPath().toString());
        assertFalse(itTargets.hasNext());
    }

    @Test
    public void findBadSourceDirectory() throws IOException, FileFinderException {
        FileFinder fileFinder = new FileFinder();
        fileFinder.setSourceDirectory(Paths.get("something_that_doesnt_exist"));

        try {
            fileFinder.find();
            Assert.fail("An exception must thrown when an invalid source directory is provided");
        } catch (FileFinderException ffe) {
            assertEquals(ffe.getMessage(), "Invalid source directory: something_that_doesnt_exist");
        }
    }

    @Test
    public void findBadTargetDirectory() throws IOException, FileFinderException {
        FileFinder fileFinder = new FileFinder();
        fileFinder.setSourceDirectory(getInputResourcesTestDir().toPath());
        fileFinder.setTargetDirectory(Paths.get("something_that_doesnt_exist"));

        try {
            fileFinder.find();
            Assert.fail("An exception must thrown when an invalid target directory is provided");
        } catch (FileFinderException ffe) {
            assertEquals(ffe.getMessage(), "Invalid target directory: something_that_doesnt_exist");
        }
    }

    FileFinder initFileFinder(boolean targetSameAsSourceDirectory, FileType... fileTypes) throws FileFinderException {
        return initFileFinder(targetSameAsSourceDirectory, null, fileTypes);
    }

    FileFinder initFileFinder(boolean targetSameAsSourceDirectory, String sourcePathFilterRegex, FileType... fileTypes) throws FileFinderException {

        FileFinder fileFinder = new FileFinder();
        fileFinder.setSourcePathFilterRegex(sourcePathFilterRegex);

        if (targetSameAsSourceDirectory) {
            fileFinder.setSourceDirectory(getInputResourcesTestDir().toPath());
            fileFinder.setTargetDirectory(fileFinder.getSourceDirectory());
        } else {
            fileFinder.setSourceDirectory(getInputResourcesTestDir("source").toPath());
            fileFinder.setTargetDirectory(getInputResourcesTestDir("target").toPath());
        }

        if (fileTypes.length > 0) {
            fileFinder.setFileTypes(fileTypes);
        }

        fileFinder.find();

        Collections.sort(fileFinder.getSources());
        Collections.sort(fileFinder.getTargets());

        return fileFinder;
    }

}

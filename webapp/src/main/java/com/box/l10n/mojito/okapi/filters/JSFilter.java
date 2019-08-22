package com.box.l10n.mojito.okapi.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.sf.okapi.common.BOMNewlineEncodingDetector;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.exceptions.OkapiUnsupportedEncodingException;
import net.sf.okapi.common.filters.AbstractFilter;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import net.sf.okapi.filters.properties.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author jyi
 */
@Configurable
public class JSFilter extends AbstractFilter {

    public static final String FILTER_CONFIG_ID = "okf_regex@mojito";
    private static final int STATE_START = 0;
    private static final int STATE_KEY = 1;
    private static final int STATE_SEP = 2;
    private static final int STATE_VALUE = 3;

    private Parameters params;
    private BufferedReader reader;
    private RawDocument input;
    private String docName;
    private String encoding;
    private boolean hasUTF8BOM;
    private String lineBreak;

    EncoderManager encoderManager;
    private IFilterWriter filterWriter;
    private LinkedList<Event> queue;
    private int parseState = 0;

    private GenericSkeleton skel;
    private String textLine;

    @Autowired
    UnescapeUtils unescapeUtils;

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    @Override
    public String getMimeType() {
        return MimeTypeMapper.JAVASCRIPT_MIME_TYPE;
    }

    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();
        list.add(new FilterConfiguration(getName() + "-js",
                getMimeType(),
                getClass().getName(),
                "Text (JS Strings)",
                "Configuration for JS .js/.ts files.",
                null));
        return list;
    }

    public JSFilter() {
        params = new Parameters();;
    }

    @Override
    protected boolean isUtf8Encoding() {
        return StandardCharsets.UTF_8.name().equals(encoding);
    }

    @Override
    protected boolean isUtf8Bom() {
        return hasUTF8BOM;
    }

    @Override
    public IParameters getParameters() {
        return params;
    }

    @Override
    public void setParameters(IParameters params) {
        this.params = (Parameters) params;
    }

    @Override
    public EncoderManager getEncoderManager() {
        if (encoderManager == null) {
            encoderManager = new EncoderManager();
            encoderManager.setMapping(getMimeType(), "com.box.l10n.mojito.okapi.filters.JSEncoder");
        }
        return encoderManager;
    }

    @Override
    public ISkeletonWriter createSkeletonWriter() {
        return new JSSkeletonWriter();
    }

    @Override
    public IFilterWriter createFilterWriter() {
        if (filterWriter != null) {
            return filterWriter;
        }
        return new GenericFilterWriter(createSkeletonWriter(), getEncoderManager());
    }

    @Override
    public void close() {
        if (input != null) {
            input.close();
        }
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
            parseState = 0;
        } catch (IOException e) {
            throw new OkapiIOException(e);
        }
    }

    @Override
    public void open(RawDocument input) {

        // initialize
        super.open(input, true);
        this.input = input;
        parseState = 1;
        queue = new LinkedList<>();

        // detect encoding
        BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(
                input.getStream(), input.getEncoding());
        detector.detectAndRemoveBom();
        input.setEncoding(detector.getEncoding());
        encoding = input.getEncoding();

        // open the input reader from the provided reader
        try {
            reader = new BufferedReader(new InputStreamReader(
                    detector.getInputStream(), encoding));
        } catch (UnsupportedEncodingException e) {
            throw new OkapiUnsupportedEncodingException(String.format(
                    "The encoding '%s' is not supported.", encoding), e);
        }
        hasUTF8BOM = detector.hasUtf8Bom();
        lineBreak = detector.getNewlineType().toString();
        if (input.getInputURI() != null) {
            docName = input.getInputURI().getPath();
        }

        // start event
        StartDocument startDoc = new StartDocument("sd");
        startDoc.setName(docName);
        startDoc.setEncoding(encoding, hasUTF8BOM);
        startDoc.setLocale(input.getSourceLocale());
        startDoc.setFilterParameters(params);
        startDoc.setFilterWriter(createFilterWriter());
        startDoc.setLineBreak(lineBreak);
        startDoc.setType(getMimeType());
        startDoc.setMimeType(getMimeType());
        queue.add(new Event(EventType.START_DOCUMENT, startDoc));
    }

    @Override
    public boolean hasNext() {
        return (parseState > 0);
    }

    @Override
    public Event next() {

        // cancel if requested
        if (super.isCanceled()) {
            parseState = 0;
            queue.clear();
            queue.add(new Event(EventType.CANCELED));
        }

        // return event in the queue
        if (queue.size() > 0) {
            return queue.poll();
        }

        // parse the input
        ITextUnit textUnit = readTextUnit();
        if (textUnit != null) {
            return new Event(EventType.TEXT_UNIT, textUnit);
        }

        // end event
        Ending ending = new Ending("ed");
        ending.setSkeleton(skel);
        parseState = 0;
        return new Event(EventType.END_DOCUMENT, ending);
    }

    private TextUnit readTextUnit() {
        try {
            TextUnit textUnit;
            skel = new GenericSkeleton();
            String comment = null;
            StringBuilder data = new StringBuilder();
            boolean emptyLine = false;

            while (true) {
                textLine = reader.readLine();

                if (textLine == null) {
                    if (emptyLine) {
                        skel.append(data.toString());
                    } else {
                        skel.append(data.substring(0, data.lastIndexOf(lineBreak)));
                    }
                    return null;
                }

                String tmp = Util.trimStart(textLine, "\t\r\n \f");

                // empty line
                if (tmp.length() == 0) {
                    data.append(textLine).append(lineBreak);
                    emptyLine = true;
                    continue;
                } else {
                    emptyLine = false;
                }

                // comments
                boolean isComment = tmp.startsWith("//");
                if (isComment) {
                    comment = tmp.substring(2).trim();
                    data.append(textLine).append(lineBreak);
                    continue;
                }

                // key/value
                boolean isKeyValue = tmp.startsWith("\"");
                if (isKeyValue) {
                    skel.append(data.toString());
                    textUnit = processKeyValueLine();
                    if (textUnit != null && comment != null) {
                        textUnit.setProperty(new Property(Property.NOTE, comment, true));
                    }
                    break;
                } else {
                    data.append(textLine).append(lineBreak);
                    continue;
                }

            }

            return textUnit;

        } catch (IOException e) {
            throw new OkapiIOException(e);
        }
    }

    private TextUnit processKeyValueLine() throws IOException {
        StringBuilder buffer = new StringBuilder();
        String key = null;
        String value = null;
        int state = STATE_START;
        int pos = 0;
        boolean backquoted = false;
        boolean done = false;

        while (!done) {
            // handle empty line
            if (!textLine.isEmpty()) {
                char c = textLine.charAt(pos);
                switch (state) {
                    case STATE_START:
                        buffer.append(c);
                        if (c == '"') {
                            skel.append(buffer.toString());
                            buffer = new StringBuilder();
                            state++;
                        }
                        break;

                    case STATE_KEY:
                        if (c == '"' && textLine.charAt(pos - 1) != '\\') {
                            skel.append(buffer.toString());
                            key = buffer.toString();
                            buffer = new StringBuilder();
                            state++;
                        }
                        buffer.append(c);
                        break;

                    case STATE_SEP:
                        buffer.append(c);
                        if (c == '"' || c == '`') {
                            skel.append(buffer.toString());
                            buffer = new StringBuilder();
                            state++;
                            if (c == '`') {
                                backquoted = true;
                            }
                        }
                        break;

                    case STATE_VALUE:
                        if ((backquoted && c == '`' && textLine.charAt(pos - 1) != '\\') || (!backquoted && c == '"' && textLine.charAt(pos - 1) != '\\')) {
                            value = unescapeUtils.unescape(buffer.toString());
                            if (backquoted) {
                                value = unescapeUtils.replaceEscapedBackquotes(value);
                            }
                            buffer = new StringBuilder();
                            state++;
                        }
                        buffer.append(c);
                        break;

                    default:
                        buffer.append(c);
                }
                pos++;
            }

            // end of line reached 
            if (pos == textLine.length()) {
                //check for multi-line value
                if (state == STATE_VALUE && backquoted) {
                    buffer.append(lineBreak);
                    textLine = reader.readLine();
                    pos = 0;
                } else {
                    done = true;
                }
            }
        }

        TextUnit textUnit = null;
        if (key != null) {
            textUnit = new TextUnit(key, value);
            textUnit.setName(key);
            textUnit.setPreserveWhitespaces(true);
            skel.addContentPlaceholder(textUnit, null);
            skel.append(buffer.toString());
            skel.append(lineBreak);
            textUnit.setSkeleton(skel);
            if (backquoted) {
                textUnit.setProperty(new Property("template", "true", true));
            }
        }

        return textUnit;
    }

}

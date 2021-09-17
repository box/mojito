package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.ArrayDeque;

/**
 * Checks the validity of the message format when double braces are used as placeholders in the target content.
 *
 * Verifies correct number of brackets in string then replaces double braces with a single brace and
 * runs the {@link MessageFormatIntegrityChecker} checks.
 */
public class MessageFormatDoubleBracesIntegrityChecker extends MessageFormatIntegrityChecker {

    private static final String LEFT_DOUBLE_BRACES_REGEX = "\\{\\{.*?";
    private static final String RIGHT_DOUBLE_BRACES_REGEX = "\\}\\}.*?";

    @Override
    public void check(String source, String content) throws MessageFormatIntegrityCheckerException {
        verifyEqualNumberOfBraces(source);
        verifyEqualNumberOfBraces(content);
        super.check(replaceDoubleBracesWithSingle(source), replaceDoubleBracesWithSingle(content));
    }

    private void verifyEqualNumberOfBraces(String str) throws MessageFormatIntegrityCheckerException {
        ArrayDeque<Character> stack = new ArrayDeque<>();
        for (Character c : str.toCharArray()) {
            if (c.equals('{')) {
                stack.push(c);
                continue;
            } else if (c.equals('}')) {
                if (stack.isEmpty()) {
                    throw new MessageFormatIntegrityCheckerException("Invalid pattern, closing bracket found with no associated opening bracket.");
                }
                stack.pop();
            }
        }
        if (!stack.isEmpty()) {
            throw new MessageFormatIntegrityCheckerException("Invalid pattern, there is more left than right braces in string.");
        }
    }

    private String replaceDoubleBracesWithSingle(String str) {
        return str.replaceAll(LEFT_DOUBLE_BRACES_REGEX, "{")
                .replaceAll(RIGHT_DOUBLE_BRACES_REGEX, "}");
    }
}

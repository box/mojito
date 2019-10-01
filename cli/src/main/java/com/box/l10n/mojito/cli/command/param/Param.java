package com.box.l10n.mojito.cli.command.param;

/**
 * @author wyau
 */
public class Param {
    public static final String BRANCH_LONG = "--branch";
    public static final String BRANCH_SHORT = "-b";
    public static final String BRANCH_DESCRIPTION = "Name of the branch";

    public static final String BRANCH_NAME_LONG = "--name";
    public static final String BRANCH_NAME_SHORT = "-n";
    public static final String BRANCH_NAME_DESCRIPTION = "Name of the branch (if none provided it will attempt to remove the \"null\" branch)";

    public static final String BRANCH_NAME_REGEX_LONG = "--name-regex";
    public static final String BRANCH_NAME_REGEX_SHORT = "-nr";
    public static final String BRANCH_NAME_REGEX_DESCRIPTION = "Filter the branches by name using a regex. To get all branch not matching \"master\" " +
            "the regex is \"(?!^master$).*\"";

    public static final String BRANCH_TRANSLATED_LONG = "--translated";
    public static final String BRANCH_TRANSLATED_SHORT = "-t";
    public static final String BRANCH_TRANSLATED_DESCRIPTION = "Filter translated branches";

    public static final String BRANCH_NULL_BRANCH_LONG = "--null-branch";
    public static final String BRANCH_NULL_BRANCH_SHORT = "-nb";
    public static final String BRANCH_NULL_BRANCH_DESCRIPTION = "Include \"null\" branch when filtering branches by name using a regex (-nr option)";

    public static final String BRANCH_CREATED_BEFORE_LAST_WEEK_LONG = "--created-before-last-week";
    public static final String BRANCH_CREATED_BEFORE_LAST_WEEK_SHORT = "-cblw";
    public static final String BRANCH_CREATED_BEFORE_LAST_WEEK_DESCRIPTION = "Filter branches that were created before last week";

    public static final String REPOSITORY_LONG = "--repository";
    public static final String REPOSITORY_SHORT = "-r";
    public static final String REPOSITORY_DESCRIPTION = "Name of the repository";

    public static final String SOURCE_REPOSITORY_LONG = "--source-repository";
    public static final String SOURCE_REPOSITORY_SHORT = "-s";
    public static final String SOURCE_REPOSITORY_DESCRIPTION = "Name of the source repository";

    public static final String TARGET_REPOSITORY_LONG = "--target-repository";
    public static final String TARGET_REPOSITORY_SHORT = "-t";
    public static final String TARGET_REPOSITORY_DESCRIPTION = "Name of the target repository";

    public static final String REPOSITORY_NAME_LONG = "--name";
    public static final String REPOSITORY_NAME_SHORT = "-n";
    public static final String REPOSITORY_NAME_DESCRIPTION = "Name of the repository to create or update";

    public static final String REPOSITORY_NEW_NAME_LONG = "--new-name";
    public static final String REPOSITORY_NEW_NAME_SHORT = "-nn";
    public static final String REPOSITORY_NEW_NAME_DESCRIPTION = "New name for the repository";

    public static final String REPOSITORY_DESCRIPTION_LONG = "--description";
    public static final String REPOSITORY_DESCRIPTION_SHORT = "-d";
    public static final String REPOSITORY_DESCRIPTION_DESCRIPTION = "Description of the repository to create or update";

    public static final String REPOSITORY_LOCALES_LONG = "--locales";
    public static final String REPOSITORY_LOCALES_SHORT = "-l";
    public static final String REPOSITORY_LOCALES_DESCRIPTION = "List of locales to add.  Separated by spaces.  Arrow separated to specify parent language. Bracket enclosed locale will set that locale to be partially translated.  e.g.(\"fr-FR\" \"(fr-CA)->fr-FR\" \"en-GB\" \"(en-CA)->en-GB\" \"en-AU\")";

    public static final String REPOSITORY_LOCALES_MAPPING_LONG = "--locale-mapping";
    public static final String REPOSITORY_LOCALES_MAPPING_SHORT = "-lm";

    public static final String REPOSITORY_SOURCE_LOCALE_LONG = "--source-locale";
    public static final String REPOSITORY_SOURCE_LOCALE_SHORT = "-sl";
    public static final String REPOSITORY_SOURCE_LOCALE_DESCRIPTION = "Locale of the source strings";

    public static final String SOURCE_DIRECTORY_LONG = "--source-directory";
    public static final String SOURCE_DIRECTORY_SHORT = "-s";
    public static final String SOURCE_DIRECTORY_DESCRIPTION = "Directory that contains source assets to be localized";

    public static final String TARGET_DIRECTORY_LONG = "--target-directory";
    public static final String TARGET_DIRECTORY_SHORT = "-t";
    public static final String TARGET_DIRECTORY_DESCRIPTION = "Target directory that will contain localized assets";

    public static final String DROP_IMPORT_STATUS = "--import-status";
    public static final String DROP_IMPORT_STATUS_DESCRIPTION = "Override the status of translations being imported";

    public static final String SOURCE_LOCALE_LONG = "--source-locale";
    public static final String SOURCE_LOCALE_SHORT = "-sl";
    public static final String SOURCE_LOCALE_DESCRIPTION = "Override the default source locale of the filter (if -ft option is used)";

    public static final String FILE_TYPE_LONG = "--file-type";
    public static final String FILE_TYPE_SHORT = "-ft";
    public static final String FILE_TYPE_DESCRIPTION = "File type (if none provided it will scan for default formats: XLIFF, XCODE_XLIFF, MAC_STRING, MAC_STRINGSDICT, ANDROID_STRINGS, PROPERTIES, PROPERTIES_NOBASENAME, PROPERTIES_JAVA, RESW, RESX, PO, XTB)";

    public static final String FILTER_OPTIONS_LONG = "--filter-options";
    public static final String FILTER_OPTIONS_SHORT = "-fo";
    public static final String FILTER_OPTIONS_DESCRIPTION = "Filter options. Format: option1=value1&option2=value2";

    public static final String SOURCE_REGEX_LONG = "--source-regex";
    public static final String SOURCE_REGEX_SHORT = "-sr";
    public static final String SOURCE_REGEX_DESCRIPTION = "Regular expression to match the path of source assets to localize";

    public static final String USERNAME_LONG = "--username";
    public static final String USERNAME_SHORT = "-un";
    public static final String USERNAME_DESCRIPTION = "Username of user";

    public static final String PASSWORD_LONG = "--password";
    public static final String PASSWORD_SHORT = "-pw";
    public static final String PASSWORD_DESCRIPTION = "Prompt for user password";

    public static final String SURNAME_LONG = "--surname";
    public static final String SURNAME_SHORT = "-sn";
    public static final String SURNAME_DESCRIPTION = "Surname of user";

    public static final String GIVEN_NAME_LONG = "--given-name";
    public static final String GIVEN_NAME_SHORT = "-gn";
    public static final String GIVEN_NAME_DESCRIPTION = "Given name of user";

    public static final String COMMON_NAME_LONG = "--common-name";
    public static final String COMMON_NAME_SHORT = "-cn";
    public static final String COMMON_NAME_DESCRIPTION = "Common name of user";

    public static final String ROLE_LONG = "--role";
    public static final String ROLE_SHORT = "-r";
    public static final String ROLE_DESCRIPTION = "Available user roles: PM, TRANSLATOR, ADMIN, USER";

    public static final String EXPORT_LOCALES_LONG = "--locales";
    public static final String EXPORT_LOCALES_SHORT = "-l";
    public static final String EXPORT_LOCALES_DESCRIPTION = "List of locales to be exported, format: fr-FR,ja-JP";

    public static final String CHECK_SLA_LONG = "--check-sla";
    public static final String CHECK_SLA_SHORT = "-cs";
    public static final String CHECK_SLA_DESCRIPTION = "whether check sla or not for repository";

}

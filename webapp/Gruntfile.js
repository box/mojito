module.exports = function (grunt)
{
    grunt.initConfig({
        eslint: {
            options: {
                format: require('eslint-tap'),
                outputFile: "target/eslint-results.txt",
                globals: [
                    // mention all globals here so that eslint can ignore them
                ],
                // turn this ON to show only errors
                quiet: false,
                "ecmaFeatures": {
                    "jsx": true
                }
            },
            // TODO: add test directories to linter when test framework has been set up.
            target: ['src/main/resources/public/**/*.js']
        }
    });
    grunt.loadNpmTasks("grunt-eslint");
    grunt.registerTask("default", ["eslint"]);
};

var sassFiles = {
    "target/classes/public/css/mojito.css": "src/main/resources/sass/mojito.scss"
};

module.exports = function (grunt)
{
    grunt.initConfig({
        sass: {
            options: {
                sourceMap: true,
                outputStyle: 'compressed'
            },
            dist: {
                files: sassFiles
            }
        },
        watch: {
            sass: {
                files: "src/main/resources/sass/*.scss",
                tasks: ["sass"]
            }
        },
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
    grunt.registerTask("default", ["eslint", "sass"]);

    grunt.loadNpmTasks("grunt-sass");
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.registerTask("buildcss", ["sass"]);
};

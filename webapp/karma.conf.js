var webpackConfig = require("./webpack.config.js");

// NOTE: The entry point from the referenced Webpack configuration has to be removed or tests will fail in weird
// and inscrutable ways. Easy enough, just define an empty entry object (null won’t work).
webpackConfig.entry = {};
webpackConfig.devtool = "inline-source-map";

module.exports = function (config)
{
    config.set({

        basePath: "",

        frameworks: ["mocha", "chai"],

        files: [
            {pattern: "src/test/**/*.js", included: true}
        ],

        exclude: [],

        preprocessors: {
            "src/test/**/*.js": ["webpack", "sourcemap"]
        },

        reporters: ["progress"],

        colors: true,

        logLevel: config.LOG_INFO,

        autoWatch: true,

        browsers: ["Chrome"],

        singleRun: false,

        webpack: webpackConfig
    });
};

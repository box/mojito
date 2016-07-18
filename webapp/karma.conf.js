module.exports = function (config)
{
    config.set({

        basePath: '',

        frameworks: ['browserify', 'mocha', 'chai'],

        files: [
            {pattern: 'src/test/**/*.js', included: true}
        ],

        exclude: [],

        preprocessors: {
            "src/test/**/*.js": ['browserify']
        },

        browserify: {
            paths: ["src/main/resources/public/js"],
            "transform": [
              [
                "babelify",
                {
                  "presets": [
                    "react",
                    "es2015"
                  ],
                  "sourceMapsAbsolute": true
                }
              ],
              "envify"
            ]
        },
        
        reporters: ['progress'],

        colors: true,

        logLevel: config.LOG_INFO,

        autoWatch: true,

        browsers: ['Chrome'],

        singleRun: false
    })
}

var path = require("path"),
    webpack = require("webpack"),
    minimize = process.argv.indexOf("--minimize") !== -1,
    inlineSourceMap = process.argv.indexOf("--inline-source-map") !== -1;

var config = {
    entry: "./src/main/resources/public/js/app.js",
    output: {path: "./target/classes/public/js", filename: "bundle.min.js"},
    module: {
        loaders: [
            {
                test: /\.jsx?$/,
                loader: "babel-loader",
                exclude: /node_modules/,
                query: {
                    presets: ["es2015", "react"]
                }
            },
            {test: /\.json$/, loader: "json"}
        ]
    },
    plugins: []
};

if (minimize) {
    
    config.plugins.push(
    new webpack.DefinePlugin({
      'process.env': {
        'NODE_ENV': JSON.stringify('production')
      }
    }));
    
    config.plugins.push(new webpack.optimize.DedupePlugin()); 
    config.plugins.push(new webpack.optimize.UglifyJsPlugin());
    config.plugins.push(new webpack.optimize.AggressiveMergingPlugin());
}

if (inlineSourceMap) {
    config.devtool = "inline-source-map";
}

module.exports = config;

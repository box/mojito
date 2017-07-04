var path = require('path');
var webpack = require("webpack");

module.exports = function (env) {

    env = env || {};

    var config = {
        entry: {
            'app' : path.resolve(__dirname, './src/main/resources/public/js/app.js'),
        },
        output: {
            path: path.resolve(__dirname, './target/classes/public'),
            publicPath: '/',
            filename: 'js/[name]-[hash].js',
            chunkFilename: 'js/[name]-[chunkhash]'
        },
        module: {
            rules: [
                {
                    test: /\.jsx?$/,
                    exclude: /node_modules/,
                    use: {
                        loader: 'babel-loader',
                        options: {
                            presets: ['es2015', 'react', 'stage-0']
                        }
                    }
                },
                {
                    test: /\.(gif|png|jpe?g|svg)$/i,
                    loaders: [
                        {
                            loader: 'file-loader',
                            options: {
                               name: 'img/[name]-[hash].[ext]',
                            }
                        },
                        {
                            loader: 'image-webpack-loader',
                            query: {
                                name: 'img/[name]-[hash].[ext]',
                                progressive: true,
                                optimizationLevel: 7,
                                interlaced: false,
                                pngquant: {
                                    quality: '65-90',
                                    speed: 4
                                }
                            }
                        }
                    ]
                },
                {
                    test: /\.properties$/,
                    exclude: /node_modules/,
                    loaders: [
                        {
                            loader: 'java-properties-flat-loader'
                        }
                    ]
                    
                }   
            ]
        },
        plugins: []
    };


    var HtmlWebpackPlugin = require('html-webpack-plugin');
    config.plugins.push(new HtmlWebpackPlugin({
        filename: path.resolve(__dirname, './target/classes/templates/index.html'),
        template: 'src/main/resources/templates/index.html',
        inject: false
    }));

    if (env.minimize) {
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

    if (env.inlineSourceMap) {
        config.devtool = "inline-source-map";
    }

    return config;
};
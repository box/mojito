var path = require('path');
var webpack = require("webpack");

module.exports = function (env) {
    devtool: 'source-map'

    env = env || {};

    var config = {
        entry: {
            'app': path.resolve(__dirname, './src/main/resources/public/js/app.js'),
            'css': path.resolve(__dirname, './src/main/resources/sass/mojito.scss')
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
                                query: {
                                    mozjpeg: {
                                        progressive: true
                                    },
                                    gifsicle: {
                                        interlaced: false
                                    },
                                    optipng: {
                                        optimizationLevel: 4
                                    },
                                    pngquant: {
                                        quality: '75-90',
                                        speed: 3
                                    },
                                },
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

                },
                {
                    // __webpack_public_path__ is not supported by ExtractTextPlugin
                    // so we inline all the fonts here. If not inlined, references 
                    // to the font are invalid if mojito is deployed with a 
                    // specific deploy path. 
                    // hardcoded for deploy path for test -->
                    //    name: '{deployPath}/fonts/[name]-[hash].[ext]'
                    
                    test: /\.(eot|ttf|woff|woff2)$/,
                    loader: 'url-loader',
                    options: {
                        name: 'fonts/[name]-[hash].[ext]'
                    }
                },

                {
                    test: /\.scss$/,
                    use: [{
                            loader: "style-loader" // creates style nodes from JS strings
                        }, {
                            loader: "css-loader" // translates CSS into CommonJS
                        }, {
                            loader: "sass-loader",
                            options: {
                                precision: 8,
                                sourceMap: true
                            }
                        }]
                },
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

    if (env.production) {
        config.plugins.push(
                new webpack.DefinePlugin({
                    'process.env': {
                        'NODE_ENV': JSON.stringify('production')
                    }
                }));

        config.plugins.push(new webpack.optimize.UglifyJsPlugin());
    }

    if (env.inlineSourceMap) {
        config.devtool = "inline-source-map";
    }

    return config;
};
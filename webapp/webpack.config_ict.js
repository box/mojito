const path = require('path');
const webpack = require("webpack");
const TerserPlugin = require("terser-webpack-plugin");


module.exports = function (env) {
    env = env || {};
    
    const isProdEnv = Boolean(env.production)
    const config = {
        entry: {
            'ict-popup': path.resolve(__dirname, './src/main/resources/public/js/ict/chrome-ict-popup.js'),
            'ict': path.resolve(__dirname, './src/main/resources/public/js/ict/chrome-ict.js')
        },
        output: {
            path: path.resolve(__dirname, '../chromeextension'),
            publicPath: '',
            filename: '[name]-bundle.js',
        },
        mode: isProdEnv ? 'production' : 'development',
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
                    use: [
                        {
                            loader: 'file-loader',
                            options: {
                                name: 'img/[name]-[contenthash].[ext]',
                            }
                        },
                        {
                            loader: 'image-webpack-loader',
                            options: {
                                name: 'img/[name]-[contenthash].[ext]',
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
                    use: [
                        {
                            loader: path.resolve('src/main/webpackloader/properties.js')
                        }
                    ]
                },
                {
                    // __webpack_public_path__ is not supported by ExtractTextPlugin
                    // so we inline all the fonts here. If not inlined, references 
                    // to the font are invalid if mojito is deployed with a 
                    // specific deploy path. 
                    // hardcoded for deploy path for test -->
                    //    name: '{deployPath}/fonts/[name]-[contenthash].[ext]'
                    
                    test: /\.(eot|ttf|woff|woff2)$/,
                    loader: 'url-loader',
                    options: {
                        name: 'fonts/[name]-[contenthash].[ext]'
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
                                sassOptions: {
                                    precision: 8
                                },
                                sourceMap: true
                            }
                        }]
                },
            ]
        },
        optimization: {
            minimizer: [new TerserPlugin()],
            minimize: isProdEnv,
        },
        performance: {
            maxEntrypointSize: 900_000, // 90KB
            maxAssetSize: 900_000 // 90KB
        },
        plugins: []
    };
    
    if (env.production) {
        config.plugins.push(
                new webpack.DefinePlugin({
                    'process.env': {
                        'NODE_ENV': JSON.stringify('production')
                    }
                }));
    }

    if (env.inlineSourceMap) {
        config.devtool = "inline-source-map";
    }

    return config;
};

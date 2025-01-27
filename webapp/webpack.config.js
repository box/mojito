const path = require('path');
const webpack = require("webpack");
const TerserPlugin = require("terser-webpack-plugin");

module.exports = (env) => {
    env = env || {};

    const isProdEnv = Boolean(env.production);
    const inlineSourceMap = Boolean(env.inlineSourceMap);

    const config = {
        entry: {
            'app': path.resolve(__dirname, './src/main/resources/public/js/app.jsx'),
            'css': path.resolve(__dirname, './src/main/resources/sass/mojito.scss')
        },
        output: {
            path: path.resolve(__dirname, './target/classes/public'),
            publicPath: '/',
            filename: 'js/[name]-[contenthash].js',
            chunkFilename: 'js/[name]-[chunkhash].js'
        },
        mode: isProdEnv ? 'production' : 'development',
        devtool: inlineSourceMap ? "inline-source-map" : false,
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
                    ],
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
            hints: 'warning',
            maxEntrypointSize: 1_800_000, // 1.8MB
            maxAssetSize: 1_800_000 // 1.8MB
        },
        plugins: [],
        resolve: {
            extensions: ['.js', '.jsx']
        }
    };

    const HtmlWebpackPlugin = require('html-webpack-plugin');
    config.plugins.push(new HtmlWebpackPlugin({
        filename: path.resolve(__dirname, './target/classes/templates/index.html'),
        template: 'src/main/resources/templates/index.html',
        favicon: 'src/main/resources/favicon.ico',
        inject: false
    }));

    if (isProdEnv) {
        config.plugins.push(
                new webpack.DefinePlugin({
                    'process.env': {
                        'NODE_ENV': JSON.stringify('production')
                    }
                }));
    }

    return config;
};
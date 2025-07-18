import path from 'path';
import webpack from 'webpack';
import TerserPlugin from 'terser-webpack-plugin';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

// Polyfill __dirname for ESM
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default (env) => {
    env = env || {};
    
    const isProdEnv = Boolean(env.production)
    const config = {
        entry: {
            'ict-popup': path.resolve(__dirname, './src/main/resources/public/js/ict/chrome-ict-popup.jsx'),
            'ict': path.resolve(__dirname, './src/main/resources/public/js/ict/chrome-ict.jsx')
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
                            presets: [
                                '@babel/preset-env',
                                '@babel/preset-react'
                            ],
                            plugins: [
                                '@babel/plugin-proposal-function-bind',
                                '@babel/plugin-proposal-export-default-from',
                                '@babel/plugin-proposal-logical-assignment-operators',
                                ['@babel/plugin-proposal-optional-chaining', { loose: false }],
                                ['@babel/plugin-proposal-pipeline-operator', { proposal: 'minimal' }],
                                ['@babel/plugin-proposal-nullish-coalescing-operator', { loose: false }],
                                '@babel/plugin-proposal-do-expressions',
                                ['@babel/plugin-proposal-decorators', { legacy: true }],
                                '@babel/plugin-proposal-function-sent',
                                '@babel/plugin-proposal-export-namespace-from',
                                '@babel/plugin-proposal-numeric-separator',
                                '@babel/plugin-proposal-throw-expressions',
                                '@babel/plugin-syntax-dynamic-import',
                                '@babel/plugin-syntax-import-meta',
                                ['@babel/plugin-proposal-class-properties', { loose: false }],
                                '@babel/plugin-proposal-json-strings'
                            ]
                        }
                    }
                },
                {
                    test: /\.(svg|png|gif|jpe?g)$/i,
                    type: 'asset',
                    generator: {
                        filename: 'img/[name]-[contenthash][ext]'
                    },
                    use: [
                        {
                            loader: 'image-webpack-loader',
                            options: {
                                mozjpeg: { progressive: true },
                                gifsicle: { interlaced: false },
                                optipng: { optimizationLevel: 4 },
                                pngquant: { quality: [0.75, 0.9], speed: 3 },
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
                    test: /\.(eot|ttf|woff|woff2)$/,
                    type: 'asset',
                    generator: {
                        filename: 'fonts/[name]-[contenthash].[ext]'
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
        plugins: [],
        resolve: {
            extensions: ['.js', '.jsx']
        }
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

const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

function makeCommonRules(argv) {
    const isDev = argv.mode !== 'production';
    return [{
        test: /\.jsx?$/, exclude: /node_modules/, use: {
            loader: 'babel-loader', options: {
                presets: [['@babel/preset-env', {modules: false}], '@babel/preset-react'], cacheDirectory: true
            }
        }
    }, {
        test: /\.(gif|png|jpe?g|svg)$/i,
        type: 'asset',
        parser: {dataUrlCondition: {maxSize: 8 * 1024}},
        generator: {filename: 'img/[name]-[contenthash][ext]'}
    }, {
        test: /\.properties$/,
        exclude: /node_modules/,
        use: [{loader: path.resolve('src/main/webpackloader/properties.js')}]
    }, {
        test: /\.(eot|ttf|woff|woff2)$/i, type: 'asset/inline'
    }, {
        test: /\.scss$/, use: [{loader: 'style-loader'}, {loader: 'css-loader'}, {
            loader: 'sass-loader', options: {sourceMap: isDev}
        }]
    }];
}

function makeBaseConfig(env, argv) {
    const isProd = env.production || argv.mode === 'production';
    return {
        name: 'base', mode: isProd ? 'production' : 'development', entry: {
            app: path.resolve(__dirname, './src/main/resources/public/js/app.js'),
            css: path.resolve(__dirname, './src/main/resources/sass/mojito.scss')
        }, output: {
            path: path.resolve(__dirname, './target/classes/public'),
            publicPath: '/',
            filename: 'js/[name]-[contenthash].js',
            chunkFilename: 'js/[name]-[contenthash].js'
        }, module: {rules: makeCommonRules(argv)}, plugins: [new HtmlWebpackPlugin({
            filename: path.resolve(__dirname, './target/classes/templates/index.html'),
            template: 'src/main/resources/templates/index.html',
            inject: false
        })], devtool: env.inlineSourceMap ? 'inline-source-map' : false
    };
}

function makeIctConfig(env, argv) {
    const isProd = env.production || argv.mode === 'production';
    return {
        name: 'ict',
        mode: isProd ? 'production' : 'development',
        entry: {
            'ict-popup': path.resolve(__dirname, './src/main/resources/public/js/ict/chrome-ict-popup.js'),
            'ict': path.resolve(__dirname, './src/main/resources/public/js/ict/chrome-ict.js')
        },
        output: {
            path: path.resolve(__dirname, '../chromeextension'), filename: '[name]-bundle.js', publicPath: 'auto'
        },
        module: {rules: makeCommonRules(argv)},
        plugins: [],
        devtool: env.inlineSourceMap ? 'inline-source-map' : false
    };
}

module.exports = (env = {}, argv = {}) => {
    const target = env.target; // "base" | "ict" | undefined
    if (target === 'base') return makeBaseConfig(env, argv);
    if (target === 'ict') return makeIctConfig(env, argv);
    // no target => build both
    return [makeBaseConfig(env, argv), makeIctConfig(env, argv)];
};

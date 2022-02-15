const path = require('path');

module.exports = {
    mode: 'development',
    entry: './src/frontend/index.js',
    // Run with: npx webpack serve
    devServer: {
        allowedHosts: 'auto',
        compress: true,
        port: 9000,
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Headers': '*',
            'Access-Control-Allow-Methods': '*',
        },
    },
    output: {
        filename: 'index.js',
        path: path.resolve(__dirname, 'build', 'resources', 'main', 'assets', 'js'),
    },
};